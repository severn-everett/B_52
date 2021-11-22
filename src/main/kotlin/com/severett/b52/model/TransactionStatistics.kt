package com.severett.b52.model

import com.severett.b52.serde.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@JvmRecord
@Serializable
data class TransactionStatistics(
    @Serializable(with = BigDecimalSerializer::class)
    val sum: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val avg: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val max: BigDecimal,
    @Serializable(with = BigDecimalSerializer::class)
    val min: BigDecimal,
    val count: Long
)
