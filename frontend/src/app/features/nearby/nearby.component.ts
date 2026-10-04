import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import * as L from 'leaflet';
import { NearbyUser, UserLocation } from '../../core/models/auth.models';
import { BuddyService } from '../../core/services/buddy.service';
import { LocationService } from '../../core/services/location.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="nearby-page">
      <a class="back-link" routerLink="/home">← Home</a>
      <header class="nearby-header">
        <div>
          <p class="eyebrow">FIND YOUR PHOTO BUDDY</p>
          <h1>Nearby photographers</h1>
          <p class="muted">Find people who are sharing their location around you.</p>
        </div>
        <div class="nearby-controls">
          <button type="button" class="secondary-button" (click)="findNearby()">Update my location</button>
        </div>
      </header>
      <p class="privacy-note">Your location is shared automatically with other users when you use Nearby. Browser location permission is required.</p>
      @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
      <div class="nearby-layout">
        <section class="map-card" aria-label="Nearby users map">
          <div #map class="nearby-map"></div>
          <p class="map-caption">Nearby map positions are approximate. Distances are rounded.</p>
        </section>
        <section class="nearby-results">
          <div class="results-heading"><h2>People nearby</h2><span>{{ nearbyUsers.length }}</span></div>
          @if (loading) { <p class="muted">Finding people nearby…</p> }
          @if (!loading && nearbyUsers.length === 0) { <p class="empty-state">No one sharing location was found within 5 km. Try again later.</p> }
          @for (person of nearbyUsers; track person.userId) {
            <article class="nearby-person">
              @if (person.profilePicture) {
                <img class="nearby-avatar" [src]="person.profilePicture" [alt]="person.firstName" />
              } @else {
                <div class="nearby-avatar avatar-fallback" aria-hidden="true">{{ person.firstName.charAt(0) }}{{ person.lastName.charAt(0) }}</div>
              }
              <div class="nearby-person-info">
                <a [routerLink]="['/profile', person.username]">{{ person.firstName }} {{ person.lastName }}</a>
                <span>{{ distanceLabel(person.distanceKm) }}@if (person.isPhotographer) { · Photographer }</span>
              </div>
              <button
                type="button"
                class="nearby-request-button"
                [disabled]="requestStatus[person.userId] === 'sending' || requestStatus[person.userId] === 'sent'"
                (click)="sendBuddyRequest(person)">
                {{ requestStatus[person.userId] === 'sent' ? 'Request sent' : requestStatus[person.userId] === 'sending' ? 'Sending…' : 'Send request' }}
              </button>
              <a class="profile-arrow" [routerLink]="['/profile', person.username]" [attr.aria-label]="'View ' + person.firstName + ' profile'">›</a>
              @if (requestErrors[person.userId]) { <p class="nearby-request-error" role="alert">{{ requestErrors[person.userId] }}</p> }
            </article>
          }
        </section>
      </div>
    </main>
  `,
})
export class NearbyComponent implements AfterViewInit, OnDestroy {
  @ViewChild('map', { static: true }) private mapElement!: ElementRef<HTMLDivElement>;
  private readonly locations = inject(LocationService);
  private readonly buddies = inject(BuddyService);
  private map?: L.Map;
  private nearbyLayer?: L.LayerGroup;
  private currentMarker?: L.CircleMarker;

  nearbyUsers: NearbyUser[] = [];
  currentLocation: UserLocation | null = null;
  loading = false;
  error = '';
  requestStatus: Record<number, 'sending' | 'sent' | undefined> = {};
  requestErrors: Record<number, string | undefined> = {};

  ngAfterViewInit(): void {
    this.map = L.map(this.mapElement.nativeElement).setView([17.385, 78.4867], 11);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(this.map);
    this.nearbyLayer = L.layerGroup().addTo(this.map);
    this.loadSavedLocation();
    setTimeout(() => this.map?.invalidateSize(), 0);
  }

  ngOnDestroy(): void { this.map?.remove(); }

  findNearby(): void { this.requestBrowserLocation(); }

  distanceLabel(distanceKm: number): string {
    return distanceKm < 1 ? `${Math.round(distanceKm * 1000)} m away` : `${distanceKm.toFixed(1)} km away`;
  }

  sendBuddyRequest(person: NearbyUser, popupButton?: HTMLButtonElement): void {
    const status = this.requestStatus[person.userId];
    if (status) {
      if (popupButton) {
        popupButton.disabled = true;
        popupButton.textContent = status === 'sent' ? 'Request sent' : 'Sending…';
      }
      return;
    }
    this.requestStatus = { ...this.requestStatus, [person.userId]: 'sending' };
    this.requestErrors = { ...this.requestErrors, [person.userId]: undefined };
    if (popupButton) {
      popupButton.disabled = true;
      popupButton.textContent = 'Sending…';
      popupButton.parentElement?.querySelector('.nearby-popup-error')?.remove();
    }
    this.buddies.sendRequest(person.userId).subscribe({
      next: () => {
        this.requestStatus = { ...this.requestStatus, [person.userId]: 'sent' };
        if (popupButton) popupButton.textContent = 'Request sent';
      },
      error: (response: HttpErrorResponse) => {
        this.requestStatus = { ...this.requestStatus, [person.userId]: undefined };
        const message = response.error?.message ?? 'Could not send buddy request.';
        this.requestErrors = {
          ...this.requestErrors,
          [person.userId]: message,
        };
        if (popupButton) {
          popupButton.disabled = false;
          popupButton.textContent = 'Send request';
          const error = document.createElement('p');
          error.className = 'nearby-popup-error';
          error.textContent = message;
          popupButton.insertAdjacentElement('afterend', error);
        }
      },
    });
  }

  private loadSavedLocation(): void {
    this.locations.getMyLocation().subscribe({
      next: location => {
        this.currentLocation = location;
        this.showCurrentLocation(location);
        this.requestBrowserLocation();
      },
      error: () => this.requestBrowserLocation(),
    });
  }

  private requestBrowserLocation(): void {
    if (!navigator.geolocation) {
      this.shareSavedLocation('This browser does not support location sharing.');
      return;
    }
    this.error = '';
    this.loading = true;
    navigator.geolocation.getCurrentPosition(
      position => {
        const { latitude, longitude, accuracy } = position.coords;
        this.locations.update(latitude, longitude, accuracy).subscribe({
          next: saved => {
            this.currentLocation = saved;
            this.showCurrentLocation(saved);
            this.searchNearby();
          },
          error: response => {
            this.loading = false;
            this.shareSavedLocation(response.error?.message ?? 'Could not save your location.');
          },
        });
      },
      () => {
        this.loading = false;
        this.shareSavedLocation('Location permission was unavailable.');
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 30000 },
    );
  }

  private shareSavedLocation(fallbackError: string): void {
    if (!this.currentLocation) {
      this.error = `${fallbackError} Allow location access and try again.`;
      return;
    }
    if (this.currentLocation.locationEnabled) {
      this.searchNearby();
      return;
    }
    this.loading = true;
    this.locations.toggle(true).subscribe({
      next: location => {
        this.currentLocation = location;
        this.showCurrentLocation(location);
        this.searchNearby();
      },
      error: response => {
        this.loading = false;
        this.showError(response, 'Could not automatically share your saved location.');
      },
    });
  }

  private searchNearby(): void {
    if (!this.currentLocation) return;
    this.error = '';
    this.loading = true;
    this.locations.nearby(this.currentLocation.latitude, this.currentLocation.longitude).subscribe({
      next: users => {
        this.nearbyUsers = users;
        this.loading = false;
        this.showNearbyUsers(users);
      },
      error: response => { this.loading = false; this.showError(response, 'Could not load nearby users.'); },
    });
  }

  private showCurrentLocation(location: UserLocation): void {
    const latLng: L.LatLngExpression = [location.latitude, location.longitude];
    this.currentMarker?.remove();
    this.currentMarker = L.circleMarker(latLng, {
      radius: 9, color: '#fff', weight: 3, fillColor: '#3578d4', fillOpacity: 1,
    }).bindPopup('Your location').addTo(this.map!);
    this.map?.setView(latLng, 13);
  }

  private showNearbyUsers(users: NearbyUser[]): void {
    this.map?.closePopup();
    this.nearbyLayer?.clearLayers();
    const mapPoints: L.LatLngExpression[] = [];
    const usersByPosition = new Map<string, NearbyUser[]>();
    if (this.currentLocation) mapPoints.push([this.currentLocation.latitude, this.currentLocation.longitude]);
    for (const user of users) {
      const positionKey = `${user.approximateLatitude.toFixed(2)},${user.approximateLongitude.toFixed(2)}`;
      const peopleAtPosition = usersByPosition.get(positionKey) ?? [];
      peopleAtPosition.push(user);
      usersByPosition.set(positionKey, peopleAtPosition);
      mapPoints.push([user.approximateLatitude, user.approximateLongitude]);
    }
    for (const people of usersByPosition.values()) {
      const [firstPerson] = people;
      const position: L.LatLngExpression = [firstPerson.approximateLatitude, firstPerson.approximateLongitude];
      const marker = people.length > 1
        ? L.marker(position, {
          icon: L.divIcon({
            className: 'nearby-cluster-marker',
            html: `<span>${people.length}</span>`,
            iconSize: [34, 34],
            iconAnchor: [17, 17],
          }),
        })
        : L.circleMarker(position, {
          radius: 8, color: '#fff', weight: 2, fillColor: '#31886c', fillOpacity: 0.95,
        });
      const popup = document.createElement('div');
      for (const person of people) {
        const entry = document.createElement('div');
        const name = document.createElement('strong');
        name.textContent = `${person.firstName} ${person.lastName}`;
        const distance = document.createElement('p');
        distance.textContent = this.distanceLabel(person.distanceKm);
        const link = document.createElement('a');
        link.href = `/profile/${encodeURIComponent(person.username)}`;
        link.textContent = 'View profile';
        const requestButton = document.createElement('button');
        requestButton.type = 'button';
        requestButton.className = 'nearby-popup-request';
        requestButton.textContent = this.requestStatus[person.userId] === 'sent' ? 'Request sent' : 'Send request';
        requestButton.disabled = this.requestStatus[person.userId] !== undefined;
        requestButton.addEventListener('click', event => {
          L.DomEvent.stopPropagation(event);
          this.sendBuddyRequest(person, requestButton);
        });
        entry.className = 'nearby-popup-person';
        entry.append(name, distance, link, requestButton);
        popup.append(entry);
      }
      marker.bindPopup(popup).addTo(this.nearbyLayer!);
    }
    if (mapPoints.length > 1) this.map?.fitBounds(L.latLngBounds(mapPoints), { padding: [28, 28], maxZoom: 13 });
  }

  private showError(response: HttpErrorResponse, fallback: string): void {
    this.error = response.error?.message ?? fallback;
  }
}
