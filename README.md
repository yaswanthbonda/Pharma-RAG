# PharmaSub — AI-Powered Drug Substitution Advisor

> A HIPAA-aware RAG (Retrieval-Augmented Generation) application that recommends safe, formulary-covered drug alternatives when a prescribed medication is out of stock.

Built with **React 18 + MUI**, **Java 17 + Spring Boot 3**, **ChromaDB**, and **Anthropic Claude** (claude-sonnet-4-6).

---

## Architecture

```
┌─────────────────┐     ┌──────────────────────┐     ┌───────────────┐
│  React 18 + MUI │────▶│ Spring Boot 3 (Java) │────▶│   ChromaDB    │
│   (Port 3000)   │     │     (Port 8080)       │     │  (Port 8001)  │
└─────────────────┘     └──────────┬───────────┘     └───────────────┘
                                   │
                          ┌────────▼────────┐
                          │  Anthropic API  │
                          │  (claude-sonnet)│
                          └─────────────────┘
```

### RAG Pipeline Flow
1. **Ingest** — OpenFDA drug data loaded into ChromaDB at startup
2. **Query** — User submits a drug + member plan + allergy info
3. **Embed** — Spring Boot embeds the query via Claude's API
4. **Retrieve** — ChromaDB returns top-5 semantically similar drug records
5. **Generate** — Claude reasons through 4 safety gates (equivalency → formulary → interactions → allergies)
6. **Respond** — Structured JSON with recommendation + confidence + citations

---

## Quickstart

### Prerequisites
- Docker Desktop installed and running
- An Anthropic API key ([get one here](https://console.anthropic.com))

### 1. Clone the repo
```bash
git clone https://github.com/YOUR_USERNAME/pharma-rag.git
cd pharma-rag
```

### 2. Set your API key
```bash
cp .env.example .env
# Edit .env and replace 'your_anthropic_api_key_here' with your real key
```

### 3. Start everything
```bash
docker compose up --build
```

This starts ChromaDB, runs the OpenFDA data ingestion, starts the Spring Boot backend, then starts the React frontend.

### 4. Open the app
Navigate to **http://localhost:3000**

---

## HIPAA Compliance Notes

This application follows HIPAA-aware design principles:

- **No PHI stored** — Member IDs are pseudonymized; no real names, DOB, or SSN are accepted
- **Audit logging** — Every substitution request is logged with timestamp, member pseudonym, and drug queried (no PII in logs)
- **No data persistence of queries** — Substitution requests are not stored in any database
- **Environment secrets** — API keys are never hardcoded; loaded via environment variables only
- **HTTPS-ready** — SSL/TLS configuration documented in `backend/src/main/resources/application.yml`
- **Input validation** — All inputs sanitized before reaching the RAG pipeline

> **Disclaimer:** This is a portfolio/demo application. It is NOT intended for real clinical use. All drug recommendations must be reviewed by a licensed pharmacist or physician.

---

## Commit History (for GitHub)

| Commit | Description |
|--------|-------------|
| `feat: project scaffold` | Docker Compose, folder structure, README |
| `feat: chromadb + openfda ingestion` | Data loader, ChromaDB client, OpenFDA integration |
| `feat: spring boot rag service` | Embedding service, vector search, ChromaDB HTTP client |
| `feat: claude api integration` | Prompt engineering, 4-gate safety logic, response parsing |
| `feat: react frontend` | Drug substitution UI, MUI components, API service layer |
| `feat: hipaa compliance layer` | Audit logging, PII sanitization, error boundaries |

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/substitution/recommend` | Get drug substitution recommendation |
| `GET` | `/api/v1/drugs/search?q={name}` | Search drugs by name |
| `GET` | `/api/v1/health` | Health check |
| `GET` | `/actuator/health` | Spring Actuator health |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, MUI v5, Axios |
| Backend | Java 17, Spring Boot 3, Spring WebFlux |
| Vector DB | ChromaDB (HTTP API) |
| LLM | Anthropic Claude claude-sonnet-4-6 |
| Drug Data | OpenFDA API (free, public) |
| Container | Docker, Docker Compose |
| Audit | SLF4J structured logging |
