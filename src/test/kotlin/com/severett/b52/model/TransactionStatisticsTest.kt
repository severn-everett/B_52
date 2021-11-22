package com.severett.b52.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionStatisticsTest {
    @ParameterizedTest
    @MethodSource("statisticsSerialization")
    fun statisticsSerialization(statistics: TransactionStatistics, expectedOutput: String) {
        val serializedStats = Json.encodeToString(statistics)
        assertEquals(expectedOutput, serializedStats)
    }

    private fun statisticsSerialization(): Stream<Arguments> {
        return Stream.of(
            // Full Statistics
            Arguments.of(
                TransactionStatistics(
                    sum = BigDecimal.valueOf(1234.5678),
                    avg = BigDecimal.valueOf(43.455),
                    max = BigDecimal.valueOf(1000),
                    min = BigDecimal.valueOf(-155.251),
                    count = 10L
                ),
                """{"sum":"1234.57","avg":"43.45","max":"1000.00","min":"-155.25","count":10}"""
            ),
            // Empty Statistics
            Arguments.of(
                TransactionStatistics(
                    sum = BigDecimal.ZERO,
                    avg = BigDecimal.ZERO,
                    max = BigDecimal.ZERO,
                    min = BigDecimal.ZERO,
                    count = 0L
                ),
                """{"sum":"0.00","avg":"0.00","max":"0.00","min":"0.00","count":0}"""
            )
        )
    }
}
