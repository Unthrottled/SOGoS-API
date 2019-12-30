import axios from 'axios';
import ImmaTeapot from './ImmaTeapot';

const serverless = require('serverless-http');
const bodyParser = require('body-parser');
const express = require('express');
const application = express();

application.use(bodyParser.json({strict: false}));
application.use(bodyParser.urlencoded({extended: true}));
application.post('/slack/error', (request, response) => {
  axios.post(process.env.SLACK_URL, {
    text: request.body.text || 'n/a',
  })
    .then(() => response.status(204).send())
    .catch(() => response.status(204).send());
});

application.use((request, response) => {
  response.status(418).send(ImmaTeapot);
});

module.exports.handler = serverless(application);
