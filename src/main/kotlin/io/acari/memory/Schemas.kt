package io.acari.memory

object UserSchema {
  const val COLLECTION: String = "user"
  const val TIME_CREATED: String = "timeCreated" // long
  const val OAUTH_IDENTIFIERS: String = "identifiers" // array of string
  const val GLOBAL_IDENTIFIER: String = "guid" // string
}

object SagaSchema {
  const val COLLECTION: String = "saga"
  const val GLOBAL_IDENTIFIER: String = "guid" // string
  const val TIME_MARKER: String = "timeMarker" // long
  const val EVENTS: String = "events" // array of string
}

object EventSchema {
  const val COLLECTION: String = "event"
  const val TIME_CREATED: String = "timeCreated"
  const val TYPE: String = ""
}
