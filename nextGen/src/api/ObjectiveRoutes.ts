import {Router} from 'express';
import omit = require('lodash/omit');
import {EMPTY, empty, Observable} from 'rxjs';
import {fromIterable} from 'rxjs/internal-compatibility';
import {filter, map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {CurrentObjectiveSchema, ObjectiveHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {APPLICATION_JSON, JSON_STREAM} from '../routes/OpenRoutes';
import {findMany, findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {
  CachedObjective,
  completeObjective,
  createObjective,
  deleteObjective,
  FOUND_OBJECTIVES,
  Objective, updateObjective,
} from '../strategy/Objectives';

const objectivesRoutes = Router();

objectivesRoutes.get('/:objectiveId', ((req, res) => {
  const objectiveId = req.params.objectiveId;
  findOne((db, mongoCallback) =>
  db.collection(ObjectiveHistorySchema.COLLECTION)
    .findOne({
      [ObjectiveHistorySchema.IDENTIFIER]: objectiveId,
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
      // todo log error
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
    }, error =>  {
      // todo log error
      res.send(500);
    });

}));

objectivesRoutes.get('/', ((req, res) => {
  const queryParameters = req.query;
  const asArray = queryParameters.asArray === 'true';

  res.setHeader('Content-Type', asArray ? APPLICATION_JSON : JSON_STREAM);
  if (asArray) {
    res.write('[');
  }

  const userIdentifier = req.header(USER_IDENTIFIER);
  findMany(db =>
    db.collection(CurrentObjectiveSchema.COLLECTION)
      .aggregate([
        {$match: {
          [CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
          }},
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
    map(objective => asArray ? `${objective},` : objective),
  )
    .subscribe(objective => {
      res.write(objective);
    }, error => {
      // todo: log error
      res.send(500);
    }, () => {
      if (asArray) {
        res.write(']');
      }
      res.end();
    });
}));

objectivesRoutes.post('/', ((req, res) => {
  const objective = req.body as Objective;
  const userIdentifier = req.header(USER_IDENTIFIER);
  createObjective(objective, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log error
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
    ).subscribe(_=> {
      res.send(204)
  }, error => {
  //    todo: log error
    res.send(500);
  });

}));

objectivesRoutes.put('/', ((req, res) => {

}));

objectivesRoutes.delete('/', ((req, res) => {

}));

export default objectivesRoutes;
