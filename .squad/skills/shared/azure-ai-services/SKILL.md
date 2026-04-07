---
name: "azure-ai-services"
description: "Azure AI Services for GymBro's AI coach backend — OpenAI, AI Search, Content Safety"
domain: "cloud-ai"
confidence: "high"
source: "adapted from microsoft/azure-skills (azure-ai skill)"
---

## Context

GymBro uses a cloud AI backend as the primary AI coach for MVP (with on-device models planned for v2.0). The backend proxies requests to Azure OpenAI for conversational coaching and uses Azure AI Search for exercise/periodization knowledge retrieval. Budget: 300€/month total Azure spend.

## Services

| Service | GymBro Use Case | Priority |
|---------|----------------|----------|
| Azure OpenAI | AI coach chat (GPT-4o-mini for cost efficiency) | P0 — MVP core |
| Azure AI Search | RAG over training knowledge base (exercises, periodization, form cues) | P1 — enhances coach quality |
| Azure Content Safety | Filter unsafe training advice (injury risk, medical claims) | P1 — safety requirement |
| Azure AI Speech | Voice logging input (v2.0) | P2 — post-MVP |

## Patterns

### Azure OpenAI Setup

- **Model:** GPT-4o-mini (cost-efficient, fast) for 90% of queries; GPT-4o for complex program generation
- **Deployment:** Use Azure OpenAI resource, not direct OpenAI API (data stays in Azure, GDPR compliant)
- **Region:** West Europe (closest to user, GDPR)
- **Rate limiting:** Implement token-bucket in backend proxy to stay within budget
- **System prompt:** Versioned in repo, A/B testable via backend config

### Backend Proxy Architecture

```
iOS App → Azure Functions (proxy) → Azure OpenAI
                                  → Azure AI Search (RAG context)
                                  → Azure Content Safety (filter)
```

- **Never call Azure OpenAI directly from iOS** — proxy controls cost, logging, prompt versioning
- Backend proxy: Azure Functions (Consumption plan for MVP, ~15€/month)
- Auth: Sign in with Apple JWT → validate in proxy → forward to OpenAI

### Cost Control (300€/month budget)

| Service | Estimated Monthly Cost |
|---------|----------------------|
| Azure OpenAI (GPT-4o-mini) | ~80€ (100K requests @ ~800 tokens avg) |
| Azure Functions (Consumption) | ~15€ |
| Azure AI Search (Basic) | ~60€ |
| Azure Content Safety | ~10€ |
| Azure Cosmos DB (serverless) | ~30€ |
| **Buffer** | ~105€ |

- Set Azure budget alerts at 200€ and 270€
- Use GPT-4o-mini as default, escalate to GPT-4o only for program generation
- Cache common responses (exercise form cues, standard programs)

### Knowledge Base (RAG)

- Index strength training literature, exercise database, periodization models
- Use hybrid search (keyword + vector) for best retrieval
- Chunk size: 512 tokens with 128 overlap
- Embedding model: text-embedding-3-small (cost-efficient)

### Content Safety Guardrails

- Filter all AI coach responses through Content Safety API
- Block: dangerous exercise modifications, medical diagnoses, injury treatment advice
- Flag: supplement recommendations, extreme calorie advice
- Always append disclaimer: "Consult a medical professional for health concerns"

## Anti-Patterns

- ❌ Calling Azure OpenAI directly from iOS app (no cost control, exposes API key)
- ❌ Using GPT-4o for simple queries (burns budget)
- ❌ Skipping Content Safety filtering (liability risk for training advice)
- ❌ Hardcoding prompts in iOS app (can't A/B test or update without app release)
- ❌ Storing conversation history in Azure OpenAI (use own database for privacy)
