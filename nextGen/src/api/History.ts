import {Router} from 'express';
import {Request, Response} from 'express-serve-static-core';
import omit = require('lodash/omit');
import {map, mergeMap, reduce, throwIfEmpty} from 'rxjs/operators';
import {Activity} from '../activity/Activities';
import {ActivityHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {getConnection} from '../Mongo';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {collectList, mongoToObservable, mongoToStream} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';

const historyRouter = Router();

const findActivity = (
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
        // todo log
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

historyRouter.post('/:userIdentifier/first/before',
  ((req, res) => {
    const sortOrder = -1;
    const comparisonString = '$lt';
    findActivity(req, res, sortOrder, comparisonString);
  }));

historyRouter.post('/:userIdentifier/first/after',
  ((req, res) => {
    const sortOrder = 1;
    const comparisonString = '$gte';
    findActivity(req, res, sortOrder, comparisonString);
  }));

const SEVEN_DAYZ = 604800000;
const getFrom = (from: string, meow: number): number => {
  const fromNumber = parseInt(from, 10);
  if (!!fromNumber) {
    return fromNumber;
  } else {
    return meow - SEVEN_DAYZ;
  }
};
const getTo = (to: string, meow: number): number => {
  const toNumber = parseInt(to, 10);
  if (!!toNumber) {
    return toNumber;
  } else {
    return meow;
  }
};

historyRouter.get('/:userIdentifier/feed',
  (req, res) => {
    const requestParameters = req.params;
    const userIdentifier = requestParameters.userIdentifier;
    const meow = rightMeow();
    const queryParameters = req.query;
    const from = getFrom(queryParameters.from, meow);
    const to = getTo(queryParameters.to, meow);
    console.log(typeof to, typeof from, typeof 69)

    getConnection()
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
      )
      .subscribe(activities => {
        res.status(200)
          .contentType(APPLICATION_JSON)
          .send(activities);
      }, error => {
        // todo: error log
      });
  });

export default historyRouter;
