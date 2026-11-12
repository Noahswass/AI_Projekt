# AI_Projekt: Sprint & Product Backlog

## 1. Product overview
A LEGO companion web app. 
Visitors can browse the LEGO catalog without logging in. 
Registered users log in to see and manage their own LEGO collection,
which is imported from Rebrickable (open API). 
An AI assistant answers questions about the catalog and,
for logged-in users, about their own collection.

Pages:
- **Landing (`/`)**: options without login (browse sets and parts, search,
    ask the AI assistant with limits) and a login/register entry point.
- **Login / Register (`/login`, `/register`)**
- **Catalog (public)**: `/catalog/sets`, `/catalog/sets/{setNum}`, `/catalog/parts`,
    search and filter by theme.
- **My area (logged in, `/me/**`)**: dashboard, `/me/collection` (own sets and parts,
    update quantity and notes), `/me/settings` (link Rebrickable account), sync status.
- **Assistant (`/assistant`)**: public with rate limits; personalised when logged in.
- **Admin (`/admin/**`)**: sync status and manual sync trigger.

## 2. Assumptions (confirm before Sprint 2)
- **Backend:** one Spring Boot 3.3.x app named `AI_Projekt`, Java 21, Maven. Package `com.example.AI_Projekt`.
    Thymeleaf UI is served from the same jar (fullstack, one deployable).
- **Catalog source:** Rebrickable API v3 (`https://rebrickable.com/api/v3`),
    authenticated with the header `Authorization: key <KEY>`.
    Rebrickable also publishes catalog data as downloads;
    if the API is too slow or rate-limited for the full daily load, switch the sync to those downloads.
- **"Updated once a day through AI":** 
    interpreted as a scheduled job (02:00 Europe/Copenhagen) that pulls and upserts the catalog.
    The LLM is used for the assistant and, optionally, to summarise daily changes. 
  **Open question:** does "through AI" mean the LLM performs the sync, or only the Q&A? 
    The design below assumes the sync is plain code, which is safer and cheaper.
- **Own data:** Rebrickable user collections need the user's Rebrickable
  identity. Decided approach: anyone can register and use their account without
  a Rebrickable account. Linking is optional and only required for the personal
  collection features. To link, the user enters their Rebrickable username and
  password once in `/me/settings`; the app exchanges them for a user token
  (`POST /users/_token/`) and stores only the token, encrypted (AES-GCM). We never
  store or log the user's Rebrickable password. **To verify:** the exact request
  fields of the token endpoint in Rebrickable's API docs.
- **LLM:** Claude Messages API, `claude-haiku-5-5` for development, behind an
  `LlmClient` interface.
- **Database:** MySQL (set with `DB_URL`), H2 in MySQL mode as the fallback and for tests. Schema versioned
  with Flyway. Hibernate `ddl-auto` is `validate`.
- **Users:** a small number of users. Single role `USER`, plus `ADMIN`.
- **Capacity:** about 6–7 points per day for a restart.

## 3. Architecture

```
Browser (Thymeleaf pages)
   │
   ▼
AI_Projekt (Spring Boot)
 ├── web/        Thymeleaf controllers (public + /me + /admin)
 ├── security/   Spring Security: form login, BCrypt, roles, CSRF
 ├── catalog/    Public catalog services + JPA repositories (read from local DB)
 ├── rebrickable/ RestClient client, DTO records, sync service
 ├── sync/       @Scheduled daily job, SyncRun log, manual trigger (admin)
 ├── collection/ User collection, encrypted Rebrickable token, import/refresh
 (Sprint 2)
 ├── ai/         LlmClient, AssistantService (catalog and collection context)
 (Sprint 2)
 └── error/      @RestControllerAdvice, ProblemDetail
   │                       │
   ▼                       ▼
MySQL                   Rebrickable API v3        Claude Messages API
(catalog, users,        (daily sync, user data)   (assistant)
 collection, sync runs)
```

Flow for the daily sync: `Scheduler → SyncService → RebrickableClient → upsert
into catalog tables → SyncRun row (status, counts, errors)`.
Flow for a catalog page: `Controller → CatalogService → Repository → Thymeleaf`
(no live API call on page load).
Flow for the assistant: `Controller → AssistantService → (catalog or own
collection lookup) → PromptBuilder → LlmClient`.

### Core data model (first draft)
| Table | Key fields |
|-------|------------|
| `theme` | id, name, parent_id |
| `lego_colour` | id, name, rgb |
| `lego_set` | id (generated), set_num (unique), name, release_year, theme_id, num_parts, set_img_url, set_url, last_modified_dt, last_sync_dt |
| `part` | part_num (unique), name, category, img_url, last_sync_dt |
| `set_part` | set_id, part_id, color_id, quantity, is_spare (inventory) |
| `minifig` | fig_num (unique), name, num_parts, img_url |
| `app_user` | id, username, password_hash, role, enabled (default true) |
| `rebrickable_link` | user_id, token_encrypted, linked_at, last_import_at (Sprint 2) |
| `collection_item` | user_id, item_type (SET/PART/MINIFIG), item_ref, color_id, quantity, notes (Sprint 2) |
| `sync_run` | id, started_at, finished_at, status, counts_json, error_message |

## 4. Product backlog

Priority: **P0** = must have, **P1** = should have, **P2** = stretch. Points are
relative estimates.

### Epic A: Foundation
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-01 | P0 | As a developer, I have a building `AI_Projekt` project | 2 | `mvn clean verify` passes on JDK 21. Package layout from section 3 exists. `.gitignore` covers `target/`, `.idea/`, `.env`. |
| PB-02 | P0 | As a developer, the app starts with H2 and a health endpoint | 2 | `mvn spring-boot:run` on 8080. `GET /actuator/health` returns `UP`. Context-load test passes. |
| PB-03 | P0 | As a developer, the schema is versioned | 2 | Flyway `V1__init.sql` is an empty baseline, `V2__catalog.sql` creates the catalog tables and `V3__user.sql` creates the user table. `ddl-auto=validate`. Migrations run on H2 and MySQL. |
| PB-04 | P0 | As the system, I call Rebrickable with auth and resilience | 3 | `RebrickableClient` sends `Authorization: key <KEY>` from env. Timeouts set. Paginated responses are followed. 429 and 5xx retried with backoff. Mapped to DTO records. WireMock tests for success, 401, 429 and timeout. |

### Epic B: Daily catalog sync
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-05 | P0 | As the owner, themes, colours and sets are synced daily | 3 | `@Scheduled` job at 02:00 `Europe/Copenhagen`. Upserts themes, colours and sets by natural key. Re-running does not duplicate rows. |
| PB-06 | P0 | As the owner, I can see the result of each sync | 3 | Each run writes a `sync_run` row with status and counts. Failed runs keep the error message. Part and minifig sync added. Test with WireMock. |
| PB-07 | P1 | As the owner, I can trigger a sync manually | 1 | `POST /admin/sync` (ADMIN only, CSRF protected) starts a run. Only one run at a time (lock). |

### Epic C: Public catalog and landing
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-08 | P0 | As a visitor, I can browse and search sets without logging in | 3 | `/catalog/sets` paginated, filter by theme, search by name or set number. Data comes from the local DB. |
| PB-09 | P0 | As a visitor, I can see a landing page with options | 2 | `/` lists browse, search, assistant and login/register options. Works without JavaScript. |
| PB-10 | P1 | As a visitor, I can see set and part details | 2 | `/catalog/sets/{setNum}` shows theme, year, parts count and inventory. `/catalog/parts` lists parts. |

### Epic D: Accounts and security
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-11 | P0 | As a visitor, I can register and log in | 3 | Form login and registration. No Rebrickable account needed to register; a new account is enabled and can log in at once. BCrypt password hashes. Session cookie. CSRF on all forms. `/me/**` redirects to `/login` when anonymous. Integration test covers register, login and access rules. |
| PB-12 | P1 | As the owner, admin pages are restricted to ADMIN | 1 | `/admin/**` returns 403 for USER. Admin account created by config or migration (no default password in git). |

### Epic E: Own data from Rebrickable (Sprint 2)
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-13 | P0 | As a user, I can link my Rebrickable account in settings | 3 | `/me/settings` accepts a Rebrickable username and password and exchanges them for a user token (`POST /users/_token/`). The password is never stored or logged. Wrong credentials give a clear message and no link. Token encrypted with AES-GCM (key from env). Token never shown or logged. Link can be removed. Linking is optional; the account works without it. WireMock test for success and wrong credentials. |
| PB-14 | P0 | As a user, my Rebrickable collection is imported | 4 | Only available with a linked account. Import pulls the user's sets and parts into `collection_item`. Re-import updates, does not duplicate. Errors shown on `/me`. |
| PB-15 | P0 | As a user, I can view and edit my collection | 3 | `/me/collection` lists items with filter by type and theme. Quantity and notes editable. Changes saved locally. Without a linked account the page shows a prompt to link in `/me/settings` instead of an error. |
| PB-16 | P1 | As a user, I can refresh my data on demand | 2 | "Refresh" button re-imports from Rebrickable. Rate-limited per user. |

### Epic F: AI assistant (Sprint 2)
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-17 | P1 | As a visitor, I can ask the assistant questions about the catalog | 4 | `/assistant` answers from catalog data only. Prompt instructs "answer only from the data, say I don't know otherwise". Sources listed. Rate-limited for anonymous users. |
| PB-18 | P1 | As a user, the assistant also answers about my collection | 3 | Logged-in answers include own collection context. Data limited per request (max items). Users cannot see another user's data. |
| PB-19 | P1 | As the owner, AI endpoints are protected from abuse | 2 | Per-IP limit for anonymous and per-user limit for logged-in use (e.g. Bucket4j or a simple in-memory limiter). Returns 429 with ProblemDetail. |
| PB-20 | P2 | As the owner, I can see LLM cost and latency | 2 | Each call logs model, latency, input and output tokens. Actuator metrics exposed. |

### Epic G: Platform
| ID | Pri | Story | Pts | Acceptance criteria |
|----|-----|-------|-----|---------------------|
| PB-21 | P1 | As a user, I get clear errors instead of stack traces | 1 | `@RestControllerAdvice` returns `ProblemDetail` for validation, Rebrickable, LLM and access errors. Pages show friendly messages. |
| PB-22 | P1 | As a developer, config and secrets come from env vars and profiles | 1 | `dev` and `prod` profiles. `.env.example` lists all variables. No secrets in git. |
| PB-23 | P1 | As a developer, I can trust changes because of tests | 3 | Unit tests for sync mapping, PromptBuilder and token encryption. Integration tests for security rules and catalog pages. Coverage at least 70% on `rebrickable`, `security` and `ai`. |
| PB-24 | P2 | As a developer, I can run the full stack with one command | 2 | `docker-compose.yml` starts MySQL and `AI_Projekt` with the `prod` profile. |

**Totals**
- Sprint 1 (P0 and P1 in Epics A–D, G): PB-01 to PB-12, PB-21 to PB-23 = 35
  points. P0 items alone = 27 points.
- Sprint 2 (Epics E and F, plus PB-24): PB-13 to PB-20, PB-24 = 26 points.

The full scope does not fit in one five-day sprint. Sprint 1 delivers the public
catalog, the daily sync and login. Sprint 2 delivers the personal collection and
the assistant.

## 5. Sprint 1 plan (5 days)

| Day | Focus | Stories | Done when |
|-----|-------|---------|-----------|
| Mon | Foundation | PB-01, PB-02, PB-22 | `mvn verify` green. `/actuator/health` returns `UP`. |
| Tue | Schema and Rebrickable client | PB-03, PB-04 | Flyway runs. A real `curl` with the key returns data. WireMock tests pass. |
| Wed | Daily sync (themes, colours, sets) | PB-05 | Running the sync fills the tables. A second run causes no duplicates. |
| Thu | Sync log, parts, public catalog | PB-06, PB-08 | `sync_run` rows are written. Sets can be browsed and searched in the browser. |
| Fri | Landing, login, demo | PB-09, PB-11, PB-21, PB-07 (if time) | Visitor can browse, register and log in. `/me` is protected. Demo runs from a clean clone. |

Stretch on Friday: PB-10, PB-12.

## 6. Sprint 2 plan (next sprint, after open questions are answered)
- Days 1–2: PB-13 (token linking, encryption), PB-14 (import).
- Days 3–4: PB-15 (collection UI), PB-16 (refresh).
- Days 5–6: PB-17 (public assistant), PB-19 (rate limits).
- Days 7–8: PB-18 (personalised assistant), PB-20 (metrics), PB-24 (Docker).

## 7. Ceremonies (solo, 15 min each)
- **Daily start:** read the day's "Done when" and pick the first story.
- **Daily end:** write done, blocked, next. Commit before stopping.
- **Friday review:** demo against the sprint goal. Move unfinished stories.
- **Retro:** one keep, one change, one try.

## 8. Definition of Done
- `mvn verify` passes locally.
- Constructor injection only (Lombok `@RequiredArgsConstructor`).
- Read-only service methods are `@Transactional(readOnly = true)`.
- Errors handled in `@RestControllerAdvice`.
- Public pages read the local DB. No live Rebrickable call on page load.
- No secrets in git. Rebrickable tokens are encrypted at rest.
- New behaviour has at least one test.

## 9. Risks and mitigations

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Rebrickable rate limits make the daily full sync too slow | High | Batch and cache. Respect 429 with backoff. Fall back to Rebrickable's catalog downloads if needed. Sync incrementally after the first full load. |
| Rebrickable terms or attribution requirements for displaying its data | Medium | Read the API terms before launch. Add attribution to catalog pages. |
| User tokens or Rebrickable passwords leak | Medium | The password is used once for the token exchange and never stored or logged. Encrypt tokens with AES-GCM. Key only in env. Never log tokens. Let users revoke or remove them. |
| LLM invents set or part facts | Medium | Answer only from DB data. Show sources. Say "I don't know" when data is missing. |
| LLM cost from anonymous traffic | Medium | Rate limits (PB-19). Cheap model. Token logging (PB-20). |
| Scope creep beyond two sprints | High | Keep P2 items as stretch. Review scope at each sprint start. |
| Rusty on Spring Security | High | Build PB-11 with a small test per rule. Timebox each story to about 1.5 hours. |
| "Updated through AI" means something different than assumed | Medium | Confirm before Sprint 1 Wednesday, when the sync is built. |