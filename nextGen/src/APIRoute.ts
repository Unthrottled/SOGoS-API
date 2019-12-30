import {getConnection} from './MongoDude';

export const handleRequest = (): Promise<any> => {
  return getConnection().then(db => {
    return new Promise((resolve, reject) => {
      db.collection('user').find({})
        .toArray((error, result) => {
          if (error) {
            reject(error);
          } else {
            resolve(result);
          }
        });
    });
  });
};
