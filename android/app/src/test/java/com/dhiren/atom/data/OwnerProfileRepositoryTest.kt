package com.dhiren.atom.data

import com.dhiren.atom.data.local.OwnerProfileDao
import com.dhiren.atom.data.local.OwnerProfileEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OwnerProfileRepositoryTest {
    @Test
    fun `gender personalizes greeting while inclusive choices use the name`() {
        assertEquals(
            "Dhiren Sir",
            OwnerProfile("Dhiren", GenderOption.Man, PronounOption.HeHim).greetingName,
        )
        assertEquals(
            "Aisha Ma'am",
            OwnerProfile("Aisha", GenderOption.Woman, PronounOption.SheHer).greetingName,
        )
        assertEquals(
            "Sam",
            OwnerProfile("Sam", GenderOption.NonBinary, PronounOption.TheyThem).greetingName,
        )
        assertEquals(
            "Alex",
            OwnerProfile("Alex", GenderOption.PreferNotToSay, PronounOption.UseMyName).greetingName,
        )
    }

    @Test
    fun `profile update normalizes name and refreshes device metadata`() = runBlocking {
        val original = ownerEntity(displayName = "Dhiren")
        val dao = FakeOwnerProfileDao(original)
        val repository = OwnerProfileRepository(
            ownerProfileDao = dao,
            clock = Clock.fixed(Instant.parse("2026-08-09T10:15:00Z"), ZoneOffset.UTC),
            zoneProvider = { ZoneId.of("Asia/Kolkata") },
            localeProvider = { Locale.forLanguageTag("en-IN") },
        )

        repository.update(
            displayName = "  Dhiren   Singh  ",
            gender = GenderOption.Man,
            pronouns = PronounOption.HeHim,
        )

        val stored = requireNotNull(dao.getOwner())
        assertEquals("Dhiren Singh", stored.displayName)
        assertEquals("Man", stored.gender)
        assertEquals("HeHim", stored.pronouns)
        assertEquals("Asia/Kolkata", stored.timezone)
        assertEquals("en-IN", stored.locale)
        assertEquals(original.createdAtUtc, stored.createdAtUtc)
        assertEquals("2026-08-09T10:15:00Z", stored.updatedAtUtc)
        assertEquals("Dhiren Singh Sir", repository.profile.first().greetingName)
    }

    private fun ownerEntity(displayName: String) = OwnerProfileEntity(
        displayName = displayName,
        gender = GenderOption.Man.name,
        pronouns = PronounOption.HeHim.name,
        timezone = "UTC",
        locale = "en",
        createdAtUtc = "2026-08-01T00:00:00Z",
        updatedAtUtc = "2026-08-01T00:00:00Z",
    )

    private class FakeOwnerProfileDao(initial: OwnerProfileEntity?) : OwnerProfileDao {
        private val owner = MutableStateFlow(initial)

        override fun observeOwner(): Flow<OwnerProfileEntity?> = owner

        override suspend fun getOwner(): OwnerProfileEntity? = owner.value

        override suspend fun upsert(owner: OwnerProfileEntity) {
            this.owner.value = owner
        }
    }
}
