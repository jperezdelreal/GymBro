# Switch Quality Audit — Phase 1+2 Code Review

**Author:** Switch (QA)  
**Date:** 2026-04-07  
**Scope:** All Swift code on master — GymBroCore, GymBroUI, App entry

## Executive Summary

The codebase has solid foundations in the tested areas (plateau detection, E1RM, progress tracking, safety filter) but has **critical gaps** in concurrency safety, test coverage, and edge case handling. **78% of source files have zero tests.** Two of three @Observable ViewModels lack @MainActor, creating data race risks on every AI chat interaction and workout session.

## Issues Filed

| # | Title | Priority | Labels |
|---|-------|----------|--------|
| #25 | Critical test coverage gaps — 78% untested | 🔴 CRITICAL | squad, mvp, testing |
| #26 | Concurrency safety violations — missing @MainActor, data races | 🔴 CRITICAL | squad, mvp, testing |
| #27 | Test quality issues — flaky timers, no mocking, missing edge cases | 🟡 HIGH | squad, mvp, testing |
| #28 | SwiftUI performance risks — heavy body computation, oversized views | 🟡 HIGH | squad, mvp, testing, frontend |
| #29 | Code smells — force unwraps, inconsistent access control | 🟠 MEDIUM | squad, mvp, testing |
| #30 | Edge cases & robustness — nil crashes, missing empty states | 🟡 HIGH | squad, mvp, testing |

## Key Metrics

- **Test coverage:** 22% of files (11/50)
- **Concurrency issues:** 6 findings (2 critical)
- **Performance risks:** 6 findings
- **Code smells:** 6 findings
- **Edge case gaps:** 7 findings

## Top 3 Blockers for MVP Ship

1. **@MainActor on CoachChatViewModel and ActiveWorkoutViewModel** — data races will cause random crashes in production
2. **PersonalRecordService force unwrap in predicate (line 68)** — will crash on any set with nil completedAt
3. **Test coverage for SmartDefaultsService and PersonalRecordService** — core features with zero verification

## Recommendation

Do NOT ship MVP without fixing #26 (concurrency) and the PersonalRecordService crash in #30. Test coverage (#25) should reach 60%+ before beta.
