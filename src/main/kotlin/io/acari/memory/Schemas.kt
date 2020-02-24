package io.acari.memory

object UserSchema {
  const val COLLECTION: String = "user"
  const val TIME_CREATED: String = "timeCreated" // long
  const val OAUTH_IDENTIFIERS: String = "identifiers" // array of string
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val MISC_USER_THINGS: String = "misc" // jsonObject
}

object EffectSchema {
  const val COLLECTION: String = "effect"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val TIME_CREATED: String = "timeCreated"
  const val TIME_OF_ANTECEDENCE: String = "antecedenceTime"
  const val NAME: String = "name"
  const val META: String = "meta"
  const val CONTENT: String = "content"
}

object CurrentActivitySchema {
  const val COLLECTION: String = "activity"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val TIME_OF_ANTECEDENCE: String = "antecedenceTime"
  const val CONTENT: String = "content"
  const val CURRENT: String = "current"
  const val PREVIOUS: String = "previous"
}

object CurrentObjectiveSchema {
  const val COLLECTION: String = "objective"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val OBJECTIVES: String = "objectives"
}

object ObjectiveHistorySchema {
  const val COLLECTION: String = "objectiveHistory"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val OBJECTIVES: String = "objectives"
  const val IDENTIFIER: String = "id"
}

object TacticalActivitySchema {
  const val COLLECTION: String = "tacticalActivity"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val IDENTIFIER: String = "id"
  const val CONTENT: String = "content"
  const val REMOVED: String = "removed"
}

object TacticalSettingsSchema {
  const val COLLECTION: String = "tacticalSettings"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val POMODORO_SETTINGS: String = "pomodoroSettings" // document
}

object ActivityHistorySchema {
  const val COLLECTION: String = "history"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val TIME_OF_ANTECEDENCE: String = "antecedenceTime"
  const val CONTENT: String = "content"
}

@Deprecated("Not used anymore because of timezones being a bitch",
  replaceWith = ReplaceWith("PomodoroHistorySchema"))
object PomodoroCompletionHistorySchema {
  const val COLLECTION: String = "pomodoro"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val DAY: String = "day"
  const val COUNT: String = "count"
}

object PomodoroHistorySchema {
  const val COLLECTION: String = "pomodoroHistory"
  const val GLOBAL_USER_IDENTIFIER: String = "guid" // string
  const val TIME_OF_ANTECEDENCE: String = "antecedenceTime"
  const val ACTIVITY_ID: String = "activityId"
}
