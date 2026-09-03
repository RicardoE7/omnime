# Omnime MVP Data Model

This document is the canonical, FROZEN data model for the Omnime MVP. It incorporates the controlled architecture stress-test amendments and the final adversarial review resolutions. It documents the persistent PostgreSQL schema, ownership, important constraints, and service-level invariants required by the locked UX and backend architecture. Further schema changes require a newly discovered genuine contradiction or an intentional post-MVP migration.

Do NOT change the schema in this document unless a genuine contradiction is found and explicitly reported. This document intentionally avoids operational logs, transient session state, recommendation-session persistence, or undo history persistence.

Contents
1. Overview / Design Principles
2. Table Definitions (detailed)
   - users
   - password_reset_tokens
   - anime
   - genre
   - anime_genre
   - anilist_tag
   - anime_anilist_tag
   - omnime_tag
   - anime_profile
   - anime_tag
   - user_anime
   - feedback_evidence
   - feedback_interpretation
   - taste_conclusion
   - taste_conclusion_interpretation
   - taste_correction
3. Enum / status definitions
4. User ↔ Anime transition rules (DB mutation summary)
5. DELETE → UNDO service/API invariant (exact locked behavior)
6. Workflow stress tests (reads/writes/transactions per workflow)
7. Data ownership map
8. JPA / Spring mapping and implications
9. Flyway / Hibernate policy and migration ordering notes
10. Final checklist and frozen status

---

## 1. Overview / Design Principles

- PostgreSQL owns persistent application truth.
- AniList supplies external anime facts, genres, and ranked tags; Omnime bootstraps and maintains a useful local catalog snapshot for search and recommendation candidate discovery.
- LLM-derived structured outputs (interpretations and materialized taste conclusions) are persisted in their minimal necessary form; full prompt/response logs are not persisted in the domain schema.
- Raw user-authored evidence is preserved verbatim and remains distinct from AI-derived interpretation.
- Recommendation sessions, Undo state, and current viewing intent are transient in MVP. No persisted RecommendationSession, No LastAction, No persisted Undo tokens, No Redis dependency.
- Delete → Undo for row deletion (WATCHED → NONE) uses a short-lived, server-generated, cryptographically-authenticated opaque Undo token returned in the mutation response (NOT persisted). See section 5 for exact invariant.

Naming and ownership:
- Tables are named in lowercase snake_case.
- Ownership categories: "PostgreSQL application truth", "AniList-owned (persisted snapshot)", "Omnime-owned domain data", "LLM-derived persisted judgment", "user-authored persistent evidence".

---

## 2. Table Definitions

For each table: PURPOSE / REQUIRED BY / OWNER / COLUMNS / PK / FKs / UNIQUE / CHECKS / INDEXES / ON DELETE / JPA / RELATIONSHIPS / WHY EXISTS / WHY NOT MERGED

Notes:
- Column types use PostgreSQL types.
- Timestamps use timestamptz where relevant.

---

### users

1. PURPOSE
- Account record and persistent onboarding state.

2. REQUIRED BY
- Authentication, onboarding routing, ownership of user data.

3. OWNER
- PostgreSQL application truth.

4. COLUMNS
- id: UUID, NOT NULL, PRIMARY KEY
  - Semantic: user identifier
- email: varchar(254), NOT NULL, UNIQUE
  - Semantic: login identifier
- password_hash: varchar(255), NOT NULL
  - Semantic: salted/hash of password
- display_name: varchar(100), NULL
- onboarding_completed: boolean, NOT NULL, DEFAULT false
- include_adult_anime: boolean, NOT NULL, DEFAULT false
  - Semantic: explicit user preference controlling whether AniList adult-classified anime may enter recommendation candidate evaluation; false by default
- taste_evidence_version: BIGINT, NOT NULL, DEFAULT 0
  - Semantic: monotonically increasing version of usable taste input
- taste_computed_version: BIGINT, NOT NULL, DEFAULT 0
  - Semantic: latest evidence version successfully incorporated into the materialized taste model
- created_at: timestamptz, NOT NULL, DEFAULT now()
- updated_at: timestamptz, NOT NULL, DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- none

7. UNIQUE CONSTRAINTS
- UNIQUE(email)

8. CHECK CONSTRAINTS
- none (format validation at app level)

9. IMPORTANT INDEXES
- unique index on email (primary lookup for login)

10. ON DELETE BEHAVIOR
- N/A (deleting a user is handled at application level and cascades via other tables' FKs)

11. JPA REPRESENTATION
- Entity: User
- No @Version on users (no optimistic locking on users)
- Collections of user-owned data should be LAZY in entity mapping

12. RELATIONSHIP OWNERSHIP
- Other tables reference users.id with ON DELETE CASCADE (user-owned data is cascade-deleted on account deletion)

13. WHY THIS TABLE MUST EXIST
- Fundamental account information and authoritative onboarding flag

14. WHY NOT MERGE INTO ANOTHER TABLE
- Distinct domain concept (authentication, PII)

---

### password_reset_tokens

1. PURPOSE
- Support secure password reset requests.

2. REQUIRED BY
- Forgot/reset password flows.

3. OWNER
- PostgreSQL application truth.

4. COLUMNS
- id: UUID PK NOT NULL
- user_id: UUID NOT NULL
- token_hash: varchar(512) NOT NULL
  - Semantic: hashed token for verification (recommended)
- expires_at: timestamptz NOT NULL
- created_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(user_id) -> users(id) ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- UNIQUE(token_hash)

8. CHECK CONSTRAINTS
- expires_at > created_at

9. IMPORTANT INDEXES
- index on token_hash for lookup

10. ON DELETE BEHAVIOR
- DELETE user -> cascade password_reset_tokens

11. JPA REPRESENTATION
- ManyToOne user (LAZY)

12. WHY EXISTS
- Security token lifecycle and invalidation

13. WHY NOT MERGE
- Security isolation; tokens are transient but require server-side invalidation

---

### anime

1. PURPOSE
- Local AniList snapshot of anime facts required for eligibility, filtering, and UI rendering.

2. REQUIRED BY
- Search/autocomplete, Home/Details rendering, deterministic filters (episode count), recommendation eligibility.

3. OWNER
- AniList-owned external fact persisted locally (operational copy in Postgres).

4. COLUMNS
- id: BIGSERIAL PRIMARY KEY
- anilist_id: integer NOT NULL UNIQUE
  - Semantic: AniList external id
- title_canonical: varchar(512) NOT NULL
- title_romaji: varchar(512) NULL
- title_english: varchar(512) NULL
- title_native: varchar(512) NULL
- description: text NULL
  - LOCKED: AniList description persisted locally (not AI-generated)
- cover_image_url: varchar(1024) NULL
- episode_count: integer NULL CHECK (episode_count >= 0)
- start_year: integer NULL
- status: varchar(32) NOT NULL
  - LOCKED: status required for MVP (NOT NULL)
- average_score: numeric(4,2) NULL
  - Persist whenever AniList supplies it; may be NULL if AniList does not provide it
- is_adult: boolean NOT NULL DEFAULT false
  - Semantic: AniList media-level adult-content classification persisted for deterministic recommendation eligibility
- last_synced_at: timestamptz NULL
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- none

7. UNIQUE CONSTRAINTS
- UNIQUE(anilist_id)

8. CHECK CONSTRAINTS
- episode_count >= 0 WHEN NOT NULL

9. IMPORTANT INDEXES
- UNIQUE index on anilist_id
- b-tree index on lower(title_canonical) for prefix search
- b-tree index on lower(title_english)
- b-tree index on lower(title_romaji)
- index on episode_count for factual filtering

10. ON DELETE BEHAVIOR
- RESTRICT/NO ACTION for user-owned FKs referencing anime (do not cascade delete user history automatically). Deleting anime is deliberate.

11. JPA REPRESENTATION
- Anime entity; title fields mapped individually; collections for genres/tags LAZY

12. RELATIONSHIP OWNERSHIP
- anime is parent of anime_genre, anime_anilist_tag, and anime_tag collections (join tables)

13. WHY THIS TABLE MUST EXIST
- Local factual data for deterministic eligibility & fast UI, including adult-content eligibility enforcement

14. WHY NOT MERGE
- AniList facts are distinct and reused across users

INGESTION INVARIANT (LOCKED)
- The backend must ensure anime.status is obtained before inserting the anime row (status is required and NOT NULL). However, anime.average_score may legitimately be NULL if AniList has not provided a score; absence of average_score must NOT block inserting the anime row. Service/UI code must tolerate average_score = NULL and treat null as "no AniList average score available".
- The backend must persist AniList media-level adult classification in anime.is_adult during catalog bootstrap/synchronization. Adult-classified anime are excluded from recommendation candidates unless the authenticated user has explicitly enabled users.include_adult_anime. Current Viewing Intent must not override this account-level content preference.

---

### genre

1. PURPOSE
- Normalized AniList genre names

2. REQUIRED BY
- Filtering and UI (genre display)

3. OWNER
- AniList-owned (persisted locally)

4. COLUMNS
- id: BIGSERIAL PK
- name: varchar(100) NOT NULL UNIQUE
- created_at/updated_at

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- none

7. UNIQUE CONSTRAINTS
- UNIQUE(name)

8. CHECK CONSTRAINTS
- name <> ''

9. IMPORTANT INDEXES
- unique index on name

10. ON DELETE BEHAVIOR
- cascade anime_genre rows when genre removed

11. JPA
- Genre entity

12. WHY EXIST
- Enables normalized filtering for eligibility and Watched search

---

### anime_genre

1. PURPOSE
- Join table anime <-> genre

2. REQUIRED BY
- Filtering and UI

3. OWNER
- local persisted AniList relationships

4. COLUMNS
- anime_id BIGINT NOT NULL
- genre_id BIGINT NOT NULL

5. PRIMARY KEY
- (anime_id, genre_id)

6. FOREIGN KEYS
- FK(anime_id) -> anime(id) ON DELETE CASCADE
- FK(genre_id) -> genre(id) ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- composite PK ensures uniqueness

8. CHECK CONSTRAINTS
- none

9. INDEXES
- index on genre_id for lookups

10. ON DELETE BEHAVIOR
- cascade mappings

11. JPA
- explicit join entity or mapped ManyToMany

12. WHY EXISTS
- normalized mapping

---

### anilist_tag

1. PURPOSE
- Persist AniList's external tag vocabulary separately from Omnime's custom tag ontology.

2. REQUIRED BY
- Catalog bootstrap/synchronization, first-pass recommendation candidate discovery, and supporting metadata for Omnime Anime Profile generation.

3. OWNER
- AniList-owned external metadata persisted locally.

4. COLUMNS
- id: BIGSERIAL PRIMARY KEY
- anilist_tag_id: integer NOT NULL UNIQUE
- name: varchar(200) NOT NULL
- description: text NULL
- category: varchar(200) NULL
- is_adult: boolean NOT NULL DEFAULT false
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- none

7. UNIQUE CONSTRAINTS
- UNIQUE(anilist_tag_id)

8. CHECK CONSTRAINTS
- name <> ''

9. IMPORTANT INDEXES
- unique index on anilist_tag_id
- index on name

10. ON DELETE BEHAVIOR
- deleting an anilist_tag cascades its anime_anilist_tag mapping rows

11. JPA REPRESENTATION
- AniListTag entity

12. WHY THIS TABLE MUST EXIST
- AniList tags are detailed external themes/elements and must remain distinguishable from Omnime-owned recommendation intelligence.

13. WHY NOT MERGE
- AniList tags are external source metadata while Omnime tags are a curated Omnime ontology with different semantics and ownership.

---

### anime_anilist_tag

1. PURPOSE
- Persist each anime's AniList tag assignment, including relevance rank and spoiler metadata.

2. REQUIRED BY
- Catalog bootstrap/synchronization, first-pass recommendation candidate discovery, and source/supporting metadata for Omnime Anime Profile generation.

3. OWNER
- AniList-owned external metadata persisted locally.

4. COLUMNS
- anime_id: BIGINT NOT NULL
- anilist_tag_id: BIGINT NOT NULL
  - Semantic: local FK to anilist_tag.id
- rank: smallint NOT NULL
  - Semantic: AniList per-anime relevance rank (0..100)
- is_general_spoiler: boolean NOT NULL DEFAULT false
- is_media_spoiler: boolean NOT NULL DEFAULT false
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- (anime_id, anilist_tag_id)

6. FOREIGN KEYS
- FK(anime_id) -> anime(id) ON DELETE CASCADE
- FK(anilist_tag_id) -> anilist_tag(id) ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- composite primary key ensures one mapping per anime + AniList tag

8. CHECK CONSTRAINTS
- rank BETWEEN 0 AND 100

9. IMPORTANT INDEXES
- index on anilist_tag_id
- index on (anime_id, rank DESC)

10. ON DELETE BEHAVIOR
- deleting anime or anilist_tag cascades mapping rows

11. JPA REPRESENTATION
- AnimeAniListTag explicit join entity; ManyToOne anime (LAZY), ManyToOne AniListTag (LAZY)

12. WHY THIS TABLE MUST EXIST
- AniList relevance rank and spoiler flags describe a tag's relationship to a particular anime, not the tag definition itself.

13. WHY NOT MERGE
- anime_anilist_tag stores AniList-owned external classification; anime_tag stores Omnime-owned recommendation intelligence and centrality.

---

### omnime_tag

1. PURPOSE
- Omnime's curated recommendation-intelligence ontology used in Omnime Anime Profiles; these tags describe qualities that materially define the viewing experience and are not AniList tags.

2. REQUIRED BY
- Omnime Anime Profile and taste modeling

3. OWNER
- Omnime-owned domain data

4. COLUMNS
- id: BIGSERIAL PK
- key: varchar(100) NOT NULL UNIQUE (machine key)
- display_name: varchar(200) NOT NULL
- description: text NULL
- created_at/updated_at

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- none

7. UNIQUE CONSTRAINTS
- UNIQUE(key)

8. CHECK CONSTRAINTS
- key <> ''

9. INDEXES
- unique index on key

10. ON DELETE BEHAVIOR
- cascade anime_tag rows on tag deletion

11. JPA
- OmnimeTag entity

12. WHY EXISTS
- persistent curated ontology for reuse

---

### anime_profile

1. PURPOSE
- Durable completion/provenance marker for one successful reusable Omnime Anime Profile per anime, including valid profiles that produce zero qualifying Omnime tags.

2. REQUIRED BY
- AnimeProfileService idempotency, zero-tag profile completion, reusable profile provenance.

3. OWNER
- Omnime-owned domain data / LLM-derived persisted judgment.

4. COLUMNS
- anime_id: BIGINT PRIMARY KEY
- model_identifier: varchar(128) NULL
- model_version: varchar(64) NULL
- generated_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- anime_id

6. FOREIGN KEYS
- FK(anime_id) -> anime.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- PRIMARY KEY(anime_id) guarantees at most one current successful profile per anime.

8. CHECK CONSTRAINTS
- none

9. IMPORTANT INDEXES
- primary-key index on anime_id
- model_version may be indexed later only if refresh operations justify it

10. ON DELETE BEHAVIOR
- deleting anime cascades its universal Omnime profile marker; this is shared catalog/profile data and is not affected by user deletion.

11. JPA REPRESENTATION
- AnimeProfile entity with one-to-one/shared-primary-key relationship to Anime.

12. WHY THIS TABLE MUST EXIST
- Absence of anime_tag rows cannot distinguish an unprofiled anime from a successfully profiled anime for which no Omnime tags cleared the inclusion threshold.

13. PROFILE COMPLETION / ATOMICITY INVARIANT (LOCKED)
- No anime_profile row = no successful reusable profile currently exists.
- anime_profile row + zero anime_tag rows = successful empty profile.
- anime_profile row + anime_tag rows = successful populated profile.
- Failed generation creates no anime_profile row. There are no PENDING/FAILED/PROCESSING profile states in MVP.
- After remote generation and validation complete outside the database transaction, AnimeProfileService MUST persist the anime_profile row and all qualifying anime_tag rows for that generation in one short transaction. A failure during persistence rolls back both marker and mappings, preventing a partial write from masquerading as a legitimate zero-tag profile.
- A legitimate successful zero-tag result intentionally commits the anime_profile row with zero anime_tag mappings.
- Profile-level model provenance belongs here rather than being duplicated on each anime_tag mapping.

---

### anime_tag (Omnime Anime Profile tag mappings)

1. PURPOSE
- Represents the tag-mapping portion of Omnime's universal anime profile: anime -> omnime_tag with centrality. Profile completion/provenance is represented by anime_profile. This table contains only Omnime-owned custom tag assignments, never AniList tag assignments.

2. REQUIRED BY
- Recommendation ranking and omnime anime profile reuse

3. OWNER
- Omnime-owned domain data (persisted characterizations; AI-derived input accepted into Omnime ownership)

4. COLUMNS
- id: BIGSERIAL PK
- anime_id: BIGINT NOT NULL
- tag_id: BIGINT NOT NULL
- centrality: varchar(16) NOT NULL CHECK (centrality IN ('CORE','STRONG','PRESENT'))
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(anime_id) -> anime.id ON DELETE CASCADE
- FK(tag_id) -> omnime_tag.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- UNIQUE(anime_id, tag_id)

8. CHECK CONSTRAINTS
- centrality allowed values only

9. IMPORTANT INDEXES
- index on tag_id
- index on (anime_id, centrality)

10. ON DELETE BEHAVIOR
- tag deletion or deliberate anime deletion cascades mapping rows

11. JPA REPRESENTATION
- AnimeTag entity; ManyToOne anime (LAZY), ManyToOne tag (LAZY)

12. WHY EXISTS
- Persists universal tag mappings and centrality for reuse

13. WHY NOT MERGE
- Multiple tags per anime with metadata (centrality) requires separate join entity

OMNIME TAG GENERATION / INCLUSION INVARIANT (LOCKED)
- Omnime Anime Profiles are generated selectively and reused. Reaching the serious recommendation shortlist before final ranking is the MVP trigger for ensuring a reusable profile exists.
- There is NO arbitrary maximum number of Omnime tags assigned to an anime.
- The LLM evaluates each candidate Omnime tag using an internal numeric recommendation-relevance judgment.
- A tag receives an anime_tag row only when its recommendation relevance clears the configured inclusion threshold.
- Java applies deterministic thresholds to map qualifying relevance judgments to CORE / STRONG / PRESENT centrality. Below-threshold candidates receive no anime_tag row.
- Exact numeric thresholds are an application-level calibration decision and are not encoded in the database schema.
- AniList tags remain persisted after an Omnime profile exists, but become supporting/source metadata rather than an equal primary recommendation signal. Recommendation logic must avoid double-weighting equivalent AniList and Omnime characteristics.

---

### user_anime

1. PURPOSE
- Canonical per-user relationship to anime; stores exactly one current state (SAVED, WATCHED, DROPPED, NOT_INTERESTED) and rating when present.

2. REQUIRED BY
- Add Anime, Saved/Watched pages, recommendation eligibility, onboarding count, Seen It/Dropped/Not Interested actions.

3. OWNER
- PostgreSQL application truth (user-owned persistent relationship)

4. COLUMNS
- id: UUID PRIMARY KEY
- user_id: UUID NOT NULL
- anime_id: BIGINT NOT NULL
- state: varchar(32) NOT NULL CHECK (state IN ('SAVED','WATCHED','DROPPED','NOT_INTERESTED'))
- rating: smallint NULL CHECK (rating BETWEEN 1 AND 10)
- rating_updated_at: timestamptz NULL
- not_interested_reason_id: UUID NULL
  - FK to feedback_evidence (see lifecycle below)
- created_at: timestamptz NOT NULL DEFAULT now()
- last_state_changed_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()
- version: integer NOT NULL DEFAULT 0
  - Semantic: optimistic lock counter (JPA @Version mapping)

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(user_id) -> users.id ON DELETE CASCADE
- FK(anime_id) -> anime.id ON DELETE RESTRICT
- FK(not_interested_reason_id) -> feedback_evidence.id ON DELETE SET NULL

7. UNIQUE CONSTRAINTS
- UNIQUE(user_id, anime_id)

8. CHECK CONSTRAINTS
- state allowed values; rating between 1..10 when present

9. IMPORTANT INDEXES
- unique index on (user_id, anime_id)
- index on (user_id, state, last_state_changed_at DESC) for Watched/Saved lists

10. ON DELETE BEHAVIOR
- Deleting users cascades user_anime rows
- Deleting anime is RESTRICT to prevent accidental user history removal
- Deleting the referenced feedback_evidence (not_interested reason) sets not_interested_reason_id to NULL

11. JPA REPRESENTATION
- UserAnime entity
- ManyToOne user (LAZY), ManyToOne anime (LAZY)
- @Version mapped to version column
- Avoid eager collections on user

12. RELATIONSHIP OWNERSHIP / FETCH STRATEGY
- UserAnime is the canonical join entity; services should fetch it for relationship mutations

13. WHY THIS TABLE MUST EXIST
- One canonical current-state per user+anime, determines recommendation eligibility and onboarding counts, stores rating for UI consumption

14. WHY NOT MERGE
- Cannot merge with feedback or anime; different ownership and semantics

USER_ANIME RATING LIFECYCLE (LOCKED)
- Rating lives on user_anime.rating and is meaningful only while the user_anime row exists (i.e., while a relationship row persists).
- If user deletes the relationship (WATCHED -> NONE deletes the user_anime row), rating is removed with the deletion (no active rating persists).
- Transitions that keep the row (e.g., WATCHED <-> DROPPED <-> SAVED <-> NOT_INTERESTED) preserve the rating by default; viewing evidence survives legitimate relationship changes unless the user explicitly clears it.

NOT_INTERESTED_REASON LIFECYCLE
- When user sets state = NOT_INTERESTED and supplies a reason:
  - Create feedback_evidence of type NOT_INTERESTED_REASON; set user_anime.not_interested_reason_id to that FE id atomically.
- When state changes away from NOT_INTERESTED:
  - Clear user_anime.not_interested_reason_id = NULL (do not delete FE row).
- When user later sets NOT_INTERESTED again:
  - If user supplies a reason, create a new FE and set pointer; older FE remains preserved but inactive.

---

### feedback_evidence

1. PURPOSE
- Persist raw user-authored evidence (positive/negative text) and corrections and not-interested reasons while preserving the original UI field context.

2. REQUIRED BY
- Add Anime optional feedback, Watched edits, Not Interested reasons, Correct My Taste

3. OWNER
- user-authored persistent evidence (Postgres)

4. COLUMNS
- id: UUID PRIMARY KEY
- user_id: UUID NOT NULL
- anime_id: BIGINT NULL
  - NULL for TASTE_CORRECTION entries not tied to a single anime
- evidence_type: varchar(32) NOT NULL CHECK (evidence_type IN ('VIEWING_EXPERIENCE','NOT_INTERESTED_REASON','TASTE_CORRECTION'))
- positive_text: text NULL
  - Semantic: exact raw text entered into the "positive" field when UX provides separate positive/negative inputs (VIEWING_EXPERIENCE)
- negative_text: text NULL
  - Semantic: exact raw text entered into the "negative" field when UX provides separate positive/negative inputs (VIEWING_EXPERIENCE)
- context_text: text NULL
  - Semantic: exact raw text entered into a neutral/single-purpose input (for NOT_INTERESTED_REASON and TASTE_CORRECTION) or any other contextual text the UI collected in a single-field input
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()
- edited_by_user: boolean NOT NULL DEFAULT false

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(user_id) -> users.id ON DELETE CASCADE
- FK(anime_id) -> anime.id ON DELETE SET NULL

7. UNIQUE CONSTRAINTS
- none

8. CHECK CONSTRAINTS / VALID SHAPES (LOCKED)
- PostgreSQL MUST enforce the evidence-type shape invariant with a DB CHECK constraint; service-layer validation mirrors the same rule for friendly API errors.
- The CHECK must permit exactly these shapes:
  - VIEWING_EXPERIENCE: context_text IS NULL AND at least one of positive_text or negative_text IS NOT NULL.
  - NOT_INTERESTED_REASON: context_text IS NOT NULL AND positive_text IS NULL AND negative_text IS NULL.
  - TASTE_CORRECTION: context_text IS NOT NULL AND positive_text IS NULL AND negative_text IS NULL.
- Canonical logical expression (the Flyway migration may name the constraint appropriately):
  CHECK (
    (evidence_type = 'VIEWING_EXPERIENCE' AND context_text IS NULL AND (positive_text IS NOT NULL OR negative_text IS NOT NULL))
    OR
    (evidence_type IN ('NOT_INTERESTED_REASON','TASTE_CORRECTION') AND context_text IS NOT NULL AND positive_text IS NULL AND negative_text IS NULL)
  )
- Raw text remains stored in the exact UI field supplied by the user; the DB constraint validates structural shape only and does not reinterpret sentiment or meaning.

9. IMPORTANT INDEXES
- index(user_id, anime_id)
- index(user_id, evidence_type)
- NOTE: context_text does NOT get an index in MVP

10. ON DELETE BEHAVIOR
- deleting user cascades evidence deletion (user-owned)
- deleting anime sets anime_id NULL to retain evidence for traceability

11. JPA REPRESENTATION
- FeedbackEvidence entity; ManyToOne user, ManyToOne anime (nullable)

12. WHY THIS TABLE MUST EXIST
- Raw verbatim preservation of user-authored evidence must preserve both raw text and the original UI field context

13. WHY NOT MERGE
- Must remain separate from interpretations and relationship rows

---

### feedback_interpretation

1. PURPOSE
- Persist the CURRENT structured AI interpretation of a feedback_evidence row.

2. REQUIRED BY
- My Taste traceability, building and explaining taste conclusions

3. OWNER
- LLM-derived persisted judgment (Postgres)

4. COLUMNS
- id: UUID PRIMARY KEY
- feedback_evidence_id: UUID NOT NULL UNIQUE
- interpretation_json: jsonb NOT NULL
- model_identifier: varchar(128) NULL
- model_version: varchar(64) NULL
- generated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(feedback_evidence_id) -> feedback_evidence.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- UNIQUE(feedback_evidence_id) (one current interpretation per evidence)

8. CHECK CONSTRAINTS
- none beyond JSON datatype

9. IMPORTANT INDEXES
- index(feedback_evidence_id)

10. ON DELETE BEHAVIOR
- Delete feedback evidence -> cascade delete interpretation

11. JPA REPRESENTATION
- FeedbackInterpretation entity; ManyToOne feedbackEvidence (LAZY)

12. WHY THIS TABLE MUST EXIST
- Enables deterministic mapping Evidence -> Structured Interpretation used in My Taste flows

13. WHY NOT MERGE
- Keep raw text and interpretation separate

---

### taste_conclusion

1. PURPOSE
- Persist CURRENT derived user-level taste conclusions (materialized insights)

2. REQUIRED BY
- My Taste UI and as input for recommendation reasoning

3. OWNER
- LLM-derived persisted judgment (Omnime-owned)

4. COLUMNS
- id: UUID PRIMARY KEY
- user_id: UUID NOT NULL
- conclusion_key: varchar(128) NOT NULL
- display_text: varchar(512) NOT NULL
- strength: smallint NOT NULL DEFAULT 0
- active: boolean NOT NULL DEFAULT true
  - Semantic: whether the conclusion belongs to the user's current materialized taste model
- model_identifier: varchar(128) NULL
- model_version: varchar(64) NULL
- created_at: timestamptz NOT NULL DEFAULT now()
- updated_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(user_id) -> users.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- UNIQUE(user_id, conclusion_key) (one current conclusion per key per user)

8. CHECK CONSTRAINTS
- strength >= 0

9. IMPORTANT INDEXES
- index(user_id)
- index(user_id, strength DESC)
- index(user_id, active, strength DESC)

10. ON DELETE BEHAVIOR
- user delete -> delete taste conclusions

11. JPA REPRESENTATION
- TasteConclusion entity; ManyToOne user

12. WHY THIS TABLE MUST EXIST
- Materialized conclusions for fast My Taste responses and stable ranking input. Conclusions referenced by user corrections retain stable identity and may be marked inactive instead of deleted.

13. WHY NOT MERGE
- Separate derived artifact from raw evidence and interpretation

---

### taste_conclusion_interpretation

1. PURPOSE
- Join table mapping which feedback_interpretation records support a taste_conclusion (many-to-many with optional weight)

2. REQUIRED BY
- My Taste traceability: Conclusion -> supporting interpretations/evidence

3. OWNER
- Omnime-owned mapping

4. COLUMNS
- conclusion_id: UUID NOT NULL
- interpretation_id: UUID NOT NULL
- contribution_weight: numeric(5,4) NULL

5. PRIMARY KEY
- (conclusion_id, interpretation_id)

6. FOREIGN KEYS
- FK(conclusion_id) -> taste_conclusion.id ON DELETE CASCADE
- FK(interpretation_id) -> feedback_interpretation.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- composite primary key ensures uniqueness

8. INDEXES
- index on conclusion_id
- index on interpretation_id

9. JPA
- explicit join entity

10. WHY EXISTS
- Traceability and ranking support

---

### taste_correction

1. PURPOSE
- Record explicit "Correct My Taste" operations; links a feedback_evidence row (the user's correction text) to the taste_conclusion targeted by the correction.

2. REQUIRED BY
- Correct My Taste UX: preserve authoritative user correction targeting a specific conclusion

3. OWNER
- user-authored persistent evidence + Omnime domain mapping (Postgres)

4. COLUMNS
- id: UUID PRIMARY KEY
- user_id: UUID NOT NULL
- taste_conclusion_id: UUID NOT NULL
- feedback_evidence_id: UUID NOT NULL
  - REQUIRED: this FE must be of type TASTE_CORRECTION
- created_at: timestamptz NOT NULL DEFAULT now()

5. PRIMARY KEY
- id

6. FOREIGN KEYS
- FK(user_id) -> users.id ON DELETE CASCADE
- FK(taste_conclusion_id) -> taste_conclusion.id ON DELETE RESTRICT
- FK(feedback_evidence_id) -> feedback_evidence.id ON DELETE CASCADE

7. UNIQUE CONSTRAINTS
- none (multiple corrections possible)

8. CHECK CONSTRAINTS
- None at DB level; application must ensure referenced FE is TASTE_CORRECTION

9. INDEXES
- index on user_id
- index on taste_conclusion_id

10. ON DELETE BEHAVIOR
- deleting user cascades correction rows; deleting feedback_evidence cascades its correction row; ordinary deletion of a referenced taste_conclusion is RESTRICTED

11. JPA
- TasteCorrection entity; ManyToOne user, ManyToOne tasteConclusion, ManyToOne feedbackEvidence

12. WHY THIS TABLE MUST EXIST
- Distinct semantic artifact: an explicit user action correcting a conclusion. By recording it separately, we preserve semantics and enable traceability.

13. IMPORTANT: SOURCE OF TRUTH
- The raw correction text MUST be stored in FeedbackEvidence (evidence_type = TASTE_CORRECTION). taste_correction references feedback_evidence_id. taste_correction must NOT carry correction_text itself.
- A TasteConclusion referenced by a correction must not be physically deleted during ordinary recomputation; mark it active=false when unsupported. If later supported again, the same stable (user_id, conclusion_key) may be reactivated.
- Full account deletion is transactional: UserService explicitly deletes the user's TasteCorrections before deleting User so the RESTRICT FK cannot obstruct aggregate-wide deletion.

---

## 3. Enum / status definitions

Use VARCHAR columns with DB CHECK constraints (migration-friendly). Java maps to enums.

- user_anime.state:
  - Values: 'SAVED','WATCHED','DROPPED','NOT_INTERESTED'
  - DB: VARCHAR(32) with CHECK
  - Java: enum UserAnimeState {SAVED, WATCHED, DROPPED, NOT_INTERESTED}

- anime_tag.centrality:
  - Values: 'CORE','STRONG','PRESENT'
  - DB: VARCHAR(16) with CHECK
  - Java: enum Centrality {CORE, STRONG, PRESENT}

- feedback_evidence.evidence_type:
  - Values: 'VIEWING_EXPERIENCE','NOT_INTERESTED_REASON','TASTE_CORRECTION'
  - DB: VARCHAR(32) with CHECK
  - Java: enum EvidenceType {VIEWING_EXPERIENCE, NOT_INTERESTED_REASON, TASTE_CORRECTION}

Rationale: VARCHAR+CHECK is easier to change in migrations than Postgres enum types.

---

## 4. User ↔ Anime transition summary (DB mutations)

Canonical rule: no user_anime row means no relationship.

Examples (high-level):

- None -> Saved:
  - INSERT user_anime {user_id, anime_id, state='SAVED', version=0}
- None -> Watched:
  - INSERT user_anime {state='WATCHED', rating if provided}
- None -> Dropped:
  - INSERT user_anime {state='DROPPED'}
- None -> Not Interested (no reason):
  - INSERT user_anime {state='NOT_INTERESTED', not_interested_reason_id = NULL}
- None -> Not Interested (with reason):
  - CREATE FE (NOT_INTERESTED_REASON), then INSERT user_anime with not_interested_reason_id referencing that FE
- Saved -> Watched:
  - UPDATE user_anime SET state='WATCHED', version++
- Saved -> Dropped:
  - UPDATE state='DROPPED'
- Saved -> Not Interested:
  - Possibly create FE, then UPDATE not_interested_reason_id and state
- Saved -> None (remove Saved):
  - DELETE user_anime row
  - rating removed with row
- Watched -> Dropped:
  - UPDATE state='DROPPED' (rating preserved)
- Watched -> None (correction remove):
  - DELETE user_anime; reactive Undo must use server-authenticated Undo token to re-create if requested
- Not Interested -> Saved:
  - UPDATE state='SAVED'; clear not_interested_reason_id pointer (FE preserved)
- Not Interested -> Not Interested (with new reason):
  - create new FE, update not_interested_reason_id to new FE

All transitions must be performed in service-layer transactions. The user_anime.version field must be used by the API for concurrency control on in-place updates — optimistic locking via @Version.

---

## 5. DELETE → UNDO service/API invariant (LOCKED)

This is the authoritative service-level rule for compensating deletions (WATCHED → NONE) in the MVP:

- When Spring deletes a user_anime row as part of a client request (e.g., user removes Watched):
  1. Spring reads and validates the authoritative current row (including version when provided) inside a transaction.
  2. Spring captures only the minimal prior persisted values needed for deterministic restoration (user_id, anime_id, prior_state, prior_rating, prior_not_interested_reason_id if relevant, and any artifact ids created by the operation).
  3. Spring performs the physical DELETE user_anime row in that transaction.
  4. On success, Spring returns a mutation response that includes: standard response data plus a short-lived, opaque, cryptographically authenticated server-generated Undo token.
     - The token is NOT persisted in PostgreSQL.
     - The token MUST be:
       - server-generated and cryptographically authenticated (signed),
       - short-lived (expiry matches client Undo toast lifetime),
       - bound to the authenticated user and the specific delete operation,
       - opaque to the client (React does not parse or alter it),
       - contain only the minimal required server-trusted fields to restore prior state.
  5. React holds the token only for the immediate Undo opportunity (toast).
  6. If Undo is requested, React returns the opaque token to Spring.
  7. On Undo request, Spring verifies:
     - token signature and expiry,
     - token user_id matches authenticated user,
     - there is currently NO user_anime row for that user+anime (absence check replaces version check when the row was deleted),
     - if a user_anime row exists, reject Undo with HTTP 409 Conflict (stale Undo).
  8. If absent and validations pass, Spring recreates user_anime using the server-authenticated prior values encoded in the token (new surrogate id and new version generated) in a single transaction, and returns the new row.
  9. If any validation fails, Undo is rejected. Spring must never trust client-supplied prior-state values; only server-authenticated token data is used to reconstruct.

Notes:
- The token format is an API/security implementation decision; do NOT lock the technology (JWT/HMAC/RSA). The token must be cryptographically authenticated and not persisted in domain DB.
- The server must ensure the token has a short configurable TTL aligned with the immediate Undo UI opportunity and is only usable by the authenticated owner. A nonce or persisted replay registry is not required for MVP: after a successful delete restoration, the recreated UNIQUE(user_id, anime_id) row causes replay of the same token to fail the absence precondition with 409.
- No LastAction table, no persisted undo state, no soft deletes.

---

## 6. Workflow stress tests (concise mapping)

For each locked MVP workflow, this section lists the principal tables read and written and whether the operation is atomic (transaction). The service layer must orchestrate these operations in a transaction where indicated.

(Selected critical workflows; the model supports all locked workflows.)

- Create account
  - Read: none
  - Write: users (INSERT)
  - Transaction: single

- Login
  - Read: users (SELECT by email)
  - Write: none

- Forgot / Reset password
  - Read: users
  - Write: password_reset_tokens (INSERT), and on reset, update users.password_hash and remove token

- Add Anime → Seen It
  - Read: anime (insert or upsert if not present after AniList fetch)
  - Write: user_anime (INSERT state='WATCHED'); transaction ensures anime exists before insert

- Add Anime → Not Interested with reason
  - Read: anime
  - Write: feedback_evidence (INSERT evidence_type=NOT_INTERESTED_REASON), user_anime (INSERT state='NOT_INTERESTED' with not_interested_reason_id linking FE id). Single transaction.

- Add rating (existing user_anime)
  - Read: user_anime
  - Write: UPDATE user_anime.rating and rating_updated_at. Single transaction.

- Edit Feedback
  - Read: feedback_evidence
  - Write: UPDATE feedback_evidence; then recompute feedback_interpretation (DELETE old FI, INSERT new FI or UPDATE). Single transaction preferred.

- Correct My Taste
  - Read: taste_conclusion
  - Write: feedback_evidence (TASTE_CORRECTION), taste_correction (links FE -> conclusion), recompute affected taste_conclusion(s) and update taste_conclusion_interpretation mappings
  - Transaction: the operations linking correction to conclusion and updating conclusions should be atomic.

- Generate recommendations (transient)
  - Read: users.include_adult_anime, anime (including anime.is_adult), genre/anime_genre, anilist_tag/anime_anilist_tag, anime_tag/omnime_tag when an Omnime profile exists, user_anime (for exclusions), feedback_interpretation, taste_conclusion
  - Deterministic eligibility: only anime with no current user_anime relationship are eligible; if users.include_adult_anime = false, anime.is_adult = true is also excluded. Current Viewing Intent cannot override either exclusion.
  - Write: none (transient response)

- Recommendation → Seen It
  - Read: user_anime (optional)
  - Write: upsert user_anime (INSERT or UPDATE to WATCHED), transactionally performed; response returns user_anime.id, version, and any created FE id(s)

- Recommendation → Not Interested with reason
  - Read: user_anime (maybe)
  - Write: feedback_evidence, update/insert user_anime state and not_interested_reason_id

- Delete account
  - Read: none
  - Write: DELETE users (cascades user-owned tables per FK ON DELETE CASCADE)
  - Transaction: single

- Undo after deletion (DELETE → Undo)
  - Read: none before deletion; on Undo request: server verifies token and absence of user_anime row
  - Write: re-insert user_anime with server-authenticated prior values (single transaction)

---

## 7. Data ownership map (concise)

- PostgreSQL persistent application truth:
  - users (including the user-controlled include_adult_anime preference), password_reset_tokens, user_anime, feedback_evidence, feedback_interpretation, taste_conclusion, taste_correction, taste_conclusion_interpretation, anime_profile, anime_tag, omnime_tag, anime_genre

- AniList-owned external fact persisted locally:
  - anime.anilist_id, anime.title_*, anime.description (AniList synopsis), anime.episode_count, anime.status, anime.average_score, anime.is_adult, genre + anime_genre, anilist_tag + anime_anilist_tag (including relevance rank and spoiler metadata)

- Omnime-owned domain data:
  - omnime_tag, anime_profile + anime_tag (Omnime Anime Profile), taste_conclusion (materialized derived data belongs to Omnime)

- LLM-derived persisted judgment:
  - feedback_interpretation (structured interpretation)
  - taste_conclusion (materialized conclusion with minimal model provenance)

- user-authored persistent evidence:
  - feedback_evidence (VIEWING_EXPERIENCE, NOT_INTERESTED_REASON, TASTE_CORRECTION)
  - ratings on user_anime.rating

- temporary/session/frontend state:
  - Current Viewing Intent (transient, not persisted)
  - Active recommendation set (transient, client-held)
  - Undo token (server-generated, opaque, short-lived; not persisted)

---

## 8. JPA / Spring implications (concise guidance)

- Entity boundaries:
  - Entities: User, PasswordResetToken, Anime, Genre, AnimeGenre (or explicit mapping), AniListTag, AnimeAniListTag, OmnimeTag, AnimeProfile, AnimeTag, UserAnime (join entity), FeedbackEvidence, FeedbackInterpretation, TasteConclusion, TasteConclusionInterpretation (join), TasteCorrection.
- Aggregate boundaries:
  - User is aggregate root for user-owned data; User deletion cascades.
  - Anime is aggregate root for shared anime info.
- Owning sides:
  - UserAnime owns relationship data; ManyToOne user and anime.
  - AnimeProfile owns reusable profile completion/provenance for anime.
  - AnimeTag owns mapping to omnime_tag and anime.
- LAZY/EAGER:
  - Default LAZY for collections to avoid N+1 (User -> user_anime should be LAZY and queries should use explicit joins or DTO projections).
- @Version:
  - Map @Version (integer) on user_anime.version only. Use optimistic locking for in-place updates and Undo preconditions.
- Cascade:
  - Use DB ON DELETE CASCADE for user-owned tables; avoid JPA cascade REMOVE from entities to prevent accidental deletions by object graph manipulation.
- N+1 risks:
  - When rendering Watched/Saved lists, use repository queries that JOIN anime and limited fields into DTOs to avoid fetching per-row anime data in separate queries.
- DTO boundaries:
  - Controllers should consume DTOs from services, not JPA entities.
- Transaction boundaries:
  - All relationship mutations and any atomic compensating operations (Delete → Undo restoration) must run inside service-layer @Transactional methods.

---

## 8A. Taste freshness and active-evidence invariants (LOCKED)

- `users.taste_evidence_version` advances when usable taste input changes: a new/replaced FeedbackInterpretation, a rating change, activation/deactivation of a Not Interested reason, an interpreted taste correction, or a relationship transition that changes evidence eligibility. It is not advanced merely because raw feedback exists before it has a usable interpretation.
- `users.taste_computed_version` records the evidence version successfully incorporated into the current materialized TasteConclusions. Equal versions mean current; `taste_evidence_version > taste_computed_version` means stale/recompute pending.
- Taste recomputation captures a target evidence version at its start. On success it advances `taste_computed_version` only to that captured target, never blindly to the latest evidence version. New evidence arriving during recomputation therefore leaves the user detectably stale for a later pass.
- Missing FeedbackInterpretation remains separately detectable from raw FeedbackEvidence. No persistent job/queue table is required for MVP recovery.
- Raw FeedbackEvidence preservation is distinct from active taste eligibility. VIEWING_EXPERIENCE evidence is active for WATCHED and DROPPED; it may remain active for SAVED when legitimate prior viewing evidence exists; it is inactive for NOT_INTERESTED and NONE/no UserAnime relationship.
- NOT_INTERESTED_REASON evidence is active only while `user_anime.not_interested_reason_id` references it. Leaving NOT_INTERESTED clears the pointer, preserves the raw evidence, and makes it inactive.
- Relationship transitions that actually change active evidence eligibility advance `taste_evidence_version`; transitions that leave the usable taste-input set unchanged do not increment it mechanically.

---

## 9. Flyway / Hibernate policy & migration ordering notes

- Flyway is authoritative for schema changes (initial migrations must be applied before running application).
- Hibernate ddl-auto = validate in development and production. Developers must run Flyway migrations locally before starting the app.
- Migration ordering:
  1. Create users and password_reset_tokens
  2. Create anime and genre
  3. Create anilist_tag and omnime_tag
  4. Create anime_genre and anime_anilist_tag
  5. Create anime_profile and anime_tag
  6. Create feedback_evidence
  7. Create feedback_interpretation
  8. Create user_anime (feedback_evidence already exists before not_interested_reason_id FK)
  9. Create taste_conclusion, taste_conclusion_interpretation, and taste_correction

Note: Because user_anime.not_interested_reason_id references feedback_evidence, ensure feedback_evidence table migration precedes adding that FK or create it as a later migration.

---

## 10. Final checklist and frozen status

Confirmations (all locked):
- rating remains on user_anime under the accepted lifecycle rules — CONFIRMED
- user_anime remains single-current-state — CONFIRMED
- primary_feedback_id removed — CONFIRMED
- not_interested_reason_id remains with lifecycle semantics — CONFIRMED
- taste_correction remains first-class and references feedback_evidence only — CONFIRMED
- no RecommendationSession table — CONFIRMED
- no LastAction table — CONFIRMED
- no persisted Current Viewing Intent — CONFIRMED
- no AI-generated anime synopsis persisted — CONFIRMED
- no beginner/cold-start persisted state — CONFIRMED
- physical account deletion cascades user-owned data — CONFIRMED
- Flyway owns schema mutation — CONFIRMED
- Hibernate ddl-auto=validate in normal dev and prod — CONFIRMED
- AniList tags are persisted separately from Omnime tags — CONFIRMED
- anime_anilist_tag preserves per-anime AniList rank and spoiler metadata — CONFIRMED
- omnime_tag/anime_tag remain exclusively Omnime-owned custom recommendation intelligence — CONFIRMED
- Omnime tag inclusion is relevance-threshold based with no arbitrary tag-count cap — CONFIRMED
- AniList tags remain supporting/source metadata after Omnime profiling and must not be double-weighted — CONFIRMED
- @Version exists only where concretely justified (user_anime) — CONFIRMED
- five Watched remains derived from counting user_anime rows WHERE state='WATCHED' — CONFIRMED
- users.include_adult_anime defaults false and is the explicit account-level opt-in for adult-classified recommendation candidates — CONFIRMED
- anime.is_adult persists AniList media-level adult classification for deterministic eligibility — CONFIRMED
- recommendation eligibility requires no current user_anime relationship; SAVED, WATCHED, DROPPED, and NOT_INTERESTED are excluded — CONFIRMED
- Current Viewing Intent cannot override relationship exclusions or the adult-content preference — CONFIRMED
- anime_profile is the durable successful-profile marker; zero anime_tag rows may still represent a completed profile — CONFIRMED
- profile-level model provenance lives on anime_profile rather than anime_tag — CONFIRMED
- users.taste_evidence_version > users.taste_computed_version means materialized taste is stale — CONFIRMED
- taste recomputation captures a target evidence version and advances taste_computed_version only to that successfully computed target — CONFIRMED
- taste_conclusion.active preserves corrected conclusion identity without forcing obsolete conclusions into the current model — CONFIRMED
- taste_correction -> taste_conclusion uses ON DELETE RESTRICT; account deletion explicitly removes corrections first in one transaction — CONFIRMED
- routine AniList synchronization does not automatically invalidate Omnime profiles — CONFIRMED
- feedback_evidence valid shapes are enforced by PostgreSQL CHECK constraint and mirrored by service validation — CONFIRMED
- anime_profile marker + all anime_tag mappings from one successful generation persist atomically in one short transaction — CONFIRMED
- derived AI failures remain durably detectable from existing state; mandatory queue/advisory-lock infrastructure is not part of MVP — CONFIRMED
- Undo tokens use a short configurable TTL plus authenticated user/operation binding and state preconditions; no persisted nonce/replay store is required — CONFIRMED

MVP DATA MODEL STATUS: FROZEN — APPROVED FOR MVP IMPLEMENTATION