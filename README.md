# Photo Buddy

Photo Buddy is a location-based social photography app. The repository is split into `backend/` (Spring Boot API) and `frontend/` (Angular SPA).

## Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 22.12+ and npm (Angular 21)
- MySQL 8+

## Local development

Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` before starting the backend. For example, in PowerShell:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "3306"
$env:DB_NAME = "photobuddy"
$env:DB_USERNAME = "your-local-mysql-user"
$env:DB_PASSWORD = "your-local-mysql-password"
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

The development and production profiles require `JWT_SECRET` as a Base64-encoded random key of at least 32 bytes. Do not reuse the example generated key across environments.

```powershell
cd backend
mvn spring-boot:run
```

```powershell
cd frontend
npm start
```

Install frontend dependencies once with `npm install` from `frontend/`.

Backend health is available at `/actuator/health`; API docs are available at `/swagger-ui.html`.

## Database migrations and API versions

The backend uses MySQL 8 and Flyway for schema changes. Create the database before the first startup:

```sql
CREATE DATABASE photobuddy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Flyway applies `backend/src/main/resources/db/migration/V1__initial_schema.sql` to an empty database. The dev and prod profiles use `spring.jpa.hibernate.ddl-auto=validate`; Hibernate does not mutate production schemas. Existing non-empty databases without Flyway history are baselined at version 1 and then validated. Back up existing data before adopting migrations, and resolve validation errors rather than deleting or resetting a database.

All REST features are available under `/api/v1` and the existing `/api` paths remain as compatibility aliases for the Angular app. API docs are exposed at `/v3/api-docs` and `/swagger-ui.html`. The chat WebSocket remains at `/ws` for compatibility with the current frontend.

To use the production profile, set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, and `PUBLIC_API_BASE_URL`. For Cloudinary media, also set `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET`. Keep production credentials in the hosting platform's secret manager and terminate HTTPS at the production ingress/load balancer.

## Authentication

The backend exposes registration, login, rotating refresh tokens, logout, and the authenticated current-user endpoint under `/api/auth`. New accounts receive `ROLE_USER`; `ROLE_ADMIN` is reserved for future authorization. Access tokens are short lived and signed with `JWT_SECRET`; random refresh tokens are stored server-side as SHA-256 hashes and revoked on rotation/logout. Both token values are kept in browser `sessionStorage` by the Angular client.

```text
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
GET  /api/auth/me      (Bearer access token)
```

The Angular app includes guarded pages for authentication, profiles, nearby users, matches, and posts. To start Angular, run `npm install` once, then `npm start` from `frontend/`.

## Location and nearby users

The `/nearby` page asks the browser for its current position. Location sharing starts off. A member can update their position and search while sharing remains off, or explicitly turn sharing on/off. Exact stored coordinates are available only to the owner; map marker coordinates for other members are rounded to two decimal places, and displayed distances are rounded.

```text
PUT /api/locations/update
GET /api/locations/my-location
PUT /api/locations/toggle
GET /api/users/nearby?latitude={lat}&longitude={lng}&radius={km}
```

The nearby query filters candidates in MySQL with a latitude/longitude bounding box, calculates Haversine distance in the database, sorts by distance, and caps the result list at 100 users.

## Profiles

Authenticated members can view profiles by ID or username and edit their own first/last name, bio, gender, photographer flag, and profile picture URL. The update endpoint derives the target user from the validated JWT principal; it accepts no user ID. Email, username, password, and role are never part of profile update DTOs.

```text
GET /api/users/{id}
GET /api/users/profile/{username}
PUT /api/users/profile
```

Profile post counts reflect published posts; buddy counts reflect accepted matches. Public profile pages allow sending a buddy request; `/matches` lists accepted buddies plus received and sent pending requests.

## Buddy requests and matches

Buddy requests are private to their participants. Only the recipient can accept or decline, and only the sender can cancel. A pending request blocks duplicate or reverse-direction requests. Rejected or cancelled pairs can request again, reusing their existing pair row. Accepting creates one canonical match record visible to both users. Matched distance is returned only when both users have enabled location sharing; it is rounded to one decimal place.

```text
POST   /api/buddy-requests/{userId}
GET    /api/buddy-requests/received
GET    /api/buddy-requests/sent
PUT    /api/buddy-requests/{id}/accept
PUT    /api/buddy-requests/{id}/reject
DELETE /api/buddy-requests/{id}
GET    /api/matches
```

## Posts, likes, and comments

`/posts` shows a paginated, newest-first community feed. Posts accept JPEG, PNG, or WebP images up to 10 MB. Development stores images under `backend/uploads` and serves them through `/api/files/{key}`; production defaults to Cloudinary. Configure `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, and `CLOUDINARY_API_SECRET` for the production storage adapter. Image bytes are never written to MySQL.

```text
POST   /api/posts                              multipart: image, caption, style
GET    /api/posts?page=0&size=10
GET    /api/posts/{id}
PUT    /api/posts/{id}                         multipart optional: image, caption, style
DELETE /api/posts/{id}                         owner only
POST   /api/posts/{id}/like
DELETE /api/posts/{id}/like
GET    /api/posts/{id}/comments?page=0&size=50
POST   /api/posts/{id}/comments                JSON: { "content": "..." }
DELETE /api/comments/{commentId}               comment owner only
```

Like and comment counts, the viewer's like state (`likedByMe`, with the existing `likedByCurrentUser` alias), and the author's public profile data are returned in the page query, without exposing persistence entities.

## Chat and notifications

Matched users can create or reopen one canonical chat room. REST endpoints list rooms, retrieve room details, return paginated history, mark messages read, and upload validated image messages. Text messages are persisted and delivered through authenticated STOMP over `/ws`; room membership is checked for subscribe and send operations. Notification list and unread-count endpoints are paginated and user-scoped.

```text
GET  /api/v1/chat/rooms
POST /api/v1/chat/rooms/{buddyId}
GET  /api/v1/chat/rooms/{roomId}
GET  /api/v1/chat/rooms/{roomId}/messages?page=0&size=30
POST /api/v1/chat/rooms/{roomId}/messages             JSON: { "text": "..." }
POST /api/v1/chat/rooms/{roomId}/messages/image
PUT  /api/v1/chat/rooms/{roomId}/read
GET  /api/v1/notifications?page=0&size=20
GET  /api/v1/notifications/unread-count
```

The referenced public website advertises an AI assistant, but this repository has no AI chat UI, provider integration, or AI-session persistence. AI chat is therefore not exposed by this backend.

## Project status

Implemented backend features include authentication with rotating refresh tokens, profiles, location and nearby discovery, buddy requests and matches, posts/likes/comments, chat, notifications, local/Cloudinary image storage, OpenAPI docs, and Flyway schema migrations. Integration tests run against H2; production migration execution still needs verification against the deployment's MySQL instance before release.
