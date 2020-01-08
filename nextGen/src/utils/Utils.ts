import winston from 'winston';
const { combine, timestamp, label, printf } = winston.format;

const myFormat = printf(({ level, message, label, timestamp }) => {
  return `${timestamp} [${label}] ${level}: ${message}`;
});

export const logger = winston.createLogger({
  format: combine(
    label({ label: 'right meow!' }),
    timestamp(),
    myFormat,
  ),
  transports: [
    new winston.transports.Console(),
  ],
});

export const rightMeow = () => new Date().valueOf();
