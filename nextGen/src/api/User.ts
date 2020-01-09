import chalk from 'chalk';
import {Db} from 'mongodb';
import {map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {ActivityTimedType, ActivityType, commenceActivity} from '../activity/Activities';
import {dispatchEffect} from '../effects/Dispatch';
import {UserSchema} from '../memory/Schemas';
import {RequestError} from '../models/Errors';
import {getConnection} from '../Mongo';
import {mongoToObservable, mongoUpdateToObservable} from '../rxjs/Convience';
import {switchIfEmpty} from '../rxjs/Operators';
import {extractClaims} from '../security/AuthorizationOperators';
import {Claims} from '../security/OAuthHandler';
import {extractUserValidationKey} from '../security/SecurityToolBox';
import {logger, rightMeow} from '../utils/Utils';

interface ClaimsAndStuff {
  request: any;
  claims: Claims;
  identityProviderId: string;
}

export const createUserIfNecessary = (claimsAndStuff: ClaimsAndStuff,
                                      db: Db) =>
  switchIfEmpty(
    mongoUpdateToObservable<any, any>(callBackSupplier => {
      const guid = uuid();
      const newUser = {
        [UserSchema.GLOBAL_USER_IDENTIFIER]: guid,
        [UserSchema.OAUTH_IDENTIFIERS]: [claimsAndStuff.identityProviderId],
        [UserSchema.TIME_CREATED]: rightMeow(),
      };
      db.collection(UserSchema.COLLECTION)
        .insertOne(newUser, callBackSupplier(newUser));
    })
      .pipe(
        mergeMap(user =>
          commenceActivity({
            guid: user[UserSchema.GLOBAL_USER_IDENTIFIER],
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
      ));

export const constructUserResponse = (claimsAndStuff: ClaimsAndStuff) => user => {
  const claims = claimsAndStuff.claims;
  const globalUserIdentifier = user.guid;
  const userVerificationKey =
    extractUserValidationKey(claims.email, globalUserIdentifier);
  const userInfo = {
    fullName: claims.name,
    userName: claims.preferred_username,
    firstName: claims.given_name,
    lastName: claims.family_name,
    email: claims.email,
    [UserSchema.GLOBAL_USER_IDENTIFIER]: globalUserIdentifier,
  };
  const security = {
    verificationKey: userVerificationKey,
  };
  return {
    security,
    information: userInfo,
  };
};

export const tryToFindUser = (db: Db,
                              claimsAndStuff: ClaimsAndStuff) =>
  mongoToObservable(callBack =>
    db.collection(UserSchema.COLLECTION)
      .findOne(
        {[UserSchema.OAUTH_IDENTIFIERS]: claimsAndStuff.identityProviderId},
        callBack));

export const userHandler = (req, res) => {
  extractClaims(req)
    .pipe(
      mergeMap(claimsAndStuff =>
        getConnection()
          .pipe(
            mergeMap(db =>
              tryToFindUser(db, claimsAndStuff)
                .pipe(
                  createUserIfNecessary(claimsAndStuff, db),
                  map(constructUserResponse(claimsAndStuff)),
                ),
            ),
          )),
      throwIfEmpty(() => new RequestError('Ya dun messed up', 400)),
    )
    .subscribe(
      user => res.send(user),
      error => {
        if (!error.code) {
          logger.error(`Unable to get user for request ${chalk.green(req.claims)} for reasons ${error}`);
        }
        return res.status(error.code || 500).end();
      },
    );
};
