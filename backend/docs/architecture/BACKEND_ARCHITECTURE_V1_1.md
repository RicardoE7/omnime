# Omnime MVP Backend Architecture

**Document Revision:** V1.1 — Deployment Provider Amendment (Vercel + Render + Neon)

## Status

**BACKEND ARCHITECTURE STATUS: FROZEN --- APPROVED FOR MVP IMPLEMENTATION**

This document defines the FROZEN backend architecture for the Omnime MVP. It has completed architecture stress testing and final adversarial review against the frozen `MVP_DATA_MODEL.md` and locked Omnime product/UX decisions. Production implementation should conform to these boundaries unless a genuine contradiction is discovered.

The frozen data model, this architecture, and the locked UX are implementation constraints. Future changes require a genuine contradiction, security/correctness issue, or intentional product evolution; they should not be made merely to introduce additional abstraction or infrastructure.

------------------------------------------------------------------------

## 1. Architectural Goals

The backend must support Omnime's core product goal:

> Recommend anime the user has not already classified that they are
> genuinely likely to enjoy.

The architecture follows these principles:

-   **Code handles facts. AI handles judgment.**
-   PostgreSQL owns persistent application truth.
-   AniList supplies external anime facts, genres, and ranked tags.
-   Omnime owns its curated recommendation intelligence and application
    rules.
-   LLMs provide judgment and interpretation but do not own
    deterministic business rules.
-   Raw user-authored evidence is preserved independently from AI
    interpretation.
-   Persistent business operations are centralized in Spring services
    rather than duplicated by frontend screens.
-   Controllers are thin HTTP boundaries.
-   JPA entities do not cross the API boundary.
-   Remote calls should not be performed inside long-lived database
    transactions.
-   Transient recommendation state remains transient unless persistence
    becomes concretely necessary.
-   Complexity must be earned by an actual MVP requirement.

The MVP intentionally does **not** introduce microservices, Kafka,
RabbitMQ, Redis, a generalized job system, event sourcing,
recommendation-session persistence, relationship history, soft deletion,
or generic framework abstractions.

------------------------------------------------------------------------

## 2. Technology and Deployment Context

### Frontend

-   React + Vite
-   Responsive/mobile-first web application
-   Tailwind CSS v3
-   Deployed independently on Vercel
-   Repository directory: `frontend/`

### Backend

-   Java
-   Spring Boot
-   Spring Web
-   Spring Data JPA / Hibernate
-   Spring Security
-   Flyway
-   PostgreSQL
-   Deployed independently on Render using Docker
-   Repository directory: `backend/`

### External services

-   AniList GraphQL API
-   LLM provider API
-   Email provider for password-reset delivery

### Repository topology

``` text
omnime/
├── frontend/
├── backend/
│   └── docs/
│       └── architecture/
│           ├── MVP_DATA_MODEL.md
│           └── BACKEND_ARCHITECTURE.md
├── .gitignore
└── README.md
```

One GitHub repository does not imply one deployment. Vercel targets
`frontend/`; Render targets `backend/` and builds from `backend/Dockerfile`; PostgreSQL is hosted independently on Neon.

------------------------------------------------------------------------

## 3. Top-Level Backend Package Structure

The backend uses feature/domain-oriented packages rather than global
folders containing every controller, service, repository, and entity.

``` text
backend/src/main/java/com/omnime/
├── auth/
├── user/
├── anime/
├── useranime/
├── feedback/
├── taste/
├── recommendation/
├── integration/
│   ├── anilist/
│   └── llm/
└── common/
    ├── config/
    ├── exception/
    └── undo/
```

Each feature contains only the layers it actually needs.

This architecture intentionally avoids global packages such as:

``` text
controller/
service/
repository/
entity/
dto/
```

because ownership should remain visible by feature.

------------------------------------------------------------------------

# 4. Request and Dependency Flow

The normal API flow is:

``` text
React
  ↓
Controller
  ↓
Request DTO
  ↓
Service
  ↓
Repository and/or collaborating domain services
  ↓
Response DTO
  ↓
Controller
  ↓
React
```

Rules:

-   Controllers handle HTTP concerns, authenticated-user context,
    request validation, and response mapping.
-   Controllers do not orchestrate repositories.
-   Controllers do not call AniList or the LLM directly.
-   Controllers do not contain business transition logic.
-   Services own business operations and transaction boundaries.
-   Repositories own persistence access for their feature.
-   Cross-feature business behavior should normally go through the
    owning service rather than directly reaching into another feature's
    repository.
-   Deliberate optimized read projections may be introduced where
    recommendation candidate acquisition or list rendering requires
    them.
-   JPA entities are persistence models, not API contracts.

------------------------------------------------------------------------

# 5. Authentication Architecture

## Package

``` text
auth/
├── controller/
│   └── AuthController
├── service/
│   └── AuthService
├── repository/
│   └── PasswordResetTokenRepository
├── entity/
│   └── PasswordResetToken
├── dto/
└── security/
    ├── SecurityConfig
    └── authentication infrastructure
```

## Ownership

`AuthService` owns authentication workflows:

-   registration
-   login
-   logout response behavior
-   forgot-password workflow
-   reset-password workflow
-   credential verification
-   establishing authenticated sessions/tokens

`AuthService` may use `UserRepository` when authentication requires
account lookup or registration, but the `User` domain remains
conceptually owned by the `user` feature.

Avoid creating overlapping `AuthService`, `AccountService`,
`SecurityService`, and `UserService` responsibilities without a concrete
need.

## Authentication mechanism (LOCKED)

The locked MVP mechanism is:

> **Stateless signed JWT authentication transported in an HttpOnly +
> Secure browser cookie.**

React should not read or store the JWT in localStorage.

The exact JWT library, token lifetime, signing mechanism, cookie domain,
and SameSite configuration remain implementation/deployment decisions
that must be finalized with the security review.

Because browser cookies are automatically attached to requests, the
final Spring Security configuration must explicitly address CSRF
protection rather than assuming CORS alone is sufficient.

### CSRF contract (LOCKED)

Spring Security CSRF protection remains enabled for authenticated cookie-based unsafe methods. The backend provides a CSRF token to React through an explicit frontend-accessible backend response mechanism (for example, a response body/header or dedicated CSRF endpoint) while retaining Spring's server-side CSRF token validation. The authentication JWT remains HttpOnly. React echoes the received CSRF token in the configured CSRF request header for POST/PUT/PATCH/DELETE requests. Requests missing or failing CSRF validation are rejected. React must not be required to directly read a cookie scoped to the Render backend domain.

CORS is configured separately with the explicit frontend origin and credentials enabled; wildcard origins are not permitted with credentialed requests. The authentication cookie remains Secure in production and uses a SameSite/domain configuration appropriate to the final Vercel/Render or custom-domain topology. Exact cookie/header names may remain implementation configuration. Integration tests must cover an authenticated unsafe request succeeding with a valid CSRF token and failing without/with an invalid token.

## Identity rule

For every user-owned operation:

> The acting user is derived from authenticated server-side identity.

The backend must never trust a client-supplied `userId` as
authorization.

## Registration

Conceptual flow:

``` text
Create Account request
→ validate email/password
→ hash password using Spring Security password encoder
→ create User
→ authenticate / issue auth cookie
```

Routing after authentication uses `users.onboarding_completed`. It does
not use Watched count as an authentication or onboarding-completion
flag.

## Password reset

``` text
Forgot Password
→ generate cryptographically secure random raw token
→ hash token
→ persist only token_hash + expiry
→ email raw token

Reset Password
→ hash supplied raw token
→ locate/verify stored token hash
→ verify expiry
→ replace password hash
→ invalidate/delete reset token
```

The raw reset token must not be stored in PostgreSQL.

## Logout

For the locked stateless-cookie design, logout clears the
authentication cookie.

The MVP intentionally does not add refresh-token infrastructure, a JWT blacklist, or a persistent session registry. Logout clears the cookie. An independently compromised JWT can remain valid until its configured expiration; the MVP accepts this stateless tradeoff and mitigates it with a reasonably short lifetime, HTTPS, Secure/HttpOnly cookie transport, CSRF protection, and signing-secret hygiene. Exact lifetime remains configuration.

------------------------------------------------------------------------

# 6. User Architecture

## Package

``` text
user/
├── entity/
│   └── User
├── repository/
│   └── UserRepository
├── service/
│   └── UserService
└── dto/
```

## Ownership

`UserService` owns Omnime account/business operations such as:

-   retrieving the current user's account information
-   updating display name
-   updating account-level preferences
-   completing onboarding
-   deleting the account

The user's adult-content preference belongs here.

`users.include_adult_anime` defaults to `false`.

## Onboarding semantics

`onboarding_completed` means:

> The onboarding experience has been completed or skipped.

It does **not** mean the user has added five Watched anime.

The five-Watched milestone is derived from authoritative `user_anime`
rows where:

``` text
state = WATCHED
```

No `beginner_mode`, `cold_start_completed`, or similar persistent state
is required.

## Account deletion

Account deletion is owned by `UserService`.

Deleting a user physically removes the `users` row. Account deletion is one transaction. Because TasteCorrection protects its referenced TasteConclusion with ON DELETE RESTRICT, UserService explicitly deletes the user's TasteCorrections before deleting User; database cascades then remove the remaining user-owned persistent data. The operation must be integration-tested with existing corrections; any concurrent conflicting write must resolve transactionally rather than bypass the RESTRICT invariant. Shared anime catalog and Omnime Anime Profile data remain.

------------------------------------------------------------------------

# 7. Anime Architecture

## Package

``` text
anime/
├── controller/
│   └── AnimeController
├── service/
│   ├── AnimeService
│   ├── AnimeCatalogService
│   └── AnimeProfileService
├── repository/
│   ├── AnimeRepository
│   ├── GenreRepository
│   ├── AnimeGenreRepository
│   ├── AniListTagRepository
│   ├── AnimeAniListTagRepository
│   ├── AnimeProfileRepository
│   ├── OmnimeTagRepository
│   └── AnimeTagRepository
├── entity/
│   ├── Anime
│   ├── Genre
│   ├── AnimeGenre
│   ├── AniListTag
│   ├── AnimeAniListTag
│   ├── AnimeProfile
│   ├── OmnimeTag
│   └── AnimeTag
└── dto/
```

The three anime services have intentionally different responsibilities.

## AnimeService --- use the catalog

`AnimeService` owns ordinary local catalog reads.

Examples:

-   search/autocomplete
-   anime details
-   factual anime lookup
-   local metadata retrieval required by other services

`AnimeService` should normally read PostgreSQL.

It does **not**:

-   synchronize AniList
-   perform routine AniList network calls
-   generate Omnime Anime Profiles
-   rank recommendations

## AnimeCatalogService --- maintain the catalog

`AnimeCatalogService` owns synchronization of AniList factual metadata
into PostgreSQL.

Responsibilities:

-   initial catalog bootstrap
-   ongoing catalog refresh/synchronization
-   transforming AniList DTOs into local persistent data
-   maintaining Anime
-   maintaining Genre / AnimeGenre
-   maintaining AniListTag / AnimeAniListTag
-   preserving AniList rank and spoiler metadata
-   persisting AniList's media-level adult classification as
    `anime.is_adult`

It uses `AniListClient`.

AniList transport DTOs live under:

``` text
integration/anilist/dto/
```

They are not JPA entities.

The initial large catalog bootstrap must be an explicit operational
process. It must not block ordinary Spring Boot startup.

An AniList outage must not make an already-populated Omnime catalog
unusable.

## AnimeProfileService --- understand the anime

`AnimeProfileService` owns reusable Omnime Anime Profile generation and
retrieval.

An Omnime Anime Profile is universal to the anime, not personalized to a
user.

Inputs may include:

-   AniList description
-   genres
-   AniList tags
-   AniList relevance ranks
-   non-spoiler source metadata
-   the allowed curated `OmnimeTag` ontology

Conceptual generation flow:

``` text
check AnimeProfileRepository for successful reusable profile
→ if reusable, return it
→ otherwise load factual source metadata
→ load allowed OmnimeTag vocabulary
→ call LLM
→ validate structured result
→ Java applies deterministic inclusion/centrality thresholds
→ in a short transaction re-check profile completion, persist AnimeProfile + qualifying AnimeTag mappings atomically
→ return profile
```

The LLM may judge recommendation relevance but may not invent arbitrary
ontology entries.

There is no arbitrary maximum number of Omnime tags per anime. A tag
receives an `anime_tag` row only when it clears the configured relevance
threshold. Java deterministically maps qualifying scores to:

``` text
CORE
STRONG
PRESENT
```

below threshold:

``` text
no anime_tag row
```

Remote LLM work must not occur while holding a long database transaction open. After generation/validation succeeds, the AnimeProfile completion marker and all qualifying AnimeTag mappings from that generation MUST commit atomically in one short transaction. A persistence failure rolls back the marker and mappings together; a deliberate successful zero-tag result commits the marker with zero mappings.

Profile generation should be independently recoverable per anime. One
failed profile must not roll back successful profiles for other anime.

## Profile trigger

`RecommendationService`, not `AnimeProfileService`, decides when an
anime has reached a sufficiently serious recommendation shortlist to
justify profiling.

The intended recommendation flow profiles serious finalists **before**
the final user-visible ranking so that Omnime's richer intelligence can
influence the final selection.

## AniList vs Omnime metadata

Three layers remain distinct:

``` text
AniList genres
→ broad external classification

AniList tags + rank
→ detailed external themes/elements

Omnime tags + centrality
→ curated recommendation intelligence
```

AniList tags remain stored after an Omnime profile exists but should
become supporting/source metadata rather than an equal duplicate ranking
signal.

------------------------------------------------------------------------

# 8. User-Anime Relationship Architecture

## Package

``` text
useranime/
├── controller/
│   └── UserAnimeController
├── service/
│   └── UserAnimeService
├── repository/
│   └── UserAnimeRepository
├── entity/
│   └── UserAnime
└── dto/
```

## Ownership

`UserAnimeService` is the single authority for current user↔anime
relationship mutations.

The frontend screen does not own the business operation.

Home, Add Anime, Saved, Watched, Anime Details, and recommendation
feedback all use the same backend relationship behavior.

Conceptual operations include:

``` text
saveAnime
markWatched
markDropped
markNotInterested
removeRelationship
updateRating
undo
```

Exact method names and endpoint contracts are not locked by this
document.

## Current-state model

Absence of a `user_anime` row means:

``` text
NONE / UNCLASSIFIED
```

A row contains exactly one current state:

``` text
SAVED
WATCHED
DROPPED
NOT_INTERESTED
```

`WATCHING` is not an MVP state.

## Recommendation eligibility

Only anime with **no current UserAnime relationship** are recommendation
candidates.

Therefore:

``` text
NONE / UNCLASSIFIED → eligible

SAVED              → excluded
WATCHED            → excluded
DROPPED            → excluded
NOT_INTERESTED     → excluded
```

Saved anime are excluded because the user has already
discovered/bookmarked them; Home recommendations are a discovery
experience.

Current Viewing Intent cannot override these relationship exclusions.

## Adult-content eligibility

AniList's media-level classification is persisted as:

``` text
anime.is_adult
```

The user's explicit preference is:

``` text
users.include_adult_anime
```

default:

``` text
false
```

Eligibility rule:

``` text
anime.is_adult = true
AND
user.include_adult_anime = false
→ exclude
```

Enabling adult anime removes this deterministic exclusion. It does not
make adult titles preferred or guarantee that they will be recommended.

Current Viewing Intent cannot override the account-level adult-content
exclusion.

## Transaction ownership

`UserAnimeService` owns the transaction for a relationship operation.

When the operation also creates raw feedback evidence,
`UserAnimeService` orchestrates the relationship mutation and delegates
evidence rules to `FeedbackService` inside the coherent database
transaction.

Example:

``` text
Not Interested + reason

UserAnimeService
→ create/transition UserAnime
→ FeedbackService creates NOT_INTERESTED_REASON evidence
→ set not_interested_reason_id
→ COMMIT
```

This prevents half-persisted relationship/reason state.

Derived AI work occurs after authoritative user data commits.

## Ratings

Ratings live on `user_anime.rating`.

Rules:

-   integer 1--10
-   nullable
-   deleting the UserAnime row removes the rating
-   in-row state transitions preserve rating by default
-   Watched → Dropped preserves rating
-   Dropped → Watched preserves rating
-   Dropped → Saved may preserve legitimate viewing rating under the
    accepted MVP simplification
-   explicit rating removal sets `rating = NULL`

Ratings express **how much** the user liked something. Feedback helps
explain **why**.

## Not Interested reason

Not Interested without a reason creates no taste evidence.

Not Interested with a reason:

``` text
FeedbackEvidence
type = NOT_INTERESTED_REASON
context_text = exact raw user text
```

and `UserAnime.not_interested_reason_id` points to the currently active
reason.

When the relationship changes away from NOT_INTERESTED:

``` text
not_interested_reason_id = NULL
```

The raw evidence remains preserved but stops contributing as active NI
evidence.

------------------------------------------------------------------------

# 9. Feedback Architecture

## Package

``` text
feedback/
├── controller/
│   └── FeedbackController
├── service/
│   ├── FeedbackService
│   └── FeedbackInterpretationService
├── repository/
│   ├── FeedbackEvidenceRepository
│   └── FeedbackInterpretationRepository
├── entity/
│   ├── FeedbackEvidence
│   └── FeedbackInterpretation
└── dto/
```

## FeedbackService --- what the user said

`FeedbackService` owns raw user-authored evidence:

-   creation
-   editing
-   structural validation
-   exact preservation

Evidence types:

``` text
VIEWING_EXPERIENCE
NOT_INTERESTED_REASON
TASTE_CORRECTION
```

Valid conceptual shapes:

``` text
VIEWING_EXPERIENCE
→ positive_text and/or negative_text
→ context_text null

NOT_INTERESTED_REASON
→ context_text required
→ positive_text/negative_text null

TASTE_CORRECTION
→ context_text required
→ positive_text/negative_text null
```

The positive and negative UI fields are contextual metadata, not
semantic truth.

Omnime preserves exactly what the user typed in the original field.

A single entry may ultimately be interpreted as:

``` text
positive
negative
contextual
mixed
```

regardless of which UI field contained it.

## FeedbackInterpretationService --- what Omnime thinks it means

`FeedbackInterpretationService` owns the current structured
interpretation of an individual `FeedbackEvidence` row.

Conceptual flow:

``` text
raw evidence already committed
→ call LLM
→ validate structured interpretation
→ persist one current FeedbackInterpretation
→ trigger/hand off affected taste recomputation
```

Raw evidence is authoritative and commits before interpretation.

An LLM failure must not roll back the user's feedback.

There is no AI interpretation history in MVP. One current interpretation
exists per evidence row.

## Editing feedback

Conceptual flow:

``` text
FeedbackController
→ FeedbackService updates raw evidence
→ invalidate/remove old interpretation
→ COMMIT
→ reinterpret
→ recompute affected taste
```

Editing feedback is distinct from Correct My Taste.

------------------------------------------------------------------------

# 10. Taste Architecture

## Package

``` text
taste/
├── controller/
│   └── TasteController
├── service/
│   ├── TasteService
│   └── TasteCorrectionService
├── repository/
│   ├── TasteConclusionRepository
│   ├── TasteConclusionInterpretationRepository
│   └── TasteCorrectionRepository
├── entity/
│   ├── TasteConclusion
│   ├── TasteConclusionInterpretation
│   └── TasteCorrection
└── dto/
```

## TasteService

`TasteService` owns the user's current aggregate taste model.

Responsibilities include:

-   deciding which evidence is currently eligible
-   reasoning across multiple pieces of interpreted evidence
-   generating/recomputing current TasteConclusions
-   maintaining conclusion strength
-   maintaining conclusion → interpretation support mappings
-   serving My Taste data
-   providing current taste input to RecommendationService

Feedback interpretation reasons about **one piece of evidence**.

Taste reasoning considers the **body of eligible evidence**.

## Evidence eligibility

Potential active inputs include:

-   legitimate/current viewing feedback
-   ratings under the locked UserAnime rating lifecycle
-   currently referenced Not Interested reason
-   explicit taste corrections

A Not Interested action with no reason contributes zero taste evidence.

A historical NI reason whose `not_interested_reason_id` pointer has been
cleared remains preserved but is not active NI evidence.

Viewing-state-only information is weak contextual evidence. State alone
should not produce an unsupported specific preference conclusion.

Ratings can support broad positive/negative association but should not
justify overly specific taste conclusions without additional evidence.

## Progressive taste

Taste may exist before five Watched anime.

There is no:

``` text
if watchedCount < 5:
    disable taste
```

Five Watched is a product threshold for the normal established
personalized experience, not a database prerequisite.

## Current-state model

Taste is the current understanding of the user, not append-only AI
history.

Recomputation should update the current conclusion associated with a
stable:

``` text
(user_id, conclusion_key)
```

rather than casually delete/recreate a continuing conclusion.

This is especially important because explicit `TasteCorrection` rows
target a `TasteConclusion`.

## TasteCorrectionService

`TasteCorrectionService` owns the **Correct My Taste** operation.

Conceptual flow:

``` text
validate target TasteConclusion
→ FeedbackService creates TASTE_CORRECTION raw evidence
→ TasteCorrection links user + conclusion + evidence
→ COMMIT authoritative correction
→ interpret correction
→ TasteService recomputes affected taste
```

The distinction is:

``` text
Edit Feedback
= the user's original evidence was wrong/incomplete

Correct My Taste
= Omnime's derived conclusion was wrong/incomplete
```

------------------------------------------------------------------------

# 11. Recommendation Architecture

## Package

``` text
recommendation/
├── controller/
│   └── RecommendationController
├── service/
│   └── RecommendationService
└── dto/
```

There is deliberately:

``` text
no Recommendation entity
no RecommendationRepository
no recommendation table
```

Recommendations and personalized recommendation explanations are
transient.

## RecommendationService role

`RecommendationService` is an orchestrator.

It may collaborate with:

-   AnimeService
-   AnimeProfileService
-   TasteService
-   an appropriate UserAnime query boundary
-   LlmClient

It does **not**:

-   create AnimeTag rows directly
-   interpret feedback
-   mutate UserAnime relationships
-   recompute taste internally
-   synchronize AniList
-   own authentication

## Recommendation pipeline

``` text
authenticated user
        ↓
current UserAnime history
        +
current Taste
        +
optional transient Current Viewing Intent
        ↓
deterministic eligibility
        ↓
local PostgreSQL candidate acquisition
        ↓
initial AI ranking using AniList metadata
        +
available taste
        +
current intent
        ↓
serious shortlist
        ↓
AnimeProfileService
generate/reuse missing Omnime profiles
        ↓
final AI ranking using richer Omnime intelligence
        ↓
quality threshold
        ↓
best ≤ 5
        ↓
personalized transient explanations
        ↓
React
```

## Deterministic eligibility

Code, not the LLM, enforces reliable factual exclusions and constraints.

At minimum:

-   any existing UserAnime relationship excludes the anime
-   adult anime are excluded unless the user explicitly enables them
-   reliable hard factual constraints parsed from Current Viewing Intent
    are enforced deterministically where possible

The LLM cannot override these exclusions.

## Candidate acquisition

The full local catalog must not be sent to the LLM.

PostgreSQL first narrows the catalog to a reasonable candidate pool.

The exact pool size is a calibration/performance decision, not frozen
here.

## Current Viewing Intent

Current Viewing Intent is temporary "what I want right now" context.

It is not durable taste.

It remains transient and can be held by the frontend/request flow during
the active recommendation experience.

Natural-language intent may need structured interpretation before
deterministic constraints can be applied. Provider-specific LLM
communication goes through `LlmClient`; RecommendationService owns how
that structured result affects the recommendation workflow.

Ambiguous intent must not be converted into invented hard constraints.

## Initial vs final ranking

The initial ranking uses cheaper/wider available information, including
AniList metadata, to identify serious finalists.

The serious shortlist then receives reusable Omnime profiles where
missing.

The final ranking uses the richer Omnime representation.

This means profiling occurs **before** the final user-visible five are
selected.

## Recommendation count

Five is a maximum, not a quota.

If fewer than five candidates clear the product's quality threshold,
return fewer.

The exact quality threshold is a calibration decision.

## 0--4 Watched users

The same recommendation pipeline must support 0--4 Watched anime.

Use whatever legitimate evidence exists.

At zero Watched, Current Viewing Intent may be the primary user-specific
signal. General catalog quality and metadata can still participate.

No separate beginner recommendation engine is required.

## Find Me 5 More

The active recommendation experience may retain:

-   Current Viewing Intent
-   IDs already shown in the active experience

These are transient exclusions, not persistent relationship facts.

Permanent eligibility remains server-authoritative.

No recommendation-history or RecommendationSession table is required for
MVP.

## Failure behavior

Individual Anime Profile generation failure:

-   should not corrupt persistent state
-   may reduce confidence in or exclude the affected candidate
-   should not necessarily fail the entire recommendation request

Core recommendation-ranking LLM failure:

-   fail cleanly / allow retry
-   do not silently pretend a deterministic fallback is equivalent to
    Omnime's AI recommendation judgment

------------------------------------------------------------------------

# 12. Integration Architecture

## AniList

``` text
integration/
└── anilist/
    ├── AniListClient
    ├── dto/
    └── config/
```

`AniListClient` encapsulates:

-   GraphQL transport
-   request/response mechanics
-   provider-specific errors
-   provider DTOs
-   AniList configuration

Business services should not know GraphQL transport details.

AniList data is transformed into Omnime's local persistence model by
`AnimeCatalogService`.

## LLM

``` text
integration/
└── llm/
    ├── LlmClient
    ├── dto/
    └── config/
```

`LlmClient` encapsulates provider-specific:

-   HTTP/API mechanics
-   model invocation
-   provider request/response formats
-   provider configuration
-   transport failures

Business services remain responsible for:

-   defining the business task
-   deciding what data is eligible input
-   validating domain constraints
-   deterministic post-processing
-   persistence

The LLM provider must not become the owner of Omnime business rules.

------------------------------------------------------------------------

# 13. DTO Architecture

JPA entities must never be exposed directly from controllers.

DTOs should be use-case oriented rather than one giant universal object
with many nullable fields.

Conceptual response examples:

``` text
AnimeSearchResultResponse
AnimeDetailsResponse
RecommendationAnimeResponse
SavedAnimeResponse
WatchedAnimeResponse
```

Conceptual request examples:

``` text
MarkWatchedRequest
MarkDroppedRequest
MarkNotInterestedRequest
UpdateRatingRequest
UpdateFeedbackRequest
TasteCorrectionRequest
RecommendationRequest
```

Exact DTO names and fields are implementation/API-contract decisions.

DTOs should expose only what the client use case needs.

------------------------------------------------------------------------

# 14. Validation and Exception Architecture

Validation occurs at two levels.

## Request/structural validation

Examples:

-   valid email shape
-   required fields
-   string length
-   rating between 1 and 10
-   basic request format

Use DTO validation where appropriate.

## Business validation

Services validate:

-   ownership
-   whether a requested transition is valid
-   whether an anime exists
-   whether a TasteConclusion belongs to the authenticated user
-   whether evidence shape matches its semantic type
-   stale UserAnime version
-   stale Undo
-   current relationship state
-   semantic relationship between created artifacts

## Central exception mapping

Use:

``` text
common/
└── exception/
```

with centralized Spring `@RestControllerAdvice`.

Candidate exception concepts include:

``` text
ResourceNotFoundException
InvalidStateTransitionException
StaleRelationshipException
ExternalServiceException
```

Typical HTTP semantics:

``` text
400 → malformed/invalid request
401 → unauthenticated
403 → authenticated but unauthorized
404 → resource not found
409 → stale state / conflicting current state
controlled 5xx → required external/core service failure
```

API errors should eventually expose:

-   stable machine-readable error code
-   safe human-readable message

The exact response schema is not frozen here.

------------------------------------------------------------------------

# 15. Transaction Architecture

Transactions belong at coherent service-level business-operation
boundaries.

Do not put transaction ownership in controllers.

Do not hold a database transaction open across slow AniList or LLM calls
unless a specific operation proves unavoidable.

## Relationship operation

Example:

``` text
UserAnimeService
@Transactional
→ validate current relationship/version
→ mutate UserAnime
→ delegate creation of required raw FeedbackEvidence
→ set required pointer
→ commit
```

Then:

``` text
after commit
→ interpret evidence
→ recompute taste
```

Core user-authored truth survives AI failure.

## Anime Profile generation

``` text
determine profile missing
→ gather source data
→ call LLM outside long transaction
→ validate
→ short transaction persists profile mappings
```

## Taste correction

Authoritative correction creation must commit independently from later
AI interpretation.

## Recommendation generation

Recommendation generation is **not** one giant `@Transactional`
operation.

It consists primarily of:

-   reads
-   external calls
-   orchestration
-   small profile persistence operations delegated to owning services

------------------------------------------------------------------------

# 16. Undo Architecture

## Package

``` text
common/
└── undo/
    └── UndoTokenService
```

`UndoTokenService` owns cryptographic token mechanics only.

`UserAnimeService` owns relationship restoration semantics.

There is:

-   no Undo table
-   no LastAction table
-   no generalized event sourcing
-   no relationship history table
-   no soft-delete system

## In-place mutation Undo

When the same UserAnime row survives:

-   response may include current UserAnime id/version
-   response includes exact IDs of artifacts created by the operation
    where needed
-   Undo request supplies expected authoritative identifiers
-   backend validates ownership, state, version, and artifact
    relationship
-   stale state returns HTTP 409

Do not infer artifacts using timestamps.

## Delete → Undo

When a relationship row is physically deleted:

``` text
Spring reads authoritative row
→ validates current version/state
→ captures minimal restore values
→ deletes row in transaction
→ returns short-lived opaque authenticated Undo token
```

The token must be:

-   server-generated
-   cryptographically authenticated
-   short-lived with a configurable TTL aligned to the immediate Undo UI opportunity
-   bound to authenticated user and operation
-   opaque to React
-   limited to minimal server-trusted restoration data
-   not persisted

On Undo:

``` text
verify signature/authenticity
→ verify expiry
→ verify authenticated user binding
→ verify (user_id, anime_id) is still absent
```

If a newer row exists:

``` text
409 Conflict
```

The same absence precondition also makes a successfully used delete-Undo token non-replayable in practice: the first restoration recreates the UNIQUE(user_id, anime_id) relationship, so replay fails with 409. MVP does not require a persisted nonce or replay registry.

Never overwrite the user's newer action.

If absent:

``` text
recreate UserAnime atomically
→ fresh surrogate PK
→ fresh @Version
```

The database unique constraint on `(user_id, anime_id)` is the final
race safeguard.

------------------------------------------------------------------------

# 17. Concurrency

`UserAnime` is the only MVP entity currently justified for JPA
optimistic locking.

``` text
@Version
```

maps to:

``` text
user_anime.version
```

Use it for in-place relationship mutations and stale-action protection.

The database uniqueness constraint:

``` text
UNIQUE(user_id, anime_id)
```

prevents duplicate relationship rows under races.

Other entities should not receive `@Version` speculatively.

Concurrent Omnime profile generation must be implemented idempotently
and respect the existing `UNIQUE(anime_id, tag_id)` constraint. The
exact concurrency technique is an implementation decision and should not
require speculative infrastructure.

------------------------------------------------------------------------

# 18. Failure and Recovery Model

The architecture distinguishes authoritative user truth from derived AI
work.

## Must survive LLM failure

These operations should remain functional without the LLM:

-   authentication
-   search
-   anime details using stored factual metadata
-   Saved
-   Watched
-   Dropped
-   Not Interested
-   relationship changes
-   rating
-   raw feedback persistence
-   account operations

## Derived work may become temporarily pending

-   FeedbackInterpretation
-   taste recomputation
-   missing Omnime Anime Profiles

For MVP, do not add Kafka/RabbitMQ/Redis solely for retries.

Derived failure must remain durably detectable from existing PostgreSQL state rather than depending exclusively on an in-memory retry. At minimum:

``` text
feedback_evidence
LEFT JOIN feedback_interpretation
WHERE interpretation is missing
```

identifies evidence awaiting interpretation, and:

``` text
users.taste_evidence_version > users.taste_computed_version
```

identifies stale taste materialization. Missing AnimeProfiles remain detectable by absence of `anime_profile` when a candidate next requires profiling.

A failed derivation should be retried on the next relevant access/operation where practical. A lightweight Spring scheduled recovery scan MAY additionally retry detectable missing/stale work if needed. A mandatory generalized worker/job system, advisory-lock protocol, dead-letter store, or new persistence table is not part of the MVP architecture. Repeated processing must remain safe through existing uniqueness/version/current-state invariants and short persistence transactions.

## AniList outage

With an already-populated catalog:

-   search remains available
-   details remain available
-   relationship operations remain available
-   recommendation candidate discovery can use local data
-   catalog refresh is temporarily unavailable

Catalog synchronization failure must not delete or invalidate the last
known usable catalog snapshot.

------------------------------------------------------------------------

# 19. Deployment Architecture

``` text
User
  ↓ HTTPS
Vercel
React/Vite (`frontend/`)
  ↓ HTTPS REST
Render
Spring Boot / Java 25 / Docker (`backend/`)
  ├── JDBC + TLS → Neon PostgreSQL
  ├── HTTPS → AniList GraphQL
  ├── HTTPS → LLM Provider
  └── HTTPS → Email Provider
```

Only Spring Boot communicates with PostgreSQL.

React must never connect directly to the database.

------------------------------------------------------------------------

# 20. Environment Variables and Secrets

## Frontend

Frontend environment configuration may include public values such as:

``` text
VITE_API_BASE_URL
```

Anything exposed through Vite browser configuration must be treated as
public.

Never put the following in frontend environment variables:

-   PostgreSQL password
-   LLM API key
-   JWT signing secret
-   email-provider secret

## Backend

Render backend environment configuration will conceptually include:

-   database connection information
-   authentication/JWT signing secret
-   LLM API key
-   selected LLM model/config
-   allowed frontend origin
-   email provider configuration

Exact environment-variable names are an implementation/deployment
decision.

Secrets must not be committed to GitHub or hardcoded in source.

Local secret/config files must be ignored appropriately.

------------------------------------------------------------------------

# 21. CORS, Cookies, and Browser Security

Production CORS must explicitly allow the deployed frontend origin.

Credentialed browser requests must not use wildcard CORS origins.

Local development origins should be environment/configuration driven
rather than hardcoded throughout application code.

Because the locked authentication design uses an HttpOnly cookie:

-   frontend requests must send credentials where required
-   cookie `Secure` behavior must be production-safe
-   SameSite/domain behavior must match the final
    Vercel/Render/custom-domain topology
-   CSRF protection must be intentionally designed
-   CORS must not be mistaken for CSRF protection

CSRF protection is required for authenticated cookie-based mutations. Spring Security CSRF protection remains enabled; the backend exposes a CSRF token through a frontend-accessible backend response mechanism that React returns in the required custom header. The authentication JWT remains HttpOnly. With the current Vercel frontend and Render backend on separate sites, React must not depend on directly reading a Render-scoped cookie. Exact Spring class/header/cookie names and SameSite/domain values remain deployment implementation details.

------------------------------------------------------------------------

# 22. Database Migration Policy

Flyway is authoritative for schema mutation.

Hibernate:

``` text
ddl-auto=validate
```

in normal development and production.

Spring startup should conceptually:

``` text
connect to PostgreSQL
→ run pending Flyway migrations
→ Hibernate validates mappings
→ application starts
```

Do not use Hibernate automatic schema creation/update as the production
migration strategy.

The canonical migration ordering and schema definitions live in
`MVP_DATA_MODEL.md`.

------------------------------------------------------------------------

# 23. Catalog Bootstrap and Synchronization

Initial AniList catalog bootstrap is an operational process separate
from ordinary application startup.

Do not:

``` text
start Spring
→ synchronously download entire AniList catalog
→ block application readiness
```

Ongoing catalog synchronization may later use lightweight Spring
scheduling if appropriate.

The exact sync cadence is not frozen.

The bootstrap uses AniList standard Page pagination inside bounded query partitions. No partition may depend on traversing beyond AniList's documented 5,000-entry pagination depth. Use the maximum useful page size and `hasNextPage`; the exact partition key is an ingestion implementation decision. AniListClient should respect provider rate-limit headers/Retry-After rather than embedding a permanently assumed requests-per-minute sleep.

Omnime MVP is a non-commercial portfolio project and retains the full local AniList factual catalog for that development context. AniList's current terms restrict mass collection while noting leniency for purely educational projects; this architecture does not treat that as blanket permission for a future commercial product. Before commercialization or broader production use, the then-current AniList terms must be reviewed and appropriate permission/licensing or an alternative data-source strategy established as necessary.

------------------------------------------------------------------------

# 24. Health Checks

Expose a lightweight backend health endpoint, potentially through Spring
Boot Actuator.

Basic application health should validate that the backend itself is
alive and able to serve its core dependencies appropriately.

Do **not** make every platform health probe synchronously call AniList
or the LLM.

An external-provider outage should not cause Render to repeatedly
restart an otherwise healthy backend.

------------------------------------------------------------------------

# 25. Environment Strategy

Initial MVP environments:

``` text
LOCAL
PRODUCTION
```

Do not introduce a staging environment until there is a concrete need.

The architecture should nevertheless keep environment-specific values
externalized so staging can be introduced later without restructuring
application code.

------------------------------------------------------------------------

# 26. JPA Rules

The JPA model must follow the frozen `MVP_DATA_MODEL.md`.

Persistent entities:

``` text
User
PasswordResetToken
Anime
Genre
AnimeGenre
AniListTag
AnimeAniListTag
OmnimeTag
AnimeTag
UserAnime
FeedbackEvidence
FeedbackInterpretation
TasteConclusion
TasteConclusionInterpretation
TasteCorrection
```

Rules:

-   collections default LAZY
-   avoid exposing entity graphs to controllers
-   use explicit joins or DTO projections for list/read use cases
-   avoid accidental cascade REMOVE across shared catalog data
-   database cascades own user-account deletion semantics
-   `@Version` only where justified: UserAnime
-   avoid N+1 behavior in Saved/Watched/search/recommendation reads

------------------------------------------------------------------------

# 27. Screen-to-Service Ownership

Frontend screens are presentation contexts, not backend business
domains.

Examples:

``` text
Add Anime
Home
Anime Details
Saved
Watched
Recommendation Feedback
```

may all trigger:

``` text
UserAnimeService.markWatched(...)
```

rather than each having separate backend implementations.

Similarly:

``` text
Edit Feedback
```

uses FeedbackService regardless of the screen from which the user opened
the editor.

``` text
Correct My Taste
```

uses TasteCorrectionService because it is a distinct domain operation.

This rule prevents UI organization from leaking into duplicated backend
business logic.

------------------------------------------------------------------------

# 28. Explicit Non-Goals

Do not introduce the following into the MVP architecture without a
demonstrated requirement:

-   microservices
-   Kafka
-   RabbitMQ
-   Redis
-   distributed job queues
-   event sourcing
-   relationship history
-   generalized audit-log infrastructure
-   persisted recommendation sessions
-   persisted Current Viewing Intent
-   recommendation-history table
-   generalized Undo persistence
-   soft deletion
-   speculative caching layers
-   generic BaseController
-   generic BaseService
-   generic BaseRepository
-   unnecessary inheritance frameworks
-   AI prompt/response logging as domain data
-   a second recommendation engine for users below five Watched
-   a separate user-profile/settings table merely for the current
    adult-content preference

------------------------------------------------------------------------

# 29. Final Stress-Test and Adversarial-Review Resolutions

The architecture stress test and final adversarial review resolved the previously open issues. These decisions are part of the frozen MVP architecture.

- **CSRF:** Spring Security CSRF remains enabled for authenticated cookie mutations; the backend exposes the separate CSRF token through a frontend-accessible response mechanism and React echoes it in the configured header while the auth JWT remains HttpOnly. The Vercel frontend must not depend on directly reading a Render-scoped cookie. CORS is separate, explicit-origin, and credential-aware; integration tests cover success/failure.
- **Profile completion:** `anime_profile` is the durable successful-profile marker. A successful profile may contain zero `anime_tag` rows. The marker and all mappings from one generation persist atomically in one short transaction.
- **Taste freshness:** `users.taste_evidence_version` and `users.taste_computed_version` provide durable stale/current detection. Recompute captures a target version and advances computed version only to the version actually incorporated.
- **Corrected conclusion lifecycle:** `taste_conclusion.active` distinguishes current from retained conclusions. Corrected conclusions are retained/inactivated rather than casually deleted; `taste_correction.taste_conclusion_id` uses ON DELETE RESTRICT.
- **Viewing evidence eligibility:** raw evidence preservation is distinct from active taste eligibility. Watched/Dropped viewing evidence is active; Saved may retain legitimate prior viewing evidence; Not Interested or NONE makes viewing-experience evidence inactive. NI reason evidence is active only while referenced by the current NI relationship. Eligibility-changing transitions advance taste evidence version.
- **Current Viewing Intent:** RecommendationService owns interpretation semantics. Only explicit constraints supported by reliable catalog facts become hard deterministic filters; ambiguous/qualitative language remains soft ranking context. Parse failure must not invent hard constraints.
- **Profile concurrency/cost:** remote LLM calls occur outside DB transactions. Rare duplicate concurrent generation is acceptable; persistence is idempotent through re-check plus `anime_profile.anime_id` uniqueness. Missing shortlist profiles may be generated with bounded parallelism and independent short persistence transactions.
- **Profile staleness:** routine AniList factual sync does not automatically invalidate an Omnime profile. Regeneration is explicit/selective for meaningful model/ontology changes, known quality issues, or deliberate maintenance.
- **Home resolution UX:** Save, Seen It, Dropped, and Not Interested persist immediately, but the acted-on recommendation card remains visually in place for the short Undo window. Undo restores business state in place; expiry removes the resolved card and fills the slot when another qualifying recommendation exists.
- **Stateless logout:** logout clears the auth cookie; no refresh-token system, blacklist, or persistent session registry in MVP. Short JWT lifetime is the accepted mitigation for residual stolen-token validity.
- **AniList bootstrap:** retain the full local factual catalog for the non-commercial portfolio MVP. Use bounded partitions + ordinary pagination, provider-aware rate limiting, and an explicit operational bootstrap separate from startup.
- **Account deletion:** one transaction; delete TasteCorrections first, then User, allowing remaining user-owned cascades without violating the correction RESTRICT invariant; integration-test the corrected-user path.
- **Migration consistency:** Flyway ordering in `MVP_DATA_MODEL.md` includes all 16 tables and respects FK dependencies; Hibernate remains validate-only. FeedbackEvidence valid shapes are enforced by a PostgreSQL CHECK constraint and mirrored in service validation.
- **Derived-work recovery:** missing/stale derived work is durably detectable from existing DB state and retried on relevant access/operation; a lightweight scheduled scan is permitted but not mandatory. No queue, advisory-lock protocol, dead-letter store, or new job table is required for MVP.
- **Undo replay/TTL:** delete-Undo tokens use a short configurable TTL, cryptographic authentication, user/operation binding, and authoritative absence/state preconditions. Successful restoration makes replay fail with 409; no persisted nonce/replay registry is required.

------------------------------------------------------------------------

# 30. Architecture Invariants

The following are hard frozen MVP implementation invariants:

1.  The frozen `MVP_DATA_MODEL.md` is the canonical persistent schema
    unless a genuine contradiction requires a controlled amendment.
2.  User-owned actions derive ownership from authenticated server
    identity.
3.  Only NONE/unclassified anime are recommendation-eligible by
    relationship state.
4.  Saved, Watched, Dropped, and Not Interested anime are excluded.
5.  Adult anime are excluded unless the user explicitly enables
    `include_adult_anime`.
6.  Current Viewing Intent cannot override relationship or adult-content
    eligibility.
7.  Five Watched is not a recommendation or taste-data prerequisite.
8.  UserAnimeService owns current relationship mutations.
9.  FeedbackService owns raw user-authored evidence.
10. FeedbackInterpretationService owns interpretation of individual
    evidence.
11. TasteService owns aggregate current taste reasoning.
12. TasteCorrectionService owns Correct My Taste.
13. AnimeService uses the local catalog.
14. AnimeCatalogService maintains AniList-derived catalog data.
15. AnimeProfileService owns reusable Omnime Anime Profiles.
16. RecommendationService orchestrates recommendation generation but
    does not absorb the responsibilities of those services.
17. JPA entities never become API contracts.
18. Remote AI calls do not wrap authoritative user writes in long
    database transactions.
19. Raw user actions commit before optional/derived AI work.
20. Recommendations, personalized explanations, Current Viewing Intent,
    and active recommendation-session state remain transient.
21. Flyway owns schema mutation.
22. Hibernate validates rather than evolves production schema.
23. Shared catalog/profile data survives user deletion.
24. No new infrastructure is added merely because it might be useful
    later.

------------------------------------------------------------------------

# 31. Adversarial Review Objective

The reviewer should **not redesign Omnime's locked product or UX merely
according to preference**.

Review this architecture against the frozen data model and identify
concrete risks in areas such as:

-   duplicate domain ownership
-   god services
-   circular dependencies
-   transaction boundaries
-   JPA ownership/fetching
-   race conditions
-   stale derived data
-   failure recovery
-   authentication/authorization
-   CSRF/CORS/cookie security
-   API boundaries
-   external-service isolation
-   catalog synchronization
-   recommendation latency/cost
-   profile-generation lifecycle
-   deployment compatibility
-   data-model contradictions
-   anything likely to force fundamental drop/recreate or major
    structural rework during implementation

For each objection, distinguish:

``` text
BLOCKER
→ likely fundamental correctness/security/architecture problem

IMPORTANT
→ should be resolved before implementation but does not invalidate the architecture

IMPLEMENTATION DETAIL
→ can safely be decided while coding without structural rework

OPTIONAL IMPROVEMENT
→ useful but not required for MVP
```

Do not recommend infrastructure or abstractions without explaining the
concrete MVP failure they prevent.

------------------------------------------------------------------------

# 32. Frozen Status

The frozen MVP backend architecture consists of:

-   feature-oriented Spring package boundaries
-   centralized relationship business logic
-   separated raw feedback, interpretation, and aggregate taste
    reasoning
-   local AniList factual catalog
-   selective reusable Omnime Anime Profiles
-   transient recommendation orchestration
-   deterministic eligibility before AI ranking
-   stateless-cookie authentication
-   Flyway-controlled PostgreSQL schema
-   Vercel frontend + Render backend + Neon PostgreSQL deployment
-   graceful separation of authoritative user writes from derived AI
    work

**BACKEND ARCHITECTURE STATUS: FROZEN --- APPROVED FOR MVP IMPLEMENTATION**

Final adversarial review is complete. Justified findings have been incorporated; optional enterprise-oriented additions that were not required for MVP correctness remain intentionally excluded.
