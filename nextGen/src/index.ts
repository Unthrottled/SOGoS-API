import cors from 'cors';
import express from 'express';
import {handleRequest} from './APIRoute';
import ImmaTeapot from './ImmaTeapot';
import {verificationHandler} from './security/OAuthHandler';
import {corsErrorHandler, corsRequestHandler} from './security/SecurityToolBox';

const serverless = require('serverless-http');
const bodyParser = require('body-parser');
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

application.use(verificationHandler);

module.exports.handler = serverless(application);
