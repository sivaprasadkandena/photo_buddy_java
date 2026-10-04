import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  standalone: true,
  imports: [AsyncPipe, RouterLink],
  template: `
    @if (!(auth.user$ | async)) {
      <header class="public-nav">
        <a class="public-brand" routerLink="/">PHOTO BUDDY</a>
        <nav aria-label="Main navigation">
          <a href="#how-it-works">How it works</a>
          <a href="#community">Community</a>
          <a href="#safety">Safety</a>
        </nav>
        <div class="public-nav-actions">
          <a class="login-link" routerLink="/login">Log in</a>
          <a class="join-link" routerLink="/register">Join Photo Buddy</a>
        </div>
      </header>
    }

    <main class="landing-page">
      <section class="landing-hero" aria-labelledby="hero-title">
        <img class="hero-photo" src="https://photo-buddy.onrender.com/static/images/photo2.png" alt="Traveler capturing a photograph on a trip" />
        <div class="hero-shade"></div>
        <div class="hero-copy">
          <p class="hero-eyebrow">TRUSTED TRAVEL PHOTO EXCHANGE</p>
          <h1 id="hero-title">Never miss the shot because you traveled alone.</h1>
          <p class="hero-description">Find a photo partner nearby, agree on the plan, and get back to enjoying the place you came to see.</p>
          @if (auth.user$ | async) {
            <a class="hero-cta" routerLink="/home">Open Photo Buddy <span aria-hidden="true">→</span></a>
          } @else {
            <div class="hero-actions">
              <a class="hero-cta" routerLink="/register">Start free <span aria-hidden="true">→</span></a>
              <a class="hero-secondary" routerLink="/login">I already have an account</a>
            </div>
          }
          <p class="hero-note">Meet in public. Share only what you choose.</p>
        </div>
        <div class="hero-index"><span>01</span><span>PHOTO BUDDY / TRAVEL COMMUNITY</span></div>
      </section>

      <section class="workflow-section" id="how-it-works" aria-labelledby="workflow-title">
        <div class="section-heading">
          <p class="section-eyebrow">A BETTER WAY TO GET THE SHOT</p>
          <h2 id="workflow-title">Good photos start with a good connection.</h2>
          <p>From first hello to the photo you came for, keep the whole exchange simple.</p>
        </div>
        <div class="workflow-steps">
          <article><span class="step-number">01</span><h3>Check in</h3><p>Share your location when you want nearby photographers to find you.</p></article>
          <article><span class="step-number">02</span><h3>Choose your buddy</h3><p>Review profiles and decide who feels right for a quick photo exchange.</p></article>
          <article><span class="step-number">03</span><h3>Coordinate</h3><p>Message first to agree on a public meeting place and expectations.</p></article>
          <article><span class="step-number">04</span><h3>Capture the moment</h3><p>Take the photo, share your favorite frames, and keep the memory.</p></article>
        </div>
      </section>

      <section class="community-section" id="community" aria-labelledby="community-title">
        <div class="community-copy">
          <p class="section-eyebrow">MADE FOR THE MOMENTS IN BETWEEN</p>
          <h2 id="community-title">A community for travelers and the people behind the camera.</h2>
          <p>Find a friendly second set of hands or connect with a professional photographer. Profiles, requests, and in-app messages help you make the plan before you meet.</p>
          <a class="text-link" routerLink="/register">Find your photo buddy <span aria-hidden="true">→</span></a>
        </div>
        <figure class="community-photo-wrap">
          <img src="https://photo-buddy.onrender.com/static/images/photo2.png" alt="A travel portrait from the Photo Buddy community" loading="lazy" />
          <figcaption><span>GOOD COMPANY, GREAT LIGHT</span><span>PHOTO BUDDY COMMUNITY</span></figcaption>
        </figure>
      </section>

      <section class="safety-section" id="safety" aria-labelledby="safety-title">
        <div class="safety-mark" aria-hidden="true">PB</div>
        <div>
          <p class="section-eyebrow">SAFETY FIRST</p>
          <h2 id="safety-title">A little planning makes meeting feel better.</h2>
          <p>Get to know each other through your profiles and chat. Choose a public place, share your location only when you mean to, and make the exchange work for you.</p>
        </div>
        <a class="safety-link" routerLink="/register">Meet your community <span aria-hidden="true">→</span></a>
      </section>

      <footer class="landing-footer">
        <a class="public-brand" routerLink="/">PHOTO BUDDY</a>
        <p>Find trusted photo partners nearby. Keep the memory yours.</p>
        <nav aria-label="Footer navigation">
          <a routerLink="/posts">Community feed</a>
          <a routerLink="/nearby">Nearby travelers</a>
          <a routerLink="/matches">Matches</a>
          <a routerLink="/chat">Messaging</a>
        </nav>
      </footer>
    </main>
  `,
})
export class LandingComponent {
  readonly auth = inject(AuthService);
}
