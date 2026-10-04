import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, switchMap, take, tap } from 'rxjs';
import { AuthUser, UserProfile } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { BuddyService } from '../../core/services/buddy.service';
import { ChatService } from '../../core/services/chat.service';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="profile-page">
      <a class="back-link" routerLink="/home">← Home</a>
      @if (loading) { <section class="profile-card"><p>Loading profile…</p></section> }
      @if (error) { <section class="profile-card"><p class="form-error" role="alert">{{ error }}</p></section> }
      @if (profile; as profile) {
        <section class="profile-card">
          <div class="profile-heading">
            @if (profile.profilePicture) {
              <img class="profile-avatar" [src]="profile.profilePicture" [alt]="profile.firstName + ' ' + profile.lastName" />
            } @else {
              <div class="profile-avatar avatar-fallback" aria-hidden="true">{{ initials(profile) }}</div>
            }
            <div class="profile-heading-copy">
              <p class="eyebrow">PHOTO BUDDY PROFILE</p>
              <h1>{{ profile.firstName }} {{ profile.lastName }}</h1>
              <p class="muted">&#64;{{ profile.username }}</p>
            </div>
          </div>
          @if (profile.isPhotographer) { <span class="photographer-badge">Photographer</span> }
          <p class="profile-bio">{{ profile.bio || 'No bio added yet.' }}</p>
          <div class="profile-stats">
            <div><strong>{{ profile.postCount }}</strong><span>Posts</span></div>
            <div><strong>{{ profile.buddyCount }}</strong><span>Buddies</span></div>
          </div>
          <div class="profile-actions">
            @if (isOwnProfile) {
              <a class="primary-button" routerLink="/profile/edit">Edit profile</a>
            } @else {
              <button type="button" class="primary-button" [disabled]="requesting || requestSent" (click)="sendBuddyRequest()">{{ requestSent ? 'Request sent' : requesting ? 'Sending…' : 'Send buddy request' }}</button>
              @if (requestError) { <p class="form-error" role="alert">{{ requestError }}</p> }
              <button type="button" class="secondary-button" [disabled]="openingChat" (click)="openChat()">{{ openingChat ? 'Opening…' : 'Message' }}</button>
              @if (chatError) { <p class="form-error" role="alert">{{ chatError }}</p> }
            }
          </div>
        </section>
      }
    </main>
  `,
})
export class ProfileComponent implements OnInit {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly auth = inject(AuthService);
  private readonly profiles = inject(ProfileService);
  private readonly buddies = inject(BuddyService);
  private readonly chats = inject(ChatService);
  private readonly router = inject(Router);
  profile: UserProfile | null = null;
  currentUsername = '';
  loading = true;
  error = '';
  requestError = '';
  requesting = false;
  requestSent = false;
  openingChat = false;
  chatError = '';

  get isOwnProfile(): boolean { return this.profile?.username === this.currentUsername; }

  ngOnInit(): void {
    this.route.paramMap.pipe(
      tap(() => { this.loading = true; this.error = ''; this.profile = null; }),
      switchMap(params => this.auth.user$.pipe(
        take(1),
        switchMap(user => this.loadForUser(params.get('username'), user)),
      )),
    ).subscribe({
      next: profile => {
        this.profile = profile;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Unable to load this profile.';
        this.loading = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  initials(profile: UserProfile): string {
    return `${profile.firstName.charAt(0)}${profile.lastName.charAt(0)}`.toUpperCase();
  }

  sendBuddyRequest(): void {
    if (!this.profile || this.requesting || this.requestSent) return;
    this.requesting = true;
    this.requestError = '';
    this.buddies.sendRequest(this.profile.id).subscribe({
      next: () => {
        this.requestSent = true;
        this.requesting = false;
        this.changeDetector.markForCheck();
      },
      error: (response: HttpErrorResponse) => {
        this.requestError = response.error?.message ?? 'Could not send buddy request.';
        this.requesting = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  openChat(): void {
    if (!this.profile || this.openingChat) return;
    this.openingChat = true;
    this.chatError = '';
    this.chats.openRoom(this.profile.id).subscribe({
      next: room => {
        this.changeDetector.markForCheck();
        void this.router.navigate(['/chat', room.roomId]);
      },
      error: (response: HttpErrorResponse) => {
        this.chatError = response.error?.message ?? 'Could not open this conversation.';
        this.openingChat = false;
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadForUser(username: string | null, currentUser: AuthUser | null): Observable<UserProfile> {
    if (currentUser) {
      this.currentUsername = currentUser.username;
      return username ? this.profiles.getProfile(username) : this.profiles.getProfile(currentUser.username);
    }
    return this.auth.me().pipe(switchMap(user => {
      this.currentUsername = user.username;
      return this.profiles.getProfile(username ?? user.username);
    }));
  }
}
