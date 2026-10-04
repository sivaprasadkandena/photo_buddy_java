import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BuddyMatch, BuddyRequest } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class BuddyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl;

  sendRequest(userId: number): Observable<BuddyRequest> {
    return this.http.post<BuddyRequest>(`${this.apiUrl}/buddy-requests/${userId}`, {});
  }

  received(): Observable<BuddyRequest[]> {
    return this.http.get<BuddyRequest[]>(`${this.apiUrl}/buddy-requests/received`);
  }

  sent(): Observable<BuddyRequest[]> {
    return this.http.get<BuddyRequest[]>(`${this.apiUrl}/buddy-requests/sent`);
  }

  matches(): Observable<BuddyMatch[]> {
    return this.http.get<BuddyMatch[]>(`${this.apiUrl}/matches`);
  }

  accept(id: number): Observable<BuddyRequest> {
    return this.http.put<BuddyRequest>(`${this.apiUrl}/buddy-requests/${id}/accept`, {});
  }

  reject(id: number): Observable<BuddyRequest> {
    return this.http.put<BuddyRequest>(`${this.apiUrl}/buddy-requests/${id}/reject`, {});
  }

  cancel(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/buddy-requests/${id}`);
  }
}
