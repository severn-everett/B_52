package com.severett.b52.util

import java.time.Instant
import java.time.format.DateTimeFormatter

fun Instant.toDateTimeString(): String = DateTimeFormatter.ISO_INSTANT.format(this)
