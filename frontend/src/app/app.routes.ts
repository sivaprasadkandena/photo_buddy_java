import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./features/home/landing.component').then(m => m.LandingComponent) },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
  { path: 'home', canActivate: [authGuard], loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  {
    path: '', canActivate: [authGuard],
    children: [
      { path: 'profile/edit', loadComponent: () => import('./features/profile/profile-edit.component').then(m => m.ProfileEditComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'profile/:username', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'nearby', loadComponent: () => import('./features/nearby/nearby.component').then(m => m.NearbyComponent) },
      { path: 'matches', loadComponent: () => import('./features/matches/matches.component').then(m => m.MatchesComponent) },
      { path: 'posts/create', loadComponent: () => import('./features/posts/create-post.component').then(m => m.CreatePostComponent) },
      { path: 'posts/:id', loadComponent: () => import('./features/posts/post-detail.component').then(m => m.PostDetailComponent) },
      { path: 'posts', loadComponent: () => import('./features/posts/post-feed.component').then(m => m.PostFeedComponent) },
      { path: 'chat/:roomId', loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent) },
      { path: 'chat', loadComponent: () => import('./features/chat/chat.component').then(m => m.ChatComponent) },
      { path: 'notifications', loadComponent: () => import('./features/placeholder.component').then(m => m.PlaceholderComponent), data: { title: 'Notifications' } },
      { path: 'settings', loadComponent: () => import('./features/placeholder.component').then(m => m.PlaceholderComponent), data: { title: 'Settings' } },
    ],
  },
  { path: '**', redirectTo: 'login' },
];
