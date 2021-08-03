package com.severett.b52.services

import com.severett.b52.model.AddTransactionResult
import com.severett.b52.model.SecondBucket
import com.severett.b52.model.Transaction
import com.severett.b52.model.TransactionStatistics
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service
class TransactionService {
    private val transactions = ConcurrentHashMap<Long, SecondBucket>()

    fun addTransaction(transaction: Transaction): AddTransactionResult {
        if (transaction.timestamp.isBefore(Instant.now().minusSeconds(SECONDS_WINDOW))) {
            return AddTransactionResult.TRANSACTION_EXPIRED
        }
        transactions.computeIfAbsent(transaction.timestamp.epochSecond) { SecondBucket() }.addTransaction(transaction)
        return AddTransactionResult.SUCCESS
    }

    fun getStatistics(): TransactionStatistics {
        val currentTimestamp = Instant.now()
        var sum = BigDecimal.ZERO
        var max: BigDecimal? = null
        var min: BigDecimal? = null
        var count = 0L
        for (second in currentTimestamp.minusSeconds(SECONDS_WINDOW).epochSecond..currentTimestamp.epochSecond) {
            transactions[second]?.getStatistics()?.let { secondBucket ->
                sum = sum.add(secondBucket.sum)
                secondBucket.max?.let { secondMax ->
                    max = max?.let { currentMax ->
                        if (currentMax.compareTo(secondMax) == -1) secondMax else currentMax
                    } ?: secondMax
                }
                secondBucket.min?.let { secondMin ->
                    min = min?.let { currentMin ->
                        if (currentMin.compareTo(secondMin) == 1) secondMin else currentMin
                    } ?: secondMin
                }
                count += secondBucket.count
            }
        }
        val avg = if (count > 0L) sum.divide(BigDecimal.valueOf(count)) else BigDecimal.ZERO
        return TransactionStatistics(
            sum = sum,
            avg = avg,
            max = max ?: BigDecimal.ZERO,
            min = min ?: BigDecimal.ZERO,
            count = count
        )
    }

    fun deleteTransactions() {
        transactions.clear()
    }

    private companion object {
        private const val SECONDS_WINDOW = 59L
    }
}
