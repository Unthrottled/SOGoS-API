import bodyParser from 'body-parser';
import express from 'express';
import serverless from 'serverless-http';
import authenticatedRoutes from './routes/AuthenticatedRoutes';
import authorizedRoutes from './routes/AuthorizedRoutes';
import openRoutes from './routes/OpenRoutes';
import {jwtHandler, verificationHandler} from './security/OAuthHandler';
import {corsErrorHandler, corsRequestHandler} from './security/SecurityToolBox';

const application = express();

application.use(corsRequestHandler);
application.use(corsErrorHandler);
application.use(bodyParser.json({strict: false}));
application.use(bodyParser.urlencoded({extended: true}));

// not secure
application.use(openRoutes);

application.use(jwtHandler);

// secure
application.use(authenticatedRoutes);

application.use(verificationHandler);

// mega secure
application.use(authorizedRoutes);

module.exports.handler = serverless(application);
