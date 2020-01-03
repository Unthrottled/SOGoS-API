import {EventTypes} from '../models/EventTypes';
import {Observable} from "rxjs";
import {toObservable} from "../rxjs/Convience";

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

export const deleteObjective = (objective: Objective): Observable<Objective> => {
  return toObservable(objective);
};

export const createObjective = (objective: Objective): Observable<Objective> => {
  return toObservable(objective);
};

export const updateObjective = (objective: Objective): Observable<Objective> => {
  return toObservable(objective);
};
