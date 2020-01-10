import {Db} from 'mongodb';
import {map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import uuid from 'uuid/v4';
import {dispatchEffect} from '../effects/Dispatch';
import {getConnection} from '../memory/Mongo';
import {UserSchema} from '../memory/Schemas';
import {ActivityTimedType, ActivityType} from '../models/Activities';
import {RequestError} from '../models/RequestErrors';
import {mongoToObservable, mongoUpdateToObservable} from '../rxjs/Convience';
import {switchIfEmpty} from '../rxjs/Operators';
import {extractClaims} from '../security/AuthorizationOperators';
import {Claims} from '../security/OAuthHandler';
import {extractUserValidationKey} from '../security/SecurityToolBox';
import {rightMeow} from '../utils/Utils';
import {commenceActivity} from './Activity';

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

export const constructUserResponse = (claimsAndStuff: ClaimsAndStuff) =>
    user => {
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

export const findOrCreateUser = req => extractClaims(req)
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
    );
