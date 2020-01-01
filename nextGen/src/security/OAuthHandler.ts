import axios from 'axios';
import jwt from 'jsonwebtoken';
import jwkToPem, {RSA} from 'jwk-to-pem';
import {ISSUER, JKWS_URL} from '../ConfigurationENV';
import {extractUserValidationKey} from './SecurityToolBox';

interface TokenHeader {
  kid: string;
  alg: string;
}

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

interface Claims {
  token_use: string;
  auth_time: number;
  iss: string;
  email: string;
  exp: number;
  username: string;
  client_id: string;
}

let cachedKeys: MapOfKidToPublicKey;

const getPublicKeys = () => {
  if (!cachedKeys) {
    return axios.get<PublicKeys>(JKWS_URL)
      .then(keyResponse => {
        cachedKeys = keyResponse.data.keys.reduce((accum, nextKey) => {
          accum[nextKey.kid] = {
            instance: nextKey, pem: jwkToPem({
              ...nextKey,
            } as RSA),
          };
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

export const jwtHandler = (request, response, next) => {
  Promise.resolve(request.headers.authorization as string)
    .then(extractJwt)
    .then(parsHeaders)
    .then(combineWithPublicKeys)
    .then(extractClaims)
    .then(verifyIssuer)
    .then(claims => {
      request.claims = claims;
      next();
    })
    .catch(code => response.send(code));
};

const getClaimsFromRequest = (request): Claims => {
  const claims = request.custom.claims as Claims;
  if (!claims) {
    throw new Error('You forgot to either put claims in the response or ' +
      'you put the authorization handler in the wrong order');
  }
  return claims;
};

export const verificationHandler = (request, response, next) => {
    const verificationKey = request.headers.verification || '';
    const globalUserIdentifier = request.headers['user-identifier'] || '';
    const claims = getClaimsFromRequest(request);
    const email = claims.email;
    const generatedVerificationKey = extractUserValidationKey(
      email,
      globalUserIdentifier,
    );
    if (verificationKey === generatedVerificationKey) {
      next();
    } else {
      response.send(403);
    }
};

const extractJwt = authHeader => {
  if (authHeader && authHeader.startsWith('Bearer ')) {
    return authHeader.substr(7);
  } else {
    return Promise.reject(401);
  }
};
const parsHeaders = token => {
  const tokenPieces = token.split('.');
  if (tokenPieces.length > 1) {
    try {
      return {
        token,
        tokenHeader:
          JSON.parse(
            Buffer.from(tokenPieces[0], 'base64')
              .toString('utf8'),
          ) as TokenHeader,
      };
    } catch (e) {}
  }
  return Promise.reject(401);

};
const combineWithPublicKeys = tokenStuff => getPublicKeys().then(keys => ({
  keys,
  tokenStuff,
}));
const extractClaims = ({keys, tokenStuff: {token, tokenHeader: {kid}}}) => {
  const publicKey = keys[kid];
  if (!!publicKey) {
    return new Promise<Claims>((resolve, reject) => {
      jwt.verify(token, publicKey.pem, ((err, decoded) => {
        if (err) {
          reject(401);
        } else {
          resolve(decoded as Claims);
        }
      }));
    });
  } else {
    return Promise.reject(401);
  }
};
const verifyIssuer = claims => {
  if (claims.iss === ISSUER) {
    return claims;
  } else {
    return Promise.reject(401);
  }
};
