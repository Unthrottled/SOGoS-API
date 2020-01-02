import omit from 'lodash/omit';
import {Cursor, Db, MongoCallback} from 'mongodb';
import {Observable} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {getConnection} from '../MongoDude';

export const toObservable = <T>(t: T) =>
  new Observable<T>(subscriber => {
    subscriber.next(t);
    subscriber.complete();
  });

export const mongoToStream = <T>(cursorSupplier: () => Cursor<T>) =>
  new Observable(subscriber => {
    const cursor: Cursor<T> = cursorSupplier();
    cursor.on('data', data => subscriber.next(data));
    cursor.on('error', error => subscriber.error(error));
    cursor.on('end', () => subscriber.complete());
  });

export const mongoToObservable = <T>(querier: (callBack: MongoCallback<T>) => void): Observable<T> => {
  return new Observable<T>(subscriber => {
    const mongoCallback: MongoCallback<T> =
      ((error, result: T) => {
        if (error) {
          subscriber.error(error);
        } else {
          if (!!result) {
            // @ts-ignore
            subscriber.next(omit(result, ['_id']));
          }
          subscriber.complete();
        }
      });
    querier(mongoCallback);
  });
};

// Yo dawg, I heard you like functions
// So I put functions with in functions,
// So you can async while you defer.
export const mongoUpdateToObservable =
  <T>(querier: (callBackSupplier: (t: T) => MongoCallback<T>) => void): Observable<T> =>
    new Observable<T>(subscriber => {
      querier((passThrough: T) => {
        const mongoCallback: MongoCallback<T> =
          ((error, result: T) => {
            if (error) {
              subscriber.error(error);
            } else {
              if (!!result) {
                subscriber.next(passThrough);
              }
              subscriber.complete();
            }
          });
        return mongoCallback;
      });
    });

export const findOne = <T>(queryPerformer: (
  db: Db,
  mongoCallback: MongoCallback<T>,
) => void): Observable<T> => {
  return getConnection()
    .pipe(
      mergeMap(db =>
      mongoToObservable<T>(querier => queryPerformer(db, querier))),
    );
};