import chalk from 'chalk';
import {Router} from 'express';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {findActivity, getActivityFeed, getFrom, getTo} from '../service/History';
import {logger, rightMeow} from '../utils/Utils';

const historyRouter = Router();

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

historyRouter.get('/:userIdentifier/feed',
  (req, res) => {
    // todo: log who looked at feed
    const requestParameters = req.params;
    const userIdentifier = requestParameters.userIdentifier;
    const meow = rightMeow();
    const queryParameters = req.query;
    const from = getFrom(queryParameters.from, meow);
    const to = getTo(queryParameters.to, meow);

    const observable = getActivityFeed(userIdentifier, to, from);
    observable
      .subscribe(activities => {
        res.status(200)
          .contentType(APPLICATION_JSON)
          .send(activities);
      }, error => {
        logger.error(`Unable to get activity feed for ${chalk.green(userIdentifier)} on behalf of ${chalk.cyan(req.header(USER_IDENTIFIER))} for reasons ${error}`);
        res.send(500);
      });
  });

export default historyRouter;
