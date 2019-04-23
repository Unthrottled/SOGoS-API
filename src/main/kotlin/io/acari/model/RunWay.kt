package io.acari.model

import java.time.Instant

data class TestObject(val message: String, val issuedDate: Instant = Instant.now())
