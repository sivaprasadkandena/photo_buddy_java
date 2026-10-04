import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const isPublicAuthRequest = /\/api\/auth\/(register|login|refresh|logout)(?:$|\?)/.test(request.url);
  const token = auth.accessToken;
  if (!isPublicAuthRequest && token) {
    request = request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(request).pipe(catchError((error: unknown) => {
    if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isPublicAuthRequest || !auth.hasRefreshToken) {
      return throwError(() => error);
    }
    return auth.refresh().pipe(
      switchMap(response => next(request.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } }))),
      catchError(refreshError => {
        auth.clearSession();
        return throwError(() => refreshError);
      }),
    );
  }));
};
