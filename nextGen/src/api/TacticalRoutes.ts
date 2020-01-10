import chalk from 'chalk';
import {Router} from 'express';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {createOrUpdatePomodoroSettings, getPomodoroSettings, PomodoroSettings} from '../service/Tactical';
import {logger} from '../utils/Utils';
import tacticalActivityRoutes from './TacticalActivityRoutes';

const tacticalRoutes = Router();

tacticalRoutes.use('/activity', tacticalActivityRoutes);

tacticalRoutes.get('/pomodoro/settings', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  getPomodoroSettings(userIdentifier)
    .subscribe(pomoSettings => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(pomoSettings);
    }, error => {
      logger.error(`Unable to get pomodoro settings for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

tacticalRoutes.post('/pomodoro/settings', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  const pomoSettings: PomodoroSettings = req.body;
  createOrUpdatePomodoroSettings(pomoSettings, userIdentifier)
    .subscribe(savedPomoSettings => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(savedPomoSettings);
    }, error => {
      logger.error(`Unable to get update pomodoro settings for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

export default tacticalRoutes;
