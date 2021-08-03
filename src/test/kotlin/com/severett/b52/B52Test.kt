package com.severett.b52

import com.severett.b52.services.TransactionService
import com.severett.b52.util.toDateTimeString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream
import kotlin.concurrent.thread

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class B52Test(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val transactionService: TransactionService
) {

    @AfterEach
    fun afterEach() {
        // Clear out the stored transaction data
        transactionService.deleteTransactions()
    }

    @Test
    fun testAddTransactions() {
        val baseTimestamp = Instant.now()
        val transactionsList = ArrayList<Pair<Double, String>>().apply {
            val timestampOne = baseTimestamp.minusSeconds(5).toDateTimeString()
            add(10.0 to timestampOne)
            add(15.0 to timestampOne)
            add(-5.0 to baseTimestamp.minusSeconds(11).toDateTimeString())
            val timestampTwo = baseTimestamp.minusSeconds(25).toDateTimeString()
            add(20000.256525 to timestampTwo)
            add(2525.10101 to timestampTwo)
        }
        transactionsList.map { (amount, timestamp) ->
            // Simulate multiple requests arriving simultaneously
            thread {
                mockMvc.post("/transactions") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"amount":"$amount","timestamp":"$timestamp"}"""
                }.andExpect { status().isCreated }
            }
        }.forEach(Thread::join)
        mockMvc.get("/statistics") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            content { contentType(MediaType.APPLICATION_JSON) }
            content {
                json(
                    """{"sum":"22545.36","avg":"4509.07","max":"20000.26","min":"-5.00","count":5}""",
                    strict = true
                )
            }
        }
    }

    @Test
    fun testExpiringTransaction() {
        val validTimestamp = Instant.now().minusSeconds(55).toDateTimeString()
        mockMvc.post("/transactions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount":"12345.678","timestamp":"$validTimestamp"}"""
        }.andExpect { status().isCreated }
        // Waiting for the transaction posted above to "expire"
        Thread.sleep(8 * 1000)
        checkNoStatistics()
    }

    @Test
    fun testAddExpiredTransaction() {
        val expiredTimestamp = Instant.now().minus(5, ChronoUnit.DAYS).toDateTimeString()
        mockMvc.post("/transactions") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"amount":"12345.678","timestamp":"$expiredTimestamp"}"""
        }.andExpect { status().isNoContent }
        checkNoStatistics()
    }

    @Test
    fun testDeleteTransactions() {
        val timestamp = Instant.now().minusSeconds(5).toDateTimeString()
        repeat(5) {
            mockMvc.post("/transactions") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"amount":"12345.678","timestamp":"$timestamp"}"""
            }.andExpect { status().isCreated }
        }
        mockMvc.delete("/transactions").andExpect { status { isNoContent() } }
        checkNoStatistics()
    }

    @Test
    fun testInvalidJSONStructure() {
        mockMvc.post("/transactions") {
            contentType = MediaType.APPLICATION_JSON
            content = "INVALID_JSON_STRUCTURE"
        }.andExpect { status { isBadRequest() } }
        checkNoStatistics()
    }

    @ParameterizedTest
    @MethodSource("genInvalidTransactionData")
    fun testInvalidTransactionData(amountStr: String, timestampStr: String) {
        val paramsList = ArrayList<String>().apply {
            if (amountStr.isNotEmpty()) {
                add("\"amount\":$amountStr")
            }
            if (timestampStr.isNotEmpty()) {
                add("\"timestamp\":$timestampStr")
            }
        }
        val requestContent = "{${paramsList.joinToString(",")}}"
        mockMvc.post("/transactions") {
            contentType = MediaType.APPLICATION_JSON
            content = requestContent
        }.andExpect { status { isUnprocessableEntity() } }
        checkNoStatistics()
    }


    private fun genInvalidTransactionData(): Stream<Arguments> {
        val validAmount = "12345.678"
        val validTimestamp = Instant.now().minusSeconds(5).toDateTimeString()
        val tomorrowTimestamp = Instant.now()
            .plus(1, ChronoUnit.DAYS)
            .toDateTimeString()
        return Stream.of(
            Arguments.of(validAmount, ""),
            Arguments.of(validAmount, "null"),
            Arguments.of(validAmount, "5"),
            Arguments.of(validAmount, "\"01/01/2021\""),
            Arguments.of(validAmount, "\"$tomorrowTimestamp\""),
            Arguments.of("", validTimestamp),
            Arguments.of("null", validTimestamp),
            Arguments.of("\"NON_NUMBER\"", validTimestamp)
        )
    }

    private fun checkNoStatistics() {
        mockMvc.get("/statistics") {
            accept = MediaType.APPLICATION_JSON
        }.andExpect {
            content { contentType(MediaType.APPLICATION_JSON) }
            content { json(EMPTY_RESPONSE, strict = true) }
        }
    }

    private companion object {
        private const val EMPTY_RESPONSE = """{"sum":"0.00","avg":"0.00","max":"0.00","min":"0.00","count":0}"""
    }
}