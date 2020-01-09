import chalk from 'chalk';
import winston from 'winston';
const { combine, timestamp, label, printf } = winston.format;

const getLevel = (level: string): string => {
  switch (level) {
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

const myFormat = printf(({ level, message, timestamp }) => {
  return `[${timestamp}] ${getLevel(level)}: ${message}`;
});

export const logger = winston.createLogger({
  format: combine(
    timestamp(),
    myFormat,
  ),
  transports: [
    new winston.transports.Console(),
  ],
});

export const rightMeow = () => new Date().valueOf();
