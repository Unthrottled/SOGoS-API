import {Router} from 'express';
import {Observable} from 'rxjs';
import {filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {UserSchema} from '../memory/Schemas';
import {RequestError} from '../models/Errors';
import {getConnection} from '../MongoDude';
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
              console.log(identityProviderId);
              db.collection(UserSchema.COLLECTION)
                .findOne({
                  [UserSchema.OAUTH_IDENTIFIERS]: [identityProviderId],
                }, ((error, result) => {
                  if (error) {
                    subscriber.error(error);
                  } else {
                    subscriber.next(result);
                    subscriber.complete();
                  }
                }));
            })),
          );
      }),
      throwIfEmpty(() => new RequestError('Ya dun messed up', 400)),
    )
    .subscribe(
      user => res.send(user),
      error => res.status(error).end(),
    );
}));

export default authenticatedRoutes;
