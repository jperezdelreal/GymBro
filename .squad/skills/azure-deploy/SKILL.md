---
name: "azure-deploy"
description: "Azure deployment workflow for GymBro backend services"
domain: "cloud-deployment"
confidence: "high"
source: "adapted from microsoft/azure-skills (azure-deploy, azure-prepare skills)"
---

## Context

GymBro's backend (AI coach proxy, user data API) deploys to Azure. The team uses Azure Developer CLI (`azd`) for infrastructure-as-code and deployment. All infrastructure is defined in Bicep files under `infra/`.

## Deployment Architecture

```
GymBro Azure Resources (West Europe)
├── Resource Group: rg-gymbro-{env}
├── Azure Functions App (AI proxy)
├── Azure Cosmos DB (serverless, user data)
├── Azure OpenAI Service
├── Azure AI Search (Basic tier)
├── Azure Content Safety
├── Azure Key Vault (secrets)
└── Application Insights (monitoring)
```

## Patterns

### Deployment Pipeline

1. **Prepare:** `azd init` → generates `azure.yaml` + `infra/` Bicep files
2. **Validate:** `azd provision --preview` → dry-run, check costs, review resources
3. **Deploy:** `azd up` → provisions infrastructure + deploys code
4. **Verify:** Check endpoints, run smoke tests, verify budget alerts

### Environment Strategy

| Environment | Purpose | Budget |
|-------------|---------|--------|
| `dev` | Development/testing | Shared within 300€/month |
| `prod` | Production | Shared within 300€/month |

- MVP uses single environment initially; split when user base grows
- Use Azure Functions Consumption plan (pay-per-execution) to minimize costs

### Infrastructure as Code (Bicep)

- All resources defined in `infra/` directory
- Use Bicep modules, not monolithic templates
- Parameterize environment name, region, SKU tiers
- Store secrets in Key Vault, reference via `@Microsoft.KeyVault` in app settings

### CI/CD Integration

- GitHub Actions workflow: `.github/workflows/deploy.yml`
- Trigger: Push to `main` branch (after PR merge from `dev`)
- Steps: `azd provision` → `azd deploy` → smoke test → notify
- Use Azure federated credentials (OIDC) — no stored secrets in GitHub

### Pre-Deploy Checklist

- [ ] `azd provision --preview` shows expected resources
- [ ] Budget alerts configured (200€, 270€)
- [ ] Key Vault contains all required secrets
- [ ] Application Insights connected
- [ ] CORS configured for iOS app domain

### Post-Deploy Verification

- [ ] Azure Functions endpoints return 200
- [ ] AI coach responds to test prompt
- [ ] Cosmos DB accepts read/write operations
- [ ] Application Insights receiving telemetry
- [ ] Budget alert emails configured

## Anti-Patterns

- ❌ Deploying without `azd provision --preview` first
- ❌ Storing Azure credentials in `.env` files committed to git
- ❌ Using portal click-ops instead of Bicep IaC
- ❌ Deploying to production without staging validation
- ❌ Skipping budget alert configuration (300€/month is a hard limit)
- ❌ Using expensive SKUs for MVP (always start with Consumption/Serverless/Basic tiers)
