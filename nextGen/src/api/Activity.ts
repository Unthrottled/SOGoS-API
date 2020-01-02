import {Router} from 'express';
import {filter, map, throwIfEmpty} from 'rxjs/operators';
import {Activity, startActivity, StoredCurrentActivity} from '../activity/Activities';
import {CurrentActivitySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {rightMeow} from '../utils/Utils';

const activityRoutes = Router();

export const STARTED_ACTIVITY = 'STARTED_ACTIVITY';
export const REMOVED_ACTIVITY = 'REMOVED_ACTIVITY';
export const UPDATED_ACTIVITY = 'UPDATED_ACTIVITY';

export const CREATED = 'CREATED';
export const UPDATED = 'UPDATED';
export const DELETED = 'DELETED';

const uploadStatus = {
  CREATED,
  UPDATED,
  DELETED,
};

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
    throwIfEmpty(() => new NoResultsError()),
  );

const getCurrentActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity => {
    if (!!storedCurrentActivity.current) {
      return storedCurrentActivity.current;
    } else {
      return storedCurrentActivity; // todo remove once data is less janky
    }
  };
  return findActivity(userIdentifier, transformer);
};

const getPreviousActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity => {
    return storedCurrentActivity.previous;
  };
  return findActivity(userIdentifier, transformer);
};

activityRoutes.get('/current', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  getCurrentActivity(userIdentifier)
    .subscribe(activity => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(activity);
    }, error => {
      // todo: log
      res.send(500);
    });
}));

activityRoutes.get('/previous', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  getPreviousActivity(userIdentifier)
    .subscribe(activity => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(activity);
    }, error => {
      // todo: log
      if (error instanceof NoResultsError) {
        res.send(404);
      } else {
        res.send(500);
      }
    });
}));

activityRoutes.post('/bulk', ((req, res) => {

}));

activityRoutes.post('/', ((req, res) => {
  const body = req.body;
  const meow = rightMeow();
  const userIdentifier = req.header(USER_IDENTIFIER);
  const activity: Activity = {
    antecedenceTime: meow,
    guid: userIdentifier,
    content: body.content,
  };
  startActivity(activity)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log
      res.send(500);
    });
}));

activityRoutes.get('/pomodoro/count', ((req, res) => {

}));

export default activityRoutes;
