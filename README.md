# PharmaSub: Drug Substitution Advisor (RAG demo)

A portfolio project that shows a Retrieval-Augmented Generation (RAG) pipeline end to end: when a prescribed drug is out of stock, suggest a safe, plan-covered alternative and explain why.

**Stack:** React 18 + MUI · Java 17 + Spring Boot 3 · ChromaDB · LangChain4j embeddings (all-MiniLM-L6-v2) · Anthropic Claude · Docker Compose

> **Not for clinical use.** All data is demo data (see [Data](#data)). Every output needs review by a licensed pharmacist.

---

## How it works

```
React UI ──▶ Spring Boot API ──▶ embed query (all-MiniLM-L6-v2, in-process)
                  │                          │
                  │                          ▼
                  │                   ChromaDB (cosine, top-5)
                  │                          │ retrieved drug records
                  ▼                          ▼
             Claude (Messages API) ◀── prompt = rules + request + retrieved context
                  │
                  ▼
       structured JSON: status, recommendation, per-candidate gate results, escalation queue
```

1. **Ingest (first start):** 41 curated drug records plus a synthetic formulary are embedded and stored in ChromaDB. If OpenFDA is reachable, each record also gets a real label excerpt.
2. **Retrieve:** the query (drug, dose, class) is embedded with the same model and matched against the collection.
3. **Reason:** Claude evaluates each retrieved candidate through four safety gates: therapeutic equivalency, formulary coverage, interactions, allergies.
4. **Respond:** one of four outcomes: `FOUND`, `NO_SAFE_SUBSTITUTE`, `NO_COVERED_SUBSTITUTE`, `UNIQUE_DRUG_NO_EQUIVALENT`, with an escalation queue when no substitute exists.

## Design decisions

- **Retrieval query contains only drug facts.** Allergies, current medications and plan are applied by the LLM afterwards. Putting them in the query would pull the embedding away from the drug documents.
- **Formulary lives in the retrieved context, not in the model's head.** The prompt forbids inventing tiers or copays; a drug missing from the member's plan fails the coverage gate.
- **Direct Anthropic API call from Spring Boot** (WebClient) instead of a Python sidecar: one language, one service to run.
- **Embeddings run in-process** (ONNX, no extra API key). Ingestion and query use the same model, and the collection uses cosine distance.
- **Safe failure:** if the model output can't be parsed, the API returns `NO_SAFE_SUBSTITUTE` with a "contact your pharmacist" message, never a guess.

## HIPAA-aware design

Demo-grade controls, not a compliance claim:

- Pseudonymized member IDs (`MBR-XXXX`); no names, DOB or SSN accepted
- Audit log records request/outcome status only, with masked member IDs and no drug or clinical details
- No persistence of requests; history is in browser memory only
- Raw model output is logged only at DEBUG; no stack traces returned to clients
- Secrets via environment variables; `.env` is git-ignored; security headers set in Nginx

## Data

| Source | What it is | Real? |
|---|---|---|
| `backend/src/main/resources/seed/drugs.json` | 41 drugs across 10 classes: interactions, contraindications, allergy flags | Curated reference, **not authoritative** |
| `backend/src/main/resources/seed/formulary.json` | Plan tiers and copays for `STANDARD-PPO-2024` and `BASIC-HMO-2024` | **Synthetic**, invented for the demo |
| OpenFDA label API | Contraindications excerpt attached to each drug (best effort) | Real FDA label text |

Each retrieved document says "synthetic demo data" next to its formulary lines so the model and the reader can tell.

## Quickstart

Prerequisites: Docker Desktop and an [Anthropic API key](https://console.anthropic.com).

```bash
git clone https://github.com/yaswanthbonda/Pharma-RAG.git
cd Pharma-RAG
cp .env.example .env        # then put your key in .env
docker compose up --build
```

Open http://localhost:3000. The first start downloads dependencies and ingests the data, so give the backend a minute (`docker compose logs -f backend`).

Try these:

| Request | Expected |
|---|---|
| Atorvastatin 40mg, plan `STANDARD-PPO-2024`, no allergies | `FOUND` (another statin) |
| Amoxicillin 500mg, allergy `penicillin` | Not amoxicillin; cephalosporin flagged for pharmacist review, or a non-penicillin option |
| Rivaroxaban 20mg, plan `BASIC-HMO-2024` | Equivalent anticoagulants may be uncovered or need prescriber approval |

## Run the tests

```bash
cd backend && mvn test
```

Unit tests cover JSON parsing of model output, the pipeline with mocked embeddings/Chroma/Claude, audit-log masking, the controller, and the seed data.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `ANTHROPIC_API_KEY` | (required) | Claude API key |
| `ANTHROPIC_MODEL` | `claude-sonnet-5-5` | Model used for reasoning |
| `OPENFDA_ENRICHMENT` | `true` | Set `false` to run ingestion fully offline |

## API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/substitution/recommend` | Get a substitution recommendation |
| `GET` | `/api/v1/health` | Application health |
| `GET` | `/actuator/health` | Spring Actuator health |

## Known limitations

- Demo data only; the clinical reference set is small and unreviewed by a pharmacist.
- The LLM performs the four safety gates. A production system would also check interactions and allergies with deterministic rules and a licensed drug database.
- No authentication, TLS, or persistent audit store. Those would be required for real PHI.
- No evaluation set yet. Next step: a labelled list of requests with expected outcomes to measure retrieval and decision accuracy.
- ChromaDB image is pinned to a 0.5.x release because the backend uses the `/api/v1` HTTP endpoints.

## Project layout

```
backend/   Spring Boot service (controller, RAG pipeline, Chroma + Claude clients, seed data, tests)
frontend/  React + MUI UI (search form, result cards, history)
docker-compose.yml   chromadb + backend + frontend
```
