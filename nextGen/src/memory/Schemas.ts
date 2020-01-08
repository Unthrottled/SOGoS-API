export const UserSchema = {
 COLLECTION: 'user',
 TIME_CREATED: 'timeCreated', // long
 OAUTH_IDENTIFIERS: 'identifiers', // array of string
 GLOBAL_USER_IDENTIFIER: 'guid', // string
};

export const EffectSchema = {
 COLLECTION: 'effect',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 TIME_CREATED: 'timeCreated',
 TIME_OF_ANTECEDENCE: 'antecedenceTime',
 NAME: 'name',
 META: 'meta',
 CONTENT: 'content',
};

export const CurrentActivitySchema = {
 COLLECTION: 'activity',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 TIME_OF_ANTECEDENCE: 'antecedenceTime',
 CONTENT: 'content',
 CURRENT: 'current',
 PREVIOUS: 'previous',
};

export const CurrentObjectiveSchema = {
 COLLECTION: 'objective',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 OBJECTIVES: 'objectives',
};

export const ObjectiveHistorySchema = {
 COLLECTION: 'objectiveHistory',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 OBJECTIVES: 'objectives',
 IDENTIFIER: 'id',
};

export const TacticalActivitySchema = {
 COLLECTION: 'tacticalActivity',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 IDENTIFIER: 'id',
 CONTENT: 'content',
 REMOVED: 'removed',
};

export const TacticalSettingsSchema = {
 COLLECTION: 'tacticalSettings',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 POMODORO_SETTINGS: 'pomodoroSettings', // document
};

export const ActivityHistorySchema = {
 COLLECTION: 'history',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 TIME_OF_ANTECEDENCE: 'antecedenceTime',
 CONTENT: 'content',
};

export const PomodoroCompletionHistorySchema = {
 COLLECTION: 'pomodoro',
 GLOBAL_USER_IDENTIFIER: 'guid', // string
 DAY: 'day',
 COUNT: 'count',
};
