import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NearbyUser, UserLocation } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class LocationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/locations`;

  update(latitude: number, longitude: number, accuracy: number): Observable<UserLocation> {
    return this.http.put<UserLocation>(`${this.apiUrl}/update`, { latitude, longitude, accuracy });
  }

  getMyLocation(): Observable<UserLocation> {
    return this.http.get<UserLocation>(`${this.apiUrl}/my-location`);
  }

  toggle(locationEnabled: boolean): Observable<UserLocation> {
    return this.http.put<UserLocation>(`${this.apiUrl}/toggle`, { locationEnabled });
  }

  nearby(latitude: number, longitude: number, radiusKm = 5): Observable<NearbyUser[]> {
    const params = new HttpParams()
      .set('latitude', latitude)
      .set('longitude', longitude)
      .set('radius', radiusKm);
    return this.http.get<NearbyUser[]>(`${environment.apiBaseUrl}/users/nearby`, { params });
  }
}
