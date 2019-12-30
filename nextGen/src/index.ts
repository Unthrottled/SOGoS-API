import {handleRequest} from './APIRoute';
import ImmaTeapot from './ImmaTeapot';

const serverless = require('serverless-http');
const bodyParser = require('body-parser');
const express = require('express');
const application = express();

application.use(bodyParser.json({strict: false}));
application.use(bodyParser.urlencoded({extended: true}));
application.get('/test', (request, response) => {
  handleRequest().then((result) => response.send(result));
});

application.use((request, response) => {
  response.status(418).send(ImmaTeapot);
});

module.exports.handler = serverless(application);
