import bodyParser from 'body-parser';
import express from 'express';
import {Cursor} from 'mongodb';
import {Observable} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import serverless from 'serverless-http';
import {getConnection} from './Mongo';
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

application.get('/test', (request, response) => {

  response.writeHead(200, {
    'Content-Type': 'application/json+stream',
  });

  response.write('[');
  getConnection()
    .pipe(
      mergeMap(db => {
        return new Observable(subscriber => {
          const cursor: Cursor<any> = db.collection('user').find({})
            .stream({
              transform: document => ({
                guid: document.guid,
              }),
            });
          cursor.on('data', data => subscriber.next(data));
          cursor.on('error', error => subscriber.error(error));
          cursor.on('end', () => subscriber.complete());
        });
      }),
    ).subscribe(item => {
      response.write(`${JSON.stringify(item)},`);
    }, error => {

    }, () => {
      response.write(']');
      response.end();
    },
  );
});

// not secure
application.use(openRoutes);

application.use(jwtHandler);

// secure
application.use(authenticatedRoutes);

application.use(verificationHandler);

// mega secure
application.use(authorizedRoutes);

module.exports.handler = serverless(application);
