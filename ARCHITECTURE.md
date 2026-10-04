# Photo Buddy architecture

This is the architecture for the requested scope. Phases 1 through 6 are implemented; the later APIs and data models remain plans until their phases are built.

## 1. Runtime architecture

```text
Angular SPA (HTTPS)
  ├─ REST/JSON ───────┐
  └─ STOMP/WebSocket ─┤
                      v
             Spring Boot API (Java 17+)
              ├─ services and authorization
              ├─ Spring Data JPA
              ├─ local/cloud file storage adapter
              └─ WebSocket message broker
                      ├─ MySQL 8 / AWS RDS
                      └─ Cloudinary (or later S3)
```

The backend is organized by conventional application layers: `config`, `controller`, `dto` (by feature), `entity`, `exception`, `mapper`, `repository`, `security`, `service/impl`, and `util`. Controllers handle transport only; services own use cases and authorization; repositories own persistence; DTOs form the API boundary.

## 2. Backend package structure

```text
com.photobuddy/
  config/ controller/
  dto/{auth,user,location,buddy,post,comment,chat,notification}/
  entity/ exception/ mapper/ repository/ security/
  service/impl/ util/
```

## 3. Angular structure

```text
src/app/
  core/{guards,interceptors,services,models}/
  shared/{components,directives,pipes}/
  features/{auth,home,profile,nearby,matches,posts,chat,notifications}/
  layout/{navbar,sidebar,footer}/
  app.routes.ts
src/environments/{environment,environment.prod}.ts
```

Use standalone feature routes, Reactive Forms, `HttpClient`, and RxJS. API and WebSocket base URLs are centralized in environment files.

## 4. Database ER diagram (text)

```text
users 1──0..1 user_locations
users 1──* buddy_requests (as sender and receiver)
users 1──* matches (as user1 and user2; canonical pair)
users 1──* posts 1──* post_likes ──1 users
posts 1──* comments ──1 users
matches 1──0..1 chat_rooms (pair unique)
chat_rooms 1──* messages ──1 users (sender)
users 1──* notifications (recipient; optional sender)
```

All relations use foreign keys. Enforce unique user email/username, one location per user, unique post/user likes, and canonical unique user pairs for matches and rooms. Index foreign keys, status/creation-time query paths, and feed timestamps.

## 5. Entity relationships

- `User` owns an optional one-to-one `UserLocation`; all profile and identity references point to `User`.
- `BuddyRequest` has sender and receiver users with a status and timestamps. Accepting creates one canonical `Match` pair.
- `Post` belongs to one user; `PostLike` and `Comment` each connect one user to one post.
- `ChatRoom` is a unique matched user pair; `Message` belongs to one room and has one sender.
- `Notification` belongs to a recipient, optionally identifies a sender, and references a related feature record by type and ID.

## 6. API list

All non-auth REST endpoints require JWT authentication.

```text
Auth:          POST /api/auth/register, /login, /refresh, /logout; GET /api/auth/me
Users:         GET /api/users/{id}, /api/users/profile/{username}; PUT /api/users/profile
Location:      PUT /api/locations/update, /api/locations/toggle; GET /api/locations/my-location
Nearby:        GET /api/users/nearby?latitude=&longitude=&radius=
Buddy:         POST /api/buddy-requests/{userId}; GET /api/buddy-requests/received, /sent
               PUT /api/buddy-requests/{id}/accept, /reject; DELETE /api/buddy-requests/{id}
Matches:       GET /api/matches
Posts:         POST /api/posts; GET /api/posts?page=&size=, /api/posts/{id}
               PUT/DELETE /api/posts/{id}; POST/DELETE /api/posts/{id}/like
Comments:      GET /api/posts/{id}/comments; POST /api/posts/{id}/comments
               DELETE /api/comments/{id}
Chat:          GET /api/chat/rooms, /api/chat/rooms/{roomId}/messages?page=&size=
Notifications: GET /api/notifications, /api/notifications/unread-count
               PUT /api/notifications/{id}/read, /api/notifications/read-all
```

Post image uploads use multipart requests, validate JPEG/PNG/WebP signatures and size, and persist only the storage URL in MySQL. `FileStorageService` selects local filesystem storage during development and Cloudinary during production. Chat attachments will use the same storage boundary in Phase 7.

## 7. Authentication flow

Registration validates input and matching confirmation, checks unique email/username, then stores a BCrypt hash. Login verifies credentials and returns short-lived access and refresh credentials. The Angular interceptor adds the access token; a route guard protects private screens. A stateless Spring Security filter validates token signature/expiry and creates the authenticated principal. User IDs for protected operations come from that principal, never request bodies. Refresh tokens should be revocable and stored hashed server-side when Phase 2 is implemented.

## 8. Location flow

The browser requests permission via `navigator.geolocation`; Angular sends latitude/longitude/accuracy to the authenticated location API. Sharing is an explicit independent toggle. Nearby search excludes disabled locations and computes Haversine distance in MySQL after an indexed latitude/longitude bounding-box filter, sorts by distance, and caps results. The map requires marker positions, so it receives coordinates rounded to two decimal places (roughly kilometre-scale); exact coordinates remain private and are returned only to their owner.

## 9. WebSocket architecture

Angular connects via STOMP to `/ws` (secure `wss` behind production HTTPS), sends to `/app/chat.send`, and receives on `/topic/chat/{roomId}`. Spring authenticates the handshake using the JWT, and checks room membership both before subscribe and before send. Chat history/read state use paginated REST; messages are persisted before publication. Typing events are ephemeral and never written to MySQL.

## 10. Image storage architecture

`FileStorageService` is an interface. Development uses `LocalFileStorageService`; production selects `CloudinaryFileStorageService` using external credentials. The server validates image MIME and file signatures, caps uploads at 10 MB, generates opaque object names, and stores only the returned URL in MySQL.

## 11. Production deployment architecture

Build Angular into static assets for S3 + CloudFront or another HTTPS static host. Run the Spring Boot jar on ECS, EC2, or Elastic Beanstalk. Use RDS MySQL with TLS; externalize credentials and JWT signing material. Route `/api` and `/ws` through HTTPS/WSS to the API. Restrict CORS to configured frontend origins, expose only health checks, use Flyway migrations before production schema validation, and keep logs free of secrets, message bodies, and precise location data. Docker and deployment artifacts belong to Phase 15.

## 12. Phase plan

1. Foundation: Maven Spring Boot and Angular workspaces, configuration profiles, dependency baseline. **Complete.**
2. JWT registration/login, refresh/revocation, Spring Security and Angular guards/interceptor. **Complete.**
3. User entity, profile DTOs/services/APIs and profile UI. **Complete.**
4. Private location updates, privacy toggle, nearby query and Leaflet view. **Complete.**
5. Buddy request rules, matches and match UI. **Complete.**
6. Paginated posts, storage interface, likes, comments and feed UI. **Complete.**
7. Matched-user chat rooms, secured STOMP messaging, paginated history and responsive UI.
8. Notification persistence/API and real-time push/badge.
15. Production configuration, Docker, migration readiness, build/deployment and security preparation.

Phases for calling, AI assistant, advanced recommendations, administration, reporting, advanced search, and ML recommendations are intentionally excluded.
