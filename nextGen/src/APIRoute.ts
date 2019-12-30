import {getConnection} from './MongoDude';

export const handleRequest = (): Promise<any> => {
  return getConnection().then(client => {
    return {yeet: 'fam ravioli'};
  });
};
