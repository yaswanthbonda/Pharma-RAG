# PharmaSub — AI-Powered Drug Substitution Advisor

> A HIPAA-aware RAG (Retrieval-Augmented Generation) application that recommends safe, formulary-covered drug alternatives when a prescribed medication is out of stock.

Built with **React 18 + MUI**, **Java 17 + Spring Boot 3**, **ChromaDB**, and **Anthropic Claude** (claude-sonnet-4-6).

\---

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

\---

## 

