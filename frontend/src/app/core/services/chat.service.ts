import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface ChatBuddy {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  profilePicture: string | null;
}

export interface ChatRoom {
  roomId: number;
  buddy: ChatBuddy;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: number;
  roomId: number;
  sender: ChatBuddy;
  text: string | null;
  imageUrl: string | null;
  timestamp: string;
  isRead: boolean;
}

export interface ChatHistoryPage {
  content: ChatMessage[];
  last: boolean;
  number: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiBaseUrl}/chat/rooms`;

  rooms(): Observable<ChatRoom[]> {
    return this.http.get<ChatRoom[]>(this.apiUrl);
  }

  openRoom(buddyId: number): Observable<ChatRoom> {
    return this.http.post<ChatRoom>(`${this.apiUrl}/${buddyId}`, {});
  }

  getRoom(roomId: number): Observable<ChatRoom> {
    return this.http.get<ChatRoom>(`${this.apiUrl}/${roomId}`);
  }

  history(roomId: number, page = 0, size = 30): Observable<ChatHistoryPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ChatHistoryPage>(`${this.apiUrl}/${roomId}/messages`, { params });
  }

  markRead(roomId: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${roomId}/read`, {});
  }
}