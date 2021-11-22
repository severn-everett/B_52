package com.severett.b52.model

import java.math.BigDecimal

@JvmRecord
data class SecondStatistics(val sum: BigDecimal, val max: BigDecimal?, val min: BigDecimal?, val count: Long)
