import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable, catchError, finalize, of, shareReplay, switchMap, tap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, AuthUser, RegisterPayload } from '../models/auth.models';

const ACCESS_TOKEN_KEY = 'photo-buddy.access-token';
const REFRESH_TOKEN_KEY = 'photo-buddy.refresh-token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/auth`;
  private readonly userSubject = new BehaviorSubject<AuthUser | null>(null);
  private refreshRequest?: Observable<AuthResponse>;
  private meRequest?: Observable<AuthUser>;
  readonly user$ = this.userSubject.asObservable();

  get accessToken(): string | null {
    return sessionStorage.getItem(ACCESS_TOKEN_KEY);
  }

  get isAuthenticated(): boolean {
    return this.accessToken !== null;
  }

  get hasRefreshToken(): boolean {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY) !== null;
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/register`, payload).pipe(tap(response => this.saveSession(response)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/login`, { email, password }).pipe(tap(response => this.saveSession(response)));
  }

  refresh(): Observable<AuthResponse> {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    if (!refreshToken) return throwError(() => new Error('No refresh token is available'));
    if (!this.refreshRequest) {
      this.refreshRequest = this.http.post<AuthResponse>(`${this.apiUrl}/refresh`, { refreshToken }).pipe(
        tap(response => this.saveSession(response)),
        finalize(() => { this.refreshRequest = undefined; }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.refreshRequest;
  }

  me(): Observable<AuthUser> {
    if (!this.meRequest) {
      this.meRequest = this.http.get<AuthUser>(`${this.apiUrl}/me`).pipe(
        tap(user => this.userSubject.next(user)),
        finalize(() => { this.meRequest = undefined; }),
        shareReplay({ bufferSize: 1, refCount: false }),
      );
    }
    return this.meRequest;
  }

  restoreSession(): void {
    if (!this.accessToken) return;
    this.me().pipe(catchError(() => of(null))).subscribe();
  }

  logout(): Observable<void> {
    const refreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);
    return this.http.post<void>(`${this.apiUrl}/logout`, { refreshToken }).pipe(
      finalize(() => this.clearSession()),
    );
  }

  clearSession(): void {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    this.userSubject.next(null);
  }

  private saveSession(response: AuthResponse): void {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken);
    this.userSubject.next(response.user);
  }
}
