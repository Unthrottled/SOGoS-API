import {ReplaceWriteOpResult} from 'mongodb';
import {Observable} from 'rxjs';
import {defaultIfEmpty, map, mergeMap} from 'rxjs/operators';
import {PomodoroCompletionHistorySchema} from '../memory/Schemas';
import {Activity} from '../models/Activities';
import {SavedPomodoro} from '../models/Pomodoro';
import {findOne, performUpdate} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';

const ONE_DAY = 86400000;
export const findPomodoro = (userIdentifier: string) => {
    const currentDay = Math.floor(rightMeow() / ONE_DAY);
    const query = {
        [PomodoroCompletionHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
        [PomodoroCompletionHistorySchema.DAY]: currentDay,
    };
    const savedPomodoroObservable = findOne<SavedPomodoro>(((db, mongoCallback) =>
            db.collection(PomodoroCompletionHistorySchema.COLLECTION)
                .findOne(query, mongoCallback)
    ));
    return {
        currentDay,
        query,
        execution: savedPomodoroObservable,
    };
};
export const writePomodoroCount = (activity: Activity): Observable<Activity> => {
    const {currentDay, query, execution} = findPomodoro(activity.guid);
    return execution
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
        );
};
