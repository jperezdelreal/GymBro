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

### External — iOS Development (from dpearson2699/swift-ios-skills)

| Skill | Domain | Why |
|-------|--------|-----|
| `healthkit` | Health Data | Read/write Apple Health data — essential for gym tracking app; covers authorization, queries, statistics, background delivery, workout sessions |
| `swiftdata` | Data Persistence | Core data persistence layer for GymBro models — queries, relationships, SwiftUI integration |
| `coreml` | On-Device AI/ML | On-device machine learning inference for exercise form analysis, personalization, recommendations |
| `swiftui-patterns` | UI Architecture | SwiftUI patterns and best practices for composable, reusable, testable UI components |
| `swiftui-gestures` | User Input | Gesture handling and detection — essential for 1-tap exercise logging, drag-to-reorder workout lists |
| `swift-concurrency` | Async Programming | async/await patterns and structured concurrency for non-blocking operations (data sync, API calls) |
| `background-processing` | Background Tasks | Background task scheduling and processing for passive workout tracking, data sync, notifications |
| `swift-testing` | Testing Framework | Swift Testing framework (macOS 15+, iOS 18+) — modern test assertion and organization |

### External — Swift Advanced (HIGH priority from twostraws + community)

| Skill | Domain | Why | Source |
|-------|--------|-----|--------|
| `swiftui-pro` | UI Mastery | Advanced SwiftUI patterns, performance optimization, complex state management, custom modifiers | twostraws/SwiftUI-Agent-Skill |
| `swiftdata-pro` | Data Pro | Advanced SwiftData schema design, migrations, complex queries, relationships, performance tuning | twostraws/SwiftData-Agent-Skill |
| `swift-concurrency-pro` | Concurrency Pro | Structured concurrency patterns, actor models, MainActor, priority levels, task cancellation strategies | twostraws/Swift-Concurrency-Agent-Skill |
| `swift-testing-pro` | Testing Pro | Advanced Swift Testing framework patterns, parameterized tests, custom assertions, test organization at scale | twostraws/Swift-Testing-Agent-Skill |
| `ios-accessibility` | A11y Standards | WCAG compliance for iOS, VoiceOver support, accessibility testing, dynamic type, color contrast | dadederk/iOS-Accessibility-Agent-Skill |
| `swiftui-performance-audit` | Performance | Performance profiling, View hierarchy optimization, memory leak detection, rendering performance | Dimillian/Skills |

## Adding New Skills

1. Create a directory under `.squad/skills/{skill-name}/`
2. Add a `SKILL.md` file with frontmatter (name, description, domain, confidence, source)
3. Include: Context, Patterns, Examples, Anti-Patterns sections
4. Update this README

## Skill Sources

- **Squad templates:** `.squad/templates/skills/` — copy to `.squad/skills/`
- **Azure skills:** [microsoft/azure-skills](https://github.com/microsoft/azure-skills) — adapt for project
- **Community skills:** [github/awesome-copilot](https://github.com/github/awesome-copilot) — adapt for project
- **Swift Community (HIGH priority):**
  - [twostraws/SwiftUI-Agent-Skill](https://github.com/twostraws/SwiftUI-Agent-Skill) → `swiftui-pro`
  - [twostraws/SwiftData-Agent-Skill](https://github.com/twostraws/SwiftData-Agent-Skill) → `swiftdata-pro`
  - [twostraws/Swift-Concurrency-Agent-Skill](https://github.com/twostraws/Swift-Concurrency-Agent-Skill) → `swift-concurrency-pro`
  - [twostraws/Swift-Testing-Agent-Skill](https://github.com/twostraws/Swift-Testing-Agent-Skill) → `swift-testing-pro`
  - [dadederk/iOS-Accessibility-Agent-Skill](https://github.com/dadederk/iOS-Accessibility-Agent-Skill) → `ios-accessibility`
  - [Dimillian/Skills](https://github.com/Dimillian/Skills) → `swiftui-performance-audit`
