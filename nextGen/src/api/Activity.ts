import chalk from 'chalk';
import {Router} from 'express';
import {defaultIfEmpty, filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {Activity, CachedActivity, startActivity, StoredCurrentActivity} from '../activity/Activities';
import {findPomodoro} from '../activity/Pomodoro';
import {CurrentActivitySchema, PomodoroCompletionHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {logger, rightMeow} from '../utils/Utils';

const activityRoutes = Router();

export const STARTED_ACTIVITY = 'STARTED_ACTIVITY';

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

const getCurrentActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity =>
    storedCurrentActivity.current;
  return findActivity(userIdentifier, transformer);
};

const getPreviousActivity = (userIdentifier: string) => {
  const transformer = storedCurrentActivity =>
    storedCurrentActivity.previous;
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
      logger.error(`Unable to get current activity for ${chalk.green(userIdentifier)} for reasons ${error}`);
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
      if (error instanceof NoResultsError) {
        res.send(404);
      } else {
        logger.error(`Unable to get previous activity for ${chalk.green(userIdentifier)} for reasons ${error}`);
        res.send(500);
      }
    });
}));

activityRoutes.post('/bulk', ((req, res) => {
  const body = req.body as CachedActivity[];
  const userIdentifier = req.header(USER_IDENTIFIER);
  body.filter(cachedActivity => cachedActivity.uploadType === EventTypes.CREATED)
    .map(cachedActivity => cachedActivity.activity)
    .map(activity => startActivity({
      antecedenceTime: activity.antecedenceTime,
      guid: userIdentifier,
      content: activity.content,
    }))
    .reduce((accum, next) =>
      accum.pipe(mergeMap(_ => next)))
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to get bulk upload activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    })
  ;
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
      logger.error(`Unable to get start activity for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

activityRoutes.get('/pomodoro/count', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findPomodoro(userIdentifier).execution
    .pipe(
      map(savedPomo => savedPomo.count),
      defaultIfEmpty(0),
      map(count => ({
        [PomodoroCompletionHistorySchema.COUNT]: count,
      })),
    ).subscribe(pomoCount => {
    res.contentType(APPLICATION_JSON)
      .status(200)
      .send(pomoCount);
  }, error => {
    logger.error(`Unable to get current pomodoro count for ${chalk.green(userIdentifier)} for reasons ${error}`);
    res.send(500);
  });
}));

export default activityRoutes;
