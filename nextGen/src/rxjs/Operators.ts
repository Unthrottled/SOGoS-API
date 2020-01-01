import {Observable} from 'rxjs';
import {catchError, throwIfEmpty} from 'rxjs/operators';

export const switchIfEmpty = <T, R>(other: Observable<T>) =>
  (og: Observable<T>): Observable<T> =>
    og.pipe(
      throwIfEmpty(),
      catchError(() => other),
    );
