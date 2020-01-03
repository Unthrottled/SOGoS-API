import {Db, MongoClient} from 'mongodb';
import {Observable} from 'rxjs';

const mongoUrl = 'mongodb://localhost:27017';

// todo look into serverless connection re-use
let connection: MongoClient;

export const getConnection = (): Observable<Db> => {
  if (connection) {
    return new Observable<Db>(
      subscriber => {
        subscriber.next(connection.db('DEFAULT_DB'));
        subscriber.complete();
      },
    );
  } else {
    return new Observable<Db>(subscriber => {
      MongoClient.connect(mongoUrl, {useNewUrlParser: true}, ((error, result) => {
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
