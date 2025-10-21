package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.AvailabilityExceptionRequest
import iterative.harmony.backend.controller.dto.AvailabilityExceptionResponse
import iterative.harmony.backend.controller.dto.AvailabilityResponse
import iterative.harmony.backend.controller.dto.WeeklyAvailabilityResponse
import iterative.harmony.backend.controller.dto.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.model.AvailabilityException
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.repository.AvailabilityExceptionRepository
import iterative.harmony.backend.repository.WeeklyAvailabilitySlotRepository
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class AvailabilityService {
    @Autowired
    private lateinit var weeklyAvailabilitySlotRepository: WeeklyAvailabilitySlotRepository
    @Autowired private lateinit var availabilityExceptionRepository: AvailabilityExceptionRepository

    private val log = getLogger()
    private val ONE_DAY = Duration.ofDays(1)
    private val ONE_HOUR = Duration.ofHours(1)
    private val THREE_MONTHS = Duration.ofDays(90)
    private val DAYS_OF_WEEK = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val END_TIME_BEFORE_START = "endTime is before startTime"
    private val SAME_START_AND_END_TIME = "startTime and endTime are the same"
    private val INVALID_DAY_OF_WEEK = "dayOfWeek is not one of $DAYS_OF_WEEK"
    private val INVALID_TIME_ZONE_ID = "Invalid timeZoneId"
    private val LESS_THAN_ONE_HOUR = "availability changes must be >=60 min"
    private val MORE_THAN_24_HOURS = "availability changes must be <= 24 hours"
    private val MORE_THAN_90_DAYS_AWAY = "availability exceptions must be within 90 days"
    private val EXCEPTION_ALREADY_EXISTS =
        "An availability exception with this start time already exists"

    fun getCurrentUserAvailability(userId: String): AvailabilityResponse {
        val uuid = UUID.fromString(userId)
        val weeklyAvailabilitySlots = weeklyAvailabilitySlotRepository.findAllByUserId(uuid)
        val availabilityExceptions = availabilityExceptionRepository.findAllByUserId(uuid)
        return AvailabilityResponse(weeklyAvailabilitySlots, availabilityExceptions)
    }

    // TODO: add method for deleting weeklyAvailability

    fun overwriteWeeklyAvailability(
        userId: String,
        slots: List<WeeklyAvailabilitySlotRequest>,
    ): WeeklyAvailabilityResponse {
        val slotErrors = verifyWeeklyAvailabilitySlots(slots)
        if (slotErrors.isNotEmpty()) return WeeklyAvailabilityResponse(errors = slotErrors)

        val mergedSlots = mergeOverlappingSlots(slots)

        val uuid = UUID.fromString(userId)
        log.info("Deleting old weekly availability slots")
        weeklyAvailabilitySlotRepository.deleteAllByUserId(uuid)

        log.info("Saving weekly availability slots")
        val slotsToSave =
            mergedSlots.map {
                WeeklyAvailabilitySlot(
                    userId = uuid,
                    dayOfWeek = it.dayOfWeek,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    timeZoneId = it.timeZoneId,
                )
            }
        val slotsFromDb = weeklyAvailabilitySlotRepository.saveAll(slotsToSave)
        return WeeklyAvailabilityResponse(slotsFromDb)
    }

    private fun verifyWeeklyAvailabilitySlots(
        slots: List<WeeklyAvailabilitySlotRequest>
    ): MutableList<Map<String, String>> {
        log.info("Verifying weekly availability slots")
        val slotErrors = mutableListOf<Map<String, String>>()

        for (slot in slots) {
            val errorMsg = mutableListOf<String>()
            if (!DAYS_OF_WEEK.contains(slot.dayOfWeek)) errorMsg.add(INVALID_DAY_OF_WEEK)
            try {
                Utils().verifyTimeZone(slot.timeZoneId)
            } catch (_: Exception) {
                errorMsg.add(INVALID_TIME_ZONE_ID)
            }

            collectTimeErrors(slot.startTime, slot.endTime, errorMsg)

            if (errorMsg.isNotEmpty()) {
                slotErrors.add(mapOf("errors" to errorMsg.toString(), "slot" to slot.toString()))
            }
        }
        return slotErrors
    }

    fun mergeOverlappingSlots(
        slots: List<WeeklyAvailabilitySlotRequest>
    ): List<WeeklyAvailabilitySlotRequest> {
        val daysToSlotsMap = mutableMapOf<String, List<WeeklyAvailabilitySlotRequest>>()
        for (day in DAYS_OF_WEEK) {
            log.info("Collecting slots for: $day")
            val slotsForDay = slots.filter { slot -> slot.dayOfWeek == day }
            if (slotsForDay.isEmpty()) {
                log.info("no slots found for $day")
                continue
            }
            if (slotsForDay.size > 16) throw IllegalArgumentException("Too many slots supplied")

            val sortedSlotsForDay = slotsForDay.sortedBy { slot -> slot.startTime }
            daysToSlotsMap.put(day, sortedSlotsForDay)
        }

        return daysToSlotsMap.flatMap { (day, sortedSlotsForDay) ->
            log.info("Merging slots for: $day")
            val mergedSlotsForDay = mutableListOf(sortedSlotsForDay.first())
            for (currentSlot in sortedSlotsForDay.drop(1)) {
                val lastMergedSlot = mergedSlotsForDay.last()
                if (currentSlot.startTime <= lastMergedSlot.endTime) {
                    log.info("Overlapping slots detected for $day")
                    val newEndTime =
                        if (currentSlot.endTime.isAfter(lastMergedSlot.endTime)) {
                            currentSlot.endTime
                        } else {
                            lastMergedSlot.endTime
                        }
                    val mergedSlot = lastMergedSlot.copy(endTime = newEndTime)

                    mergedSlotsForDay[mergedSlotsForDay.lastIndex] = mergedSlot
                } else {
                    mergedSlotsForDay.add(currentSlot)
                }
            }
            mergedSlotsForDay
        }
    }

    fun addAvailabilityException(
        userId: String,
        request: AvailabilityExceptionRequest,
    ): AvailabilityExceptionResponse {
        val uuid = UUID.fromString(userId)
        val requestErrors = verifyAvailabilityException(uuid, request)
        if (requestErrors.isNotEmpty())
            return AvailabilityExceptionResponse(errors = requestErrors.toString())

        val exceptionToSave =
            AvailabilityException(
                userId = uuid,
                startTime = request.startTime,
                endTime = request.endTime,
                comment = request.comment,
            )
        log.info("Saving availability exception")
        val exceptionFromDb = availabilityExceptionRepository.save(exceptionToSave)
        return AvailabilityExceptionResponse(exceptionFromDb)
    }

    private fun verifyAvailabilityException(
        userId: UUID,
        request: AvailabilityExceptionRequest,
    ): MutableList<String> {
        log.info("Verifying availability exceptions")

        val errorMsg = mutableListOf<String>()
        val startTime = request.startTime
        val endTime = request.endTime
        val threeMonthsInTheFuture = Instant.now().plus(THREE_MONTHS)

        collectTimeErrors(startTime, endTime, errorMsg)
        if (endTime.isAfter(threeMonthsInTheFuture)) errorMsg.add(MORE_THAN_90_DAYS_AWAY)

        val exceptionAlreadyExists =
            availabilityExceptionRepository.existsByUserIdAndStartTimeEquals(userId, startTime)
        if (exceptionAlreadyExists) errorMsg.add(EXCEPTION_ALREADY_EXISTS)

        return errorMsg
    }

    private fun collectTimeErrors(
        startTime: Temporal,
        endTime: Temporal,
        errorMsg: MutableList<String>,
    ) {
        val duration = Duration.between(startTime, endTime)
        if (duration.equals(Duration.ZERO)) errorMsg.add(SAME_START_AND_END_TIME)
        if (duration < Duration.ZERO) errorMsg.add(END_TIME_BEFORE_START)
        if (duration < ONE_HOUR) errorMsg.add(LESS_THAN_ONE_HOUR)
        if (duration >= ONE_DAY) errorMsg.add(MORE_THAN_24_HOURS)
    }

    // TODO: add a method for deleting an availabilityException using ID

}
