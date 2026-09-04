# Omnime

**AI-powered anime discovery that learns your taste and recommends what you're genuinely likely to enjoy.**

Omnime is an anime recommendation platform built around one goal:

> Recommend anime you haven't seen that you're genuinely likely to enjoy.

Rather than relying primarily on popularity, genres, or simple rating comparisons, Omnime builds an understanding of both the anime it recommends and the individual watching them.

## How It Works

Omnime combines:

- AniList anime metadata
- AI-generated anime profiles
- A curated Omnime tag system
- User ratings and viewing feedback
- AI-generated taste profiles
- Deterministic recommendation filtering
- AI-powered ranking and explanations

The guiding principle behind the recommendation system is:

> **Code handles facts. AI handles judgment.**

The backend handles factual rules such as whether an anime has already been watched, saved, dropped, or marked not interested. AI is then used where interpretation matters: understanding taste, evaluating strong candidates, and explaining why a recommendation fits.

## Tech Stack

### Frontend
- React
- TypeScript
- Vite
- Tailwind CSS

### Backend
- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway

### Data & Integrations
- PostgreSQL
- AniList GraphQL API
- LLM integration

### Deployment
- Vercel — frontend
- Render — backend
- Neon — PostgreSQL
- Docker — backend containerization

## Architecture

Omnime uses a React frontend communicating with a Spring Boot REST API.

```text
React / Vite
    │
    │ HTTPS
    ▼
Spring Boot
    │
    ├── PostgreSQL
    ├── AniList
    └── LLM Provider