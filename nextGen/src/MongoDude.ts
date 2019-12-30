import {Db, MongoClient} from 'mongodb';

const mongoUrl = 'mongodb://localhost:27017';

let connection: MongoClient;

export const getConnection = (): Promise<Db> => {
  if (connection) {
    return Promise.resolve(connection.db("DEFAULT_DB"));
  } else {
    return new Promise<any>((resolve, reject) => {
      MongoClient.connect(mongoUrl, {useNewUrlParser: true}, ((error, result) => {
        if (error) {
          reject(error);
        } else {
          connection = result;
          resolve(result.db("DEFAULT_DB"));
        }
      }));
    });
  }
};
