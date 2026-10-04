import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [AsyncPipe, RouterLink, RouterLinkActive],
  template: `
    @if (auth.user$ | async; as user) {
      <header class="site-header">
        <a class="brand" routerLink="/home">PHOTO BUDDY</a>
        <nav class="primary-nav" aria-label="Primary navigation">
          <a routerLink="/home" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">Home</a>
          <a routerLink="/nearby" routerLinkActive="active">Nearby</a>
          <a routerLink="/matches" routerLinkActive="active">Matches</a>
          <a routerLink="/posts" routerLinkActive="active">Photos</a>
          <a routerLink="/chat" routerLinkActive="active">Messages</a>
          <a routerLink="/notifications" routerLinkActive="active">Notifications</a>
          <a routerLink="/settings" routerLinkActive="active">Settings</a>
        </nav>
        <div class="account-actions">
          <a class="profile-link" [routerLink]="['/profile', user.username]" routerLinkActive="active">{{ user.firstName }}</a>
          <button type="button" aria-label="Sign out" title="Sign out" (click)="logout()">Sign out</button>
        </div>
      </header>
    }
  `,
  styles: [`
    :host { display: block; }
    .site-header {
      display: flex;
      align-items: center;
      gap: 22px;
      min-height: 68px;
      border-bottom: 1px solid #dce5dc;
      padding: 10px max(24px, calc((100vw - 1280px) / 2));
      background: #fff;
    }
    .brand { flex: 0 0 auto; color: #205845; font-size: .95rem; font-weight: 850; letter-spacing: .06em; }
    .primary-nav { display: flex; flex: 1; align-items: center; gap: 4px; min-width: 0; }
    .primary-nav a, .profile-link {
      display: inline-flex;
      align-items: center;
      min-height: 38px;
      border-radius: 6px;
      padding: 0 10px;
      color: #52665b;
      font-size: .88rem;
      font-weight: 650;
      white-space: nowrap;
    }
    .primary-nav a:hover, .profile-link:hover { background: #edf4ef; color: #205845; text-decoration: none; }
    .primary-nav a.active, .profile-link.active { background: #e4f2ea; color: #205845; }
    .account-actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
    .account-actions button {
      min-height: 36px;
      border: 1px solid #d5dfd7;
      border-radius: 6px;
      padding: 0 11px;
      background: #fff;
      color: #52665b;
      font-size: .83rem;
    }
    .account-actions button:hover { border-color: #a8c2b1; background: #f4f8f4; }
    @media (max-width: 980px) {
      .site-header { flex-wrap: wrap; gap: 8px 16px; padding-inline: 16px; }
      .primary-nav { order: 3; flex-basis: 100%; overflow-x: auto; }
      .primary-nav a { padding-inline: 9px; }
      .account-actions { margin-left: auto; }
    }
  `],
})
export class NavbarComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigateByUrl('/login'),
      error: () => {
        this.auth.clearSession();
        this.router.navigateByUrl('/login');
      },
    });
  }
}