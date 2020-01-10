import {Db} from 'mongodb';
import {Observable} from 'rxjs';
import {defaultIfEmpty, filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {dispatchEffect} from '../effects/Dispatch';
import {ActivityHistorySchema, CurrentActivitySchema, PomodoroCompletionHistorySchema} from '../memory/Schemas';
import {Activity, ActivityType, CachedActivity, StoredCurrentActivity} from '../models/Activities';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {getConnection} from '../Mongo';
import {findOne, mongoToObservable, toObservable} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';
import {findPomodoro, writePomodoroCount} from './Pomodoro';

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

export const startActivity = (activity: Activity): Observable<any> =>
  getConnection().pipe(
    mergeMap(db => commenceActivity(activity, db)),
  );
export const commenceActivity = (act: Activity, db: Db): Observable<any> =>
  toObservable(act)
    .pipe(
      mergeMap(activity => shouldTime(activity) ?
        updateCurrentActivity(activity, db) : toObservable(activity),
      ),
      mergeMap(activity => writeActivityLog(activity, db)),
      dispatchEffect<Activity>(db, activity => ({
        guid: activity.guid,
        timeCreated: rightMeow(),
        antecedenceTime: activity.antecedenceTime,
        name: 'STARTED_ACTIVITY',
        content: activity.content,
        meta: {},
      })),
      mergeMap(activity => {
        if (activity.content.name === 'RECOVERY' &&
          !!activity.content.autoStart) {
          return writePomodoroCount(activity);
        } else {
          return toObservable(activity);
        }
      }),
    );
const updateCurrentActivity = (newActivity: Activity, db: Db): Observable<Activity> =>
  findCurrentActivity(newActivity.guid, db)
    .pipe(
      mergeMap(possiblyPreviousActivity =>
        isOlder(possiblyPreviousActivity, newActivity) ?
          writeNewCurrentActivity(
            possiblyPreviousActivity,
            newActivity,
            db,
          ) :
          toObservable(newActivity),
      ),
    );
const writeNewCurrentActivity = (
  previousActivity: Activity,
  newActivity: Activity,
  db: Db): Observable<Activity> =>
  mongoToObservable(callBack => {
      const stuffToSave: StoredCurrentActivity = {
        current: newActivity,
        guid: newActivity.guid,
        previous: previousActivity,
      };
      db.collection(CurrentActivitySchema.COLLECTION)
        .replaceOne({
          [CurrentActivitySchema.GLOBAL_USER_IDENTIFIER]: newActivity.guid,
        }, stuffToSave, {upsert: true}, callBack);
    },
  ).pipe(
    map(_ => newActivity),
  );
const isOlder = (left: Activity, right: Activity) =>
  left && left.antecedenceTime < right.antecedenceTime;
export const findCurrentActivity = (
  globalUserIdentifier: string,
  db: Db,
): Observable<Activity> =>
  mongoToObservable<StoredCurrentActivity>(callBack =>
    db.collection(CurrentActivitySchema.COLLECTION)
      .findOne({
        guid: globalUserIdentifier,
      }, callBack),
  ).pipe(
    map(storedCurrentActivity => storedCurrentActivity.current),
    defaultIfEmpty(),
  );
const shouldTime = (activity: Activity) => {
  return activity.content &&
    activity.content.type &&
    activity.content.type === ActivityType.ACTIVE;
};
const writeActivityLog = (activity: Activity, db: Db): Observable<Activity> =>
  mongoToObservable(callBack =>
    db.collection(ActivityHistorySchema.COLLECTION)
      .insertOne(activity, callBack),
  ).pipe(map(_ => activity));
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
