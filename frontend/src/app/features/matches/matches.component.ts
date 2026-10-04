import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router, RouterLink } from '@angular/router';
import { Subject, forkJoin, interval, takeUntil } from 'rxjs';
import { BuddyMatch, BuddyRequest } from '../../core/models/auth.models';
import { BuddyService } from '../../core/services/buddy.service';
import { ChatService } from '../../core/services/chat.service';

type MatchTab = 'matches' | 'received' | 'sent';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="matches-page">
      <a class="back-link" routerLink="/home">← Home</a>
      <header class="matches-header">
        <p class="eyebrow">YOUR PHOTO COMMUNITY</p>
        <h1>Buddies & requests</h1>
        <p class="muted">Accept a request to add someone to your matches.</p>
      </header>
      <nav class="match-tabs" aria-label="Buddy lists">
        <button type="button" [class.active-tab]="tab === 'matches'" (click)="select('matches')">Matches</button>
        <button type="button" [class.active-tab]="tab === 'received'" (click)="select('received')">Received <span>{{ received.length }}</span></button>
        <button type="button" [class.active-tab]="tab === 'sent'" (click)="select('sent')">Sent <span>{{ sent.length }}</span></button>
      </nav>
      @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
      <section class="match-list">
        @if (loading) { <p class="muted">Loading…</p> }
        @if (!loading && tab === 'matches' && matches.length === 0) { <p class="empty-state">No matches yet. Find people nearby and send a buddy request.</p> }
        @for (match of matches; track match.id) {
          @if (tab === 'matches') {
            <article class="match-person">
              <div class="match-avatar">{{ initials(match.user.firstName, match.user.lastName) }}</div>
              <div class="match-person-copy"><a [routerLink]="['/profile', match.user.username]">{{ match.user.firstName }} {{ match.user.lastName }}</a><span>{{ match.distanceKm === null ? 'Location distance unavailable' : distanceLabel(match.distanceKm) }}</span></div>
              <button class="secondary-button" type="button" [disabled]="openingChatId === match.user.id" (click)="openChat(match)">{{ openingChatId === match.user.id ? 'Opening…' : 'Message' }}</button>
            </article>
          }
        }
        @if (!loading && tab === 'received' && received.length === 0) { <p class="empty-state">No pending requests received.</p> }
        @for (request of received; track request.id) {
          @if (tab === 'received') {
            <article class="match-person">
              <div class="match-avatar">{{ initials(request.sender.firstName, request.sender.lastName) }}</div>
              <div class="match-person-copy"><a [routerLink]="['/profile', request.sender.username]">{{ request.sender.firstName }} {{ request.sender.lastName }}</a><span>Wants to connect</span></div>
              <div class="request-actions"><button type="button" [disabled]="busyId === request.id" (click)="accept(request)">Accept</button><button class="secondary-button" type="button" [disabled]="busyId === request.id" (click)="reject(request)">Decline</button></div>
            </article>
          }
        }
        @if (!loading && tab === 'sent' && sent.length === 0) { <p class="empty-state">No pending requests sent.</p> }
        @for (request of sent; track request.id) {
          @if (tab === 'sent') {
            <article class="match-person">
              <div class="match-avatar">{{ initials(request.receiver.firstName, request.receiver.lastName) }}</div>
              <div class="match-person-copy"><a [routerLink]="['/profile', request.receiver.username]">{{ request.receiver.firstName }} {{ request.receiver.lastName }}</a><span>Request pending</span></div>
              <button class="secondary-button" type="button" [disabled]="busyId === request.id" (click)="cancel(request)">Cancel</button>
            </article>
          }
        }
      </section>
    </main>
  `,
})
export class MatchesComponent implements OnInit, OnDestroy {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly buddies = inject(BuddyService);
  private readonly chats = inject(ChatService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();
  private readonly refreshIntervalMs = 10000;
  private hasLoaded = false;
  private refreshing = false;
  private refreshQueued = false;
  tab: MatchTab = 'matches';
  matches: BuddyMatch[] = [];
  received: BuddyRequest[] = [];
  sent: BuddyRequest[] = [];
  loading = true;
  error = '';
  busyId: number | null = null;
  openingChatId: number | null = null;

  ngOnInit(): void {
    this.refresh();
    interval(this.refreshIntervalMs).pipe(takeUntil(this.destroy$)).subscribe(() => this.refresh());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  select(tab: MatchTab): void { this.tab = tab; this.error = ''; }

  initials(first: string, last: string): string { return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase(); }

  distanceLabel(km: number): string { return km < 1 ? `${Math.round(km * 1000)} m away` : `${km.toFixed(1)} km away`; }

  accept(request: BuddyRequest): void { this.runAction(request.id, () => this.buddies.accept(request.id).subscribe({ next: () => { this.busyId = null; this.refresh(); }, error: e => this.fail(e) })); }
  reject(request: BuddyRequest): void { this.runAction(request.id, () => this.buddies.reject(request.id).subscribe({ next: () => { this.busyId = null; this.refresh(); }, error: e => this.fail(e) })); }
  cancel(request: BuddyRequest): void { this.runAction(request.id, () => this.buddies.cancel(request.id).subscribe({ next: () => { this.busyId = null; this.refresh(); }, error: e => this.fail(e) })); }

  openChat(match: BuddyMatch): void {
    if (this.openingChatId !== null) return;
    this.openingChatId = match.user.id;
    this.error = '';
    this.chats.openRoom(match.user.id).subscribe({
      next: room => {
        this.openingChatId = null;
        this.changeDetector.markForCheck();
        void this.router.navigate(['/chat', room.roomId]);
      },
      error: response => {
        this.openingChatId = null;
        this.fail(response);
      },
    });
  }

  private runAction(id: number, action: () => void): void { this.busyId = id; this.error = ''; action(); }

  private refresh(): void {
    if (this.refreshing) {
      this.refreshQueued = true;
      return;
    }
    this.refreshing = true;
    if (!this.hasLoaded) this.loading = true;
    forkJoin({ matches: this.buddies.matches(), received: this.buddies.received(), sent: this.buddies.sent() }).subscribe({
      next: result => {
        this.matches = result.matches;
        this.received = result.received;
        this.sent = result.sent;
        this.loading = false;
        this.hasLoaded = true;
        this.changeDetector.markForCheck();
        this.finishRefresh();
      },
      error: e => {
        this.hasLoaded = true;
        this.fail(e);
        this.finishRefresh();
      },
    });
  }

  private finishRefresh(): void {
    this.refreshing = false;
    if (this.refreshQueued) {
      this.refreshQueued = false;
      this.refresh();
    }
  }

  private fail(response: HttpErrorResponse): void {
    this.error = response.error?.message ?? 'Could not load buddy data. Please try again.';
    this.loading = false;
    this.busyId = null;
    this.changeDetector.markForCheck();
  }
}
