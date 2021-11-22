package com.severett.b52.model

import mu.KLogging
import java.math.BigDecimal
import java.util.concurrent.Semaphore

class SecondBucket {
    private val mutex = Semaphore(1)
    private var sum = BigDecimal.ZERO
    private lateinit var max: BigDecimal
    private lateinit var min: BigDecimal
    private var count = 0L

    fun addTransaction(transaction: Transaction) {
        try {
            mutex.acquire()
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
        } finally {
            mutex.release()
        }
    }

    fun getStatistics(): SecondStatistics {
        try {
            mutex.acquire()
            return SecondStatistics(
                sum = sum,
                max = if (this::max.isInitialized) max else null,
                min = if (this::min.isInitialized) min else null,
                count = count
            )
        } finally {
            mutex.release()
        }
    }

    private companion object : KLogging()
}
