import omit from 'lodash/omit';
import { Db, MongoCallback} from 'mongodb';
import {Observable} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {Stream} from 'stream';
import {getConnection} from '../MongoDude';

export const toObservable = <T>(t: T) =>
  new Observable<T>(subscriber => {
    subscriber.next(t);
    subscriber.complete();
  });

export const mongoToStream = <T>(cursorSupplier: () => Stream) =>
  new Observable<T>(subscriber => {
    const cursor: Stream = cursorSupplier();
    cursor.on('data', data => subscriber.next(omit(data, '_id')));
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
  <T, U>(querier: (callBackSupplier: (t: T) => MongoCallback<U>) => void): Observable<T> =>
    new Observable<T>(subscriber => {
      querier((passThrough: T) => {
        const mongoCallback: MongoCallback<U> =
          ((error, result) => {
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

export const findMany = <T>(queryPerformer: (
  db: Db,
) => Stream): Observable<T> => {
  return getConnection()
    .pipe(
      mergeMap(db =>
      mongoToStream<T>(() => queryPerformer(db))),
    );
};

export const performUpdate = <T, U>(queryPerformer: (
  db: Db,
  callBackSupplier: (t: T) => MongoCallback<U>,
) => void): Observable<T> => {
  return getConnection()
    .pipe(
      mergeMap(db =>
      mongoUpdateToObservable<T, U>(querier => queryPerformer(db, querier))),
    );
};
