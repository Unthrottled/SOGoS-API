import axios from 'axios';
import jwt from 'jsonwebtoken';
import jwkToPem, {RSA} from 'jwk-to-pem';
import {Observable} from 'rxjs';
import {JKWS_URL} from '../ConfigurationENV';

interface PublicKey {
  alg: string;
  e: string;
  kid: string;
  kty: string;
  n: string;
  use: string;
}
interface PublicKeyMeta {
  instance: PublicKey;
  pem: string;
}

interface PublicKeys {
  keys: PublicKey[];
}

interface MapOfKidToPublicKey {
  [key: string]: PublicKeyMeta;
}

let cachedKeys: MapOfKidToPublicKey;

const getPublicKeys = () => {
  if (!cachedKeys) {
    return axios.get<PublicKeys>(JKWS_URL)
      .then(keyResponse => {
        cachedKeys = keyResponse.data.keys.reduce((accum, nextKey) => {
          accum[nextKey.kid] = {instance: nextKey, pem: jwkToPem({
              ...nextKey,
            } as RSA)};
          return accum;
        }, {} as MapOfKidToPublicKey);
        return cachedKeys;
      }).catch((e) => {
        throw new Error('Unable to fetch public key for reasons ' + e.message);
      });
  } else {
    return Promise.resolve(cachedKeys);
  }
};

export const verificationHandler = (request, response, next) => {
  Promise.resolve(request.headers.Authorization as string)
    .then(authHeader => {
      if (authHeader && authHeader.startsWith('Bearer ')) {
        return authHeader.substr(8);
      } else {
        return Promise.reject(401);
      }
    })
    .catch((code) => response.send(code));
};
