# Architecture

This page shows how a question travels through the service. Diagrams are Mermaid, so they render directly on GitHub.

## Components

```mermaid
flowchart LR
    client([Client])

    subgraph app["Spring Boot app (hybrid-rag)"]
        direction TB
        controller["AskController<br/>POST /api/ask<br/>@Valid AskRequest"]
        service["RagService"]
        http["HttpClientConfig<br/>RestClient.Builder<br/>connect 10s / read 90s"]
        controller --> service
        http -. "cloned into two clients" .-> service
    end

    subgraph qdrant["Qdrant Cloud"]
        direction TB
        infer["Cloud inference<br/>all-minilm-l6-v2"]
        dense[("Dense vectors<br/>(unnamed)")]
        bm25[("BM25 index<br/>on payload 'text'")]
        rrf{{"RRF fusion"}}
        infer --> dense
        dense --> rrf
        bm25 --> rrf
    end

    openai["OpenAI<br/>/v1/chat/completions"]

    client -- "JSON {question}" --> controller
    service -- "POST /collections/{collection}/points/query<br/>header: api-key" --> qdrant
    service -- "POST /chat/completions<br/>header: Authorization: Bearer" --> openai
    controller -- "JSON {answer, sources}" --> client
```

## Request flow

```mermaid
flowchart TD
    A([POST /api/ask]) --> B{"question valid?<br/>not blank, ≤ 4000 chars"}
    B -- no --> B1[/"400 Bad Request"/]
    B -- yes --> C["RagService.ask(question)"]

    C --> D["buildHybridSearchRequest<br/>prefetch 1: dense, text + model, limit 10<br/>prefetch 2: BM25 using 'text', limit 10<br/>query: fusion = rrf, limit 5, with_payload"]
    D --> E["queryQdrant<br/>POST /collections/{collection}/points/query"]
    E --> F{"result.points<br/>non-empty?"}
    F -- no --> F1[/"200: 'No matching Stack Overflow<br/>records were found.' + []"/]
    F -- yes --> G["buildContext<br/>skip hits missing question or accepted_answer<br/>number as [Source N]<br/>truncate question 1500, answer 4000 chars"]
    G --> H{"any usable hits?"}
    H -- no --> H1[/"200: 'Matching records had no<br/>usable accepted answers.' + []"/]
    H -- yes --> I["buildLlmRequest<br/>model + system prompt +<br/>user: question + retrieved context"]
    I --> J["callLlm<br/>POST /chat/completions"]
    J --> K{"choices[0].message.content<br/>present and not blank?"}
    K -- no --> K1[/"IllegalStateException → 500"/]
    K -- yes --> L[/"200: {answer, sources[id, question]}"/]
```

## Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant C as AskController
    participant S as RagService
    participant Q as Qdrant Cloud
    participant O as OpenAI

    Client->>C: POST /api/ask {question}
    C->>C: Bean Validation (@NotBlank, @Size 4000)
    C->>S: ask(question)
    S->>Q: points/query (dense + BM25 prefetch, RRF, limit 5)
    Q->>Q: embed query, run both searches, fuse with RRF
    Q-->>S: result.points[] with payload
    alt no points
        S-->>C: "No matching Stack Overflow records were found."
    else no usable payloads
        S-->>C: "Matching records had no usable accepted answers."
    else usable context
        S->>O: chat/completions (system prompt + question + [Source N] context)
        O-->>S: choices[0].message.content
        S-->>C: AskResponse(answer, sources)
    end
    C-->>Client: 200 JSON
```

Errors from either upstream (timeouts, 4xx, 5xx) are not caught in `RagService`, so they surface as Spring's default 500 response.
