import {Observable} from 'rxjs';
import {defaultIfEmpty, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {createEffect} from '../effects/Dispatch';
import {TacticalSettingsSchema} from '../memory/Schemas';
import {findOne, performUpdate} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';

export interface PomodoroSettings {
    loadDuration: number; // milliseconds
    shortRecoveryDuration: number;
    longRecoveryDuration: number;
}

export interface StoredPomodoroSettings {
    guid: string;
    pomodoroSettings: PomodoroSettings;
}

interface SavedPomoSettings {
    guid: string;
    pomodoroSettings: PomodoroSettings;
}

const UPDATED_POMODORO_SETTINGS = 'UPDATED_POMODORO_SETTINGS';

export const createOrUpdatePomodoroSettings = (
    pomoSettings: PomodoroSettings,
    userIdentifier: string,
): Observable<SavedPomoSettings> => {
    const savPomoSett: SavedPomoSettings = {
        guid: userIdentifier, pomodoroSettings: pomoSettings,
    };
    return performUpdate<SavedPomoSettings, any>((db, callBackSupplier) =>
        db.collection(TacticalSettingsSchema.COLLECTION)
            .replaceOne({
                [TacticalSettingsSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
            }, savPomoSett, {upsert: true}, callBackSupplier(savPomoSett)),
    ).pipe(
        mergeMap(savedPomoSettings => {
            return createEffect({
                timeCreated: rightMeow(),
                guid: userIdentifier,
                antecedenceTime: rightMeow(),
                name: UPDATED_POMODORO_SETTINGS,
                content: pomoSettings,
                meta: {},
            }).pipe(
                map(_ => savedPomoSettings),
                throwIfEmpty(() => Error('Create/update pomo was empty for some rasin')),
            );
        }),
    );
};

export const getPomodoroSettings = (userIdentifier: string) =>
  findOne<StoredPomodoroSettings>((db, mongoCallback) =>
    db.collection(TacticalSettingsSchema.COLLECTION)
      .findOne({
        [TacticalSettingsSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
      }, mongoCallback),
  )
    .pipe(
      map(storedPomo => storedPomo.pomodoroSettings),
      defaultIfEmpty({}),
    );
