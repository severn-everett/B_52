package com.severett.b52.controller

import com.severett.b52.model.AddTransactionResult
import com.severett.b52.model.TransactionStatistics
import com.severett.b52.model.Transaction
import com.severett.b52.services.TransactionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Controller
class TransactionController(private val transactionService: TransactionService) {
    @RequestMapping(value = ["/transactions"], method = [RequestMethod.POST])
    fun addTransaction(transaction: Transaction): ResponseEntity<String> {
        return when (transactionService.addTransaction(transaction)) {
            AddTransactionResult.SUCCESS -> ResponseEntity(HttpStatus.ACCEPTED)
            AddTransactionResult.TRANSACTION_EXPIRED -> ResponseEntity(HttpStatus.NO_CONTENT)
        }
    }

    @RequestMapping(value = ["/statistics"], method = [RequestMethod.GET])
    fun getStatistics(): TransactionStatistics {
        return transactionService.getStatistics()
    }

    @RequestMapping(value = ["/transactions"], method = [RequestMethod.DELETE])
    fun deleteTransactions(): ResponseEntity<String> {
        transactionService.deleteTransactions()
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }
}
