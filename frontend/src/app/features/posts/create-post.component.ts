import { Component, inject } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { PostStyle } from '../../core/models/auth.models';
import { PostService } from '../../core/services/post.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, TitleCasePipe],
  template: `
    <main class="post-form-page">
      <a class="back-link" routerLink="/posts">← Back to posts</a>
      <section class="post-form-card">
        <p class="eyebrow">SHARE A MOMENT</p><h1>Create a post</h1>
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>Photo <input type="file" accept="image/jpeg,image/png,image/webp" (change)="selectImage($event)" /></label>
          <small>JPEG, PNG, or WebP, up to 10 MB.</small>
          @if (preview) { <img class="post-preview" [src]="preview" alt="Selected photo preview" /> }
          <label>Caption <textarea rows="4" maxlength="2200" formControlName="caption" placeholder="Add a caption"></textarea></label>
          <label>Photo style <select formControlName="style"><option value="">Choose a style</option>@for (style of styles; track style) { <option [value]="style">{{ style | titlecase }}</option> }</select></label>
          @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
          <button type="submit" [disabled]="form.invalid || !image || saving">{{ saving ? 'Publishing…' : 'Publish post' }}</button>
        </form>
      </section>
    </main>
  `,
})
export class CreatePostComponent {
  private readonly fb = inject(FormBuilder);
  private readonly posts = inject(PostService);
  private readonly router = inject(Router);
  readonly styles: PostStyle[] = ['PORTRAIT', 'LANDSCAPE', 'STREET', 'NATURE', 'FASHION', 'TRAVEL', 'OTHER'];
  readonly form = this.fb.nonNullable.group({ caption: ['', Validators.maxLength(2200)], style: ['', Validators.required] });
  image: File | null = null;
  preview = '';
  saving = false;
  error = '';

  selectImage(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.image = file;
    this.error = '';
    if (this.preview) URL.revokeObjectURL(this.preview);
    this.preview = file ? URL.createObjectURL(file) : '';
  }

  submit(): void {
    if (!this.image || this.form.invalid || this.saving) return;
    if (this.image.size > 10 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(this.image.type)) {
      this.error = 'Choose a JPEG, PNG, or WebP image up to 10 MB.';
      return;
    }
    this.saving = true;
    this.posts.create(this.image, this.form.controls.caption.value, this.form.controls.style.value as PostStyle).subscribe({
      next: post => this.router.navigate(['/posts', post.postId]),
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Could not publish your post.';
        this.saving = false;
      },
    });
  }
}
