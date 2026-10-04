import { Component, OnInit, inject } from '@angular/core';
import { AsyncPipe, DatePipe, TitleCasePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { PostService } from '../../core/services/post.service';
import { PostComment, PostItem } from '../../core/models/auth.models';

@Component({
  standalone: true,
  imports: [AsyncPipe, DatePipe, TitleCasePipe, RouterLink],
  template: `
    <main class="post-detail-page"><a class="back-link" routerLink="/posts">← Back to posts</a>
      @if (error) { <p class="form-error" role="alert">{{ error }}</p> }
      @if (post; as post) {
        <article class="post-detail-card">
          <header class="post-author"><div class="post-avatar">{{ post.user.firstName[0] }}{{ post.user.lastName[0] }}</div><div><a [routerLink]="['/profile', post.user.username]">{{ post.user.firstName }} {{ post.user.lastName }}</a><span>{{ post.createdAt | date:'medium' }} · {{ post.style | titlecase }}</span></div>
            @if ((auth.user$ | async)?.id === post.user.id) { <button class="post-delete" type="button" (click)="deletePost()">Delete post</button> }
          </header>
          <img class="detail-image" [src]="post.imageUrl" [alt]="post.caption || 'Photo post'" />
          @if (post.caption) { <p class="post-caption">{{ post.caption }}</p> }
          <div class="detail-like-row"><button type="button" [class.liked]="post.likedByCurrentUser" (click)="toggleLike()">{{ post.likedByCurrentUser ? '♥ Liked' : '♡ Like' }} · {{ post.likeCount }}</button><span>{{ post.commentCount }} comments</span></div>
          <section class="comments-section"><h2>Comments</h2>
            @for (comment of comments; track comment.id) { <article class="comment-row"><div><a [routerLink]="['/profile', comment.user.username]">{{ comment.user.firstName }} {{ comment.user.lastName }}</a><p>{{ comment.content }}</p><time>{{ comment.createdAt | date:'short' }}</time></div>@if ((auth.user$ | async)?.id === comment.user.id) { <button class="post-delete" type="button" (click)="deleteComment(comment)">Delete</button> }</article> }
            @if (!comments.length) { <p class="muted">No comments yet.</p> }
            @if (!commentsLast) { <button class="secondary-button" type="button" (click)="loadMoreComments()">Load more comments</button> }
            <form class="comment-form" (submit)="$event.preventDefault(); submitComment()"><label for="new-comment">Add a comment</label><textarea id="new-comment" maxlength="1000" rows="3" [value]="commentDraft" (input)="commentDraft = $any($event.target).value"></textarea><button type="submit" [disabled]="!commentDraft.trim() || submitting">{{ submitting ? 'Posting…' : 'Comment' }}</button></form>
          </section>
        </article>
      } @else if (loading) { <p class="muted">Loading post…</p> }
    </main>
  `,
})
export class PostDetailComponent implements OnInit {
  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(PostService);
  post: PostItem | null = null;
  comments: PostComment[] = [];
  commentPage = 0;
  commentsLast = true;
  commentDraft = '';
  loading = true;
  submitting = false;
  error = '';
  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = Number(params.get('id'));
      if (!Number.isInteger(id) || id < 1) { this.error = 'Post was not found.'; this.loading = false; return; }
      this.service.get(id).subscribe({ next: post => { this.post = post; this.loading = false; this.loadComments(0); }, error: e => this.fail(e) });
    });
  }
  toggleLike(): void {
    if (!this.post) return;
    const request = this.post.likedByCurrentUser ? this.service.unlike(this.post.postId) : this.service.like(this.post.postId);
    request.subscribe({ next: response => { if (this.post) { this.post.likeCount = response.likeCount; this.post.likedByCurrentUser = response.likedByCurrentUser; } }, error: e => this.fail(e) });
  }
  loadMoreComments(): void { this.loadComments(this.commentPage + 1); }
  submitComment(): void {
    if (!this.post || !this.commentDraft.trim() || this.submitting) return;
    this.submitting = true;
    this.service.addComment(this.post.postId, this.commentDraft.trim()).subscribe({
      next: comment => { this.comments = [...this.comments, comment]; this.commentDraft = ''; this.submitting = false; this.post!.commentCount++; },
      error: e => { this.fail(e); this.submitting = false; },
    });
  }
  deleteComment(comment: PostComment): void {
    this.service.deleteComment(comment.id).subscribe({ next: () => { this.comments = this.comments.filter(item => item.id !== comment.id); if (this.post) this.post.commentCount--; }, error: e => this.fail(e) });
  }
  deletePost(): void {
    if (!this.post || !confirm('Delete this post?')) return;
    this.service.delete(this.post.postId).subscribe({ next: () => this.router.navigateByUrl('/posts'), error: e => this.fail(e) });
  }
  private loadComments(page: number): void {
    if (!this.post) return;
    this.service.comments(this.post.postId, page).subscribe({ next: result => { this.comments = page ? [...this.comments, ...result.content] : result.content; this.commentPage = result.number; this.commentsLast = result.last; }, error: e => this.fail(e) });
  }
  private fail(response: HttpErrorResponse): void { this.error = response.error?.message ?? 'Something went wrong. Please try again.'; this.loading = false; }
}
