import chalk from 'chalk';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {logger} from './Utils';

export const requestLogger = (req, res, next) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  if (userIdentifier) {
    logger.info(`Incoming Request from [${chalk.green(userIdentifier)}] to path ${chalk.green(req.path)}`);
    const userAgent = req.header('User-Agent');
    if (userAgent) {
      logger.debug(`[${chalk.green(userIdentifier)}] USER-AGENT=${userAgent}`);
    }
  }
  next();
};
