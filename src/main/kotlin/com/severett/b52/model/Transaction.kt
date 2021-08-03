package com.severett.b52.model

import com.severett.b52.serde.TransactionSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.Instant

@Serializable(with = TransactionSerializer::class)
data class Transaction(val amount: BigDecimal, val timestamp: Instant) {
    init {
        require(!timestamp.isAfter(Instant.now()))
    }
}
