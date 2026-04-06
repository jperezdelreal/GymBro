---
name: "security-review"
description: "Security review patterns for GymBro iOS app and Azure backend"
domain: "security"
confidence: "high"
source: "adapted from github/awesome-copilot (security-review skill)"
---

## Context

GymBro handles sensitive health data (HealthKit), payment information (StoreKit), and user credentials (Sign in with Apple JWT). The app has both an iOS client and Azure backend. Security review must cover both attack surfaces.

## GymBro-Specific Security Scope

### iOS Client

| Area | What to Check |
|------|---------------|
| HealthKit data | Encrypted at rest, not logged, not sent to analytics |
| Keychain usage | JWT tokens stored in Keychain, not UserDefaults |
| Network security | TLS 1.2+ enforced, certificate pinning for backend API |
| Local data | SwiftData encryption enabled, no sensitive data in logs |
| StoreKit | Receipt validation, no client-side purchase unlocking |

### Azure Backend

| Area | What to Check |
|------|---------------|
| API authentication | JWT validation on every endpoint, token expiry checked |
| Azure OpenAI proxy | Rate limiting, input sanitization, no prompt injection |
| Cosmos DB | Firewall rules, managed identity auth (no connection strings) |
| Key Vault | All secrets in Key Vault, no env vars with credentials |
| Functions | HTTPS-only, CORS restricted to app bundle ID |

### AI-Specific Security

| Area | What to Check |
|------|---------------|
| Prompt injection | User input sanitized before passing to LLM |
| Data exfiltration | AI responses don't leak system prompts or other user data |
| Token limits | Max input/output tokens enforced to prevent abuse |
| Conversation isolation | User A's conversation never visible to User B |

## Patterns

### Security Review Trigger Points

Run security review when:
- Any new API endpoint is added
- Authentication or authorization logic changes
- AI prompt system instructions change
- New Azure resources are provisioned
- Dependencies are updated
- Data model changes affect HealthKit or user PII

### Review Severity Levels

| Level | GymBro Examples |
|-------|----------------|
| 🔴 CRITICAL | JWT validation bypass, HealthKit data exposed, prompt injection enabling data leak |
| 🟠 HIGH | Missing rate limiting on AI proxy, hardcoded API key, CORS misconfiguration |
| 🟡 MEDIUM | Verbose error messages exposing internals, missing input validation |
| 🔵 LOW | Missing security headers, outdated dependency (no known CVE) |

### Mandatory Security Checks (Pre-Release)

- [ ] No secrets in source code (use secret-handling skill)
- [ ] All API endpoints require authentication
- [ ] Rate limiting on AI coach proxy (per-user, per-minute)
- [ ] Input sanitization on all user-provided text sent to LLM
- [ ] HealthKit data never leaves device except via user-initiated export
- [ ] CloudKit data encrypted end-to-end
- [ ] No `print()` or `NSLog()` of sensitive data in release builds
- [ ] App Transport Security (ATS) enabled, no exceptions
- [ ] Certificate pinning for backend API domain

## Anti-Patterns

- ❌ Storing JWT tokens in UserDefaults (use Keychain)
- ❌ Logging HealthKit data for debugging
- ❌ Passing raw user input to Azure OpenAI without sanitization
- ❌ Using connection strings instead of managed identity for Azure services
- ❌ Disabling ATS for development and forgetting to re-enable
- ❌ Client-side subscription validation without server verification
