package dev.rubentxu.hodei.packages.application.shared

/**
 * A generic class that holds a value with its loading status.
 * @param <S> The type of the success value.
 * @param <E> The type of the error.
 */
sealed class Result<out S, out E> {
    data class Success<out S>(val value: S) : Result<S, Nothing>()

    data class Failure<out E>(val error: E) : Result<Nothing, E>()

    val isSuccess get() = this is Success<S>
    val isFailure get() = this is Failure<E>

    fun getOrNull(): S? = if (this is Success<S>) value else null

    fun errorOrNull(): E? = if (this is Failure<E>) error else null

    companion object {
        fun <S> success(value: S): Result<S, Nothing> = Success(value)

        fun <E> failure(error: E): Result<Nothing, E> = Failure(error)
    }
}

inline fun <S, E, R> Result<S, E>.fold(
    onSuccess: (value: S) -> R,
    onFailure: (error: E) -> R,
): R =
    when (this) {
        is Result.Success -> onSuccess(value)
        is Result.Failure -> onFailure(error)
    }

inline fun <S, E, T> Result<S, E>.map(transform: (S) -> T): Result<T, E> {
    return when (this) {
        is Result.Success -> Result.Success(transform(value))
        is Result.Failure -> this
    }
}

inline fun <S, E, T> Result<S, E>.flatMap(transform: (S) -> Result<T, E>): Result<T, E> {
    return when (this) {
        is Result.Success -> transform(value)
        is Result.Failure -> this
    }
}
