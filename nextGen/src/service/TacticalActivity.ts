import {EMPTY, Observable} from "rxjs";
import {EventTypes} from "../models/EventTypes";
import {fromIterable} from "rxjs/internal-compatibility";
import {filter, map, mergeMap, reduce} from "rxjs/operators";
import {ColorType} from "../strategy/Objectives";
import {collectList, findMany, performUpdate} from "../rxjs/Convience";
import {TacticalActivitySchema} from "../memory/Schemas";
import omit from "lodash/omit";
import {rightMeow} from "../utils/Utils";
import {createEffect} from "../effects/Dispatch";

const CREATED_TACTICAL_ACTIVITY = 'CREATED_TACTICAL_ACTIVITY';  // was STARTED_TACTICAL_ACTIVITY.
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
    db.collection(TacticalActivitySchema.COLLECTION)
        .replaceOne({
            [TacticalActivitySchema.IDENTIFIER]: tacticalActivity.id,
            [TacticalActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
        }, {
            ...tacticalActivity,
            guid: userIdentifier,
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

export function findTacticalActivities(userIdentifier: string) {
    return findMany(db =>
        db.collection(TacticalActivitySchema.COLLECTION)
            .find({
                [TacticalActivitySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
                [TacticalActivitySchema.REMOVED]: false,
            }),
    ).pipe(
        map((tacticalActivity: TacticalActivity) => omit(tacticalActivity, ['_id'])),
        collectList<TacticalActivity>(),
    );
}

export const deleteTacticalActivity = (
    tacticalActivity: TacticalActivity,
    userIdentifier: string): Observable<TacticalActivity> => {
    return performTacticalActivityUpdate(
        tacticalActivity,
        userIdentifier,
        true,
        REMOVED_TACTICAL_ACTIVITY,
    );
};
export const updateTacticalActivity = (
    tacticalActivity: TacticalActivity,
    userIdentifier: string): Observable<TacticalActivity> => {
    return performTacticalActivityUpdate(
        tacticalActivity,
        userIdentifier,
        false,
        UPDATED_TACTICAL_ACTIVITY,
    );
};
export const createTacticalActivity = (
    tacticalActivity: TacticalActivity,
    userIdentifier: string): Observable<TacticalActivity> => {
    return performTacticalActivityUpdate(
        tacticalActivity,
        userIdentifier,
        false,
        CREATED_TACTICAL_ACTIVITY,
    );
};

export function uploadTacticalActivities(objectives: CachedTacticalActivity[], userIdentifier: string) {
    return fromIterable(objectives)
        .pipe(
            filter(cachedTacticalActivity => !!cachedTacticalActivity.uploadType),
            mergeMap(cachedTacticalActivity => {
                switch (cachedTacticalActivity.uploadType) {
                    case EventTypes.DELETED:
                        return deleteTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
                    case EventTypes.UPDATED:
                        return updateTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
                    case EventTypes.CREATED:
                        return createTacticalActivity(cachedTacticalActivity.activity, userIdentifier);
                    default:
                        return EMPTY;
                }
            }),
            reduce(acc => acc, {}),
        );
}

export function massUpdateTacticalActivities(tacticalActivities: TacticalActivity[], userIdentifier: string) {
    return fromIterable(tacticalActivities)
        .pipe(
            mergeMap(tacticalActivity =>
                updateTacticalActivity(tacticalActivity, userIdentifier)),
            reduce(acc => acc, {}),
        );
}
