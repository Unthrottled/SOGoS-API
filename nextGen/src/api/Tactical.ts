import {Router} from 'express';
import {Observable} from 'rxjs';
import {defaultIfEmpty, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {createEffect} from '../effects/Dispatch';
import {TacticalSettingsSchema} from '../memory/Schemas';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {findOne, performUpdate} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import tacticalActivityRoutes from '../tactical/TacticalActivity';
import {rightMeow} from '../utils/Utils';

const tacticalRoutes = Router();

tacticalRoutes.use('/activity', tacticalActivityRoutes);

export interface PomodoroSettings {
  loadDuration: number; // milliseconds
  shortRecoveryDuration: number;
  longRecoveryDuration: number;
}

interface StoredPomodoroSettings {
  guid: string;
  pomodoroSettings: PomodoroSettings;
}

tacticalRoutes.get('/pomodoro/settings', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findOne<StoredPomodoroSettings>((db, mongoCallback) =>
    db.collection(TacticalSettingsSchema.COLLECTION)
      .findOne({
        [TacticalSettingsSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
      }, mongoCallback),
  )
    .pipe(
      map(storedPomo => storedPomo.pomodoroSettings),
      defaultIfEmpty({}),
    )
    .subscribe(pomoSettings => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(pomoSettings);
    }, error => {
      // todo: log error
      res.send(500);
    });
}));

interface SavedPomoSettings {
  guid: string;
  pomodoroSettings: PomodoroSettings;
}

const UPDATED_POMODORO_SETTINGS = 'UPDATED_POMODORO_SETTINGS';

const createOrUpdatePomodoroSettings = (
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
        meta: {}, // todo: extract meta
      }).pipe(map(_ => savedPomoSettings));
    }),
  );
};

tacticalRoutes.post('/pomodoro/settings', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  const pomoSettings: PomodoroSettings = req.body;
  createOrUpdatePomodoroSettings(pomoSettings, userIdentifier)
    .pipe(
      throwIfEmpty(() => Error('Create/update pomo was empty for some rasin')),
    )
    .subscribe(pomoSettings => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(pomoSettings);
    }, error => {
      // todo: log error
      res.send(500);
    });
}));

export default tacticalRoutes;
