package iterative.harmony.backend.service

import iterative.harmony.backend.controller.requests.TimeOffRequest
import iterative.harmony.backend.controller.requests.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.controller.responses.AvailabilityResponse
import iterative.harmony.backend.controller.responses.PlayerAvailabilityResponse
import iterative.harmony.backend.controller.responses.TeamAvailabilityResponse
import iterative.harmony.backend.controller.responses.TimeOffMapper
import iterative.harmony.backend.controller.responses.TimeOffResponse
import iterative.harmony.backend.controller.responses.WeeklyAvailabilityResponse
import iterative.harmony.backend.controller.responses.WeeklyAvailabilitySlotMapper
import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.TeamRepository
import iterative.harmony.backend.repository.TimeOffRepository
import iterative.harmony.backend.repository.UserRepository
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
import iterative.harmony.backend.util.CacheConstants.TEAM_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.CacheConstants.USER_AVAILABILITY_BY_ID
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AvailabilityService {
    @Autowired private lateinit var teamRepository: TeamRepository
    @Autowired
    private lateinit var weeklyAvailabilitySlotRepository: WeeklyAvailabilitySlotRepository
    @Autowired private lateinit var timeOffRepository: TimeOffRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var playerRepository: PlayerRepository
    @Autowired private lateinit var timeOffMapper: TimeOffMapper
    @Autowired private lateinit var weeklyAvailabilitySlotMapper: WeeklyAvailabilitySlotMapper
    @Autowired private lateinit var cacheService: CacheService

    private val log = getLogger()
    private val ONE_DAY = Duration.ofDays(1)
    private val ONE_HOUR = Duration.ofHours(1)
    private val THREE_MONTHS = Duration.ofDays(90)

    fun clearCachedAvailabilityForUserTeams(userId: String) {
        val uuid = UUID.fromString(userId)
        val user = userRepository.findById(uuid).get()
        log.debug("Clearing $USER_AVAILABILITY_BY_ID cache for userId: $userId")
        cacheService.clearUserAvailabilityCache(userId)
        val teamIds = user.players.map { player -> Pair(player.id, player.team?.id) }
        teamIds.forEach { (playerId, teamId) ->
            if (teamId == null) {
                log.debug("No team found for playerId: $playerId. Skipping cache clear.")
            } else {
                log.debug(
                    "Clearing $TEAM_AVAILABILITY_BY_ID cache for [playerId: $playerId, teamId: $teamId]"
                )
                cacheService.clearTeamAvailabilityCache(teamId)
            }
        }
    }

    fun getUserProxyFromUuidString(userId: String): User {
        val uuid = UUID.fromString(userId)
        return userRepository.getReferenceById(uuid)
    }

    @Cacheable(value = [USER_AVAILABILITY_BY_ID], key = "#userId")
    fun getCurrentUserAvailability(userId: String): AvailabilityResponse {
        val user = getUserProxyFromUuidString(userId)
        val weeklyAvailabilitySlots = weeklyAvailabilitySlotRepository.findAllByUser(user)
        val timeOffs = getFutureTimeOffs(user)
        return AvailabilityResponse(
            weeklyAvailabilitySlotMapper.toWeeklyAvailabilitySlotResponseList(
                weeklyAvailabilitySlots
            ),
            timeOffMapper.toTimeOffResponseList(timeOffs),
        )
    }

    private fun getFutureTimeOffs(user: User): List<TimeOff> {
        val now = Instant.now()
        return timeOffRepository.findFutureTimeOffForUser(user.userId!!, now)
    }

    @Transactional
    fun deleteWeeklyAvailability(userId: String) {
        log.info("Deleting weekly availability slots")
        val user = getUserProxyFromUuidString(userId)
        weeklyAvailabilitySlotRepository.deleteAllByUser(user)
        clearCachedAvailabilityForUserTeams(userId)
    }

    @Transactional
    fun overwriteWeeklyAvailability(
        userId: String,
        slots: List<WeeklyAvailabilitySlotRequest>,
    ): WeeklyAvailabilityResponse {
        val slotErrors = verifyWeeklyAvailabilitySlots(slots)
        if (slotErrors.isNotEmpty()) return WeeklyAvailabilityResponse(errors = slotErrors)

        val mergedSlots = mergeOverlappingSlots(slots)

        val userProxy = getUserProxyFromUuidString(userId)

        log.debug("Deleting old weekly availability slots")
        weeklyAvailabilitySlotRepository.deleteAllByUser(userProxy)

        log.debug("Saving weekly availability slots")
        val slotsToSave =
            mergedSlots.map {
                WeeklyAvailabilitySlot(
                    user = userProxy,
                    dayOfWeek = it.dayOfWeek,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    timeZoneId = it.timeZoneId,
                )
            }
        val slotsFromDb = weeklyAvailabilitySlotRepository.saveAll(slotsToSave)
        clearCachedAvailabilityForUserTeams(userId)
        return WeeklyAvailabilityResponse(
            weeklyAvailabilitySlotMapper.toWeeklyAvailabilitySlotResponseList(slotsFromDb)
        )
    }

    private fun verifyWeeklyAvailabilitySlots(
        slots: List<WeeklyAvailabilitySlotRequest>
    ): MutableList<Map<String, String>> {
        log.debug("Verifying weekly availability slots")
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
                log.debug("Merging slots for: $day")
                if (slotsForDay.size > 16) throw IllegalArgumentException("Too many slots supplied")
                val sortedSlotsForDay = slotsForDay.sortedBy { slot -> slot.startTime }
                val mergedSlotsForDay = mutableListOf(sortedSlotsForDay.first())
                for (currentSlot in sortedSlotsForDay.drop(1)) {
                    val lastMergedSlot = mergedSlotsForDay.last()
                    if (currentSlot.startTime <= lastMergedSlot.endTime) {
                        log.debug("Overlapping slots detected for $day")
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

    @Transactional
    fun addTimeOff(userId: String, request: TimeOffRequest): TimeOffResponse {
        val userProxy = getUserProxyFromUuidString(userId)
        val requestErrors = verifyTimeOff(userProxy, request)
        if (requestErrors.isNotEmpty()) return TimeOffResponse(errors = requestErrors.toString())

        log.debug("searching for old timeOff")
        deleteExpiredExceptions(userProxy)

        val timeOffToSave =
            TimeOff(
                user = userProxy,
                startTime = request.startTime,
                endTime = request.endTime,
                comment = request.comment,
            )
        log.debug("Saving timeOff")
        val timeOffFromDb = timeOffRepository.save(timeOffToSave)
        clearCachedAvailabilityForUserTeams(userId)
        return timeOffMapper.toTimeOffResponse(timeOffFromDb)
    }

    @Transactional
    fun deleteExpiredExceptions(user: User) {
        val now = Instant.now()
        val expiredTimeOffs: List<TimeOff> =
            timeOffRepository.findAllByUserAndEndTimeIsBefore(user, now)
        if (expiredTimeOffs.count() > 0) {
            log.debug("${expiredTimeOffs.count()} expired exceptions found. Deleting...")
            timeOffRepository.deleteAll(expiredTimeOffs)
        }
    }

    @Cacheable(value = [TEAM_AVAILABILITY_BY_ID], key = "#teamId")
    fun getTeamAvailability(teamId: Long): TeamAvailabilityResponse {
        log.debug("generating team schedule")
        val team = teamRepository.findById(teamId)
        if (!team.isPresent) return TeamAvailabilityResponse(error = "Team not found")
        log.debug("Found team: ${team.get().name}")
        val teamPlayers = playerRepository.findAllByTeam(team.get())
        log.debug("Found players: {}", teamPlayers.map { p -> p.name })
        val now = Instant.now()
        val sevenDaysFromNow = now.plus(Duration.ofDays(7))

        val avails = TeamAvailabilityResponse()
        teamPlayers.forEach { player ->
            log.debug("generating schedule for: ${player.name}")
            val weeklyAvailabilitySlots =
                weeklyAvailabilitySlotRepository.findAllByUser(player.user)
            val timeOffsInTheNextWeek =
                timeOffRepository.findTimeOffWithinNextWeekForUser(
                    player.user.userId!!,
                    now,
                    sevenDaysFromNow,
                )
            val availability =
                AvailabilityResponse(
                    weeklyAvailabilitySlotMapper.toWeeklyAvailabilitySlotResponseList(
                        weeklyAvailabilitySlots
                    ),
                    timeOffMapper.toTimeOffResponseList(timeOffsInTheNextWeek),
                )
            avails.playerSchedules.add(
                PlayerAvailabilityResponse(player.id!!, player.name, availability)
            )
        }
        return avails
    }

    private fun verifyTimeOff(user: User, request: TimeOffRequest): MutableList<String> {
        log.debug("Verifying timeOff request")

        val errorMsg = mutableListOf<String>()
        val startTime = request.startTime
        val endTime = request.endTime
        val threeMonthsInTheFuture = Instant.now().plus(THREE_MONTHS)

        collectTimeErrors(startTime, endTime, errorMsg)
        if (endTime.isAfter(threeMonthsInTheFuture)) errorMsg.add(MORE_THAN_90_DAYS_AWAY)

        val exceptionAlreadyExists =
            timeOffRepository.existsByUserAndStartTimeEquals(user, startTime)
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

    @Transactional
    fun deleteTimeOff(userId: String, timeOffId: Long) {
        log.debug("Deleting timeOff: $timeOffId")
        val userProxy = getUserProxyFromUuidString(userId)
        val timeOff = timeOffRepository.findByIdAndUser(timeOffId, userProxy)
        if (!timeOff.isPresent)
            throw IllegalArgumentException(
                "TimeOff with id $timeOffId not found or unassociated with current user"
            )
        timeOffRepository.delete(timeOff.get())
        clearCachedAvailabilityForUserTeams(userId)
    }
}
