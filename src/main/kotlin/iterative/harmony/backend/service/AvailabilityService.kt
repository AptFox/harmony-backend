package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.AvailabilityResponse
import iterative.harmony.backend.controller.dto.TimeOffRequest
import iterative.harmony.backend.controller.dto.TimeOffResponse
import iterative.harmony.backend.controller.dto.WeeklyAvailabilityResponse
import iterative.harmony.backend.controller.dto.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.repository.TimeOffRepository
import iterative.harmony.backend.repository.WeeklyAvailabilitySlotRepository
import iterative.harmony.backend.util.AvailabilityConstants.DAYS_OF_WEEK
import iterative.harmony.backend.util.AvailabilityConstants.END_TIME_BEFORE_START
import iterative.harmony.backend.util.AvailabilityConstants.INVALID_DAY_OF_WEEK
import iterative.harmony.backend.util.AvailabilityConstants.INVALID_TIME_ZONE_ID
import iterative.harmony.backend.util.AvailabilityConstants.LESS_THAN_ONE_HOUR
import iterative.harmony.backend.util.AvailabilityConstants.MORE_THAN_24_HOURS
import iterative.harmony.backend.util.AvailabilityConstants.MORE_THAN_90_DAYS_AWAY
import iterative.harmony.backend.util.AvailabilityConstants.SAME_START_AND_END_TIME
import iterative.harmony.backend.util.AvailabilityConstants.TIME_OFF_ALREADY_EXISTS
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
    @Autowired private lateinit var timeOffRepository: TimeOffRepository

    private val log = getLogger()
    private val ONE_DAY = Duration.ofDays(1)
    private val ONE_HOUR = Duration.ofHours(1)
    private val THREE_MONTHS = Duration.ofDays(90)

    fun getCurrentUserAvailability(userId: String): AvailabilityResponse {
        val uuid = UUID.fromString(userId)
        val now = Instant.now()
        val weeklyAvailabilitySlots = weeklyAvailabilitySlotRepository.findAllByUserId(uuid)
        val timeOffs =
            timeOffRepository.findAllByUserIdAndStartTimeIsAfterOrEndTimeIsAfter(uuid, now, now)
        return AvailabilityResponse(weeklyAvailabilitySlots, timeOffs)
    }

    fun deleteWeeklyAvailability(userId: String) {
        log.info("Deleting weekly availability slots")
        val uuid = UUID.fromString(userId)
        weeklyAvailabilitySlotRepository.deleteAllByUserId(uuid)
    }

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

    private fun mergeOverlappingSlots(
        slots: List<WeeklyAvailabilitySlotRequest>
    ): List<WeeklyAvailabilitySlotRequest> {
        return slots
            .groupBy { it.dayOfWeek }
            .flatMap { (day, slotsForDay) ->
                log.info("Merging slots for: $day")
                if (slotsForDay.size > 16) throw IllegalArgumentException("Too many slots supplied")
                val sortedSlotsForDay = slotsForDay.sortedBy { slot -> slot.startTime }
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

    fun addTimeOff(userId: String, request: TimeOffRequest): TimeOffResponse {
        val uuid = UUID.fromString(userId)
        val requestErrors = verifyTimeOff(uuid, request)
        if (requestErrors.isNotEmpty()) return TimeOffResponse(errors = requestErrors.toString())

        log.info("searching for old timeOff")
        deleteExpiredExceptions(uuid)

        val exceptionToSave =
            TimeOff(
                userId = uuid,
                startTime = request.startTime,
                endTime = request.endTime,
                comment = request.comment,
            )
        log.info("Saving timeOff")
        val exceptionFromDb = timeOffRepository.save(exceptionToSave)
        return TimeOffResponse(exceptionFromDb)
    }

    private fun deleteExpiredExceptions(userId: UUID) {
        val now = Instant.now()
        val expiredTimeOffs: List<TimeOff> =
            timeOffRepository.findAllByUserIdAndEndTimeIsBefore(userId, now)
        if (expiredTimeOffs.count() > 0) {
            log.info("${expiredTimeOffs.count()} expired exceptions found. Deleting...")
            timeOffRepository.deleteAll(expiredTimeOffs)
        }
    }

    private fun verifyTimeOff(userId: UUID, request: TimeOffRequest): MutableList<String> {
        log.info("Verifying timeOff request")

        val errorMsg = mutableListOf<String>()
        val startTime = request.startTime
        val endTime = request.endTime
        val threeMonthsInTheFuture = Instant.now().plus(THREE_MONTHS)

        collectTimeErrors(startTime, endTime, errorMsg)
        if (endTime.isAfter(threeMonthsInTheFuture)) errorMsg.add(MORE_THAN_90_DAYS_AWAY)

        val exceptionAlreadyExists =
            timeOffRepository.existsByUserIdAndStartTimeEquals(userId, startTime)
        if (exceptionAlreadyExists) errorMsg.add(TIME_OFF_ALREADY_EXISTS)

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

    fun deleteTimeOff(userId: String, timeOffId: Long) {
        log.info("Deleting timeOff: $timeOffId")
        val uuid = UUID.fromString(userId)
        timeOffRepository.deleteByIdAndUserId(timeOffId, uuid)
    }
}
