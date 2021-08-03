package com.severett.b52.serde

import com.severett.b52.exception.JsonParsingException
import com.severett.b52.model.Transaction
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import java.math.BigDecimal
import java.time.Instant

class TransactionSerializer : KSerializer<Transaction> {
    private val bigDecimalSerializer = BigDecimalSerializer()
    private val instantSerializer = InstantSerializer()

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Transaction") {
        element("amount", bigDecimalSerializer.descriptor)
        element("timestamp", instantSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: Transaction) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, AMOUNT_INDEX, bigDecimalSerializer, value.amount)
            encodeSerializableElement(descriptor, TIMESTAMP_INDEX, instantSerializer, value.timestamp)
        }
    }

    override fun deserialize(decoder: Decoder): Transaction {
        return decoder.decodeStructure(descriptor) {
            var rawAmount: BigDecimal? = null
            var rawTimestamp: Instant? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    AMOUNT_INDEX -> {
                        try {
                            rawAmount = decodeSerializableElement(descriptor, index, bigDecimalSerializer)
                        } catch (e: Exception) {
                            throw JsonParsingException(e.message ?: "Unknown cause")
                        }
                    }
                    TIMESTAMP_INDEX -> {
                        try {
                            rawTimestamp = decodeSerializableElement(descriptor, index, instantSerializer)
                        } catch (e: Exception) {
                            throw JsonParsingException(e.message ?: "Unknown cause")
                        }
                        if (Instant.now().isBefore(rawTimestamp)) {
                            throw JsonParsingException("Field 'timestamp' must not be in the future")
                        }
                    }
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            val amount = rawAmount ?: throw JsonParsingException("Field 'amount' missing")
            val timestamp = rawTimestamp ?: throw JsonParsingException("Field 'timestamp' missing")
            Transaction(amount, timestamp)
        }
    }

    private companion object {
        private const val AMOUNT_INDEX = 0
        private const val TIMESTAMP_INDEX = 1
    }
}
