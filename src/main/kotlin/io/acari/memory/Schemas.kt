package io.acari.memory

object UserSchema {
  const val COLLECTION: String = "user"
  const val TIME_CREATED: String = "timeCreated" // long
  const val OAUTH_IDENTIFIERS: String = "identifiers" // array of string
  const val GLOBAL_IDENTIFIER: String = "guid" // string
}

object EffectSchema {
  const val COLLECTION: String = "effect"
  const val GLOBAL_IDENTIFIER: String = "guid" // string
  const val TIME_CREATED: String = "timeCreated"
  const val NAME: String = "name"
  const val META: String = "meta"
  const val CONTENT: String = "content"
}
