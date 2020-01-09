import omit from 'lodash/omit';
import {EMPTY} from 'rxjs';
import {fromIterable} from 'rxjs/internal-compatibility';
import {filter, map, mergeMap, reduce, throwIfEmpty} from 'rxjs/operators';
import {CurrentObjectiveSchema, ObjectiveHistorySchema} from '../memory/Schemas';
import {NoResultsError} from '../models/Errors';
import {EventTypes} from '../models/EventTypes';
import {collectList, findMany, findOne} from '../rxjs/Convience';
import {
    CachedObjective,
    completeObjective,
    createObjective,
    deleteObjective,
    FOUND_OBJECTIVES,
    Objective,
    updateObjective,
} from '../strategy/Objectives';

export const findSingleObjective = (objectiveId, userIdentifier: string) => findOne((db, mongoCallback) =>
    db.collection(ObjectiveHistorySchema.COLLECTION)
        .findOne({
            [ObjectiveHistorySchema.IDENTIFIER]: objectiveId,
            [ObjectiveHistorySchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
        }, mongoCallback),
).pipe(
    throwIfEmpty(() => new NoResultsError()),
);

export const findObjectives = (userIdentifier: string) => findMany(db =>
    db.collection(CurrentObjectiveSchema.COLLECTION)
        .aggregate([
            {
                $match: {
                    [CurrentObjectiveSchema.GLOBAL_USER_IDENTIFIER]: userIdentifier,
                },
            },
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
    collectList<Objective>(),
);

export const uploadObjectives = (objectives: CachedObjective[], userIdentifier: string) => fromIterable(objectives)
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
        reduce(acc => acc, {}),
    );
