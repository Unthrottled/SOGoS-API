package io.acari.util

import io.reactivex.Maybe
import io.reactivex.Single
import java.util.*

fun <T> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

fun <T> T?.toSingle(): Single<T?> = Single.just(this)
fun <T> T?.toMaybe(): Maybe<T?> = Maybe.just(this)
fun <T> T?.toSingletonList(): MutableList<T?> = Collections.singletonList(this)

