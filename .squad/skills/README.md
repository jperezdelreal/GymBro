# GymBro Team Skills

Skills train agents on project-specific patterns, guardrails, and workflows. Each skill lives in its own directory with a `SKILL.md` file.

## Installed Skills

### Built-in (from Squad templates)

| Skill | Domain | Why |
|-------|--------|-----|
| `windows-compatibility` | Platform | Team develops on Windows — path handling, git commands, timestamps |
| `git-workflow` | Version Control | Branch model (dev-first), worktrees, PR conventions |
| `test-discipline` | Quality | API changes require test updates in same commit |
| `secret-handling` | Security | Never read `.env`, never write secrets to `.squad/` files |
| `docs-standards` | Documentation | Microsoft Style Guide, sentence-case, active voice |
| `reviewer-protocol` | Orchestration | Strict lockout on rejection — original author can't self-revise |

### Custom (project-specific)

| Skill | Domain | Why |
|-------|--------|-----|
| `project-conventions` | Conventions | GymBro Swift/SwiftUI/MVVM patterns, file structure, naming, performance budgets |

### External — Azure (adapted from microsoft/azure-skills)

| Skill | Domain | Why |
|-------|--------|-----|
| `azure-ai-services` | Cloud AI | OpenAI proxy, AI Search RAG, Content Safety for AI coach |
| `azure-deploy` | Deployment | `azd` workflow, Bicep IaC, CI/CD, environment strategy |
| `azure-cost-management` | Cost | 300€/month budget allocation, alerts, optimization rules |

### External — Community (adapted from github/awesome-copilot)

| Skill | Domain | Why |
|-------|--------|-----|
| `apple-appstore-review` | iOS Compliance | HealthKit permissions, IAP, privacy manifests, AI disclaimers |
| `ai-prompt-safety` | AI Safety | Training advice safety, injury risk, medical boundaries |
| `security-review` | Security | iOS + Azure attack surface, HealthKit data protection, prompt injection |

## Adding New Skills

1. Create a directory under `.squad/skills/{skill-name}/`
2. Add a `SKILL.md` file with frontmatter (name, description, domain, confidence, source)
3. Include: Context, Patterns, Examples, Anti-Patterns sections
4. Update this README

## Skill Sources

- **Squad templates:** `.squad/templates/skills/` — copy to `.squad/skills/`
- **Azure skills:** [microsoft/azure-skills](https://github.com/microsoft/azure-skills) — adapt for project
- **Community skills:** [github/awesome-copilot](https://github.com/github/awesome-copilot) — adapt for project
