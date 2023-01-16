package com.severett.b52.model

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

class SecondBucket {
    private val mutex = Mutex()
    private var sum = BigDecimal.ZERO
    private lateinit var max: BigDecimal
    private lateinit var min: BigDecimal
    private var count = 0L

    suspend fun addTransaction(transaction: Transaction) {
        mutex.withLock {
            logger.debug { "Received Transaction: $transaction" }
            val transactionAmt = transaction.amount
            if (count == 0L) {
                max = transactionAmt
                min = transactionAmt
            } else {
                if (max compareTo transactionAmt == -1) {
                    max = transactionAmt
                }
                if (min compareTo transactionAmt == 1) {
                    min = transactionAmt
                }
            }
            sum = sum.add(transactionAmt)
            count++
        }
    }

    suspend fun getStatistics(): SecondStatistics {
        return mutex.withLock {
            SecondStatistics(
                sum = sum,
                max = if (this::max.isInitialized) max else null,
                min = if (this::min.isInitialized) min else null,
                count = count
            )
        }
    }
}
