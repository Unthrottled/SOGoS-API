import {MongoClient} from 'mongodb';

const mongoUrl = 'mongodb://localhost:27017';

let connection: MongoClient;

export const getConnection = (): Promise<MongoClient> => {
  if (connection) {
    return Promise.resolve(connection);
  } else {
    return new Promise<any>((resolve, reject) => {
      MongoClient.connect(mongoUrl, {useNewUrlParser: true}, ((error, result) => {
        if (error) {
          reject(error);
        } else {
          connection = result;
          resolve(result);
        }
      }));
    });
  }
};
