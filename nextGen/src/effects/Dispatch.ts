import {Db} from 'mongodb';
import {Observable} from 'rxjs';
import {map, mergeMap} from 'rxjs/operators';
import {EffectSchema} from '../memory/Schemas';
import {getConnection} from '../Mongo';
import {mongoToObservable} from '../rxjs/Convience';

export interface Effect {
  guid: string;
  timeCreated: number;
  antecedenceTime: number;
  name: string;
  content: any;
  meta: any;
}

export const dispatchEffect = <T>(db: Db, effectCreator: (t: T) => Effect) =>
  (other: Observable<T>): Observable<T> => {
    return other.pipe(
      mergeMap(t =>
        mongoToObservable(callBack =>
          db.collection(EffectSchema.COLLECTION)
            .insertOne(effectCreator(t), callBack))
          .pipe(
            map(_ => t),
          ),
      ),
    );
  };

export const createEffect = (effect: Effect): Observable<any> => {
  return getConnection()
    .pipe(
      mergeMap(db =>
        mongoToObservable(callBack =>
          db.collection(EffectSchema.COLLECTION)
            .insertOne(effect, callBack))),
    );
};
