import cors from 'cors';
import crypto from 'crypto';
import {CONFIG} from '../config/config';
import {CORS_ORIGIN_URL, HMAC_KEY} from '../ConfigurationENV';

const allowedOrigin = CORS_ORIGIN_URL || CONFIG.security['allowed-origin'];

export const corsErrorHandler = (err, req, res, next) => {
  if (err.message && err.message.endsWith('is not an allowed origin.')) {
    res.status(403).end(err.message);
  } else {
    next();
  }
};

export const corsRequestHandler = cors(
  {
    origin: ((requestOrigin, callback) => {
      if (requestOrigin === allowedOrigin || !requestOrigin) {
        callback(null, true);
      } else {
        callback(new Error(`${requestOrigin} is not an allowed origin.`), false);
      }
    }),
    allowedHeaders: [
      'x-requested-with',
      'Access-Control-Allow-Origin',
      'origin',
      'Content-Type',
      'accept',
      'X-Amz-Date',
      'X-Api-Key',
      'X-Amz-Security-Token',
      'Sec-Fetch-Mode',

      // SOGoS Things
      'Authorization',
      'Verification',
      'User-Identifier',
      'User-Agent',
    ],
    methods: [
      'DELETE', 'GET', 'HEAD', 'OPTIONS', 'PATCH', 'POST', 'PUT',
    ],
  },
);

const hashingFunction = crypto.createHmac('SHA256', HMAC_KEY);

export const hashString = (value: string) => hashingFunction.update(value).digest('hex');

export const extractUserValidationKey =
  (emailAddress: string, globalUserIdentifier: string): string =>
    hashString(`${emailAddress}(◡‿◡✿)${globalUserIdentifier}`);
