import chalk from 'chalk';
import {findOrCreateUser} from '../service/User';
import {logger} from '../utils/Utils';

export const userHandler = (req, res) => {
  findOrCreateUser(req)
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
