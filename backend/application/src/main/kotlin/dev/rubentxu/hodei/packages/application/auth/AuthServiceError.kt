package dev.rubentxu.hodei.packages.application.auth

sealed class AuthServiceError {
    data class ValidationFailed(val reason: String) : AuthServiceError()

    data object AdminAlreadyExists : AuthServiceError()

    data object UserNotFound : AuthServiceError()

    data object InvalidCredentials : AuthServiceError()

    data class UnexpectedError(val message: String, val cause: Throwable? = null) : AuthServiceError()
}
