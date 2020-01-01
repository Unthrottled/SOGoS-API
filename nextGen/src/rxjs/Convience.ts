import {MongoCallback} from 'mongodb';
import {Observable} from 'rxjs';

export const toObservable = <T>(t: T) =>
new Observable<T>(subscriber => {
  subscriber.next(t);
  subscriber.complete();
});

export const mongoToObservable = <T>(querier: (callBack: MongoCallback<T>) => void): Observable<T> => {
  return new Observable<T>(subscriber => {
    const mongoCallback: MongoCallback<T> =
      ((error, result) => {
        if (error) {
          subscriber.error(error);
        } else {
          if (!!result) {
            subscriber.next(result);
          }
          subscriber.complete();
        }
      });
    querier(mongoCallback);
  });
};
