package io.acari.util

import java.util.*

fun <T> T?.toOptional() = Optional.ofNullable(this)
