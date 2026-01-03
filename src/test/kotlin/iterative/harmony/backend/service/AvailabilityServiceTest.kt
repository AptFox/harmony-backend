package iterative.harmony.backend.service

import iterative.harmony.backend.controller.requests.TimeOffRequest
import iterative.harmony.backend.controller.requests.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.controller.responses.AvailabilityResponse
import iterative.harmony.backend.controller.responses.TimeOffMapper
import iterative.harmony.backend.controller.responses.TimeOffResponse
import iterative.harmony.backend.controller.responses.WeeklyAvailabilitySlotMapper
import iterative.harmony.backend.controller.responses.WeeklyAvailabilitySlotResponse
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.repository.OrganizationRepository
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.TimeOffRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.repository.WeeklyAvailabilitySlotRepository
import iterative.harmony.backend.util.AvailabilityConstants.DAYS_OF_WEEK
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.internal.verification.Times
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AvailabilityServiceTest {
    @Mock
    private lateinit var weeklyAvailabilitySlotRepositoryMock: WeeklyAvailabilitySlotRepository
    @Mock private lateinit var timeOffRepositoryMock: TimeOffRepository
    @Mock private lateinit var userRepositoryMock: UserRepository
    @Mock private lateinit var playerRepositoryMock: PlayerRepository
    @Mock private lateinit var orgRepositoryMock: OrganizationRepository
    @Mock private lateinit var weeklyAvailabilitySlotMapperMock: WeeklyAvailabilitySlotMapper
    @Mock private lateinit var timeOffMapperMock: TimeOffMapper

    @InjectMocks private lateinit var availabilityService: AvailabilityService

    val userId: UUID = UUID.randomUUID()
    val orgId: Long = 3L
    val userMock = mock<User> { on { userId } doReturn userId }
    val playerMock: Player = mock<Player> { on { id } doReturn 2L }
    val orgMock: Organization = mock<Organization>()
    val testComment = "test comment"
    val dayOfWeek = "Mon"
    val newYorkTimeZoneId = "America/New_York"

    @Test
    fun `getCurrentUserAvailability returns an AvailabilityResponse`() {
        val timeOffMock = mock<TimeOff>()
        val timeOffs: List<TimeOff> = mutableListOf(timeOffMock)
        val weeklyAvailabilitySlots: List<WeeklyAvailabilitySlot> =
            mutableListOf(
                WeeklyAvailabilitySlot(
                    userMock,
                    playerMock,
                    dayOfWeek,
                    startTime = LocalTime.NOON,
                    endTime = LocalTime.NOON.plus(Duration.ofHours(1)),
                    timeZoneId = newYorkTimeZoneId,
                )
            )
        val weeklyAvailabilitySlotResponseMock = mock<WeeklyAvailabilitySlotResponse>()
        val timeOffResponseMock = mock<TimeOffResponse>()
        val expected =
            AvailabilityResponse(
                listOf(weeklyAvailabilitySlotResponseMock),
                listOf(timeOffResponseMock),
            )

        whenever(weeklyAvailabilitySlotRepositoryMock.findAllByUser(userMock))
            .thenReturn(weeklyAvailabilitySlots)
        whenever(timeOffRepositoryMock.findFutureTimeOffForUser(any(), any())).thenReturn(timeOffs)
        whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
        whenever(
                weeklyAvailabilitySlotMapperMock.toWeeklyAvailabilitySlotResponseList(
                    weeklyAvailabilitySlots
                )
            )
            .thenReturn(listOf(weeklyAvailabilitySlotResponseMock))
        whenever(timeOffMapperMock.toTimeOffResponseList(timeOffs))
            .thenReturn(listOf(timeOffResponseMock))

        val actual = availabilityService.getCurrentUserAvailability(userId.toString())
        assertEquals(
            expected.weeklyAvailabilitySlots.first(),
            actual.weeklyAvailabilitySlots.first(),
        )
        assertEquals(expected.timeOffs.first(), actual.timeOffs.first())
    }

    @Test
    fun `deleteWeeklyAvailability deletes all WeeklyAvailabilitySlots for user`() {
        whenever(weeklyAvailabilitySlotRepositoryMock.deleteAllByUser(userMock)).thenReturn(null)
        whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
        availabilityService.deleteWeeklyAvailability(userId.toString())
        verify(weeklyAvailabilitySlotRepositoryMock).deleteAllByUser(userMock)
    }

    @Nested
    @DisplayName("overwriteWeeklyAvailability")
    inner class OverwriteWeeklyAvailability() {
        @Nested
        @DisplayName("when called with invalid slots")
        inner class InvalidSlots() {
            @Test
            fun `when slot timeZoneId is invalid should return error`() {
                val requests: MutableList<WeeklyAvailabilitySlotRequest> =
                    mutableListOf(
                        WeeklyAvailabilitySlotRequest(
                            "Mon",
                            startTime = LocalTime.NOON,
                            endTime = LocalTime.NOON.plus(Duration.ofHours(1)),
                            timeZoneId = "1America/New_York",
                        )
                    )
                val expected = "[Invalid timeZoneId]"
                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.errors?.get(0)?.get("errors")
                assertEquals(expected, actual)
            }

            @Test
            fun `when startTime and endTime are the same should return error`() {
                val requests: MutableList<WeeklyAvailabilitySlotRequest> =
                    mutableListOf(
                        WeeklyAvailabilitySlotRequest(
                            "Mon",
                            startTime = LocalTime.NOON,
                            endTime = LocalTime.NOON,
                            timeZoneId = "America/New_York",
                        )
                    )
                val expected =
                    "[startTime and endTime are the same, availability changes must be >=60 min]"
                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.errors?.get(0)?.get("errors")
                assertEquals(expected, actual)
            }

            @Test
            fun `when endTime is before startTime should return error`() {
                val requests: MutableList<WeeklyAvailabilitySlotRequest> =
                    mutableListOf(
                        WeeklyAvailabilitySlotRequest(
                            "Mon",
                            startTime = LocalTime.NOON.plus(Duration.ofHours(1)),
                            endTime = LocalTime.NOON,
                            timeZoneId = "America/New_York",
                        )
                    )
                val expected =
                    "[endTime is before startTime, availability changes must be >=60 min]"
                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.errors?.get(0)?.get("errors")
                assertEquals(expected, actual)
            }

            @Test
            fun `when dayOfWeek is invalid should return error`() {
                val requests: MutableList<WeeklyAvailabilitySlotRequest> =
                    mutableListOf(
                        WeeklyAvailabilitySlotRequest(
                            "TMon",
                            startTime = LocalTime.NOON,
                            endTime = LocalTime.NOON.plus(Duration.ofHours(1)),
                            timeZoneId = "America/New_York",
                        )
                    )
                val expected = "[dayOfWeek is not one of [Mon, Tue, Wed, Thu, Fri, Sat, Sun]]"
                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.errors?.get(0)?.get("errors")
                assertEquals(expected, actual)
            }
        }

        @Nested
        @DisplayName("when called with overlapping slots")
        inner class OverlappingSlots() {
            @Test
            fun `slots are merged, saved and returned`() {
                val timeZoneId = "America/New_York"
                val expectedMergedStartTime = LocalTime.NOON
                val expectedMergedEndTime = LocalTime.NOON.plus(Duration.ofHours(6))
                val requests: MutableList<WeeklyAvailabilitySlotRequest> = mutableListOf()
                for (i in 1..6) {
                    requests.add(
                        WeeklyAvailabilitySlotRequest(
                            dayOfWeek,
                            startTime = expectedMergedStartTime,
                            endTime = LocalTime.NOON.plus(Duration.ofHours(i.toLong())),
                            timeZoneId,
                        )
                    )
                }
                val expectedSlot =
                    WeeklyAvailabilitySlot(
                        user = userMock,
                        playerMock,
                        dayOfWeek = dayOfWeek,
                        startTime = expectedMergedStartTime,
                        endTime = expectedMergedEndTime,
                        timeZoneId = timeZoneId,
                    )
                val expectedMergedSlots = mutableListOf(expectedSlot)

                val expectedWeeklyAvailabilitySlot =
                    WeeklyAvailabilitySlot(
                        userMock,
                        playerMock,
                        dayOfWeek,
                        startTime = expectedMergedStartTime,
                        endTime = expectedMergedEndTime,
                        timeZoneId = "America/New_York",
                    )

                val expected = mock<WeeklyAvailabilitySlotResponse>()

                whenever(weeklyAvailabilitySlotRepositoryMock.saveAll(expectedMergedSlots))
                    .thenReturn(mutableListOf(expectedWeeklyAvailabilitySlot))
                whenever(weeklyAvailabilitySlotRepositoryMock.deleteAllByUser(userMock))
                    .thenReturn(null)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
                whenever(playerRepositoryMock.findByUser(userMock))
                    .thenReturn(Optional.of(playerMock))
                whenever(
                        weeklyAvailabilitySlotMapperMock.toWeeklyAvailabilitySlotResponseList(
                            mutableListOf(expectedWeeklyAvailabilitySlot)
                        )
                    )
                    .thenReturn(listOf(expected))

                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.weeklyAvailabilitySlots?.get(0)
                assertEquals(expected, actual)
                verify(weeklyAvailabilitySlotRepositoryMock).saveAll(expectedMergedSlots)
                verify(weeklyAvailabilitySlotRepositoryMock).deleteAllByUser(userMock)
            }
        }

        @Nested
        @DisplayName("when called with non-overlapping slots")
        inner class ValidSlots() {
            @Test
            fun `separate slots are saved and returned`() {
                val timeZoneId = "America/New_York"
                val expectedMergedStartTime = LocalTime.NOON
                val expectedMergedEndTime = LocalTime.NOON.plus(Duration.ofHours(6))
                val requests: MutableList<WeeklyAvailabilitySlotRequest> = mutableListOf()
                for (i in 0..6) {
                    requests.add(
                        WeeklyAvailabilitySlotRequest(
                            DAYS_OF_WEEK.elementAt(i),
                            startTime = expectedMergedStartTime,
                            endTime = expectedMergedEndTime,
                            timeZoneId,
                        )
                    )
                }

                val expectedUnmergedSlots: MutableList<WeeklyAvailabilitySlot> = mutableListOf()

                for (i in 0..6) {
                    expectedUnmergedSlots.add(
                        WeeklyAvailabilitySlot(
                            user = userMock,
                            player = playerMock,
                            dayOfWeek = DAYS_OF_WEEK.elementAt(i),
                            startTime = expectedMergedStartTime,
                            endTime = expectedMergedEndTime,
                            timeZoneId = timeZoneId,
                        )
                    )
                }

                val expected = mutableListOf<WeeklyAvailabilitySlotResponse>()
                for (i in 0..6) {
                    expected.add(mock<WeeklyAvailabilitySlotResponse>())
                }

                whenever(weeklyAvailabilitySlotRepositoryMock.saveAll(expectedUnmergedSlots))
                    .thenReturn(expectedUnmergedSlots)
                whenever(weeklyAvailabilitySlotRepositoryMock.deleteAllByUser(userMock))
                    .thenReturn(null)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
                whenever(playerRepositoryMock.findByUser(userMock))
                    .thenReturn(Optional.of(playerMock))
                whenever(
                        weeklyAvailabilitySlotMapperMock.toWeeklyAvailabilitySlotResponseList(
                            expectedUnmergedSlots
                        )
                    )
                    .thenReturn(expected)

                val response =
                    availabilityService.overwriteWeeklyAvailability(userId.toString(), requests)
                val actual = response.weeklyAvailabilitySlots
                for (i in 0..6) {
                    val actualSlot = actual?.get(i)
                    assertEquals(expected[i], actualSlot)
                }
                verify(weeklyAvailabilitySlotRepositoryMock).saveAll(expectedUnmergedSlots)
                verify(weeklyAvailabilitySlotRepositoryMock).deleteAllByUser(userMock)
            }
        }
    }

    @Nested
    @DisplayName("addTimeOff")
    inner class AddTimeOff() {
        @Nested
        @DisplayName("when called with invalid TimeOffs")
        inner class InvalidTimeOff() {

            fun generateExceptionRequest(
                amountToAdd: Duration,
                minus: Boolean = false,
            ): TimeOffRequest {
                val expectedStartTime = Instant.now()
                val expectedEndTime =
                    if (minus) {
                        expectedStartTime.minus(amountToAdd)
                    } else {
                        expectedStartTime.plus(amountToAdd)
                    }

                return TimeOffRequest(
                    startTime = expectedStartTime,
                    endTime = expectedEndTime,
                    comment = null,
                )
            }

            @Test
            fun `when startTime and endTime are the same should return error`() {
                val request = generateExceptionRequest(Duration.ofHours(0))

                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)

                val response = availabilityService.addTimeOff(userId.toString(), request)
                val expected =
                    "[startTime and endTime are the same, availability changes must be >=60 min]"
                val actual = response.errors
                assertEquals(expected, actual)
                verify(timeOffRepositoryMock, Times(0)).save(any())
            }

            @Test
            fun `when endTime is before startTime should return error`() {
                val request = generateExceptionRequest(Duration.ofHours(1), true)

                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)

                val response = availabilityService.addTimeOff(userId.toString(), request)
                val expected =
                    "[endTime is before startTime, availability changes must be >=60 min]"
                val actual = response.errors
                assertEquals(expected, actual)
                verify(timeOffRepositoryMock, Times(0)).save(any())
            }

            @Test
            fun `when exception is more than 24 hours should return error`() {
                val request = generateExceptionRequest(Duration.ofHours(25))

                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)

                val response = availabilityService.addTimeOff(userId.toString(), request)
                val expected = "[availability changes must be <= 24 hours]"
                val actual = response.errors
                assertEquals(expected, actual)
                verify(timeOffRepositoryMock, Times(0)).save(any())
            }

            @Test
            fun `when exception is less than 1 hour should return error`() {
                val request = generateExceptionRequest(Duration.ofMinutes(6))

                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)

                val response = availabilityService.addTimeOff(userId.toString(), request)
                val expected = "[availability changes must be >=60 min]"
                val actual = response.errors
                assertEquals(expected, actual)
                verify(timeOffRepositoryMock, Times(0)).save(any())
            }

            @Test
            fun `when exception is more than 3 months in the future should return error`() {
                val request = generateExceptionRequest(Duration.ofDays(91))

                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)

                val response = availabilityService.addTimeOff(userId.toString(), request)
                val expected =
                    "[availability changes must be <= 24 hours, time off must be within 90 days]"
                val actual = response.errors
                assertEquals(expected, actual)
                verify(timeOffRepositoryMock, Times(0)).save(any())
            }
        }

        @Nested
        @DisplayName("when called with valid exceptions")
        inner class ValidExceptions() {
            @Test
            fun `expired exceptions are deleted and new exceptions are saved and returned`() {
                val expectedStartTime = Instant.now()
                val expectedEndTime = expectedStartTime.plus(Duration.ofHours(4))

                val request =
                    TimeOffRequest(
                        startTime = expectedStartTime,
                        endTime = expectedEndTime,
                        comment = null,
                    )
                val expected =
                    TimeOff(
                        user = userMock,
                        player = playerMock,
                        startTime = request.startTime,
                        endTime = request.endTime,
                        comment = null,
                    )
                val timeOffResponseMock = mock<TimeOffResponse>()
                whenever(timeOffRepositoryMock.save(expected)).thenReturn(expected)
                whenever(
                        timeOffRepositoryMock.existsByUserAndStartTimeEquals(
                            userMock,
                            request.startTime,
                        )
                    )
                    .thenReturn(false)
                whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
                whenever(playerRepositoryMock.findByUser(userMock))
                    .thenReturn(Optional.of(playerMock))
                whenever(timeOffMapperMock.toTimeOffResponse(expected))
                    .thenReturn(timeOffResponseMock)

                val actual = availabilityService.addTimeOff(userId.toString(), request)
                assertEquals(timeOffResponseMock, actual)
            }
        }
    }

    @Test
    fun `deleteTimeOff deletes supplied TimeOff for user`() {
        val exceptionId = 1L
        val timeOffMock = mock<TimeOff>()
        whenever(timeOffRepositoryMock.findByIdAndUser(exceptionId, userMock))
            .doReturn(Optional.of(timeOffMock))
        whenever(userRepositoryMock.getReferenceById(userId)).thenReturn(userMock)
        availabilityService.deleteTimeOff(userId.toString(), exceptionId)
        verify(timeOffRepositoryMock).delete(timeOffMock)
    }

    // TODO: write tests for getTeamSchedule
    @Nested
    @DisplayName("getTeamSchedule")
    inner class GetTeamSchedule() {

        // TODO: write tests for error states

        // TODO: write tests for success states

    }
}
