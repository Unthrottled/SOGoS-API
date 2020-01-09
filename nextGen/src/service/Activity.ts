import {defaultIfEmpty, filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {Activity, CachedActivity, startActivity, StoredCurrentActivity} from '../activity/Activities';
import {findPomodoro} from '../activity/Pomodoro';
import {CurrentActivitySchema, PomodoroCompletionHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {findOne} from '../rxjs/Convience';

const findActivity = (
  userIdentifier: string,
  transformer: (storedCurrentActivity: StoredCurrentActivity) => (Activity),
) => findOne<StoredCurrentActivity>((db, mongoCallback) => {
  db.collection(CurrentActivitySchema.COLLECTION)
    .findOne({
      [CurrentActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
    }, mongoCallback);
})
  .pipe(
    map(transformer),
    filter(Boolean),
    map((activity: Activity) => ({
      antecedenceTime: activity.antecedenceTime,
      content: activity.content,
    })),
    throwIfEmpty(() => new NoResultsError('No Activity Found')),
  );
export const getCurrentActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity =>
    storedCurrentActivity.current;
  return findActivity(userIdentifier, transformer);
};

export const getPreviousActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity =>
    storedCurrentActivity.previous;
  return findActivity(userIdentifier, transformer);
};

export const uploadActivities = (body: CachedActivity[], userIdentifier: string) =>
  body.filter(cachedActivity => cachedActivity.uploadType === EventTypes.CREATED)
  .map(cachedActivity => cachedActivity.activity)
  .map(activity => startActivity({
    antecedenceTime: activity.antecedenceTime,
    guid: userIdentifier,
    content: activity.content,
  }))
  .reduce((accum, next) =>
    accum.pipe(mergeMap(_ => next)));

export const findPomodoroCount = (userIdentifier: string) => findPomodoro(userIdentifier).execution
  .pipe(
    map(savedPomo => savedPomo.count),
    defaultIfEmpty(0),
    map(count => ({
      [PomodoroCompletionHistorySchema.COUNT]: count,
    })),
  );
export const STARTED_ACTIVITY = 'STARTED_ACTIVITY';
