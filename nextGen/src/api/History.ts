import {response, Router} from 'express';
import {map, mergeMap} from 'rxjs/operators';
import {ActivityHistorySchema} from '../memory/Schemas';
import {getConnection} from '../MongoDude';
import {APPLICATION_JSON, JSON_STREAM} from '../routes/OpenRoutes';
import {mongoToStream} from '../rxjs/Convience';
import {rightMeow} from '../utils/Utils';

const historyRouter = Router();

historyRouter.post('/:userIdentifier/first/before',
  ((req, res) => {
    const sortOrder = -1;
    const comparisonString = '$lt';
  }));

historyRouter.post('/:userIdentifier/first/after',
  ((req, res) => {
    const sortOrder = -1;
    const comparisonString = '$lt';
  }));

const SEVEN_DAYZ = 604800000;
const getFrom = (from: any, meow: number) => {
  const fromNumber = parseInt(from, 10);
  if (!!fromNumber) {
    return from;
  } else {
    return meow - SEVEN_DAYZ;
  }
};
const getTo = (to: any, meow: number) => {
  const toNumber = parseInt(to, 10);
  if (!!toNumber) {
    return to;
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
    const to = getTo(queryParameters.from, meow);

    const asArray = queryParameters.asArray === 'true';

    res.setHeader('Content-Type', asArray ? APPLICATION_JSON : JSON_STREAM);
    if (asArray) {
      res.write('[');
    }

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
              }))),
        map(activity => asArray ? `${activity}` : activity),
      )
      .subscribe(activity => {
        response.write(activity);
      }, error => {
        // todo: error log
      }, () => {
        if (asArray) {
          response.write(']');
        }
        res.end();
      });
  });

export default historyRouter;
