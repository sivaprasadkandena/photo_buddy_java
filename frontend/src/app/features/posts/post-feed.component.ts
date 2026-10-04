import { Component, OnInit, inject } from '@angular/core';
import { AsyncPipe, DatePipe, TitleCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { PostService } from '../../core/services/post.service';
import { PostItem, PostPage } from '../../core/models/auth.models';

@Component({
  standalone: true,
  imports: [AsyncPipe, DatePipe, TitleCasePipe, RouterLink],
  template: `
    <main class="post-feed-page">
      <header class="post-feed-header"><div><p class="eyebrow">PHOTO BUDDY COMMUNITY</p><h1>Photo feed</h1><p class="muted">Moments shared by your photography community.</p></div><a class="primary-button" routerLink="/posts/create">Create post</a></header>
      @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
      @if (loading) { <p class="muted">Loading posts…</p> }
      @if (!loading && posts.length === 0) { <section class="post-empty"><h2>No posts yet</h2><p>Share your first photo with the community.</p><a routerLink="/posts/create">Create a post</a></section> }
      <section class="post-feed-list">
        @for (post of posts; track post.postId) {
          <article class="post-card">
            <header class="post-author"><div class="post-avatar">{{ initials(post) }}</div><div><a [routerLink]="['/profile', post.user.username]">{{ post.user.firstName }} {{ post.user.lastName }}</a><span>{{ post.createdAt | date:'mediumDate' }} · {{ post.style | titlecase }}</span></div></header>
            <a [routerLink]="['/posts', post.postId]"><img class="feed-image" [src]="post.imageUrl" [alt]="post.caption || 'Photo shared by ' + post.user.firstName" loading="lazy" /></a>
            @if (post.caption) { <p class="post-caption">{{ post.caption }}</p> }
            <footer class="post-actions"><button type="button" [class.liked]="post.likedByCurrentUser" (click)="toggleLike(post)">{{ post.likedByCurrentUser ? '♥ Liked' : '♡ Like' }} · {{ post.likeCount }}</button><a [routerLink]="['/posts', post.postId]">Comments · {{ post.commentCount }}</a>
              @if ((auth.user$ | async)?.id === post.user.id) { <button class="post-delete" type="button" (click)="delete(post)">Delete</button> }
            </footer>
          </article>
        }
      </section>
      @if (pageData && pageData.totalPages > 1) { <nav class="page-controls"><button type="button" [disabled]="pageData.first" (click)="load(pageData.number - 1)">Previous</button><span>Page {{ pageData.number + 1 }} of {{ pageData.totalPages }}</span><button type="button" [disabled]="pageData.last" (click)="load(pageData.number + 1)">Next</button></nav> }
    </main>
  `,
})
export class PostFeedComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly service = inject(PostService);
  posts: PostItem[] = [];
  pageData: PostPage | null = null;
  loading = true;
  error = '';
  ngOnInit(): void { this.load(0); }
  initials(post: PostItem): string { return `${post.user.firstName[0]}${post.user.lastName[0]}`.toUpperCase(); }
  load(page: number): void {
    this.loading = true; this.error = '';
    this.service.feed(page).subscribe({ next: data => { this.pageData = data; this.posts = data.content; this.loading = false; }, error: e => this.fail(e) });
  }
  toggleLike(post: PostItem): void {
    const request = post.likedByCurrentUser ? this.service.unlike(post.postId) : this.service.like(post.postId);
    request.subscribe({ next: result => { post.likeCount = result.likeCount; post.likedByCurrentUser = result.likedByCurrentUser; }, error: e => this.fail(e) });
  }
  delete(post: PostItem): void {
    if (!confirm('Delete this post?')) return;
    this.service.delete(post.postId).subscribe({ next: () => this.load(this.pageData?.number ?? 0), error: e => this.fail(e) });
  }
  private fail(response: HttpErrorResponse): void { this.error = response.error?.message ?? 'Could not load posts. Please try again.'; this.loading = false; }
}
