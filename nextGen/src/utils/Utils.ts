import chalk from 'chalk';
import winston from 'winston';
import {LOGGING_LEVEL} from '../ConfigurationENV';

const {combine, timestamp, printf} = winston.format;

const getLevel = (level: string): string => {
  switch (level) {
    case 'debug':
      return chalk.cyan(level);
    case 'info':
      return chalk.green(level);
    case 'warn':
      return chalk.yellow(level);
    case 'error':
      return chalk.red(level);
    default:
      return level;
  }
};

const myFormat = printf(({level, message, timestamp: logTimestamp}) => {
  return `[${logTimestamp}] ${getLevel(level)}: ${message}`;
});

export const logger = winston.createLogger({
  level: LOGGING_LEVEL,
  format: combine(
    timestamp(),
    myFormat,
  ),
  transports: [
    new winston.transports.Console(),
  ],
});

export const rightMeow = () => new Date().valueOf();
