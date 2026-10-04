import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  standalone: true,
  template: `
    <main class="dashboard-page">
      <header class="dashboard-heading">
        <div>
          <p class="eyebrow">YOUR PHOTO BUDDY</p>
          <h1>Welcome{{ (auth.user$ | async)?.firstName ? ', ' + (auth.user$ | async)?.firstName : '' }}.</h1>
          <p class="muted">Make room for the moments you came to remember.</p>
        </div>
        <div class="dashboard-actions">
          <a class="primary-button" routerLink="/nearby">Explore nearby <span aria-hidden="true">→</span></a>
          <a class="secondary-button" routerLink="/posts/create">Share a photo</a>
        </div>
      </header>

      <section class="dashboard-feature" aria-label="Photo Buddy community">
        <img src="https://photo-buddy.onrender.com/static/images/photo2.png" alt="A traveler enjoying a photo moment" />
        <div class="dashboard-feature-shade"></div>
        <div class="dashboard-feature-copy">
          <p>TRAVEL PHOTO EXCHANGE</p>
          <h2>Find your people. Keep the moment.</h2>
          <a routerLink="/nearby">See who's nearby <span aria-hidden="true">→</span></a>
        </div>
      </section>

      <section class="dashboard-links" aria-label="Community destinations">
        <a routerLink="/nearby"><span>01 / DISCOVER</span><strong>Nearby travelers</strong><span class="destination-arrow" aria-hidden="true">↗</span></a>
        <a routerLink="/matches"><span>02 / CONNECT</span><strong>Your matches</strong><span class="destination-arrow" aria-hidden="true">↗</span></a>
        <a routerLink="/chat"><span>03 / COORDINATE</span><strong>Messages</strong><span class="destination-arrow" aria-hidden="true">↗</span></a>
        <a routerLink="/posts"><span>04 / REMEMBER</span><strong>Community feed</strong><span class="destination-arrow" aria-hidden="true">↗</span></a>
      </section>
      @if (auth.user$ | async; as user) {
        <footer class="dashboard-profile"><span>READY FOR YOUR NEXT PHOTO?</span><a [routerLink]="['/profile', user.username]">View your profile <span aria-hidden="true">→</span></a></footer>
      }
    </main>
  `,
  imports: [AsyncPipe, RouterLink],
  styles: [`
    .dashboard-page { width: min(100% - 48px, 1240px); margin: 44px auto 72px; }
    .dashboard-heading { display: flex; justify-content: space-between; align-items: end; gap: 28px; margin-bottom: 32px; }
    .dashboard-heading h1 { margin: 8px 0; font-family: Georgia, 'Times New Roman', serif; font-size: 48px; font-weight: 500; }
    .dashboard-heading .muted { margin: 0; }
    .dashboard-actions { display: flex; flex-wrap: wrap; gap: 10px; }
    .dashboard-feature { position: relative; display: flex; min-height: 390px; align-items: end; overflow: hidden; background: #31584b; color: #fff; }
    .dashboard-feature > img, .dashboard-feature-shade { position: absolute; inset: 0; width: 100%; height: 100%; }
    .dashboard-feature > img { object-fit: cover; object-position: center 42%; }
    .dashboard-feature-shade { background: linear-gradient(0deg, #142820d9 0%, #14282065 48%, #14282000 100%); }
    .dashboard-feature-copy { position: relative; z-index: 1; max-width: 700px; padding: 36px 42px; }
    .dashboard-feature-copy > p { margin: 0 0 12px; color: #d2e8d9; font-size: .72rem; font-weight: 800; letter-spacing: .14em; }
    .dashboard-feature-copy h2 { margin: 0 0 18px; font-family: Georgia, 'Times New Roman', serif; font-size: 42px; font-weight: 500; }
    .dashboard-feature-copy a { color: #fff; font-weight: 700; }
    .dashboard-links { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); border-top: 1px solid #cfdbd1; margin-top: 52px; }
    .dashboard-links > a { position: relative; display: grid; min-height: 125px; align-content: space-between; gap: 20px; border-bottom: 1px solid #cfdbd1; padding: 18px 24px 18px 0; color: #19352d; }
    .dashboard-links > a + a { border-left: 1px solid #cfdbd1; padding-left: 24px; }
    .dashboard-links > a > span:first-child, .dashboard-profile > span { color: #27745e; font-size: .68rem; font-weight: 800; letter-spacing: .1em; }
    .dashboard-links strong { font-family: Georgia, 'Times New Roman', serif; font-size: 1.35rem; font-weight: 500; }
    .destination-arrow { position: absolute; right: 18px; bottom: 18px; color: #27745e; font-size: 1.2rem; }
    .dashboard-profile { display: flex; justify-content: space-between; gap: 20px; border-bottom: 1px solid #cfdbd1; padding: 21px 0; }
    .dashboard-profile a { color: #27745e; font-weight: 700; }
    @media (max-width: 760px) {
      .dashboard-page { width: min(100% - 32px, 1240px); margin-top: 28px; }
      .dashboard-heading { align-items: flex-start; flex-direction: column; }
      .dashboard-heading h1 { font-size: 40px; }
      .dashboard-feature { min-height: 340px; }
      .dashboard-feature-copy { padding: 28px 24px; }
      .dashboard-feature-copy h2 { font-size: 34px; }
      .dashboard-links { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 36px; }
      .dashboard-links > a { min-height: 110px; padding-right: 10px; }
      .dashboard-links > a + a { padding-left: 16px; }
      .dashboard-links > a:nth-child(3) { border-left: 0; padding-left: 0; }
      .dashboard-links > a:nth-child(n + 3) { border-top: 0; }
      .dashboard-links strong { font-size: 1.15rem; }
      .dashboard-profile { align-items: flex-start; flex-direction: column; }
    }
  `],
})
export class HomeComponent {
  readonly auth = inject(AuthService);
}
