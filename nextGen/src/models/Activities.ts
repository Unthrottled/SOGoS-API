import {EventTypes} from './EventTypes';

export interface StoredCurrentActivity {
  guid: string;
  current: Activity;
  previous: Activity;
}

export enum ActivityType {
  ACTIVE = 'ACTIVE',
  PASSIVE = 'PASSIVE',
  NA = 'NA',
}

export enum ActivityStrategy {
  GENERIC = 'GENERIC',
}

export enum ActivityTimedType {
  NA = 'NA',
  NONE = 'NONE',
  TIMER = 'TIMER',
  STOP_WATCH = 'STOP_WATCH',
}

export interface CachedActivity {
  uploadType: EventTypes.CREATED | EventTypes.UPDATED | EventTypes.DELETED;
  activity: Activity;
}

export interface ActivityContent {
  uuid: string;
  name: string;
  timedType: ActivityTimedType;
  type: ActivityType;
  paused?: boolean;
  autoStart?: boolean;
  veryFirstActivity?: boolean;
  activityID?: string;
  duration?: number;
  workStartedWomboCombo?: number;
}

export interface Activity {
  antecedenceTime: number;
  content: ActivityContent;
  guid: string;
}
