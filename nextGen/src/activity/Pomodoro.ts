import {ReplaceWriteOpResult} from 'mongodb';
import {Observable} from 'rxjs';
import {defaultIfEmpty, map, mergeMap} from 'rxjs/operators';
import {PomodoroCompletionHistorySchema} from '../memory/Schemas';
import {findOne, performUpdate} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';
import {Activity} from './Activities';

const ONE_DAY = 86400000;

interface SavedPomodoro {
  guid: string;
  day: number;
  count: number;
}

export const writePomodoroCount = (activity: Activity): Observable<Activity> => {
  const currentDay = Math.floor(rightMeow() / ONE_DAY);
  const query = {
    [PomodoroCompletionHistorySchema.GLOBAL_USER_IDENTIFIER]: activity.guid,
    [PomodoroCompletionHistorySchema.DAY]: currentDay,
  };
  return findOne<SavedPomodoro>(((db, mongoCallback) =>
      db.collection(PomodoroCompletionHistorySchema.COLLECTION)
        .findOne(query, mongoCallback)
  ))
    .pipe(
      map(savedPomo => ({
        ...savedPomo,
        count: savedPomo.count + 1,
      })),
      defaultIfEmpty({
        guid: activity.guid,
        day: currentDay,
        count: 1,
      }),
      mergeMap(savedPomo =>
        performUpdate<SavedPomodoro, ReplaceWriteOpResult>((db, callBackSupplier) =>
          db.collection(PomodoroCompletionHistorySchema.COLLECTION)
            .replaceOne(query, savedPomo, {upsert: true}, callBackSupplier(savedPomo)),
        )),
      map(_ => activity),
    )
    ;
};
