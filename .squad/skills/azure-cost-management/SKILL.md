---
name: "azure-cost-management"
description: "Azure cost tracking and optimization for GymBro's 300€/month budget"
domain: "cloud-cost"
confidence: "high"
source: "adapted from microsoft/azure-skills (azure-cost skill)"
---

## Context

GymBro operates on a strict 300€/month Azure budget. Every Azure resource decision must factor in cost. The team uses Azure Cost Management APIs and budget alerts to stay within limits.

## Budget Allocation

| Service | Monthly Target | Tier |
|---------|---------------|------|
| Azure OpenAI (GPT-4o-mini) | ~80€ | Pay-as-you-go |
| Azure AI Search | ~60€ | Basic (1 unit) |
| Azure Cosmos DB | ~30€ | Serverless |
| Azure Functions | ~15€ | Consumption |
| Azure Content Safety | ~10€ | S0 |
| Application Insights | ~5€ | Pay-as-you-go |
| Azure Key Vault | ~2€ | Standard |
| **Buffer/Growth** | ~98€ | — |
| **Total** | **300€** | — |

## Patterns

### Budget Alerts

- Set alerts at **200€** (warning) and **270€** (critical) via Azure portal or Bicep
- Alert recipients: team email + Azure action group
- Review spending weekly during MVP development

### Cost Optimization Rules

1. **Always use serverless/consumption tiers** for MVP
2. **GPT-4o-mini is default** — only escalate to GPT-4o for program generation
3. **Cache aggressively** — exercise descriptions, form cues, standard programs
4. **Set token limits** — max 2000 tokens per AI coach response
5. **Batch AI Search queries** — prefetch context once per conversation, not per message
6. **Auto-scale to zero** — Functions Consumption plan stops billing when idle

### Monitoring Commands

```bash
# Check current month spend
az cost management query --type ActualCost --timeframe MonthToDate

# List top cost drivers
az cost management query --type ActualCost --timeframe MonthToDate --grouping-dimensions ServiceName

# Check resource group costs
az cost management query --scope /subscriptions/{sub-id}/resourceGroups/rg-gymbro-prod
```

### Scaling Thresholds

| Metric | Action |
|--------|--------|
| AI coach requests > 5K/day | Evaluate caching strategy |
| Cosmos DB RU > 1000/s sustained | Consider provisioned throughput |
| Functions cold starts > 2s | Consider Premium plan |
| Search queries > 50/s | Consider Standard tier |
| Monthly spend > 250€ | Review and optimize |

## Anti-Patterns

- ❌ Provisioning Standard/Premium tiers before validating MVP load
- ❌ Ignoring budget alerts ("we'll optimize later")
- ❌ Using GPT-4o for all queries (3-5x more expensive than mini)
- ❌ Not setting token limits on AI responses (runaway costs)
- ❌ Keeping dev resources running 24/7 (use auto-shutdown)
- ❌ Deploying to multiple regions before needed (doubles all costs)
