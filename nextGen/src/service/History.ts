import chalk from 'chalk';
import {Request, Response} from 'express-serve-static-core';
import omit from 'lodash/omit';
import {map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {getConnection} from '../memory/Mongo';
import {ActivityHistorySchema} from '../memory/Schemas';
import {Activity} from '../models/Activities';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {collectList, mongoToObservable, mongoToStream} from '../rxjs/Convience';
import {logger} from '../utils/Utils';

export const findActivity = (
    request: Request,
    res: Response,
    sortOrder: number,
    comparisonString: string,
) => {
    const userIdentifier = request.params.userIdentifier;
    const bodyAsJson = request.body;
    const relativeTime = bodyAsJson.relativeTime;
    if (!!relativeTime) {
        getConnection()
            .pipe(
                mergeMap(db =>
                    mongoToObservable(callBack =>
                        db.collection(ActivityHistorySchema.COLLECTION)
                            .findOne({
                                [ActivityHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
                                [ActivityHistorySchema.TIME_OF_ANTECEDENCE]: {
                                    [comparisonString]: relativeTime,
                                },
                            }, {
                                sort: {
                                    [ActivityHistorySchema.TIME_OF_ANTECEDENCE]: sortOrder,
                                },
                            }, callBack))),
                throwIfEmpty(() => new NoResultsError()),
                map((activity: Activity) => omit(activity, [ActivityHistorySchema.GLOBAL_USER_IDENTIFIER])),
            ).subscribe(activity => {
            res.contentType(APPLICATION_JSON)
                .status(200)
                .send(activity);
        }, error => {
            if (!(error instanceof NoResultsError)) {
                logger.error(`Unable to find activity for ${chalk.green(userIdentifier)} for reasons ${error}`);
                res.send(500);
            } else {
                res.send(404);
            }
        });
    } else {
        res.status(400)
            .send('Expected a number in "relativeTime" in the request.');
    }
};
export const getFrom = (from: string, meow: number): number => {
    const fromNumber = parseInt(from, 10);
    if (!!fromNumber) {
        return fromNumber;
    } else {
        return meow - SEVEN_DAYZ;
    }
};
export const getTo = (to: string, meow: number): number => {
    const toNumber = parseInt(to, 10);
    if (!!toNumber) {
        return toNumber;
    } else {
        return meow;
    }
};
export const getActivityFeed = (userIdentifier, to: number, from: number) => getConnection()
    .pipe(
        mergeMap(db =>
            mongoToStream(() =>
                db.collection(ActivityHistorySchema.COLLECTION)
                    .find({
                        [ActivityHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
                        [ActivityHistorySchema.TIME_OF_ANTECEDENCE]: {
                            $lt: to,
                            $gte: from,
                        },
                    }).stream())),
        collectList<Activity>(),
    );
const SEVEN_DAYZ = 604800000;
