import chalk from 'chalk';
import {Router} from 'express';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {
  CachedTacticalActivity,
  createTacticalActivity,
  deleteTacticalActivity,
  findTacticalActivities,
  massUpdateTacticalActivities,
  TacticalActivity,
  updateTacticalActivity,
  uploadTacticalActivities,
} from '../service/TacticalActivity';
import {logger} from '../utils/Utils';

const tacticalActivityRoutes = Router();

tacticalActivityRoutes.get('/', (req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findTacticalActivities(userIdentifier)
    .subscribe(tacticalActivities => {
      res.status(200)
        .contentType(APPLICATION_JSON)
        .send(tacticalActivities);
    }, error => {
      logger.error(`Unable to get tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

tacticalActivityRoutes.post('/bulk', (req, res) => {
  const objectives = req.body as CachedTacticalActivity[];
  const userIdentifier = req.header(USER_IDENTIFIER);

  uploadTacticalActivities(objectives, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to bulk upload tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

tacticalActivityRoutes.post('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  createTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to create tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

tacticalActivityRoutes.put('/bulk', (req, res) => {
  const tacticalActivities = req.body as TacticalActivity[];
  const userIdentifier = req.header(USER_IDENTIFIER);
  massUpdateTacticalActivities(tacticalActivities, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to bulk update tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

tacticalActivityRoutes.put('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  updateTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to update tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

tacticalActivityRoutes.delete('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  deleteTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to remove tactical activities for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
});

export default tacticalActivityRoutes;
