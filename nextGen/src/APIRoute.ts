import {Observable} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {getConnection} from './MongoDude';

export const handleRequest = (): Observable<any> => {
  return getConnection()
    .pipe(
      mergeMap(db => {
        return new Observable(subscriber => {
          db.collection('user').find({})
            .toArray((error, result) => {
              if (error) {
                subscriber.error(error);
              } else {
                subscriber.next(result);
                subscriber.complete();
              }
            });
        });
      }),
    );
};
