package com.dhiren.atom.data

import com.dhiren.atom.data.local.OwnerProfileDao
import com.dhiren.atom.data.local.OwnerProfileEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class GenderOption(
    val label: String,
    val honorific: String?,
) {
    Man("Man", "Sir"),
    Woman("Woman", "Ma'am"),
    NonBinary("Non-binary", null),
    PreferNotToSay("Prefer not to say", null),
    ;

    companion object {
        fun fromStorage(value: String): GenderOption =
            entries.firstOrNull { it.name == value } ?: PreferNotToSay
    }
}

enum class PronounOption(val label: String) {
    HeHim("He / him"),
    SheHer("She / her"),
    TheyThem("They / them"),
    UseMyName("Use my name"),
    PreferNotToSay("Prefer not to say"),
    ;

    companion object {
        fun fromStorage(value: String): PronounOption =
            entries.firstOrNull { it.name == value } ?: PreferNotToSay
    }
}

data class OwnerProfile(
    val displayName: String,
    val gender: GenderOption,
    val pronouns: PronounOption,
) {
    val greetingName: String
        get() = listOfNotNull(displayName, gender.honorific).joinToString(" ")

    val initial: String
        get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"

    companion object {
        val Default = OwnerProfile(
            displayName = "Dhiren",
            gender = GenderOption.Man,
            pronouns = PronounOption.HeHim,
        )
    }
}

class OwnerProfileRepository(
    private val ownerProfileDao: OwnerProfileDao,
    private val clock: Clock = Clock.systemUTC(),
    private val zoneProvider: () -> ZoneId = { ZoneId.systemDefault() },
    private val localeProvider: () -> Locale = { Locale.getDefault() },
) {
    val profile: Flow<OwnerProfile> = ownerProfileDao.observeOwner().map { entity ->
        entity?.toProfile() ?: OwnerProfile.Default
    }

    suspend fun update(
        displayName: String,
        gender: GenderOption,
        pronouns: PronounOption,
    ) {
        val normalizedName = normalizeProfileName(displayName)
        require(normalizedName.isNotEmpty()) { "Profile name cannot be empty" }
        val existing = ownerProfileDao.getOwner()
        val now = Instant.now(clock).toString()
        ownerProfileDao.upsert(
            OwnerProfileEntity(
                id = 1,
                displayName = normalizedName,
                gender = gender.name,
                pronouns = pronouns.name,
                timezone = zoneProvider().id,
                locale = localeProvider().toLanguageTag(),
                createdAtUtc = existing?.createdAtUtc ?: now,
                updatedAtUtc = now,
            ),
        )
    }
}

internal fun normalizeProfileName(value: String): String =
    value.trim().replace(Regex("\\s+"), " ").take(40)

internal fun OwnerProfileEntity.toProfile(): OwnerProfile = OwnerProfile(
    displayName = normalizeProfileName(displayName).ifEmpty { OwnerProfile.Default.displayName },
    gender = GenderOption.fromStorage(gender),
    pronouns = PronounOption.fromStorage(pronouns),
)
