import {Router} from 'express';
import {Observable} from 'rxjs';
import {filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {UserSchema} from '../memory/Schemas';
import {RequestError} from '../models/Errors';
import {getConnection} from '../MongoDude';
import {switchIfEmpty} from '../rxjs/Operators';
import {Claims} from '../security/OAuthHandler';
import {hashString} from '../security/SecurityToolBox';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', ((req, res) => {
  new Observable(subscriber => {
    subscriber.next(req);
    subscriber.complete();
  })
    .pipe(
      // @ts-ignore
      filter(request => !!request.claims),
      // @ts-ignore
      map(request => request.claims as Claims),
      filter(claims => !!claims.email),
      map(claims => {
        return hashString(claims.email);
      }),
      mergeMap(identityProviderId => {
        return getConnection()
          .pipe(
            mergeMap(db => new Observable(subscriber => {
              db.collection(UserSchema.COLLECTION)
                .findOne({
                  [UserSchema.OAUTH_IDENTIFIERS]: identityProviderId,
                }, ((error, result) => {
                  if (error) {
                    subscriber.error(error);
                  } else {
                    if (!!result) {
                      subscriber.next(result);
                    }
                    subscriber.complete();
                  }
                }));
            })),
            switchIfEmpty(new Observable(subscriber => {
              const guid = uuid();
              const meow = new Date().valueOf();
              const newUser = {
                [UserSchema.GLOBAL_USER_IDENTIFIER]: guid,
                [UserSchema.OAUTH_IDENTIFIERS]: [identityProviderId],
                [UserSchema.TIME_CREATED]: meow,
              };
              subscriber.next(newUser);
            })),
          );
      }),
      throwIfEmpty(() => new RequestError('Ya dun messed up', 400)),
    )
    .subscribe(
      user => res.send(user),
      error => res.status(error.code || 500).end(),
    );
}));

export default authenticatedRoutes;
