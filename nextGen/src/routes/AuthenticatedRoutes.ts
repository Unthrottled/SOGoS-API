import {Router} from 'express';
import {map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {ActivityTimedType, ActivityType, commenceActivity} from '../activity/Activities';
import {dispatchEffect} from '../effects/Dispatch';
import {UserSchema} from '../memory/Schemas';
import {RequestError} from '../models/Errors';
import {getConnection} from '../MongoDude';
import {mongoToObservable} from '../rxjs/Convience';
import {switchIfEmpty} from '../rxjs/Operators';
import {extractClaims} from '../security/AuthorizationOperators';

const authenticatedRoutes = Router();

authenticatedRoutes.get('/user', (req, res) => {
  extractClaims(req)
    .pipe(
      mergeMap(claimsAndStuff =>
        getConnection()
          .pipe(
            mergeMap(db => mongoToObservable(callBack =>
              db.collection(UserSchema.COLLECTION)
                .findOne(
                  {[UserSchema.OAUTH_IDENTIFIERS]: claimsAndStuff.identityProviderId},
                  callBack))
              .pipe(
                switchIfEmpty(
                  mongoToObservable(callBack => {
                    const guid = uuid();
                    const meow = new Date().valueOf();
                    const newUser = {
                      [UserSchema.GLOBAL_USER_IDENTIFIER]: guid,
                      [UserSchema.OAUTH_IDENTIFIERS]: [claimsAndStuff.identityProviderId],
                      [UserSchema.TIME_CREATED]: meow,
                    };
                    db.collection(UserSchema.COLLECTION)
                      .insertOne(newUser, callBack);
                  })
                    .pipe(
                      mergeMap(user =>
                        commenceActivity({
                          userIdentifier: user[UserSchema.GLOBAL_USER_IDENTIFIER],
                          antecedenceTime: user[UserSchema.TIME_CREATED],
                          content: {
                            name: 'RECOVERY',
                            type: ActivityType.ACTIVE,
                            timedType: ActivityTimedType.NONE,
                            veryFirstActivity: true,
                            uuid: uuid(),
                          },
                        }, db).pipe(map(_ => user)),
                      ),
                      dispatchEffect(db, user => ({
                        guid: user[UserSchema.GLOBAL_USER_IDENTIFIER],
                        timeCreated: user[UserSchema.TIME_CREATED],
                        antecedenceTime: user[UserSchema.TIME_CREATED],
                        name: 'USER_CREATED',
                        content: claimsAndStuff.claims,
                        meta: {},
                      })),
                    )),
              ),
            ),
          )),
      throwIfEmpty(() => new RequestError('Ya dun messed up', 400)),
    )
    .subscribe(
      user => res.send(user),
      error => res.status(error.code || 500).end(),
    );
});

export default authenticatedRoutes;
