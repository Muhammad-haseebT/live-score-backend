---
title: Fyp
emoji: 🌍
colorFrom: red
colorTo: purple
sdk: docker
pinned: false
---

# Live Score Backend (FYP)

This is a Spring Boot backend for live scoring and tournament statistics (cricket-focused).

This README is written for **offline work**: if your teacher asks for changes without internet, you should be able to find where code lives and what to edit.

## Tech Stack

- Spring Boot 3
- Spring Data JPA (PostgreSQL)
- Spring Cache (in-memory)

## Run Locally (Docker)

### 1) Create env file

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

### 2) Run

```bash
docker compose up --build
```

Backend:
- `http://localhost:7860`

Postgres:
- `localhost:5432`

## Configuration (Environment Variables)

All sensitive config is handled by env vars (see `src/main/resources/application.properties`).

### Database

- `DATABASE_URL` (default: `jdbc:postgresql://localhost:5432/livescore`)
- `DATABASE_USERNAME` (default: `postgres`)
- `DATABASE_PASSWORD` (default: `postgres`)

### Upload / Media

- `UPLOAD_MODE` (`local` or `imagekit`, default: `local`)

If using local uploads (container-friendly):

- `MEDIA_LOCAL_PATH` (default: `/tmp/livescore-media`)

If using ImageKit:

- `IMAGEKIT_PUBLIC_KEY`
- `IMAGEKIT_PRIVATE_KEY`
- `IMAGEKIT_URL_ENDPOINT`

## Project Structure

- `Controller/` HTTP endpoints
- `Service/` business logic
- `Interface/` Spring Data repositories and aggregate queries
- `DTO/` response/request objects
- `Entity/` JPA entities
- `Util/` reusable helpers

## Key Services (Where to Edit)

### Live scoring

- `LiveSCoringService.scoring(ScoreDTO)`
  - creates and saves `CricketBall`
  - calls `StatsService.updateTournamentStats(ball)`
  - evicts caches after saving a ball

### Player/tournament stats

- `StatsService`
  - `optimizePlayerStats(...)`
  - `getPlayerTournamentStats(...)`
  - `getTournamentPlayerStatsDto(...)` (complete per-player tournament stats)
  - `getMatchScorecard(matchId)`

### Awards/leaders

- `AwardService.computeTournamentAwards(tournamentId)`
  - calculates:
    - top batsmen
    - top bowlers
    - best batsman/bowler
    - highest scorer
    - man of tournament

## Cricket Rules (Single Source of Truth)

Cricket rules are centralized in:

- `com.livescore.backend.Util.CricketRules`

Rules:

- **Balls faced**:
  - wide does **not** count
  - no-ball **does** count (even if illegal)

- **Runs conceded (bowler)**:
  - byes/leg-byes are **not** charged to the bowler
  - wides/no-balls are charged

- **Wickets credited to bowler**:
  - runout/retired are excluded
  - bowled/caught/lbw/stumped/hit-wicket are included

## Caching (Spring Cache)

Caching is enabled in `BackendApplication` via `@EnableCaching`.

Cache manager is in:

- `com.livescore.backend.Config.CacheConfig` (in-memory `ConcurrentMapCacheManager`)

## Passwords (BCrypt)

Passwords are stored using **BCrypt**.

Backward compatibility:

- If an old account still has a Base64 password in DB, the backend will accept it on login and then automatically upgrade it to BCrypt.

Caches:

- `tournamentStats` (key: `tournamentId`)
- `tournamentAwards` (key: `tournamentId`)
- `playerStats` (key: `tournamentId:playerId`)
- `tournamentPlayerStats` (key: `tournamentId:playerId`)

Eviction happens in:

- `LiveSCoringService.scoring()`

It evicts tournament caches and player caches for batsman/bowler/fielder.

## Main Endpoints

### Tournament

- `GET /tournament/{id}/stats`
- `GET /tournament/{id}/awards`

### Player

- `GET /player/{playerId}/Cricket?tournamentId=...`
- `GET /player/{playerId}/tournamentStats?tournamentId=...`

## Complete Endpoint List

### WebSocket

- **WebSocket**: `GET ws://<host>:7860/ws?matchId=<matchId>`
  - Client can send JSON payloads compatible with `ScoreDTO`
  - Supported `eventType` spellings are normalized (e.g. `no-ball`, `no_ball`, `noball`, `nb`)

## Frontend Guide (Simple)

### 1) Login

Endpoint:

- `POST /account/login`

Body:

```json
{
  "username": "user@example.com",
  "password": "123456"
}
```

Response (example):

```json
{
  "id": 1,
  "name": "Haseeb",
  "role": "ADMIN",
  "playerId": 10,
  "username": "user@example.com"
}
```

### 2) WebSocket live scoring

Connect:

```js
const ws = new WebSocket("ws://localhost:7860/ws?matchId=123");

ws.onmessage = (e) => {
  const msg = JSON.parse(e.data);
  console.log("score update", msg);
};
```

Subscribe after connect (optional):

```js
ws.onopen = () => {
  ws.send(JSON.stringify({ action: "subscribe", matchId: 123 }));
};
```

Send a scoring event (example):

```js
ws.send(JSON.stringify({
  matchId: 123,
  inningsId: 1,
  eventType: "run",
  event: "1",
  batsmanId: 10,
  bowlerId: 11,
  overs: 0,
  balls: 0
}));
```

Notes:

- The server broadcasts back an updated `ScoreDTO` to all subscribers of that `matchId`.
- If you deploy in Docker, expose port `7860` and set CORS/allowed origins as needed.

### Account

- `POST /account`
- `POST /account/login`
- `GET /account/{id}`
- `GET /account`
- `PUT /account/{id}`
- `DELETE /account/{id}`
- `PUT /account/{id}/restore`
- `GET /account/players/{tid}`

### Player

- `POST /player`
- `PUT /player/{id}`
- `DELETE /player/{id}`
- `PUT /player/{id}/restore`
- `GET /player`
- `GET /player/{id}`

### Player Stats

- `GET /player/{playerId}/stats?tournamentId=...&matchId=...` (matchId optional)
- `GET /player/{playerId}/Cricket?tournamentId=...&matchId=...` (matchId optional)
- `GET /player/{playerId}/tournamentStats?tournamentId=...`

### Team

- `POST /team/{id}/{playerId}`
- `POST /team/u/{id}`
- `DELETE /team/{id}`
- `GET /team`
- `GET /team/{id}`
- `GET /team/tournament/{tid}`
- `GET /team/tournament/account/{tid}/{aid}`

### Team Request

- `POST /teamRequest`
- `PUT /teamRequest/{id}`
- `DELETE /teamRequest/{id}`
- `GET /teamRequest/{id}`
- `PUT /teamRequest/approve/{id}`
- `PUT /teamRequest/reject/{id}`
- `GET /teamRequest`

### Player Request

- `POST /playerRequest`
- `PUT /playerRequest/{id}`
- `DELETE /playerRequest/{id}`
- `GET /playerRequest`
- `GET /playerRequest/{id}`
- `PUT /playerRequest/approve/{id}`
- `PUT /playerRequest/reject/{id}`
- `GET /playerRequest/player/{playerId}`

### Season

- `POST /season`
- `GET /season/{id}`
- `GET /season/tournaments/{id}/{sid}`
- `GET /season`
- `PUT /season/{id}`
- `DELETE /season/{id}`
- `GET /season/names`
- `POST /add-sports`

### Sports

- `POST /sports`
- `GET /sports/{id}`
- `GET /sports`
- `PUT /sports/{id}`
- `DELETE /sports/{id}`

### Match

- `POST /match`
- `PUT /match/{id}`
- `DELETE /match/{id}`
- `GET /match/{id}`
- `GET /match`
- `GET /match/tournament/{tournamentId}`
- `GET /match/team/{teamId}`
- `GET /match/status/{status}`
- `GET /match/date/{date}`
- `GET /match/time/{time}`
- `PUT /match/start/{id}`
- `PUT /match/end/{id}`
- `PUT /match/abandon/{id}`
- `GET /match/sport?name=...&status=...`
- `GET /match/scorer/{id}`

### Match Stats / Awards

- `GET /match/{matchId}/scorecard`
- `GET /match/{matchId}/awards`
- `GET /match/tournaments/{tournamentId}/awards`

### Tournament

- `POST /tournament`
- `GET /tournament/{id}`
- `GET /tournament`
- `PUT /tournament/{id}`
- `DELETE /tournament/{id}`
- `GET /tournament/overview/{id}`
- `GET /tournament/{id}/stats`
- `GET /tournament/{id}/awards`

### Points Table

- `POST /ptsTable`
- `PUT /ptsTable/{id}`
- `DELETE /ptsTable/{id}`
- `GET /ptsTable`
- `GET /ptsTable/{id}`
- `GET /ptsTable/tournament/{tournamentId}`

### Cricket Ball

- `GET /cricketBall`
- `GET /cricketBall/{over}/{balls}/{matchID}/{inningsId}`

### Stats (raw)

- `GET /stats`
- `GET /stats/{id}`

### Media

- `POST /media` (multipart: `file`, `matchId`)
- `GET /media/{id}`
- `GET /media`
- `GET /media/season/{id}/{page}/{size}`
- `GET /media/tournament/{id}/{page}/{size}`
- `GET /media/sport/{id}/{page}/{size}`
