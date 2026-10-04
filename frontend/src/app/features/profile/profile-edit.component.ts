import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { Gender } from '../../core/models/auth.models';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-card register-card">
      <a class="back-link" routerLink="/profile">← Profile</a>
      <h1>Edit profile</h1>
      <p class="muted">Update how other Photo Buddy members see you.</p>
      @if (loading) { <p>Loading your details…</p> }
      @if (!loading) {
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <div class="form-row">
            <label>First name<input formControlName="firstName" autocomplete="given-name" /></label>
            <label>Last name<input formControlName="lastName" autocomplete="family-name" /></label>
          </div>
          <label>Bio<textarea formControlName="bio" maxlength="500" rows="4"></textarea></label>
          <label>Gender
            <select formControlName="gender">
              @for (option of genders; track option.value) { <option [value]="option.value">{{ option.label }}</option> }
            </select>
          </label>
          <label>Profile picture URL<input type="url" formControlName="profilePicture" /></label>
          <label class="checkbox-label"><input type="checkbox" formControlName="isPhotographer" /> I enjoy helping others with photography</label>
          @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
          <div class="profile-actions">
            <button type="submit" [disabled]="form.invalid || submitting">{{ submitting ? 'Saving…' : 'Save changes' }}</button>
            <a class="secondary-button" routerLink="/profile">Cancel</a>
          </div>
        </form>
      }
    </main>
  `,
})
export class ProfileEditComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly profiles = inject(ProfileService);
  private readonly router = inject(Router);
  readonly genders: { value: Gender; label: string }[] = [
    { value: 'FEMALE', label: 'Female' }, { value: 'MALE', label: 'Male' },
    { value: 'NON_BINARY', label: 'Non-binary' }, { value: 'OTHER', label: 'Other' },
    { value: 'PREFER_NOT_TO_SAY', label: 'Prefer not to say' },
  ];
  readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(60)]],
    lastName: ['', [Validators.required, Validators.maxLength(60)]],
    bio: ['', Validators.maxLength(500)],
    gender: ['PREFER_NOT_TO_SAY' as Gender, Validators.required],
    profilePicture: ['', [Validators.maxLength(2048), Validators.pattern(/^$|^https?:\/\/.+$/)]],
    isPhotographer: [false],
  });
  username = '';
  loading = true;
  submitting = false;
  error = '';

  ngOnInit(): void {
    this.auth.me().pipe(switchMap(user => {
      this.username = user.username;
      return this.profiles.getProfile(user.username);
    })).subscribe({
      next: profile => {
        this.form.patchValue({
          firstName: profile.firstName,
          lastName: profile.lastName,
          bio: profile.bio ?? '',
          gender: profile.gender,
          profilePicture: profile.profilePicture ?? '',
          isPhotographer: profile.isPhotographer,
        });
        this.loading = false;
      },
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Unable to load your profile.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.submitting) return;
    this.submitting = true;
    this.error = '';
    const value = this.form.getRawValue();
    this.profiles.updateProfile({
      ...value,
      bio: value.bio.trim() || null,
      profilePicture: value.profilePicture.trim() || null,
    }).subscribe({
      next: profile => this.router.navigate(['/profile', profile.username]),
      error: (response: HttpErrorResponse) => {
        this.error = response.error?.message ?? 'Unable to save profile changes.';
        this.submitting = false;
      },
    });
  }
}
