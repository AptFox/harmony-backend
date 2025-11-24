package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.AvailabilityResponse
import iterative.harmony.backend.controller.dto.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.model.AvailabilityException
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.repository.AvailabilityExceptionRepository
import iterative.harmony.backend.repository.WeeklyAvailabilitySlotRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AvailabilityServiceTest {
    @Mock
    private lateinit var weeklyAvailabilitySlotRepositoryMock: WeeklyAvailabilitySlotRepository
    @Mock private lateinit var availabilityExceptionRepositoryMock: AvailabilityExceptionRepository

    @InjectMocks private lateinit var availabilityService: AvailabilityService

    val id: Long = 1L
    val userId: UUID = UUID.randomUUID()
    val playerId: Long = 2L
    val comment = "test comment"
    val dayOfWeek = "Mon"

    @Test
    fun `getCurrentUserAvailability returns an AvailabilityResponse`() {
        val availabilityExceptions: MutableList<AvailabilityException> =
            mutableListOf(
                AvailabilityException(
                    id,
                    userId,
                    playerId,
                    startTime = Instant.now(),
                    endTime = Instant.now().plus(Duration.ofHours(1)),
                    comment,
                )
            )
        val weeklyAvailabilitySlots: MutableList<WeeklyAvailabilitySlot> =
            mutableListOf(
                WeeklyAvailabilitySlot(
                    id,
                    userId,
                    playerId,
                    dayOfWeek,
                    startTime = LocalTime.NOON,
                    endTime = LocalTime.NOON.plus(Duration.ofHours(1)),
                    timeZoneId = "America/New_York",
                )
            )
        val expected = AvailabilityResponse(weeklyAvailabilitySlots, availabilityExceptions)

        whenever(weeklyAvailabilitySlotRepositoryMock.findAllByUserId(userId))
            .thenReturn(weeklyAvailabilitySlots)
        whenever(
                availabilityExceptionRepositoryMock
                    .findAllByUserIdAndStartTimeIsAfterOrEndTimeIsAfter(any(), any(), any())
            )
            .thenReturn(availabilityExceptions)

        val actual = availabilityService.getCurrentUserAvailability(userId.toString())
        assertEquals(
            expected.weeklyAvailabilitySlots.first(),
            actual.weeklyAvailabilitySlots.first(),
        )
        assertEquals(expected.availabilityExceptions.first(), actual.availabilityExceptions.first())
    }

    @Test
    fun `deleteWeeklyAvailability deletes all WeeklyAvailabilitySlots for user`() {
        whenever(weeklyAvailabilitySlotRepositoryMock.deleteAllByUserId(userId)).thenReturn(null)
        availabilityService.deleteWeeklyAvailability(userId.toString())
        verify(weeklyAvailabilitySlotRepositoryMock).deleteAllByUserId(userId)
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
                throw NotImplementedError()
            }
        }

        @Nested
        @DisplayName("when called with valid slots")
        inner class ValidSlots() {
            @Test
            fun `slots are saved and returned`() {
                throw NotImplementedError()
            }
        }
    }

    @Nested
    @DisplayName("addAvailabilityException")
    inner class AddAvailabilityException() {
        @Nested
        @DisplayName("when called with invalid AvailabilityExceptions")
        inner class InvalidExceptions() {
            @Test
            fun `when times are invalid format should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when comment is invalid should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when startTime and endTime are the same should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when endTime is before startTime should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when exception is more than 24 hours should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when exception is less than 1 hour should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when exception is in the past should return error`() {
                throw NotImplementedError()
            }

            @Test
            fun `when exception is more than 3 months in the future should return error`() {
                throw NotImplementedError()
            }
        }

        @Nested
        @DisplayName("when called with valid exceptions")
        inner class ValidExceptions() {
            @Test
            fun `expired exceptions are deleted and new exceptions are saved and returned`() {
                throw NotImplementedError()
            }
        }
    }

    @Test
    fun `deleteAvailabilityException deletes supplied AvailabilityException for user`() {
        val exceptionId = 1L
        doNothing()
            .whenever(availabilityExceptionRepositoryMock)
            .deleteByIdAndUserId(exceptionId, userId)
        availabilityService.deleteAvailabilityException(userId.toString(), exceptionId)
        verify(availabilityExceptionRepositoryMock).deleteByIdAndUserId(exceptionId, userId)
    }
}
