import chalk from 'chalk';
import {Router} from 'express';
import {Activity, CachedActivity} from '../models/Activities';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {
  findPomodoroCount,
  getCurrentActivity,
  getPreviousActivity,
  startActivity,
  uploadActivities,
} from '../service/Activity';
import {logger, rightMeow} from '../utils/Utils';

const activityRoutes = Router();

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
  uploadActivities(body, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to get bulk upload activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
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
  findPomodoroCount(userIdentifier)
    .subscribe(pomoCount => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(pomoCount);
    }, error => {
      logger.error(`Unable to get current pomodoro count for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

export default activityRoutes;
