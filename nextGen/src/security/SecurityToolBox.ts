import cors from 'cors';
export const corsRequestHandler = cors(
  {
    origin: CORS_ORIGIN_URL,
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
