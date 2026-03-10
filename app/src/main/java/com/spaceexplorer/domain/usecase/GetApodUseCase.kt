package com.spaceexplorer.domain.usecase

import com.spaceexplorer.domain.model.Apod
import com.spaceexplorer.domain.repository.ApodRepository
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

class GetApodUseCase @Inject constructor(
    private val repository: ApodRepository
) {
    suspend operator fun invoke(date: String? = null): Result<Apod> {
        if (date != null) {
            val parsed = try {
                LocalDate.parse(date)
            } catch (e: DateTimeParseException) {
                return Result.failure(IllegalArgumentException("Ungültiges Datumsformat: $date"))
            }
            val firstApodDate = LocalDate.of(1995, 6, 16)
            val today = LocalDate.now()
            when {
                parsed.isBefore(firstApodDate) ->
                    return Result.failure(IllegalArgumentException("APOD existiert erst ab dem 16.06.1995."))
                parsed.isAfter(today) ->
                    return Result.failure(IllegalArgumentException("Das Datum darf nicht in der Zukunft liegen."))
            }
        }
        return repository.getApod(date)
    }
}
