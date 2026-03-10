package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private val FIRST_APOD_DATE = LocalDate.of(1995, 6, 16)
private const val MAX_RANGE_DAYS = 365L

class GetApodRangeUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(startDate: String, endDate: String): Result<List<Apod>> {
        val start = try {
            LocalDate.parse(startDate)
        } catch (e: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Ungültiges Startdatum: $startDate"))
        }
        val end = try {
            LocalDate.parse(endDate)
        } catch (e: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Ungültiges Enddatum: $endDate"))
        }
        val today = LocalDate.now()
        when {
            start.isBefore(FIRST_APOD_DATE) ->
                return Result.failure(IllegalArgumentException("Startdatum darf nicht vor dem 16.06.1995 liegen."))
            end.isAfter(today) ->
                return Result.failure(IllegalArgumentException("Enddatum darf nicht in der Zukunft liegen."))
            start.isAfter(end) ->
                return Result.failure(IllegalArgumentException("Startdatum muss vor dem Enddatum liegen."))
            ChronoUnit.DAYS.between(start, end) > MAX_RANGE_DAYS ->
                return Result.failure(IllegalArgumentException("Der Zeitraum darf maximal $MAX_RANGE_DAYS Tage umfassen."))
        }
        return repository.getApodRange(startDate, endDate)
    }
}
