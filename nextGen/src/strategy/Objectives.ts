import {Observable} from 'rxjs';
import {map, mergeMap} from 'rxjs/operators';
import {createEffect} from '../effects/Dispatch';
import {CurrentObjectiveSchema, ObjectiveHistorySchema} from '../memory/Schemas';
import {EventTypes} from '../models/EventTypes';
import {findOne, performUpdate, toObservable} from '../rxjs/Convience';
import {switchIfEmpty} from '../rxjs/Operators';
import {rightMeow} from '../utils/Utils';

export const CREATED_OBJECTIVE = 'CREATED_OBJECTIVE';
export const COMPLETED_OBJECTIVE = 'COMPLETED_OBJECTIVE';
export const UPDATED_OBJECTIVE = 'UPDATED_OBJECTIVE';
export const REMOVED_OBJECTIVE = 'REMOVED_OBJECTIVE';
export const FOUND_OBJECTIVES = 'foundObjectives';

const mappings = {
  CREATED: CREATED_OBJECTIVE,
  UPDATED: UPDATED_OBJECTIVE,
  DELETED: REMOVED_OBJECTIVE,
};

export interface KeyResult {
  id: string;
  objectiveId: string;
  valueStatement: string;
  antecedenceTime?: number;
}

export interface ColorType {
  hex: string;
  opacity: number;
}

export interface IconCustomization {
  background: ColorType;
}

export interface Objective {
  id: string;
  valueStatement: string;
  antecedenceTime: number;
  keyResults: KeyResult[];
  iconCustomization: IconCustomization;
  associatedActivities: string[];
  categories: string[];
}

export interface CachedObjective {
  uploadType: EventTypes;
  objective: Objective;
}

export interface ObjectiveCacheEvent {
  objective: CachedObjective;
  userGUID: string;
}

export const deleteObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return toObservable(objective);
};

export const completeObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return toObservable(objective);
};

interface StoredObjectives {
  guid: string;
  objectives: string[];
}

function performObjectivePersistence(userIdentifier: string, objective: Objective, effectName: string) {
  return findOne((db, mongoCallback) =>
    db.collection(CurrentObjectiveSchema.COLLECTION)
      .findOne({
        [CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
      }, mongoCallback),
  ).pipe(
    mergeMap((objectives: StoredObjectives) => {
      if (objectives.objectives.find(id => id === objective.id)) {
        return toObservable(objective);
      } else {
        objectives.objectives = getNewList(objectives.objectives, objective);
        return performUpdate<Objective, any>(((db, callBackSupplier) => {
          db.collection(CurrentObjectiveSchema.COLLECTION)
            .replaceOne({
              [CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
            }, objectives, {upsert: true}, callBackSupplier(objective));
        }));
      }
    }),
    switchIfEmpty(performUpdate(((db, callBackSupplier) => {
      const docs: StoredObjectives = {
        guid: userIdentifier,
        objectives: [objective.id],
      };
      db.collection(CurrentObjectiveSchema.COLLECTION)
        .insertOne(docs, callBackSupplier(objective));
    }))),
    map(objecto => {
      objecto[CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER] = userIdentifier;
      return objecto;
    }),
    mergeMap(objecto => {
      return performUpdate<Objective, any>(((db, callBackSupplier) => {
        db.collection(ObjectiveHistorySchema.COLLECTION)
          .replaceOne({
            [ObjectiveHistorySchema.IDENTIFIER]: objecto.id,
            [ObjectiveHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
          }, objecto, {upsert: true}, callBackSupplier(objecto));
      }));
    }),
    mergeMap(objecto => {
      const meow = rightMeow();
      return createEffect({
        meta: {},
        content: objecto,
        name: effectName,
        antecedenceTime: objecto.antecedenceTime || meow,
        timeCreated: meow,
        guid: userIdentifier,
      }).pipe(map(_ => objecto));
    }),
  );
}

export const createObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return performObjectivePersistence(userIdentifier, objective, CREATED_OBJECTIVE);
};

// Note: Can Update a non-existing objective, will just create objective.
export const updateObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return performObjectivePersistence(userIdentifier, objective, UPDATED_OBJECTIVE);
};

export const MAX_OBJECTIVES = 5;

const getNewList = (currentObjectives: string[], objective: Objective): string[] => {
    return [
      ...(currentObjectives.length >= MAX_OBJECTIVES ?
        currentObjectives.slice(1) : currentObjectives),
      objective.id,
    ];
};
