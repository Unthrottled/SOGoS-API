package io.acari.util

import io.reactivex.Single
import java.util.*

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

fun <T> T?.toSingle(): Single<T?> = Single.just(this)

