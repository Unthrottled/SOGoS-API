import {Db, MongoClient} from 'mongodb';
import {Observable} from 'rxjs';

const mongoUrl = 'mongodb://localhost:27017';

// todo look into serverless connection re-use
export const getConnection = (): Observable<Db> =>
  new Observable<Db>(subscriber => {
    MongoClient.connect(mongoUrl, {useNewUrlParser: true}, ((error, result) => {
      if (error) {
        subscriber.error(error);
      } else {
        subscriber.next(result.db('DEFAULT_DB'));
        subscriber.complete();
      }
    }));
  });
