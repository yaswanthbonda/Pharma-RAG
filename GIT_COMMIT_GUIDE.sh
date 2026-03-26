#!/bin/bash
# PharmaSub — Complete GitHub Commit Guide
# Run these commands in order after creating your empty GitHub repo

# ── ONE-TIME SETUP ──────────────────────────────────────────────────────────
git init
git remote add origin https://github.com/YOUR_USERNAME/pharma-rag.git
git branch -M main

# COMMIT 1 — Project scaffold
git add .gitignore .env.example README.md docker-compose.yml GIT_COMMIT_GUIDE.sh
git commit -m "feat: project scaffold — Docker Compose, README, HIPAA gitignore

- Docker Compose orchestrates 4 services: ChromaDB, ingest, Spring Boot, React
- .env.example documents required env vars (API key never committed — HIPAA)
- .gitignore explicitly excludes audit logs, PHI-adjacent files, secrets
- README documents full architecture, quickstart, HIPAA compliance notes"

# COMMIT 2 — ChromaDB vector store + OpenFDA drug ingestion
git add scripts/
git add backend/src/main/java/com/pharmarag/model/DrugDocument.java
git add backend/src/main/java/com/pharmarag/service/ChromaService.java
git add backend/src/main/java/com/pharmarag/service/OpenFdaIngestionService.java
git commit -m "feat: ChromaDB vector store + OpenFDA drug data ingestion pipeline

- ChromaService: collection lifecycle, batch add, cosine similarity search
- OpenFdaIngestionService: fetches 10 drug classes from OpenFDA on startup
- Python ingest.py: seeds ChromaDB with deterministic 384-dim embeddings
- DrugDocument model with toEmbeddingText() for consistent vector representation
- HIPAA: only public FDA drug data ingested — zero patient data stored"

# COMMIT 3 — Spring Boot RAG service core
git add backend/src/main/java/com/pharmarag/service/RagPipelineService.java
git add backend/src/main/java/com/pharmarag/service/ClaudeService.java
git add backend/src/main/java/com/pharmarag/model/SubstitutionRequest.java
git add backend/src/main/java/com/pharmarag/model/SubstitutionResponse.java
git add backend/src/main/java/com/pharmarag/config/WebClientConfig.java
git add backend/src/main/java/com/pharmarag/config/JacksonConfig.java
git commit -m "feat: RAG pipeline — embed, retrieve, generate (Java + ChromaDB + Claude)

- RagPipelineService: full 5-step pipeline orchestration
- ClaudeService: Anthropic Messages API via non-blocking WebClient
- SubstitutionRequest: Jakarta-validated (HIPAA member ID format enforced)
- SubstitutionResponse: status, recommendations, gate results, RAG metadata"

# COMMIT 4 — Claude prompt engineering + HIPAA compliance layer
git add backend/src/main/java/com/pharmarag/controller/
git add backend/src/main/java/com/pharmarag/audit/
git add backend/src/main/java/com/pharmarag/config/CorsConfig.java
git add backend/src/main/java/com/pharmarag/config/JacksonConfig.java
git add backend/src/main/java/com/pharmarag/PharmaRagApplication.java
git add backend/src/main/resources/application.yml
git add backend/pom.xml
git add backend/Dockerfile
git commit -m "feat: Claude prompt engineering, REST API, HIPAA audit layer

- Chain-of-thought system prompt enforces 4 safety gates in strict sequence
- Handles all 4 no-substitute failure scenarios with specific escalation queues:
    drug_interaction  -> clinical-review-urgent
    allergy_class     -> physician-contact-required
    formulary_gap     -> benefits-specialist
    unique_drug       -> specialty-pharmacy-director
- AuditLogger: member ID masked to last-4, zero PHI in log output
- GlobalExceptionHandler: never exposes stack traces to client
- Spring Boot 3.2 / Java 17 / multi-stage Docker (non-root user)"

# COMMIT 5 — Unit and integration tests
git add backend/src/test/
git commit -m "test: unit + integration tests for RAG pipeline and HIPAA compliance

- RagPipelineServiceTest: happy path, interaction-blocked, malformed response
- SubstitutionControllerTest: health, valid request, HIPAA format validation
- AuditLoggerTest: verifies member ID never appears unmasked in logs
- application-test.yml: test profile prevents real API connections"

# COMMIT 6 — React 18 + MUI frontend
git add frontend/
git commit -m "feat: React 18 + MUI v5 frontend — drug substitution UI

- RagInfoPanel: collapsible pipeline explainer with tech stack chips
- DrugSearchForm: allergy chips, medication list, plan ID, validation
- RecommendationCard: tier badge, copay, gate checks, confidence bar, RAG metadata
- CandidateTable: 4-gate pass/fail table for all evaluated candidates
- LoadingSkeleton: animated pipeline step progress while Claude reasons
- HipaaDisclaimer: always-visible expandable HIPAA/clinical notice
- HistoryDrawer: in-memory session history (never persisted — HIPAA)
- Nginx: X-Frame-Options DENY, Cache-Control no-store security headers"

# ── PUSH ────────────────────────────────────────────────────────────────────
git push -u origin main

echo ""
echo "Done! All 6 commits pushed."
echo ""
echo "To run locally:"
echo "  cp .env.example .env"
echo "  # add your Anthropic API key to .env"
echo "  docker compose up --build"
echo "  open http://localhost:3000"
echo ""
echo "Demo drugs to try:"
echo "  Drug: Lipitor 40mg | Class: statin      | Member: MBR-DEMO0001"
echo "  Drug: Bactrim      | Allergy: Sulfa      | Member: MBR-DEMO0002"
echo "  Drug: Metoprolol   | Class: beta blocker | Member: MBR-DEMO0003"
