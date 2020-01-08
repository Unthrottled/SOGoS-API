import {Router} from 'express';
import omit from 'lodash/omit';
import {EMPTY, Observable} from 'rxjs';
import {fromIterable} from 'rxjs/internal-compatibility';
import {filter, map, mergeMap} from 'rxjs/operators';
import {createEffect} from '../effects/Dispatch';
import {TacticalActivitySchema} from '../memory/Schemas';
import {EventTypes} from '../models/EventTypes';
import {APPLICATION_JSON} from '../routes/OpenRoutes';
import {collectList, findMany, performUpdate} from '../rxjs/Convience';
import {USER_IDENTIFIER} from '../security/SecurityToolBox';
import {ColorType} from '../strategy/Objectives';
import {rightMeow} from '../utils/Utils';

const tacticalActivityRoutes = Router();

const CREATED_TACTICAL_ACTIVITY = 'STARTED_TACTICAL_ACTIVITY';
const REMOVED_TACTICAL_ACTIVITY = 'REMOVED_TACTICAL_ACTIVITY';
const UPDATED_TACTICAL_ACTIVITY = 'UPDATED_TACTICAL_ACTIVITY';

export interface CachedTacticalActivity {
  uploadType: EventTypes;
  activity: TacticalActivity;
}

export interface TacticalActivity {
  id: string;
  name: string;
  rank: number;
  antecedenceTime?: number;
  iconCustomization: {
    background: ColorType,
    line: ColorType,
  };
  categories: string[];
  hidden?: boolean;
}

const performTacticalActivityUpdate = (
  tacticalActivity: TacticalActivity,
  userIdentifier: string,
  removed: boolean,
  name: string,
) => performUpdate<TacticalActivity, any>((db, callBackSupplier) =>
  db.collection(TacticalActivitySchema.GLOBAL_USER_IDENTIFIER)
    .replaceOne({
      [TacticalActivitySchema.IDENTIFIER]: tacticalActivity.id,
      [TacticalActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
    }, {
      ...tacticalActivity,
      removed,
    }, {upsert: true}, callBackSupplier(tacticalActivity)),
).pipe(
  mergeMap(savedTactAct => {
      const meow = rightMeow();
      return createEffect({
        name,
        meta: {},
        content: savedTactAct,
        antecedenceTime: savedTactAct.antecedenceTime || meow,
        guid: userIdentifier,
        timeCreated: meow,
      });
    },
  ),
);

tacticalActivityRoutes.get('/', (req, res) => {
  const userIdentifier = req.header(USER_IDENTIFIER);
  findMany(db =>
    db.collection(TacticalActivitySchema.COLLECTION)
      .find({
        [TacticalActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
        [TacticalActivitySchema.REMOVED]: false,
      }),
  ).pipe(
    map((tacticalActivity: TacticalActivity) => omit(tacticalActivity, ['_id'])),
    collectList<TacticalActivity>(),
  )
    .subscribe(tacticalActivities => {
      res.status(200)
        .contentType(APPLICATION_JSON)
        .send(tacticalActivities);
    }, error => {
      // todo: log error
      res.send(500);
    });
});
const deleteTacticalActivity = (
  tacticalActivity: TacticalActivity,
  userIdentifier: string): Observable<TacticalActivity> => {
  return performTacticalActivityUpdate(
    tacticalActivity,
    userIdentifier,
    true,
    REMOVED_TACTICAL_ACTIVITY,
  );
};

const updateTacticalActivity = (
  tacticalActivity: TacticalActivity,
  userIdentifier: string): Observable<TacticalActivity> => {
  return performTacticalActivityUpdate(
    tacticalActivity,
    userIdentifier,
    false,
    UPDATED_TACTICAL_ACTIVITY,
  );
};

const createTacticalActivity = (
  tacticalActivity: TacticalActivity,
  userIdentifier: string): Observable<TacticalActivity> => {
  return performTacticalActivityUpdate(
    tacticalActivity,
    userIdentifier,
    false,
    CREATED_TACTICAL_ACTIVITY,
  );
};

tacticalActivityRoutes.post('/bulk', (req, res) => {
  const objectives = req.body as CachedTacticalActivity[];
  const userIdentifier = req.header(USER_IDENTIFIER);

  fromIterable(objectives)
    .pipe(
      filter(cachedTacticalActivity => !!cachedTacticalActivity.uploadType),
      mergeMap(cachedTacticalActivity => {
        switch (cachedTacticalActivity.uploadType) {
          case EventTypes.DELETED: // todo: remember to pick up antecedence time
            return deleteTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
          case EventTypes.UPDATED:
            return updateTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
          case EventTypes.CREATED:
            return createTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
          default:
            return EMPTY;
        }
      }),
    ).subscribe(_ => {
    res.send(204);
  }, error => {
    //    todo: log error
    res.send(500);
  });
});

tacticalActivityRoutes.post('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  createTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log error
      res.send(500);
    });
});

tacticalActivityRoutes.put('/bulk', (req, res) => {
  const tacticalActivities = req.body as TacticalActivity[];
  const userIdentifier = req.header(USER_IDENTIFIER);
  fromIterable(tacticalActivities)
    .pipe(
      mergeMap(tacticalActivity => // todo use antecedence time.
        updateTacticalActivity(tacticalActivity, userIdentifier)),
    )
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log error
      res.send(500);
    });
});

tacticalActivityRoutes.put('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  updateTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log error
      res.send(500);
    });
});

tacticalActivityRoutes.delete('/', (req, res) => {
  const tacticalActivity = req.body as TacticalActivity;
  const userIdentifier = req.header(USER_IDENTIFIER);
  deleteTacticalActivity(tacticalActivity, userIdentifier)
    .subscribe(_ => {
      res.send(204);
    }, error => {
      // todo: log error
      res.send(500);
    });
});

export default tacticalActivityRoutes;
