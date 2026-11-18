# Stack Overflow Hybrid RAG — Spring Boot

Spring Boot 3 (Java 17) endpoint that retrieves Stack Overflow Q&A records from your existing Qdrant Cloud collection using **dense semantic search + BM25 full-text search + Qdrant RRF**, then sends the top five source Q&As to OpenAI for a grounded answer.

## Prerequisites
- Java 17+ and Maven 3.9+
- Qdrant Cloud collection `stackoverflow` already populated with the Python ingestion script (unnamed 384-dimensional dense vectors, payload fields `question`, `accepted_answer`, `text`)
- **BM25-scoring full-text index on payload field `text`** (not merely a sparse vector named `bm25`)
- Qdrant Cloud inference enabled and the `sentence-transformers/all-minilm-l6-v2` model available
- OpenAI API access to your configured chat-completions model


## Configure environment variables
PowerShell (current session only):

```powershell
$env:QDRANT_URL = "https://YOUR-CLUSTER.cloud.qdrant.io"
$env:QDRANT_API_KEY = "YOUR_QDRANT_KEY"
$env:OPENAI_API_KEY = "YOUR_OPENAI_KEY"
# Optional: $env:OPENAI_MODEL = "gpt-4.1-mini"
```


## Run

```powershell
mvn spring-boot:run
```

## Send a request

```powershell
$body = @{ question = "How do I handle Kafka consumer retries in Spring Boot?" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/ask -ContentType 'application/json' -Body $body
```

Response shape:

```json
{
  "answer": "... [Source 1] ...",
  "sources": [
    { "id": "12345", "question": "..." }
  ]
}
```

## Request flow

1. `POST /api/ask` validates `question`.
2. Qdrant Cloud infers a dense query embedding with the same MiniLM model used during ingestion.
3. Qdrant BM25 searches your indexed `text` payload.
4. Qdrant fuses the two top-10 lists using RRF, returns top five Q&A payloads.
5. Spring Boot creates a bounded prompt; OpenAI Chat Completions synthesizes an answer.
6. Spring Boot responds with the answer and retrieved source IDs/questions.

## Improvements for a production project

- Split `RagService` into Qdrant and OpenAI client services.
- Add retries/circuit breakers and exception mapping (upstream 429, 5xx, timeouts).
- Add evaluations for Recall@5, MRR, answer faithfulness, and p95 latency.
- Preserve Stack Overflow question URLs and attribution/license metadata in ingestion and responses.
- Make retrieved-context budgeting token-aware; cap request size, enforce auth/rate limits before exposing publicly.

Docs: https://qdrant.tech/documentation/search/text-search/hybrid-search/
