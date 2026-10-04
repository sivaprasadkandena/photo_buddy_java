import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-card">
      <a class="brand" routerLink="/login">Photo Buddy</a>
      <h1>Welcome back</h1>
      <p class="muted">Sign in to find your next photography buddy.</p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <label>Email<input type="email" formControlName="email" autocomplete="email" /></label>
        @if (form.controls.email.touched && form.controls.email.invalid) { <small>Enter a valid email address.</small> }
        <label>Password<input type="password" formControlName="password" autocomplete="current-password" /></label>
        @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
        <button type="submit" [disabled]="form.invalid || submitting">{{ submitting ? 'Signing in…' : 'Sign in' }}</button>
      </form>
      <p class="muted">New to Photo Buddy? <a routerLink="/register">Create an account</a></p>
    </main>
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  error = '';
  submitting = false;

  submit(): void {
    if (this.form.invalid || this.submitting) return;
    this.error = '';
    this.submitting = true;
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl?.startsWith('/') ? returnUrl : '/home');
      },
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Unable to sign in. Please try again.';
        this.submitting = false;
      },
    });
  }
}
