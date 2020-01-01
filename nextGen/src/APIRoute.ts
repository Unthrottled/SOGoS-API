import {Cursor} from 'mongodb';
import {Observable} from 'rxjs';
import {mergeMap} from 'rxjs/operators';
import {getConnection} from './MongoDude';

export const handleRequest = (): Observable<any> => {
  return getConnection()
    .pipe(
      mergeMap(db => {
        return new Observable(subscriber => {
          const cursor: Cursor<any> = db.collection('user').find({})
            .stream({
              transform: document => ({
                guid: document.guid,
              }),
            });
          cursor.on('data', data => subscriber.next(data));
          cursor.on('error', error => subscriber.error(error));
          cursor.on('end', () => subscriber.complete());
        });
      }),
    );
};
