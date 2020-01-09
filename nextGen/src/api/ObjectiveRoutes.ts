import chalk from 'chalk';
import {Router} from 'express';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {findObjectives, findSingleObjective, uploadObjectives} from '../service/Objective';
import {
  CachedObjective,
  completeObjective,
  createObjective,
  deleteObjective,
  Objective,
  updateObjective,
} from '../strategy/Objectives';
import {logger} from '../utils/Utils';

const objectivesRoutes = Router();

objectivesRoutes.get('/:objectiveId', ((req, res) => {
  const objectiveId = req.params.objectiveId;
  const userIdentifier = req.header(USER_IDENTIFIER);
  findSingleObjective(objectiveId, userIdentifier)
    .subscribe(objective => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(objective);
    }, error => {
      if (error instanceof NoResultsError) {
        res.status(404);
      } else {
        logger.error(`Unable to get objective ${chalk.yellow(objectiveId)} for ${chalk.green(userIdentifier)} for reasons ${error}`);
        res.status(500);
      }
    });
}));

objectivesRoutes.post('/:objectiveId/complete', ((req, res) => {
  const objective: Objective = req.body;
  const userIdentifier = req.header(USER_IDENTIFIER);
  completeObjective(objective, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to complete objective ${chalk.yellow(req.params.objectiveId)} for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

objectivesRoutes.get('/', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findObjectives(userIdentifier)
    .subscribe(objectives => {
      res.status(200)
        .contentType(APPLICATION_JSON)
        .send(objectives);
    }, error => {
      logger.error(`Unable to get objectives for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

objectivesRoutes.post('/', ((req, res) => {
  const objective = req.body as Objective;
  const userIdentifier = req.header(USER_IDENTIFIER);
  createObjective(objective, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to create objective for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

objectivesRoutes.post('/bulk', ((req, res) => {
  const objectives = req.body as CachedObjective[];
  const userIdentifier = req.header(USER_IDENTIFIER);

  uploadObjectives(objectives, userIdentifier).subscribe(_ => {
    res.send(204);
  }, error => {
    logger.error(`Unable to bulk upload objectives for ${chalk.green(userIdentifier)} for reasons ${error}`);
    res.send(500);
  });
}));

objectivesRoutes.put('/', ((req, res) => {
  const objective = req.body as Objective;
  const userIdentifier = req.header(USER_IDENTIFIER);
  updateObjective(objective, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to update objective for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

objectivesRoutes.delete('/', ((req, res) => {
  const objective = req.body as Objective;
  const userIdentifier = req.header(USER_IDENTIFIER);
  deleteObjective(objective, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      logger.error(`Unable to delete objective for ${chalk.green(userIdentifier)} for reasons ${error}`);
      res.send(500);
    });
}));

export default objectivesRoutes;
