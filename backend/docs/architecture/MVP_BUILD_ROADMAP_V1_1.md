# Omnime MVP Build Roadmap V1.1

**Status:** FROZEN / APPROVED FOR MVP IMPLEMENTATION  
**Authority:** This roadmap governs implementation order. `MVP_DATA_MODEL.md` governs persistence. `BACKEND_ARCHITECTURE.md` governs backend responsibilities and behavior. If this roadmap conflicts with either frozen architecture document, the frozen architecture documents win.

## Product Vision

Omnime is an AI-powered anime discovery platform focused on one problem:

**Recommend anime the user has not seen that they are genuinely likely to enjoy.**

Omnime is not a general-purpose anime database. Anime information exists to support the recommendation experience.

The application combines:

- AniList factual metadata
- AniList genres and detailed AniList tags/ranks
- A curated Omnime tag ontology
- Reusable AI-generated Omnime anime profiles
- User ratings and structured feedback evidence
- AI interpretations and evolving taste conclusions
- Deterministic backend eligibility and candidate narrowing
- AI-powered candidate judgment, final ranking, and explanations

### Guiding Principle

**Code handles facts. AI handles judgment.**

Spring Boot/PostgreSQL owns factual state, persistence, eligibility, hard constraints, validation, and candidate narrowing. The LLM interprets qualitative evidence, reasons about taste, profiles serious recommendation candidates, ranks plausible candidates, and explains why the strongest recommendations fit.

---

# MVP Technology Stack

## Frontend

- React
- Vite
- TypeScript
- Tailwind CSS v3
- React Router
- Responsive/mobile-first design
- Manrope
- Omnime Violet V1 design system
- Vercel deployment

## Backend

- Java
- Spring Boot
- Spring Data JPA
- Hibernate
- Spring Security
- Flyway
- REST APIs
- Render deployment via Docker

## Database

- PostgreSQL
- Neon PostgreSQL

## External Services

- AniList GraphQL API
- LLM API
- Email provider for password reset

---

# Development Rules

## Build Vertically

After the shared foundation exists, build Omnime as vertical feature slices:

**Migration/Data → Repository → Service → Controller/API → React UI → Integration Tests → Deployment**

Do not build all 16 JPA entities merely because the complete schema is already designed. Implement persistent pieces when their feature slice reaches them, while respecting the frozen data model and Flyway dependency order.

## Frozen Architecture Is a Guardrail

The complete MVP data model and backend architecture were intentionally designed before substantial production coding. Implementation inconvenience is not permission to casually redesign them.

If implementation appears to require a fundamental schema or architecture change:

1. Stop.
2. Determine whether the code is wrong or the frozen design truly missed a requirement.
3. Amend the architecture only when the change is genuinely necessary.
4. Record the migration/design consequence explicitly.

Normal implementation details remain flexible.

## Schema Migration Timing

Feature milestones govern when JPA entities, repositories, services, APIs, and UI behavior are implemented. Flyway migrations may create prerequisite tables earlier than their feature milestone when required by the frozen schema dependency order. Early schema creation does not authorize early feature implementation.

## Quality Is Continuous

Security, responsiveness, validation, loading/error/empty states, transaction safety, and reasonable query behavior are part of each feature when it is built. Later hardening milestones are system-wide audits, not the first time these concerns are addressed.

## Deployment Is Continuous

Keep the development application deployable. Every few milestones should produce a visible new capability in the live development application.

## “Day” Means Milestone

The numbered days preserve the original 30-step structure. They are implementation milestones, not promises that every milestone consumes exactly one calendar day.

---

# Day 1 — Product, UX & Visual System

**Status: COMPLETE**

## Goal

Define how Omnime works and looks before feature implementation.

## Completed

- Locked MVP boundaries and central user loop.
- Completed screen flow and UX behavior.
- Completed UX notes for the MVP screens.
- Completed high-fidelity reference mockups.
- Locked recommendation-first navigation.
- Locked onboarding behavior.
- Locked Home, Add Anime, Anime Details, Watched, Saved, My Taste, Account, and feedback interactions.
- Locked desktop sidebar and mobile bottom navigation.
- Locked Omnime Violet V1 palette.
- Locked Manrope typography system.
- Locked shape/depth system.
- Locked responsive system and `lg` architecture switch.
- Locked Tabler Icons direction.

## Core UX Decisions

- Home is recommendation-first.
- Add Anime is optimized for fast history entry.
- Watched and Saved are distinct purposeful screens.
- My Taste exposes Omnime's interpretation without fake confidence percentages.
- Feedback distinguishes raw user evidence from AI interpretation.
- “Correct My Taste” is distinct from editing raw feedback.
- Current Viewing Intent is temporary recommendation context, not permanent taste.
- Adult anime is excluded by default unless the user opts in.
- Recommendations remain useful for users with 0–4 watched anime; five watched titles marks the established personalized experience, not an eligibility gate.

## Deliverable

**Complete.** Omnime has a locked product/UX/visual reference before production feature implementation.

---

# Day 2 — PostgreSQL, Architecture Gate & Local Foundation

**Status: COMPLETE**

## Goal

Establish the permanent backend/database foundation and get the local application shell communicating end-to-end.

## PostgreSQL Learning

**Status: COMPLETE**

Covered the PostgreSQL concepts needed to begin development:

- Databases
- Tables
- Primary keys
- Foreign keys
- Constraints
- Identity/sequence-backed identifiers
- PostgreSQL data types
- CRUD
- Joins
- Index basics

## Architecture Gate

**Status: COMPLETE**

Before substantial production coding:

- Designed the complete 16-table MVP data model.
- Defined PostgreSQL/AniList/LLM/user ownership boundaries.
- Defined PKs, FKs, uniqueness, checks, deletion behavior, and important indexes.
- Defined JPA ownership/fetching rules.
- Defined Flyway dependency ordering and Hibernate `ddl-auto=validate` policy.
- Defined authentication, CSRF, CORS, and stateless JWT architecture.
- Defined UserAnime lifecycle and recommendation eligibility.
- Defined FeedbackEvidence → FeedbackInterpretation → TasteConclusion architecture.
- Defined TasteCorrection lifecycle and taste freshness versions.
- Defined AnimeProfile completion, atomicity, provenance, reuse, concurrency, and staleness semantics.
- Defined transient recommendation and Current Viewing Intent architecture.
- Defined Undo semantics.
- Defined AniList catalog/bootstrap and rate-limit strategy.
- Defined deployment architecture.
- Stress-tested the design.
- Completed adversarial AI reviews and final consistency review.
- Froze `MVP_DATA_MODEL.md` and `BACKEND_ARCHITECTURE.md`.

## Backend Foundation

Initialize Spring Boot with:

- PostgreSQL JDBC driver
- Spring Data JPA
- Hibernate
- Flyway
- Spring Security
- Validation
- Environment-based database credentials

Create the feature-oriented package skeleton defined by the frozen backend architecture:

- `auth`
- `user`
- `anime`
- `useranime`
- `feedback`
- `taste`
- `recommendation`
- `integration/anilist`
- `integration/llm`
- `common/config`
- `common/exception`
- `common/undo`

Do not create fake implementations merely to populate the packages.

## Frontend Foundation

Initialize React/Vite/TypeScript and establish:

- React Router
- API service/client layer
- Environment variables
- Shared component/layout structure
- Tailwind CSS v3
- Locked Omnime semantic design tokens
- Manrope
- Responsive application shell
- Desktop sidebar
- Mobile bottom navigation

The first frontend should already be recognizably Omnime even though most product features are not functional yet.

## Local Integration

Create the minimum local path needed to prove:

**React → Spring Boot → PostgreSQL**

Use a simple health/connectivity flow rather than prematurely implementing domain features.

## Deliverable

React, Spring Boot, and PostgreSQL communicate successfully locally inside a visually recognizable Omnime shell.

---

# Day 3 — First Development Deployment

**Status: COMPLETE**

## Goal

Get Omnime publicly accessible early and prove the real deployment topology.

## Deploy

- React/Vite → Vercel
- Spring Boot/Java 25 → Render Web Service via Docker
- PostgreSQL → Neon

Configure environment variables and secrets independently for frontend/backend.

## Browser Security Foundation

- Explicit frontend origin.
- Credentialed CORS; never wildcard with credentials.
- HTTPS in deployed environment.
- Cookie/domain/SameSite configuration compatible with the actual Vercel/Render topology.
- CSRF architecture remains enabled as authenticated mutation endpoints arrive.

## Health

Verify:

**Browser → Vercel → Render Spring Boot → Neon PostgreSQL**

Health checks must not depend on live AniList or LLM availability.

## Visual Target

The deployed shell should already use the locked Omnime palette, typography, navigation, spacing, radii, and responsive architecture. It does not need fake feature content.

## Deliverable

A publicly accessible **Omnime — In Active Development** application with frontend/backend/database communication.

---

# Day 4 — Authentication & Persistent User Accounts

## Goal

Create the identity foundation before user-owned product data exists.

## Database

Implement the required Flyway migrations for:

- `users`
- `password_reset_tokens`

Implement corresponding JPA entities/repositories only as needed by this slice.

## Backend

Build the locked auth architecture:

- Register
- Login
- Logout
- Authenticated-current-user resolution
- Password hashing using established Spring Security facilities
- Signed short-lived JWT in HttpOnly + Secure cookie
- CSRF protection for authenticated cookie mutations
- Forgot/reset-password flow with hashed reset tokens
- Onboarding-completed state
- Adult-content preference field

The authenticated server identity is authoritative; never trust a client-supplied user ID.

## Frontend

Build the locked Register/Login experience and:

- Authentication state
- Protected application routes
- Logout
- Account entry point
- Correct routing based on `onboarding_completed`

## Tests

Include authentication/authorization and CSRF success/failure coverage as the endpoints are built.

## Deliverable

A user can register, log in, leave, return, log in again, and access their persistent account securely.

---

# Day 5 — AniList Factual Catalog Bootstrap

## Goal

Give Omnime its persistent factual anime knowledge without mixing AniList facts with Omnime judgment.

## Database

Implement the relevant frozen schema in dependency order:

- `anime`
- `genre`
- `anilist_tag`
- `anime_genre`
- `anime_anilist_tag`

## Backend

Build `AniListClient` and `AnimeCatalogService` for controlled bootstrap/sync.

Persist the factual fields defined by the frozen model, including:

- AniList identity
- Titles
- Description
- Cover image URL
- Episode count
- Start year
- Status
- Average score when available
- Adult flag
- Genres
- AniList tags, ranks, and spoiler metadata

Do not store image files.

## Bootstrap Rules

- Bootstrap is an operational action, not application startup behavior.
- Use bounded partitions so no AniList Page traversal exceeds the API depth limit.
- Within each partition use normal `Page(page, perPage)` pagination and `hasNextPage`.
- Respect current rate-limit response headers and `Retry-After` rather than a fixed historical sleep.
- Upsert by unique AniList identity so overlap/restarts are safe.
- A routine AniList sync does not automatically invalidate Omnime AnimeProfiles.

## Deliverable

A useful local factual anime catalog exists in PostgreSQL with genres and AniList tag metadata clearly separated from Omnime-owned intelligence.

---

# Day 6 — Add Anime Search

## Goal

Ship the first real anime-facing product feature using the local catalog.

## Backend

Build ordinary local search through `AnimeService`/repository/controller DTOs.

React should not search AniList directly. Normal product search should read Omnime's PostgreSQL catalog.

## Frontend

Implement the locked Add Anime search experience:

- Search/autocomplete
- Cover/title/episode metadata
- Loading state
- Empty state
- Error state
- Responsive results
- Non-functional or appropriately disabled relationship actions until their backend slice arrives

Avoid raw API calls scattered through components; use the frontend API layer.

## Deliverable

A user can search Omnime's real anime catalog through React → Spring Boot → PostgreSQL in the deployed application.

---

# Day 7 — Anime Details

## Goal

Create the first complete factual anime reading experience.

## Backend

Implement the Anime Details use case using local PostgreSQL data and Omnime-specific DTOs. Ordinary details reads should not require a live AniList call.

## Frontend

Build the locked Anime Details representation with available factual metadata and relationship-aware action placeholders where functionality has not yet arrived.

The personalized “Why Omnime recommends this” area is contextual and remains absent/inactive outside recommendation context until the recommendation slice exists.

## Deliverable

**Search → Anime result → Anime Details** works end-to-end and responsively.

---

# Day 8 — UserAnime Relationship Foundation

## Goal

Create the single authoritative relationship between a user and an anime.

## Database

Implement `user_anime` according to the frozen model.

MVP relationship states are exactly:

- `SAVED`
- `WATCHED`
- `DROPPED`
- `NOT_INTERESTED`

No row means **None/unclassified**.

There is no `WATCHING` state in the MVP.

## Backend

`UserAnimeService` owns persistent relationship mutations across all screens.

Implement the foundational operations required to:

- Save
- Mark Watched
- Mark Dropped
- Mark Not Interested without a reason
- Change relationship state
- Remove/revert a relationship where allowed
- Set/clear rating according to locked lifecycle rules

Respect:

- One current relationship per `(user, anime)`.
- Optimistic versioning on UserAnime.
- Only None/unclassified anime are recommendation-eligible.
- Saved, Watched, Dropped, and Not Interested are excluded.
- Adult-content filtering remains separate.

## Taste Freshness

As UserAnime mutations are implemented, also implement the frozen taste-freshness rules for this authority boundary:

- Advance `users.taste_evidence_version` transactionally when a rating change or relationship transition changes usable/active taste input or evidence eligibility.
- Do not advance the version for exclusion-only changes that introduce no usable taste input, such as Not Interested with no reason.
- Keep the authoritative UserAnime mutation and any required freshness advancement in the same transaction.
- Add focused integration coverage for the canonical transitions as they are introduced.

## Frontend

Activate the shared relationship controls in Add Anime and Anime Details using the locked `Seen It │ ⌄` behavior and appropriate states.

## Deliverable

The same anime relationship behaves consistently no matter which implemented screen changes it.

---

# Day 9 — Watched & Saved Management

## Goal

Make persistent viewing history and watch-later state useful outside individual anime pages.

## Frontend

Build the locked representations:

### Watched

- Watched by default
- Deliberate Dropped filter
- Search/filter/sort as defined by UX
- Rating display/edit behavior
- Responsive compact cards below `lg`; table/grid hybrid at `lg+`

### Saved

- Saved by default
- Optional Dropped/Not Interested view according to locked UX
- Responsive 2/3/4-column grid
- Relationship actions reuse UserAnime operations

## Backend

Add only the queries/use cases needed by these screens. Use projections where list reads justify them; avoid entity-shaped API responses and N+1 behavior.

## Deliverable

Users can manage Watched, Dropped, Saved, and Not Interested relationships from the intended screens with consistent persistence.

---

# Day 10 — Fast Onboarding & Initial History

## Goal

Let a brand-new user establish enough history for Omnime to begin learning without repetitive detail-page navigation.

## Frontend

Implement the locked onboarding flow using the same Add Anime/UserAnime operations:

- Search
- Quick Watched entry
- Optional 1–10 rating
- Progressive education
- Skip education where allowed
- No genre survey
- No separate onboarding data model

Onboarding completion is explicit experience state, not derived from Watched count.

## Recommendation Readiness

Users with 0–4 watched anime are still valid Omnime users and may receive recommendations later. Five watched titles marks the normal established personalized experience, not a hard prerequisite.

## Deliverable

A new user can create an account, quickly add viewing history, complete/skip onboarding education, and reach Home.

---

# Day 11 — Feedback Evidence

## Goal

Capture exactly what the user says before AI interprets it.

## Database

Implement `feedback_evidence` and the frozen DB CHECK enforcing valid evidence shapes.

Evidence types:

- `VIEWING_EXPERIENCE`
- `NOT_INTERESTED_REASON`
- `TASTE_CORRECTION`

## Backend

`FeedbackService` owns raw evidence rules and structural validation.

For viewing feedback:

- Positive and/or negative text may be provided.
- One entry may contain mixed sentiment.
- Do not silently rewrite or move the user's text.

For Not Interested:

- A reason is optional.
- No reason means no FeedbackEvidence and no taste signal.
- A supplied reason is persisted as contextual evidence and linked through the current UserAnime pointer.

## Taste Freshness Boundary

Raw FeedbackEvidence is authoritative input, but raw creation alone is not automatically usable interpreted taste input.

- Preserve the frozen rule that `taste_evidence_version` advances when usable taste input or its eligibility changes, not merely because arbitrary raw text was stored.
- When a Not Interested reason is activated/cleared through UserAnime, apply the corresponding freshness rule transactionally with that authoritative relationship/pointer change.
- Editing evidence must invalidate/remove the prior interpretation before the replacement interpretation can become current.

## Frontend

Activate the locked feedback surfaces for implemented relationship workflows. Preserve the user's wording and clearly distinguish feedback entry from later AI interpretation.

## Deliverable

Omnime persistently captures structured raw taste evidence without pretending that raw text is already a taste conclusion.

---

# Day 12 — Feedback Interpretation

## Goal

Turn individual raw evidence into structured AI interpretation without risking authoritative user data.

## Database

Implement `feedback_interpretation`.

## Backend

Build `FeedbackInterpretationService` using `LlmClient`.

Flow:

1. Commit authoritative raw evidence first.
2. After commit, interpret the evidence outside the DB transaction.
3. Validate AI output.
4. Persist one current interpretation with model provenance.
5. Advance `users.taste_evidence_version` transactionally when a new/replaced current interpretation becomes usable taste input, according to the frozen freshness semantics.
6. Add focused tests proving failed interpretation does not falsely mark nonexistent interpreted input as incorporated and successful replacement interpretation leaves taste detectably stale until recompute.

If the LLM fails:

- Raw evidence remains valid.
- Missing interpretation remains durably detectable.
- Retry may occur on subsequent relevant access/operation and/or a lightweight scheduled recovery scan.
- No queue infrastructure or advisory-lock system is required for MVP.

Editing raw evidence invalidates/removes its old interpretation, commits the edit, then reinterprets.

## Deliverable

Individual feedback can be interpreted, failed interpretations are recoverable, and no LLM call is required to preserve the user's original evidence.

---

# Day 13 — Omnime Tag Ontology

## Goal

Create Omnime's controlled recommendation vocabulary.

## Database

Implement `omnime_tag`.

## Ontology

Curate the MVP vocabulary around recommendation-relevant viewing qualities such as:

- Tone
- Narrative style
- Character dynamics
- Pacing
- Themes
- Conflict
- Setting
- Emotional qualities
- Story structure

The LLM may judge relevance and assign approved tags. It may not invent new ontology entries during profile generation.

## Deliverable

Omnime has a controlled vocabulary distinct from AniList genres and AniList tags.

---

# Day 14 — Reusable Anime Profiles

## Goal

Give serious recommendation candidates reusable Omnime-owned viewing-experience intelligence.

## Database

Implement:

- `anime_profile`
- `anime_tag`

`anime_profile` is the durable completion marker:

- No row = no successful reusable profile.
- Row + zero AnimeTags = successful empty profile.
- Row + tags = successful populated profile.

Profile provenance belongs on AnimeProfile, not each AnimeTag.

## Backend

Build `AnimeProfileService`.

Input may include factual synopsis/metadata, genres, AniList tags/ranks, and the approved Omnime ontology.

The LLM judges numeric recommendation relevance. Java deterministically decides inclusion and maps qualifying tags to:

- `CORE`
- `STRONG`
- `PRESENT`

There is no arbitrary tag-count cap.

## Persistence & Failure

- Do not hold a DB transaction open during the LLM call.
- After successful generation, persist AnimeProfile and zero-or-more AnimeTags atomically in one short transaction.
- Failed generation leaves no AnimeProfile row.
- Concurrent duplicate generation is acceptable; persistence re-check/PK uniqueness resolves the race and reuses the winner.

## Scope

Do not pre-profile the entire AniList catalog merely because it exists locally. Profiles are reusable and generated selectively when recommendation work reaches a serious shortlist. A small controlled set may be generated during development to test the feature.

## Deliverable

Omnime can generate, validate, atomically persist, and reuse a recommendation-focused AnimeProfile.

---

# Day 15 — Taste Conclusions

## Goal

Turn interpreted evidence into an evolving, explainable representation of the user's taste.

## Database

Implement:

- `taste_conclusion`
- `taste_conclusion_interpretation`

## Backend

Build `TasteService` to reason across eligible FeedbackInterpretations.

Taste is not one opaque `UserTasteProfile` blob. Current taste consists of active conclusions with traceability to supporting interpretations.

Rules include:

- Ratings indicate **how much** the user liked something.
- Feedback helps explain **why**.
- Rating alone may provide broad association but should not fabricate detailed reasoning.
- Conclusion strength is internal evidence strength, not a fake user-facing confidence percentage.
- Stable `(user_id, conclusion_key)` identity supports updating/reactivating conclusions.
- Unsupported uncorrected conclusions may disappear.
- Inactive conclusions are excluded from current My Taste/recommendation reasoning.

## Freshness

Use the frozen `users.taste_evidence_version` and `users.taste_computed_version` semantics.

A recompute captures its target evidence version and may only advance computed version to that captured target. Evidence arriving during recompute therefore leaves taste correctly stale.

Implement integration coverage proving:

- recompute advances `taste_computed_version` only to the captured target,
- newly arriving evidence during recompute leaves `taste_evidence_version > taste_computed_version`,
- current taste can therefore be durably detected as stale.

## Deliverable

Omnime can derive persistent, traceable, current taste conclusions from interpreted user evidence.

---

# Day 16 — My Taste & Explicit Corrections

## Goal

Expose Omnime's understanding to the user and let the user correct it authoritatively.

## Database

Implement `taste_correction`.

## Backend

Build `TasteCorrectionService` using the frozen lifecycle:

- Validate target conclusion.
- Persist correction as FeedbackEvidence.
- Link correction to the conclusion.
- Commit authoritative data.
- Interpret after commit.
- Advance taste evidence freshness when the correction's interpretation becomes usable input.
- Recompute taste using the frozen captured-target version semantics.

`taste_correction.taste_conclusion_id` remains `ON DELETE RESTRICT`.

A corrected conclusion may become inactive but must remain traceable while a correction references it. Full account deletion explicitly deletes TasteCorrections first, then the User, in one transaction so remaining user-owned data can cascade.

## Frontend

Build the locked My Taste experience:

- Readable taste conclusions
- Evidence/interpretation context where designed
- No fake percentages
- `Correct My Taste` distinct from `Edit Feedback`
- Responsive reading layout and See All behavior

## Deliverable

Users can inspect and explicitly correct Omnime's understanding without destroying the evidence trail.

---

# Day 17 — Recommendation Eligibility & Candidate Narrowing

## Goal

Build the deterministic factual half of recommendation generation.

## Eligibility

Only anime with **no UserAnime relationship** are relationship-eligible.

Exclude:

- Saved
- Watched
- Dropped
- Not Interested

Also exclude adult anime when `include_adult_anime=false`.

Current Viewing Intent cannot override relationship exclusions or adult-content exclusion.

## Backend

Build the PostgreSQL/JPA candidate acquisition needed by `RecommendationService`.

Use factual/local signals to reduce the 20,000+ catalog to a manageable plausible pool without loading the entire catalog into Java or sending it to the LLM.

The exact candidate count is an implementation/tuning decision, not permanent architecture.

## Testing

Prove excluded anime cannot enter the candidate set, including state transitions and adult preference behavior.

## Deliverable

Given a user, Spring Boot can return a bounded set of factually eligible, plausible candidate anime without LLM ranking.

---

# Day 18 — Current Viewing Intent

## Goal

Let the user temporarily tell Omnime what they want **right now** without rewriting permanent taste.

## Backend

Implement transient `ParsedViewingIntent` semantics inside the recommendation boundary.

- Explicit requirements backed by reliable facts may become hard constraints.
- Ambiguous/qualitative language remains soft preference.
- Never invent numeric thresholds: “short” does not automatically mean 12 episodes.
- Explicit temporary intent may override conflicting long-term taste preferences.
- Intent may not override relationship eligibility or adult-content exclusion.
- Parse failure must not invent hard constraints; degrade gracefully using taste/catalog/general quality and raw intent as soft context where useful.

Do not create a CurrentViewingIntent table.

## Frontend

Build the locked optional natural-language intent control and its clear/change behavior within the active recommendation experience.

## Deliverable

A user can influence the current recommendation session without permanently changing My Taste.

---

# Day 19 — Initial AI Candidate Ranking

## Goal

Use AI judgment only after deterministic narrowing.

## Backend

Send a bounded candidate set plus relevant current taste and viewing intent to the LLM using factual AniList metadata available locally.

The LLM may compare and rank candidates, but may not:

- Search AniList
- Search PostgreSQL
- Search the full catalog
- Decide relationship eligibility
- Override hard constraints
- Override adult exclusion
- Invent anime
- Invent Omnime ontology entries

Validate returned identities against the supplied candidate set.

## Purpose

This ranking identifies a **serious shortlist** worth spending profile-generation work on. It is not yet the final recommendation result.

## Deliverable

Omnime can use AI judgment to turn a bounded factual candidate pool into a small serious shortlist.

---

# Day 20 — Final Recommendation Ranking & Explanations

## Goal

Complete Omnime's central recommendation engine.

## Pipeline

For the serious shortlist:

1. Reuse existing AnimeProfiles.
2. Generate only missing profiles, with bounded concurrency.
3. Allow an individual profile failure to degrade gracefully rather than corrupting the whole request.
4. Send successful reusable profiles, Omnime tags, relevant factual metadata, current taste, and optional viewing intent into final AI judgment.
5. Validate final output against the supplied candidates and current hard eligibility.
6. Apply the quality threshold.
7. Return the strongest **up to five** recommendations.
8. Produce grounded, concise personalized explanations.

Five is a maximum, not a quota.

## Failure Behavior

- A core ranking LLM failure returns a clean retryable failure state.
- One missing profile does not rollback successful profiles or authoritative data.
- No recommendation result needs its own persistent table for MVP.

## Deliverable

**User evidence → Taste → Eligibility → Candidate narrowing → Initial AI judgment → Serious shortlist → Reusable profiles → Final AI judgment → ≤5 recommendations** works end-to-end at the service/API level.

---

# Day 21 — Recommendation Home

## Goal

Expose the completed recommendation pipeline through Omnime's primary experience.

## Frontend

Implement the locked Home design:

- #1 recommendation visually dominant
- #2–#5 secondary recommendations
- Artwork and useful metadata
- Concise “Why Omnime recommends this” reasoning
- Save
- Seen It split control
- Not Interested
- More Info
- Loading/error/empty/no-eligible states
- Responsive reflow preserving #1 dominance

Avoid unrelated dashboard filler. **The recommendations are the content.**

## Find Me 5 More

Use the active transient viewing intent and IDs already shown in the current request/session context to seek another set. Do not create recommendation-history/session tables merely for this behavior.

## Deliverable

A user opens Home and receives a polished, personalized set of unseen actionable recommendations.

---

# Day 22 — Recommendation Resolution, Undo & Feedback Loop

## Goal

Close the central loop: recommendation → reaction → persistence → future recommendation change.

## Backend

Use the same UserAnime/Feedback services already established. Recommendation screens do not own duplicate relationship logic.

Before exposing the first Undo endpoint, implement `common/undo/UndoTokenService` and focused unit tests. This service owns only the cryptographic token mechanics defined by the frozen architecture; it does not persist Undo state. Undo is intentionally implemented here, at its first real recommendation-resolution consumer, rather than as unused infrastructure earlier in the roadmap.

Implement the locked Undo behavior:

- React holds short-lived UI state.
- Opaque cryptographically authenticated token.
- Short configurable TTL aligned with the Undo experience.
- Bind token to user + operation + restoration data.
- Enforce current-state/version/absence preconditions.
- Delete→Undo recreates atomically with a fresh PK/version.
- Successful/stale replay fails through state preconditions; no persistent Undo table or nonce registry is required.

## Home UX

When the user resolves a recommendation through Save/Watched/Dropped/Not Interested:

- Persist immediately.
- Keep the resolved card in its exact slot during the short Undo window.
- Show resolved visual state + toast/Undo.
- Undo restores/reverses and keeps the card.
- After expiry, remove the resolved card and fill with another eligible recommendation when available.

Save is a positive resolution: **keep this, show me another.**

## Taste Effects

Only meaningful taste-input/eligibility changes advance the appropriate freshness version. Not Interested without a reason remains exclusion-only. Undo must apply the same frozen freshness rules as the authoritative mutation it reverses/restores so version state never silently diverges from active taste evidence.

## Deliverable

The core loop visibly reacts to user decisions, supports safe short-lived Undo, and future recommendation work respects the new facts.

---

# Day 23 — Anime Details Recommendation Context & Relationship Polish

## Goal

Make Anime Details fully relationship-aware and useful when reached from recommendations or management screens.

## Frontend/Backend

Complete contextual behavior:

- Recommendation explanation appears only when recommendation context supplies it.
- Seen It acts immediately through UserAnimeService.
- Save ↔ Saved behavior is correct.
- Saved + Seen It → Watched removes Saved by changing the single relationship state.
- Dropped/Not Interested recovery follows locked transition semantics.
- Rating preservation/clearing follows the frozen lifecycle.
- Feedback eligibility follows current relationship state.

## Deliverable

Anime Details behaves correctly regardless of whether the user arrived from Add Anime, Home, Watched, or Saved.

---

# Day 24 — Account, Adult Preference & Full Relationship Audit

## Goal

Complete account-level controls and audit relationship behavior across the application.

## Account

Implement:

- Display/account information defined for MVP
- `Include adult anime` toggle, default OFF
- Logout
- Delete account

Account deletion is a transactional UserService operation:

1. Explicitly delete the user's TasteCorrections.
2. Delete the User.
3. Allow DB cascades to remove remaining user-owned rows.
4. Roll back the whole operation on failure.

Shared anime/catalog/profile data remains.

## Relationship Audit

Test all supported relationship transitions and their effects on:

- Recommendation eligibility
- Rating lifecycle
- Feedback evidence activity
- Not Interested reason activity
- Taste evidence version
- UI state across Home/Add/Details/Watched/Saved

## Deliverable

Account controls and relationship semantics are complete and consistent across the MVP.

---

# Day 25 — Resilience & Edge-Case Audit

## Goal

Audit failure behavior that should already exist within individual slices.

Test at minimum:

- Brand-new user
- 0–4 watched anime
- Five+ watched anime
- No ratings
- Ratings without written feedback
- Written feedback without positive text or without negative text where valid
- Hundreds of UserAnime rows
- AniList unavailable during sync/bootstrap
- LLM unavailable during interpretation
- LLM unavailable during ranking
- Invalid LLM response
- AnimeProfile generation failure
- Missing FeedbackInterpretation
- Stale taste (`taste_evidence_version > taste_computed_version`)
- Canonical rating, relationship, Not Interested reason, interpretation replacement, correction, and Undo paths advance/preserve taste freshness exactly as defined by the frozen architecture
- Anime missing optional metadata
- Duplicate/racing relationship writes
- Database failure
- Candidate pool too small
- No eligible recommendations
- Current Viewing Intent parse failure
- Undo expiry/stale replay

## Recovery

Verify durable derived-work detection:

- FeedbackEvidence missing FeedbackInterpretation is discoverable.
- Users with evidence version greater than computed version are discoverable.
- Recovery does not require Kafka, Redis, or a persistent job table.

## Deliverable

Failure produces controlled states/retries rather than blank screens, silent corruption, or lost authoritative user input.

---

# Day 26 — Responsive & Accessibility Audit

## Goal

Audit the complete product across the responsive system that was used throughout implementation.

Test:

- Small phones
- Large phones
- Tablets
- Desktop
- Desktop sidebar at `lg+`
- Mobile bottom navigation below `lg`
- Reserved content space around fixed navigation
- Touch targets around 44px where appropriate
- No page-level horizontal scrolling
- Stable 16px-and-below typography
- Saved 2/3/4-column behavior
- Watched mobile-card → desktop-table behavior
- My Taste readable line lengths
- Mobile/desktop modal/sheet representations
- Keyboard/focus states
- Form labels and useful accessible names
- Contrast and disabled/focus/error states

## Deliverable

Omnime feels intentionally designed on mobile and desktop rather than retrofitted at the end.

---

# Day 27 — Backend Performance, Catalog & Cost Audit

## Goal

Verify the implemented system handles the local anime catalog and external AI/API costs intentionally.

## PostgreSQL/JPA

Review actual queries and use query plans where useful:

- Candidate narrowing
- UserAnime user/status reads
- Anime tag joins
- AniList tag joins
- Search/autocomplete
- Taste evidence/conclusion reads
- N+1 risks
- Oversized entity graphs/API responses
- Index usage where queries demonstrate need

**20,000+ anime in PostgreSQL is not inherently a problem. Loading or processing all of them for one recommendation request is.**

## External Calls

Check for:

- Excessive AniList calls
- Excessive LLM calls
- Duplicate AnimeProfile generation
- Unbounded profile-generation concurrency
- Long DB transactions around remote calls
- Unnecessary taste recomputation

## AniList Operational Review

Verify bootstrap partitions, pagination, rate-limit handling, restart safety, and sync behavior against then-current API behavior before a full refresh.

## Deliverable

Recommendation requests and catalog operations use intentionally bounded datasets and external calls.

---

# Day 28 — Security, Deployment & Core-Loop Audit

## Goal

Perform a final system-wide security/deployment review and test Omnime as a real new user.

## Security Audit

Review:

- Password hashing
- Authentication/authorization
- Short-lived signed JWT cookie configuration
- CSRF enforcement
- CORS
- Secure/SameSite cookie behavior in deployed topology
- Reset-token hashing/expiry
- API validation
- Authenticated user ownership checks
- Secrets/environment variables
- Safe error responses
- Logging/PII redaction
- No secrets in GitHub

No refresh-token system, JWT blacklist, Redis session store, or token-version infrastructure is required unless a new requirement has emerged.

## Core Product Loop

Using a fresh deployed account:

1. Register.
2. Complete/skip onboarding education.
3. Add watched anime quickly.
4. Rate several anime.
5. Add positive/negative viewing feedback.
6. Verify feedback interpretation/taste conclusions.
7. Inspect My Taste.
8. Correct an inaccurate conclusion.
9. Receive recommendations.
10. Use Current Viewing Intent.
11. Read explanations.
12. Save one recommendation.
13. Mark one Not Interested, with and without a reason as separate cases.
14. Mark one Watched and provide feedback.
15. Exercise Undo.
16. Request another recommendation set.
17. Verify excluded relationships never return.
18. Logout.
19. Return later and verify persistent state.

## Deliverable

The deployed MVP core loop works securely without developer intervention.

---

# Day 29 — Portfolio & Engineering Documentation

## Goal

Make both the product and the engineering decisions understandable to recruiters and developers.

## README

Explain:

- What Omnime does and does not try to be
- React/Vite → Spring Boot → PostgreSQL deployment architecture
- AniList factual catalog layer
- AniList tags vs Omnime tags
- FeedbackEvidence → FeedbackInterpretation → TasteConclusion
- Taste corrections and explainability
- Deterministic eligibility
- Current Viewing Intent
- Candidate narrowing
- Initial AI ranking
- Selective reusable AnimeProfile generation
- Final AI ranking and grounded explanations
- Why recommendation results remain transient
- Why AI does not search the full catalog
- Cost/failure controls
- Key security decisions

## Portfolio

Present:

**Omnime — AI-Powered Anime Recommendation Platform**

Include:

- Live Demo
- GitHub
- Project Details
- Product screenshots
- Short explanation of the engineering challenge and solution

Use **In Active Development** until the stable MVP release is tagged.

## Deliverable

Someone unfamiliar with Omnime can understand what it solves, how it works, and what engineering responsibilities the project demonstrates.

---

# Day 30 — Stable MVP Release

## Goal

Freeze features, test the deployed product, fix release-blocking issues, and tag the first stable MVP.

Do not add new features during this milestone.

## Verify in the Deployed Application

- Vercel frontend
- Render Spring Boot
- Neon PostgreSQL
- Register/login/logout/reset
- CSRF/CORS/cookies
- Onboarding
- Catalog/search
- Anime Details
- UserAnime transitions
- Ratings
- FeedbackEvidence
- FeedbackInterpretation
- My Taste
- Taste corrections
- Taste freshness/recovery
- Adult-content preference
- Recommendation eligibility
- Current Viewing Intent
- Initial AI ranking
- AnimeProfile generation/reuse
- Final ranking
- Recommendation explanations
- Home
- Find Me 5 More
- Saved
- Watched/Dropped management
- Not Interested behavior
- Undo
- Mobile/tablet/desktop
- Loading/error/empty/retry states
- Account deletion
- Persistence across sessions

Fix deployment-specific issues and tag the stable release in GitHub.

## Deliverable

**Omnime MVP v1.0** is publicly usable and demonstrates the complete central recommendation loop.

---

# MVP Success Criteria

A user can:

1. Create an account and return later with persistent data.
2. Quickly add anime they have watched.
3. Rate watched anime from 1–10.
4. Explain what they liked and/or disliked through structured viewing feedback.
5. Save, Watched, Drop, or mark anime Not Interested through one consistent relationship system.
6. Optionally explain why an anime is Not Interested; absence of a reason remains exclusion-only.
7. Have Omnime interpret individual feedback without changing the original wording.
8. See evolving, traceable My Taste conclusions.
9. Correct an inaccurate taste conclusion explicitly.
10. Receive recommendations containing only relationship-eligible anime and respecting adult-content preference.
11. Receive useful recommendations even before reaching five Watched titles, with personalization improving as evidence grows.
12. Temporarily tell Omnime what they want right now without permanently rewriting taste.
13. Understand why each recommendation was selected without fake precision.
14. Save or resolve recommendations and receive fresh actionable replacements.
15. Use short-lived Undo for supported recommendation actions.
16. Return later with persistent account/history/taste information intact.
17. Receive increasingly personalized recommendations as meaningful evidence changes.

---

# Final Recommendation Architecture

## Stage 1 — Factual Anime Knowledge

AniList
↓
PostgreSQL `anime` + genres + AniList tags/ranks

This is factual/source metadata. Routine product search and recommendation candidate acquisition operate from the local catalog.

## Stage 2 — User Evidence

UserAnime relationships + ratings
↓
FeedbackEvidence
↓
FeedbackInterpretation
↓
TasteConclusion + traceability
↓
TasteCorrection when the user explicitly corrects Omnime

Raw evidence and AI interpretation remain separate.

## Stage 3 — Deterministic Eligibility & Candidate Narrowing

Spring Boot/PostgreSQL:

- Excludes Saved/Watched/Dropped/Not Interested
- Excludes adult anime unless opted in
- Applies reliable hard constraints
- Narrows the local catalog to a bounded plausible pool

**Code handles facts.**

## Stage 4 — Initial AI Judgment

Bounded candidates + taste + optional Current Viewing Intent
↓
LLM comparison/ranking
↓
Serious shortlist

No full-catalog LLM search.

## Stage 5 — Reusable Anime Intelligence

For serious-shortlist candidates:

- Reuse AnimeProfile when present.
- Generate only missing profiles.
- LLM judges approved Omnime-tag relevance.
- Java deterministically applies inclusion/centrality rules.
- Persist successful AnimeProfile + AnimeTags atomically.

AniList tags remain supporting source metadata rather than equal primary Omnime recommendation signals.

## Stage 6 — Final AI Judgment

Serious shortlist + reusable profiles + Omnime tags + factual metadata + taste + optional intent
↓
LLM final comparison/ranking/explanations
↓
Backend validates eligibility and identities
↓
Quality threshold
↓
Strongest **up to five** recommendations

**AI handles judgment. Code validates the result.**

## Stage 7 — Feedback Loop

User resolves recommendations
↓
UserAnime/FeedbackEvidence persists authoritative facts
↓
Interpretation/taste freshness changes when meaningful
↓
Future eligibility/candidate narrowing/ranking changes

Recommendation results themselves remain transient in MVP.

---

# Data Ownership Summary

## PostgreSQL Persistent Truth

Persistent user/account state, shared anime catalog, AniList source metadata, Omnime ontology, reusable AnimeProfiles/AnimeTags, UserAnime relationships, raw FeedbackEvidence, current FeedbackInterpretations, current/retained TasteConclusions and correction traceability.

## AniList

External factual source for anime metadata, genres, and detailed source tags/ranks.

## LLM

Judgment/interpretation provider for:

- Feedback interpretation
- Taste reasoning
- Current Viewing Intent parsing where useful
- Initial candidate judgment
- AnimeProfile tag relevance
- Final candidate ranking
- Human-readable recommendation explanations

The LLM does not own authoritative application state.

## Transient

- Recommendation result sets
- Personalized recommendation explanations outside persisted profile/taste structures
- Current Viewing Intent
- ParsedViewingIntent
- “Find me 5 more” shown-ID context
- Short-lived frontend resolved-card state

---

# Explicitly Not MVP

Do not delay release for:

- Social features
- Friends/followers
- Community reviews/forums
- Chat or conversational Ask Omnime
- Voice input
- News feeds
- Large traditional anime browsing system
- Native mobile applications
- Multiple recommendation algorithms/modes
- Elaborate public profiles
- Complex personalization controls
- Recommendation-history analytics
- Persistent recommendation snapshots
- WATCHING relationship state
- Generic UserAnime notes field
- Redis
- Kafka/RabbitMQ
- Microservices
- Kubernetes
- Event sourcing
- Soft deletion/history architecture
- Persistent Undo history
- Refresh-token/JWT-blacklist infrastructure

First prove the central promise:

**Omnime understands what I like and consistently finds anime I haven't seen that I actually want to watch.**
