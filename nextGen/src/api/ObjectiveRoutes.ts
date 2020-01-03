import {Router} from 'express';
import omit = require('lodash/omit');
import {fromIterable} from 'rxjs/internal-compatibility';
import {map, mergeMap, throwIfEmpty} from 'rxjs/operators';
import {CurrentObjectiveSchema, ObjectiveHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON, JSON_STREAM} from '../routes/OpenRoutes';
import {findMany, findOne, mongoToStream} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {completeObjective, FOUND_OBJECTIVES, Objective} from '../strategy/Objectives';

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

}));

objectivesRoutes.post('/bulk', ((req, res) => {

}));

objectivesRoutes.put('/', ((req, res) => {

}));

objectivesRoutes.delete('/', ((req, res) => {

}));

export default objectivesRoutes;
