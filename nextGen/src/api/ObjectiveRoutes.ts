import {Router} from 'express';
import omit = require('lodash/omit');
import {EMPTY} from 'rxjs';
import {fromIterable} from 'rxjs/internal-compatibility';
import {filter, map, mergeMap, reduce, throwIfEmpty} from 'rxjs/operators';
import {CurrentObjectiveSchema, ObjectiveHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {collectList, findMany, findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {
  CachedObjective,
  completeObjective,
  createObjective,
  deleteObjective,
  FOUND_OBJECTIVES,
  Objective,
  updateObjective,
} from '../strategy/Objectives';
import {logger} from "../utils/Utils";
import chalk from "chalk";

const objectivesRoutes = Router();

objectivesRoutes.get('/:objectiveId', ((req, res) => {
  const objectiveId = req.params.objectiveId;
  const userIdentifier = req.header(USER_IDENTIFIER);
  findOne((db, mongoCallback) =>
    db.collection(ObjectiveHistorySchema.COLLECTION)
      .findOne({
        [ObjectiveHistorySchema.IDENTIFIER]: objectiveId,
        [ObjectiveHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
      }, mongoCallback),
  ).pipe(
    throwIfEmpty(() => new NoResultsError()),
  ).subscribe(objective => {
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
  findMany(db =>
    db.collection(CurrentObjectiveSchema.COLLECTION)
      .aggregate([
        {
          $match: {
            [CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
          },
        },
        {
          $lookup: {
            from: ObjectiveHistorySchema.COLLECTION,
            localField: CurrentObjectiveSchema.OBJECTIVES,
            foreignField: ObjectiveHistorySchema.IDENTIFIER,
            as: FOUND_OBJECTIVES,
          },
        },
      ]),
  ).pipe(
    mergeMap(foundResult => fromIterable(foundResult[FOUND_OBJECTIVES])),
    map((objective: Objective) => omit(objective, ['_id'])),
    collectList<Objective>(),
  )
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

  fromIterable(objectives)
    .pipe(
      filter(cachedObjective => !!cachedObjective.uploadType),
      mergeMap(cachedObjective => {
        switch (cachedObjective.uploadType) {
          case EventTypes.COMPLETED:
            return completeObjective(cachedObjective.objective, userIdentifier);
          case EventTypes.DELETED:
            return deleteObjective(cachedObjective.objective, userIdentifier);
          case EventTypes.UPDATED:
            return updateObjective(cachedObjective.objective, userIdentifier);
          case EventTypes.CREATED:
            return createObjective(cachedObjective.objective, userIdentifier);
          default:
            return EMPTY;
        }
      }),
      reduce(acc => acc, {}),
    ).subscribe(_ => {
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
