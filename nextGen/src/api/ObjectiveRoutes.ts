import {Router} from 'express';
import {throwIfEmpty} from 'rxjs/operators';
import {ObjectiveHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {findOne} from '../rxjs/Convience';

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

}));

objectivesRoutes.get('/', ((req, res) => {

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
