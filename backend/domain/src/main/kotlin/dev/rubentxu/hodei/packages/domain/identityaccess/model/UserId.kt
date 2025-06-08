package dev.rubentxu.hodei.packages.domain.identityaccess.model

@JvmInline
value class UserId(val value: String) {
    companion object {
        fun anonymous(): UserId {
            return UserId("anonymous")
        }
    }
}