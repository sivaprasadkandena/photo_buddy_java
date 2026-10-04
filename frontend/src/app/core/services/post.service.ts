import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CommentPage, PostComment, PostItem, PostPage, PostStyle } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class PostService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/posts`;

  feed(page = 0, size = 10): Observable<PostPage> {
    return this.http.get<PostPage>(this.apiUrl, { params: new HttpParams().set('page', page).set('size', size) });
  }
  get(postId: number): Observable<PostItem> { return this.http.get<PostItem>(`${this.apiUrl}/${postId}`); }
  create(image: File, caption: string, style: PostStyle): Observable<PostItem> {
    const data = new FormData();
    data.append('image', image); data.append('caption', caption); data.append('style', style);
    return this.http.post<PostItem>(this.apiUrl, data);
  }
  like(postId: number): Observable<{ postId: number; likeCount: number; likedByCurrentUser: boolean }> {
    return this.http.post<{ postId: number; likeCount: number; likedByCurrentUser: boolean }>(`${this.apiUrl}/${postId}/like`, {});
  }
  unlike(postId: number): Observable<{ postId: number; likeCount: number; likedByCurrentUser: boolean }> {
    return this.http.delete<{ postId: number; likeCount: number; likedByCurrentUser: boolean }>(`${this.apiUrl}/${postId}/like`);
  }
  delete(postId: number): Observable<void> { return this.http.delete<void>(`${this.apiUrl}/${postId}`); }
  comments(postId: number, page = 0, size = 50): Observable<CommentPage> {
    return this.http.get<CommentPage>(`${this.apiUrl}/${postId}/comments`, { params: new HttpParams().set('page', page).set('size', size) });
  }
  addComment(postId: number, content: string): Observable<PostComment> {
    return this.http.post<PostComment>(`${this.apiUrl}/${postId}/comments`, { content });
  }
  deleteComment(commentId: number): Observable<void> { return this.http.delete<void>(`${environment.apiBaseUrl}/comments/${commentId}`); }
}
