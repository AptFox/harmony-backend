package iterative.harmony.backend.service

import iterative.harmony.backend.repository.AvailabilityExceptionRepository
import iterative.harmony.backend.repository.WeeklyAvailabilitySlotRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AvailabilityServiceTest {
    @Mock
    private lateinit var weeklyAvailabilitySlotRepositoryMock: WeeklyAvailabilitySlotRepository
    @Mock private lateinit var availabilityExceptionRepositoryMock: AvailabilityExceptionRepository

    @InjectMocks private lateinit var availabilityService: AvailabilityService

    @Test fun `getCurrentUserAvailability returns an AvailabilityResponse`() {}

    @Test fun `deleteWeeklyAvailability deletes all WeeklyAvailabilitySlots for user`() {}

    @Nested
    @DisplayName("overwriteWeeklyAvailability")
    inner class OverwriteWeeklyAvailability() {
        @Nested
        @DisplayName("when called with invalid slots")
        inner class InvalidSlots() {
            @Test fun `when slot times are invalid format should return error`() {}

            @Test fun `when slot timeZoneId is invalid should return error`() {}

            @Test fun `when startTime and endTime are the same should return error`() {}

            @Test fun `when endTime is before startTime should return error`() {}

            @Test fun `when dayOfWeek is invalid should return error`() {}
        }

        @Nested @DisplayName("when called with overlapping slots") inner class OverlappingSlots() {}

        @Nested @DisplayName("when called with valid slots") inner class ValidSlots() {}
    }

    @Nested
    @DisplayName("addAvailabilityException")
    inner class AddAvailabilityException() {
        @Nested
        @DisplayName("when called with invalid AvailabilityExceptions")
        inner class InvalidExceptions() {
            @Test fun `when times are invalid format should return error`() {}

            @Test fun `when comment is invalid should return error`() {}

            @Test fun `when startTime and endTime are the same should return error`() {}

            @Test fun `when endTime is before startTime should return error`() {}

            @Test fun `when exception is more than 24 hours should return error`() {}

            @Test fun `when exception is less than 1 hour should return error`() {}

            @Test fun `when exception is in the past should return error`() {}

            @Test fun `when exception is more than 3 months in the future should return error`() {}
        }

        @Nested @DisplayName("when called with valid exceptions") inner class ValidExceptions() {}
    }

    @Test fun `deleteAvailabilityException deletes supplied AvailabilityException for user`() {}
}
