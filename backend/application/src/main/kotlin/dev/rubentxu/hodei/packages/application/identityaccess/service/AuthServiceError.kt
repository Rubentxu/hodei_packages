package dev.rubentxu.hodei.packages.application.identityaccess.service

sealed class AuthServiceError : Exception {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable?) : super(message, cause)

    class ValidationFailed(reason: String) : AuthServiceError(reason)

    object AdminAlreadyExists : AuthServiceError("Admin user already exists")

    object UserNotFound : AuthServiceError("User not found")

    object InvalidCredentials : AuthServiceError("Invalid credentials")

    class UnexpectedError(message: String, cause: Throwable? = null) : AuthServiceError(message, cause)
}
