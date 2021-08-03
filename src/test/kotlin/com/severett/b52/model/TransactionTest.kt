package com.severett.b52.model

import com.severett.b52.exception.JsonParsingException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionTest {

    @Test
    fun goodSerialization() {
        val timestamp = Instant.now()
        val amount = 12345.678
        val timestampStr = DateTimeFormatter.ISO_INSTANT.format(timestamp)
        val transaction = Json.decodeFromString<Transaction>(
            "{\"amount\":\"$amount\",\"timestamp\":\"$timestampStr\"}"
        )
        assertEquals(amount, transaction.amount.toDouble())
        assertEquals(timestamp, transaction.timestamp)
    }

    @Test
    fun invalidJSON() {
        assertThrows<SerializationException> { Json.decodeFromString<Transaction>("INVALID JSON") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "null", "\"NON_NUMBER\""])
    fun invalidAmount(amountStr: String) {
        val timestamp = Instant.now()
        val timestampStr = DateTimeFormatter.ISO_INSTANT.format(timestamp)
        assertThrows<JsonParsingException> {
            val rawStr = if (amountStr.isEmpty()) {
                "{\"timestamp\":\"$timestampStr\"}"
            } else {
                "{\"amount\":$amountStr,\"timestamp\":\"$timestampStr\"}"
            }
            Json.decodeFromString<Transaction>(rawStr)
        }
    }

    @ParameterizedTest
    @MethodSource("invalidTimestamp")
    fun invalidTimestamp(timestampStr: String) {
        val amount = 12345.678
        assertThrows<JsonParsingException> {
            val rawStr = if (timestampStr.isEmpty()) {
                "{\"amount\":$amount}"
            } else {
                "{\"amount\":$amount,\"timestamp\":$timestampStr}"
            }
            Json.decodeFromString<Transaction>(rawStr)
        }
    }

    private fun invalidTimestamp(): Stream<Arguments> {
        val tomorrowTimestamp = Instant.now().plus(1, ChronoUnit.DAYS)
        return Stream.of(
            Arguments.of(""),
            Arguments.of("null"),
            Arguments.of("5"),
            Arguments.of("\"01/01/2021\""),
            Arguments.of("\"${DateTimeFormatter.ISO_INSTANT.format(tomorrowTimestamp)}\"")
        )
    }
}
