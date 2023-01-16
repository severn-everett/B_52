package com.severett.b52

import com.severett.b52.services.TransactionService
import com.severett.b52.util.toDateTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

private const val EMPTY_RESPONSE = """{"sum":"0.00","avg":"0.00","max":"0.00","min":"0.00","count":0}"""

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebFlux
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class B52Test(
    @Autowired private val webTestClient: WebTestClient,
    @Autowired private val transactionService: TransactionService
) {

    @AfterEach
    fun afterEach() {
        // Clear out the stored transaction data
        transactionService.deleteTransactions()
    }

    @Test
    fun testAddTransactions(): Unit = runBlocking {
        val baseTimestamp = Instant.now()
        val transactionsList = buildList {
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
            launch {
                webTestClient.post()
                    .uri("/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue("""{"amount":"$amount","timestamp":"$timestamp"}"""))
                    .exchange()
                    .expectStatus().isCreated
            }
        }.forEach { it.join() }
        webTestClient.get()
            .uri("/statistics")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody()
            .json("""{"sum":"22545.36","avg":"4509.07","max":"20000.26","min":"-5.00","count":5}""")
    }

    @Test
    fun testExpiringTransaction(): Unit = runBlocking {
        val validTimestamp = Instant.now().minusSeconds(55).toDateTimeString()
        webTestClient.post()
            .uri("/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("""{"amount":"12345.678","timestamp":"$validTimestamp"}"""))
            .exchange()
        // Waiting for the transaction posted above to "expire"
        delay(8 * 1000)
        checkNoStatistics()
    }

    @Test
    fun testAddExpiredTransaction() {
        val expiredTimestamp = Instant.now().minus(5, ChronoUnit.DAYS).toDateTimeString()
        webTestClient.post()
            .uri("/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("""{"amount":"12345.678","timestamp":"$expiredTimestamp"}"""))
            .exchange()
            .expectStatus().isNoContent
        checkNoStatistics()
    }

    @Test
    fun testDeleteTransactions() {
        val timestamp = Instant.now().minusSeconds(5).toDateTimeString()
        repeat(5) {
            webTestClient.post()
                .uri("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue("""{"amount":"12345.678","timestamp":"$timestamp"}"""))
                .exchange()
                .expectStatus().isCreated
        }
        webTestClient.delete()
            .uri("/transactions")
            .exchange()
            .expectStatus().isNoContent
        checkNoStatistics()
    }

    @Test
    fun testInvalidJSONStructure() {
        webTestClient.post()
            .uri("/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue("INVALID_JSON_STRUCTURE"))
            .exchange()
            .expectStatus().isBadRequest
        checkNoStatistics()
    }

    @ParameterizedTest
    @MethodSource("genInvalidTransactionData")
    fun testInvalidTransactionData(amountStr: String, timestampStr: String) {
        val paramsList = buildList {
            if (amountStr.isNotEmpty()) {
                add("\"amount\":$amountStr")
            }
            if (timestampStr.isNotEmpty()) {
                add("\"timestamp\":$timestampStr")
            }
        }
        val requestContent = "{${paramsList.joinToString(",")}}"
        webTestClient.post()
            .uri("/transactions")
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(requestContent))
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
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
        webTestClient.get()
            .uri("/statistics")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectBody().json(EMPTY_RESPONSE)
    }
}