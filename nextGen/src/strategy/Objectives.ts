import {Observable} from 'rxjs';
import {EventTypes} from '../models/EventTypes';
import {toObservable} from '../rxjs/Convience';

export const CREATED_OBJECTIVE = "CREATED_OBJECTIVE"
export const COMPLETED_OBJECTIVE = "COMPLETED_OBJECTIVE"
export const UPDATED_OBJECTIVE = "UPDATED_OBJECTIVE"
export const REMOVED_OBJECTIVE = "REMOVED_OBJECTIVE"
export const FOUND_OBJECTIVES = "foundObjectives"

const mappings = {
  CREATED: CREATED_OBJECTIVE,
  UPDATED: UPDATED_OBJECTIVE,
  DELETED: REMOVED_OBJECTIVE
}


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

export const createObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return toObservable(objective);
};

export const updateObjective = (
  objective: Objective,
  userIdentifier: string,
): Observable<Objective> => {
  return toObservable(objective);
};
