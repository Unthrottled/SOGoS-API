import {Router} from 'express';
import {Observable} from 'rxjs';
import {mergeMap, throwIfEmpty} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {UserSchema} from '../memory/Schemas';
import {RequestError} from '../models/Errors';
import {getConnection} from '../MongoDude';
import {mongoToObservable} from '../rxjs/Convience';
import {switchIfEmpty} from '../rxjs/Operators';
import {extractClaims} from '../security/AuthorizationOperators';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', ((req, res) => {
  extractClaims(req)
    .pipe(
      mergeMap(stuff => {
        return getConnection()
          .pipe(
            mergeMap(db => mongoToObservable(callBack =>
              db.collection(UserSchema.COLLECTION)
                .findOne(
                  {[UserSchema.OAUTH_IDENTIFIERS]: stuff.identityProviderId},
                  callBack)),
            ),
            switchIfEmpty(new Observable(subscriber => {
              const guid = uuid();
              const meow = new Date().valueOf();
              const newUser = {
                [UserSchema.GLOBAL_USER_IDENTIFIER]: guid,
                [UserSchema.OAUTH_IDENTIFIERS]: [stuff.identityProviderId],
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
