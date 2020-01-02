import {Router} from 'express';
import {map, throwIfEmpty} from 'rxjs/operators';
import {Activity, StoredCurrentActivity} from '../activity/Activities';
import {CurrentActivitySchema} from '../memory/Schemas';
import {getConnection} from '../MongoDude';
import {findOne} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {APPLICATION_JSON} from "../routes/OpenRoutes";

const activityRoutes = Router();

export const STARTED_ACTIVITY = 'STARTED_ACTIVITY';
export const REMOVED_ACTIVITY = 'REMOVED_ACTIVITY';
export const UPDATED_ACTIVITY = 'UPDATED_ACTIVITY';

export const CREATED = 'CREATED';
export const UPDATED = 'UPDATED';
export const DELETED = 'DELETED';

const uploadStatus = {
  CREATED,
  UPDATED,
  DELETED,
};

activityRoutes.get('/current', ((req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findOne<StoredCurrentActivity>((db, mongoCallback) => {
    db.collection(CurrentActivitySchema.COLLECTION)
      .findOne({
        [CurrentActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
      }, mongoCallback);
  })
    .pipe(
      map(storedCurrentActivity => {
        if (!!storedCurrentActivity.current) {
          return storedCurrentActivity.current;
        } else {
          return storedCurrentActivity; // todo remove once data is less janky
        }
      }),
      map((activity: Activity) => ({
        antecedenceTime: activity.antecedenceTime,
        content: activity.content,
      })),
      throwIfEmpty(), // should always have current activity
    )
    .subscribe(activity => {
      res.contentType(APPLICATION_JSON)
        .status(200)
        .send(activity);
    }, error => {
      // todo: log
      res.send(500)
    })
  ;
}));

activityRoutes.get('/previous', ((req, res) => {

}));

activityRoutes.post('/bulk', ((req, res) => {

}));

activityRoutes.post('/', ((req, res) => {

}));

activityRoutes.put('/', ((req, res) => {

}));

activityRoutes.delete('/', ((req, res) => {

}));

activityRoutes.get('/pomodoro/count', ((req, res) => {

}));

export default activityRoutes;
