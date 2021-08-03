package com.severett.b52.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter

@Configuration
open class KotlinSerializationConfiguration {

    @Bean
    open fun configBean(): KotlinSerializationJsonHttpMessageConverter {
        val jsonConfig = Json { ignoreUnknownKeys = true }
        return KotlinSerializationJsonHttpMessageConverter(jsonConfig)
    }
}
