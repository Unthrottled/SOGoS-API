import {Router} from 'express';
import {EMPTY, Observable} from 'rxjs';
import {defaultIfEmpty, map, throwIfEmpty} from 'rxjs/operators';
import {TacticalSettingsSchema} from '../memory/Schemas';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import tacticalActivityRoutes from '../tactical/TacticalActivity';

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

const createOrUpdatePomodoroSettings = (
  pomoSettings: PomodoroSettings,
  userIdentifier: string,
): Observable<PomodoroSettings> => {
  return EMPTY; // todo: dis
};

tacticalRoutes.post('/pomodoro/settings', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  const pomoSettings: PomodoroSettings = req.body;
  createOrUpdatePomodoroSettings(pomoSettings, userIdentifier)
    .pipe(
      throwIfEmpty(()=> Error('Create/update pomo was empty for some rasin')),
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
