package com.severett.b52.model

import com.severett.b52.serde.BigDecimalSerializer
import com.severett.b52.serde.InstantSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable
data class Transaction(
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant
) {
    init {
        require(!timestamp.isAfter(Instant.now()))
    }
}
