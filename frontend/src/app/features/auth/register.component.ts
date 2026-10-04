import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { Gender } from '../../core/models/auth.models';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-card register-card">
      <a class="brand" routerLink="/login">Photo Buddy</a>
      <h1>Create your account</h1>
      <p class="muted">Meet people who love capturing the moment.</p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="form-row">
          <label>First name<input formControlName="firstName" autocomplete="given-name" /></label>
          <label>Last name<input formControlName="lastName" autocomplete="family-name" /></label>
        </div>
        <label>Username<input formControlName="username" autocomplete="username" /></label>
        @if (form.controls.username.touched && form.controls.username.invalid) { <small>Use 3–30 letters, numbers, or underscores.</small> }
        <label>Email<input type="email" formControlName="email" autocomplete="email" /></label>
        <label>Gender
          <select formControlName="gender">
            <option value="" disabled>Select an option</option>
            @for (option of genders; track option.value) { <option [value]="option.value">{{ option.label }}</option> }
          </select>
        </label>
        <label>Password<input type="password" formControlName="password" autocomplete="new-password" />
          <small>12–72 characters, including uppercase, lowercase, a number, and a symbol.</small>
        </label>
        <label>Confirm password<input type="password" formControlName="confirmPassword" autocomplete="new-password" /></label>
        @if (form.controls.confirmPassword.touched && form.controls.password.value !== form.controls.confirmPassword.value) { <small>Passwords do not match.</small> }
        <label>Bio <span class="muted">(optional)</span><textarea formControlName="bio" maxlength="500" rows="3"></textarea></label>
        <label>Profile picture URL <span class="muted">(optional)</span><input type="url" formControlName="profilePicture" /></label>
        <label class="checkbox-label"><input type="checkbox" formControlName="isPhotographer" /> I enjoy helping others with photography</label>
        @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
        <button type="submit" [disabled]="form.invalid || submitting || form.controls.password.value !== form.controls.confirmPassword.value">
          {{ submitting ? 'Creating account…' : 'Create account' }}
        </button>
      </form>
      <p class="muted">Already have an account? <a routerLink="/login">Sign in</a></p>
    </main>
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly genders: { value: Gender; label: string }[] = [
    { value: 'FEMALE', label: 'Female' }, { value: 'MALE', label: 'Male' },
    { value: 'NON_BINARY', label: 'Non-binary' }, { value: 'OTHER', label: 'Other' },
    { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
  ];
  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30), Validators.pattern(/^[A-Za-z0-9_]+$/)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72), Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{12,72}$/)]],
    confirmPassword: ['', Validators.required],
    gender: ['' as Gender | '', Validators.required],
    bio: ['', Validators.maxLength(500)],
    profilePicture: ['', Validators.pattern(/^$|^https?:\/\/.+$/)],
    isPhotographer: [false],
  });
  error = '';
  submitting = false;

  submit(): void {
    if (this.form.invalid || this.submitting) return;
    const value = this.form.getRawValue();
    if (value.password !== value.confirmPassword || !value.gender) return;
    this.submitting = true;
    this.error = '';
    this.auth.register({ ...value, gender: value.gender, bio: value.bio || undefined, profilePicture: value.profilePicture || undefined }).subscribe({
      next: () => this.router.navigateByUrl('/home'),
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Unable to create your account. Please try again.';
        this.submitting = false;
      },
    });
  }
}
