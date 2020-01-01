import bodyParser from 'body-parser';
import express from 'express';
import serverless from 'serverless-http';
import {handleRequest} from './APIRoute';
import openRoutes from './routes/OpenRoutes';
import {verificationHandler} from './security/OAuthHandler';
import {corsErrorHandler, corsRequestHandler} from './security/SecurityToolBox';

const application = express();

application.use(corsRequestHandler);
application.use(corsErrorHandler);
application.use(bodyParser.json({strict: false}));
application.use(bodyParser.urlencoded({extended: true}));

// todo: revist streams
application.get('/test', (request, response) => {
  handleRequest()
    .subscribe(item => {
        response.write(JSON.stringify(item));
      }, error => {

      }, () => {
        response.end();
      },
    );
});
application.use(openRoutes);
application.use(verificationHandler);

module.exports.handler = serverless(application);
