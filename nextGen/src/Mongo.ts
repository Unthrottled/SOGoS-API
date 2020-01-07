import {Db, MongoClient} from 'mongodb';
import {Observable} from 'rxjs';
import {CONNECTION_STRING} from './ConfigurationENV';

let connection: MongoClient;

export const getConnection = (): Observable<Db> => {
  if (connection && connection.isConnected()) {
    console.log('using cache');
    return new Observable<Db>(
      subscriber => {
        subscriber.next(connection.db('DEFAULT_DB'));
        subscriber.complete();
      },
    );
  } else {
    console.log('new connection');
    return new Observable<Db>(subscriber => {
      MongoClient.connect(CONNECTION_STRING, {useNewUrlParser: true}, ((error, result) => {
        if (error) {
          subscriber.error(error);
        } else {
          connection = result;
          subscriber.next(result.db('DEFAULT_DB'));
          subscriber.complete();
        }
      }));
    });
  }
};
