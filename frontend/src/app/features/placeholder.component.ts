import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  standalone: true,
  template: `
    <main class="home-card">
      <p class="eyebrow">PHOTO BUDDY</p>
      <h1>{{ title }}</h1>
      <p>This page will be added in a later feature phase.</p>
    </main>
  `,
})
export class PlaceholderComponent {
  private readonly route = inject(ActivatedRoute);
  readonly title = this.route.snapshot.data['title'] as string;
}
