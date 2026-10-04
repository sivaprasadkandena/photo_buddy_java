import { ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { Subject, Subscription, distinctUntilChanged, interval, map, of, switchMap, take, takeUntil } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { BuddyService } from '../../core/services/buddy.service';
import { BuddyMatch } from '../../core/models/auth.models';
import { ChatMessage, ChatRoom, ChatService } from '../../core/services/chat.service';
import { environment } from '../../../environments/environment';

@Component({
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <main class="chat-page">
      <header class="chat-page-header">
        <div><p class="eyebrow">PHOTO BUDDY</p><h1>Messages</h1></div>
        <a class="secondary-button" routerLink="/matches">View matches</a>
      </header>
      @if (pageError) { <p class="form-error" role="alert">{{ pageError }}</p> }
      <div class="chat-layout">
        <aside class="chat-sidebar" aria-label="Chats and matched buddies">
          <section class="chat-list-section">
            <h2>Chats <span>{{ rooms.length }}</span></h2>
            @if (loadingRooms) { <p class="chat-empty-hint">Loading chats…</p> }
            @if (!loadingRooms && rooms.length === 0) { <p class="chat-empty-hint">Your conversations will appear here.</p> }
            @for (room of rooms; track room.roomId) {
              <button type="button" class="chat-contact" [class.selected]="selectedRoom?.roomId === room.roomId" (click)="selectRoom(room)">
                @if (room.buddy.profilePicture) {
                  <img class="chat-avatar" [src]="room.buddy.profilePicture" [alt]="room.buddy.firstName" />
                } @else {
                  <span class="chat-avatar chat-avatar-fallback">{{ initials(room.buddy) }}</span>
                }
                <span class="chat-contact-copy"><strong>{{ room.buddy.firstName }} {{ room.buddy.lastName }}</strong><small>&#64;{{ room.buddy.username }}</small></span>
              </button>
            }
          </section>
          <section class="chat-list-section matched-list">
            <h2>Matched buddies <span>{{ matches.length }}</span></h2>
            @if (loadingMatches) { <p class="chat-empty-hint">Loading matches…</p> }
            @if (!loadingMatches && matches.length === 0) { <p class="chat-empty-hint">Accept a buddy request to start a conversation.</p> }
            @for (match of matches; track match.id) {
              <button type="button" class="chat-contact" (click)="startChat(match)">
                @if (match.user.profilePicture) {
                  <img class="chat-avatar" [src]="match.user.profilePicture" [alt]="match.user.firstName" />
                } @else {
                  <span class="chat-avatar chat-avatar-fallback">{{ initials(match.user) }}</span>
                }
                <span class="chat-contact-copy"><strong>{{ match.user.firstName }} {{ match.user.lastName }}</strong><small>{{ hasRoom(match.user.id) ? 'Open conversation' : 'Start a conversation' }}</small></span>
              </button>
            }
          </section>
        </aside>

        <section class="chat-thread" aria-label="Conversation">
          @if (loadingRoom) {
            <div class="chat-welcome"><p class="muted">Opening conversation…</p></div>
          } @else if (selectedRoom; as room) {
            <header class="chat-thread-header">
              @if (room.buddy.profilePicture) {
                <img class="chat-avatar" [src]="room.buddy.profilePicture" [alt]="room.buddy.firstName" />
              } @else {
                <span class="chat-avatar chat-avatar-fallback">{{ initials(room.buddy) }}</span>
              }
              <div><h2>{{ room.buddy.firstName }} {{ room.buddy.lastName }}</h2><a [routerLink]="['/profile', room.buddy.username]">&#64;{{ room.buddy.username }}</a></div>
              <span class="connection-state" [class.connected]="connected">{{ connected ? 'Connected' : 'Connecting…' }}</span>
            </header>
            <div class="chat-message-list" aria-live="polite">
              @if (loadingMessages) { <p class="chat-empty-hint">Loading conversation…</p> }
              @if (!loadingMessages && messages.length === 0) { <p class="chat-empty-hint thread-empty">No messages yet. Say hello.</p> }
              @for (message of messages; track message.id) {
                <article class="chat-message" [class.own-message]="message.sender.id === currentUserId">
                  <div class="chat-bubble">
                    @if (message.text) { <p>{{ message.text }}</p> }
                    @if (message.imageUrl) { <img class="chat-image" [src]="message.imageUrl" alt="Shared image" /> }
                    <time>{{ message.timestamp | date:'shortTime' }}</time>
                  </div>
                </article>
              }
            </div>
            @if (chatError) { <p class="chat-inline-error" role="alert">{{ chatError }}</p> }
            <form class="chat-composer" (ngSubmit)="sendMessage()">
              <textarea name="message" [(ngModel)]="draft" rows="2" maxlength="4000" placeholder="Write a message…" [disabled]="!connected"></textarea>
              <button type="submit" [disabled]="!connected || !draft.trim()">Send</button>
            </form>
          } @else {
            <div class="chat-welcome"><p class="eyebrow">YOUR COMMUNITY</p><h2>Start a conversation</h2><p class="muted">Choose a chat or a matched buddy to message.</p></div>
          }
        </section>
      </div>
    </main>
  `,
})
export class ChatComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly buddies = inject(BuddyService);
  private readonly chats = inject(ChatService);
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly client = new Client({
    brokerURL: environment.websocketUrl.replace(/^http/, 'ws'),
    reconnectDelay: 5000,
    connectionTimeout: 10000,
    connectHeaders: {},
    onConnect: () => this.onConnected(),
    onWebSocketClose: () => {
      this.connected = false;
      this.changeDetector.markForCheck();
    },
    onWebSocketError: () => {
      this.connected = false;
      this.chatError = 'Could not connect to chat. Check that the backend is running and allows this frontend origin.';
      this.changeDetector.markForCheck();
    },
    onStompError: frame => {
      this.chatError = frame.headers['message'] ?? 'Chat connection error.';
      this.changeDetector.markForCheck();
    },
  });
  private roomSubscription?: StompSubscription;
  private errorSubscription?: StompSubscription;
  private historyRequest?: Subscription;
  private readonly destroy$ = new Subject<void>();
  private readonly refreshIntervalMs = 10000;

  rooms: ChatRoom[] = [];
  matches: BuddyMatch[] = [];
  selectedRoom: ChatRoom | null = null;
  messages: ChatMessage[] = [];
  currentUserId = 0;
  draft = '';
  connected = false;
  loadingRooms = true;
  loadingMatches = true;
  loadingRoom = false;
  loadingMessages = false;
  pageError = '';
  chatError = '';

  ngOnInit(): void {
    const roomIdParameter = this.route.snapshot.paramMap.get('roomId');
    const routeRoomId = roomIdParameter === null ? null : Number(roomIdParameter);
    this.auth.user$.pipe(
      take(1),
      switchMap(user => user ? of(user) : this.auth.me()),
    ).subscribe({
      next: user => {
        this.currentUserId = user.id;
        this.changeDetector.markForCheck();
        this.loadRooms(routeRoomId);
        this.refreshMatches();
        this.route.paramMap.pipe(
          map(params => params.get('roomId')),
          distinctUntilChanged(),
          takeUntil(this.destroy$),
        ).subscribe(roomId => this.openRouteRoom(roomId));
      },
      error: response => this.showPageError(response),
    });

    interval(this.refreshIntervalMs).pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.loadRooms(this.selectedRoom?.roomId ?? null);
      this.refreshMatches();
      if (this.selectedRoom) {
        this.loadHistory(this.selectedRoom.roomId, false, false);
      }
    });
  }

  private openRouteRoom(roomIdParameter: string | null): void {
    if (roomIdParameter === null) return;
    const roomId = Number(roomIdParameter);
    if (!Number.isSafeInteger(roomId) || roomId < 1) {
      this.pageError = 'Invalid chat room.';
      this.loadingRoom = false;
      this.changeDetector.markForCheck();
      return;
    }
    if (this.selectedRoom?.roomId === roomId) return;

    this.loadingRoom = true;
  this.changeDetector.markForCheck();
    this.chats.getRoom(roomId).subscribe({
      next: room => {
        if (this.route.snapshot.paramMap.get('roomId') !== roomIdParameter) return;
        this.pageError = '';
        this.rooms = [room, ...this.rooms.filter(existing => existing.roomId !== room.roomId)];
        this.loadingRoom = false;
        this.selectRoom(room, false);
        this.changeDetector.markForCheck();
      },
      error: response => {
        if (this.route.snapshot.paramMap.get('roomId') !== roomIdParameter) return;
        this.loadingRoom = false;
        this.showPageError(response);
        this.changeDetector.markForCheck();
      },
    });
  }

  private loadRooms(routeRoomId: number | null): void {
    this.chats.rooms().subscribe({
      next: rooms => {
        this.rooms = rooms;
        this.loadingRooms = false;
        if (routeRoomId === null && rooms.length && !this.selectedRoom) this.selectRoom(rooms[0], false);
        this.changeDetector.markForCheck();
      },
      error: response => {
        this.loadingRooms = false;
        this.showPageError(response);
      },
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.historyRequest?.unsubscribe();
    this.roomSubscription?.unsubscribe();
    this.errorSubscription?.unsubscribe();
    void this.client.deactivate();
  }

  initials(buddy: { firstName: string; lastName: string }): string {
    return `${buddy.firstName.charAt(0)}${buddy.lastName.charAt(0)}`.toUpperCase();
  }

  hasRoom(buddyId: number): boolean { return this.rooms.some(room => room.buddy.id === buddyId); }

  selectRoom(room: ChatRoom, updateUrl = true): void {
    this.selectedRoom = room;
    this.messages = [];
    this.chatError = '';
    this.loadingMessages = true;
    this.loadHistory(room.roomId, true, true);
    if (updateUrl) void this.router.navigate(['/chat', room.roomId]);
    this.subscribeToSelectedRoom();
    if (!this.client.active) {
      const token = this.auth.accessToken;
      if (!token) {
        this.chatError = 'Your session has expired. Sign in again to continue.';
        return;
      }
      this.client.connectHeaders = { Authorization: `Bearer ${token}` };
      this.client.activate();
    }
  }

  private loadHistory(roomId: number, showLoading: boolean, markRead: boolean): void {
    this.historyRequest?.unsubscribe();
    if (showLoading) this.loadingMessages = true;
    this.historyRequest = this.chats.history(roomId).subscribe({
      next: page => {
        if (this.selectedRoom?.roomId !== roomId) return;
        this.mergeMessages([...page.content].reverse());
        this.loadingMessages = false;
        if (markRead) this.chats.markRead(roomId).subscribe({ error: () => undefined });
        this.changeDetector.markForCheck();
        this.changeDetector.markForCheck();
      },
      error: response => {
        if (this.selectedRoom?.roomId !== roomId) return;
        this.loadingMessages = false;
        this.showChatError(response);
      },
    });
  }

  startChat(match: BuddyMatch): void {
    const existingRoom = this.rooms.find(room => room.buddy.id === match.user.id);
    if (existingRoom) {
      this.selectRoom(existingRoom);
      return;
    }
    this.chats.openRoom(match.user.id).subscribe({
      next: room => {
        this.rooms = [room, ...this.rooms];
        this.selectRoom(room);
      },
      error: response => this.showPageError(response),
    });
  }

  sendMessage(): void {
    const text = this.draft.trim();
    if (!text || !this.selectedRoom || !this.client.connected) return;
    this.chatError = '';
    this.client.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({ roomId: this.selectedRoom.roomId, text }),
    });
    this.draft = '';
  }

  private onConnected(): void {
    this.connected = true;
    this.chatError = '';
    this.errorSubscription?.unsubscribe();
    this.errorSubscription = this.client.subscribe('/user/queue/errors', message => {
      this.chatError = (JSON.parse(message.body) as { message?: string }).message ?? 'Message could not be sent.';
      this.changeDetector.markForCheck();
    });
    this.subscribeToSelectedRoom();
    this.changeDetector.markForCheck();
  }

  private subscribeToSelectedRoom(): void {
    this.roomSubscription?.unsubscribe();
    if (!this.client.connected || !this.selectedRoom) return;
    const roomId = this.selectedRoom.roomId;
    this.roomSubscription = this.client.subscribe(`/topic/chat/${roomId}`, message => this.receiveMessage(message, roomId));
  }

  private receiveMessage(frame: IMessage, roomId: number): void {
    const message = JSON.parse(frame.body) as ChatMessage;
    if (message.roomId !== roomId || this.selectedRoom?.roomId !== roomId) return;
    this.mergeMessages([message]);
    this.changeDetector.markForCheck();
    if (message.sender.id !== this.currentUserId) {
      this.chats.markRead(roomId).subscribe({ error: () => undefined });
    }
  }

  private mergeMessages(incoming: ChatMessage[]): void {
    const byId = new Map(this.messages.map(message => [message.id, message]));
    for (const message of incoming) byId.set(message.id, message);
    this.messages = [...byId.values()].sort((first, second) => Date.parse(first.timestamp) - Date.parse(second.timestamp));
  }

  private refreshMatches(): void {
    this.buddies.matches().subscribe({
      next: matches => {
        this.matches = matches;
        this.loadingMatches = false;
        this.changeDetector.markForCheck();
      },
      error: response => {
        this.loadingMatches = false;
        this.showPageError(response);
      },
    });
  }

  private showPageError(response: HttpErrorResponse): void {
    this.pageError = response.error?.message ?? 'Could not load your chats.';
    this.changeDetector.markForCheck();
  }

  private showChatError(response: HttpErrorResponse): void {
    this.chatError = response.error?.message ?? 'Could not load this conversation.';
    this.changeDetector.markForCheck();
  }
}