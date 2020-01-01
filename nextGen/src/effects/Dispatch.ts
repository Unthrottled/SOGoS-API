import {Db} from 'mongodb';
import {Observable} from 'rxjs';
import {ignoreElements, map, mergeMap} from 'rxjs/operators';
import {EffectSchema} from '../memory/Schemas';
import {mongoToObservable, toObservable} from '../rxjs/Convience';

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
  // const observable = mongoToObservable(callBack =>
  //   db.collection(EffectSchema.COLLECTION)
  //     .insertOne(effect, callBack));
    return other.pipe(
      mergeMap(t =>
        toObservable(effectCreator(t))
          .pipe(
            map(effect => {
              console.log('i got dis effect', effect);
              return effect;
            }),
            map(_ => t),
          ),
      ),
    );
  }
;
