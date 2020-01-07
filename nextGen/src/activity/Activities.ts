import {Db} from 'mongodb';
import {Observable} from 'rxjs';
import {defaultIfEmpty, map, mergeMap} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {dispatchEffect} from '../effects/Dispatch';
import {ActivityHistorySchema, CurrentActivitySchema, UserSchema} from '../memory/Schemas';
import {EventTypes} from '../models/EventTypes';
import {getConnection} from '../Mongo';
import {mongoToObservable, toObservable} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';
import {writePomodoroCount} from './Pomodoro';

export interface StoredCurrentActivity {
  guid: string;
  current: Activity;
  previous: Activity;
}

export enum ActivityType {
  ACTIVE = 'ACTIVE',
  PASSIVE = 'PASSIVE',
  NA = 'NA',
}

export enum ActivityStrategy {
  GENERIC = 'GENERIC',
}

export enum ActivityTimedType {
  NA = 'NA',
  NONE = 'NONE',
  TIMER = 'TIMER',
  STOP_WATCH = 'STOP_WATCH',
}

export interface CachedActivity {
  uploadType: EventTypes.CREATED | EventTypes.UPDATED | EventTypes.DELETED;
  activity: Activity;
}

export interface ActivityContent {
  uuid: string;
  name: string;
  timedType: ActivityTimedType;
  type: ActivityType;
  paused?: boolean;
  autoStart?: boolean;
  veryFirstActivity?: boolean;
  activityID?: string;
  duration?: number;
  workStartedWomboCombo?: number;
}

export interface Activity {
  antecedenceTime: number;
  content: ActivityContent;
  guid: string;
}

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
