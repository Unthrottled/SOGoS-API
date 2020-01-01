import {filter, map, throwIfEmpty} from 'rxjs/operators';
import {RequestError} from '../models/Errors';
import {toObservable} from '../rxjs/Convience';
import {Claims} from './OAuthHandler';
import {hashString} from './SecurityToolBox';

export const extractClaims = (req) =>
  toObservable(req)
    .pipe(
      // @ts-ignore
      filter(request => !!request.claims),
      // @ts-ignore
      map(request => request.claims as Claims),
      filter(claims => !!claims.email),
      map(claims => {
        return {
          request: req,
          claims,
          identityProviderId: hashString(claims.email),
        };
      }),
      throwIfEmpty(() => new RequestError('Ya dun messed up', 400)),
    );
