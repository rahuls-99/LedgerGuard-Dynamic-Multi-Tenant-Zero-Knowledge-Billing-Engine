# LedgerGuard - Mid-Project Review



**Prepared by:** Rachana

**Project:** LedgerGuard - Dynamic Multi-Tenant Zero-Knowledge Billing Engine

**Review:** Mid-Project Review

**Document status:** Draft - ready for review preparation

**Last updated:** 26 November 2026



---



## 1. Scope - Promised vs Done



This section tracks the promised Week 1 and Week 2 work across the frontend and backend.



The complete original project report is required to populate every promised item. The following entries are based on currently available evidence.



| Area | Work item | Week | Status | Evidence |

|---|---|---|---|---|

| Frontend | UI polish and accessibility improvements | Week 2 / Day 13 | Done | \[ACCESSIBILITY\_REPORT.md](ACCESSIBILITY\_REPORT.md) |

| Frontend | Vitest component and hook tests | Week 2 / Day 13 | Done | 12 test files, 90 tests passed |

| Frontend | Production build | Week 2 / Day 13 | Done | Vite production build passed |

| Backend | Tenant isolation audit | Week 2 | Partly verified | \[ISOLATION\_AUDIT.md](ISOLATION\_AUDIT.md) |

| Backend | Crash recovery evidence | Week 2 | Partly verified | \[Crash evidence](evidence/) |

| Frontend | UI state validation | Week 2 | Initial validation evidence received; additional states pending | docs/UI_STATE_MATRIX.md and four screenshots |

| Documentation | Consolidated bug tracker | Week 2 / Day 14 | In progress | \[BUGS.md](BUGS.md) |

| Documentation | Week 2 screenshot pack | Week 2 / Day 14 | In progress | \[Screenshot index](screenshots/week2/INDEX.md) |

| Documentation | Mid-project review | Week 2 / Day 14 | In progress | This document |



**Note:** Other Week 1 and Week 2 commitments must be added from the original project report before the final review.



## 2. What Is Demonstrated



The following results are supported by the evidence currently available.



| Demonstration | Expected result | Observed result | Status |

|---|---|---|---|

| Frontend component tests | All migrated tests pass | 12 files, 90 tests passed | Passed |

| Frontend production build | TypeScript compilation and Vite build succeed | 103 modules transformed; build successful | Passed |

| Backend test execution | Tests complete without failures | 29 tests, 0 failures, 0 errors, 0 skipped | Passed |

| Tenant context cleanup | TenantContext cleared after requests | TenantContext=null recorded | Passed in audit |

| Cross-tenant wallet lookup | Other tenant's wallet inaccessible | HTTP 404 recorded | Passed |

| AFTER\_DEBIT crash recovery | Rolled-back transaction does not move money | ROLLED\_BACK, balance 1000.00, zero ledger entries | Observed |

| AFTER\_PENDING\_INSERT crash simulation | Real process crash at the requested point | Request returned HTTP 200 SUCCESS instead | Needs investigation |



### 2.1 Test numbers



**Frontend test results**



- Test files passed: 12

- Individual tests passed: 90

- Failed tests: 0

- Production build: Passed



**Soumili's additional UI validation results**

Source: UI_STATE_MATRIX.md, supplied by Soumili on 26 November 2026.

- Playwright: 5/5 tests passed.
- Vitest: 90/90 tests passed across 12 test files.
- TypeScript check: Passed.
- Production build: Successful.
- Vite modules transformed: 103.
- Confirmed UI defects: None in the tested scope.

Validated UI states include login, password visibility, empty-form validation, authenticated dashboard, user details, navigation and logout.

Four supplementary screenshots were received and copied into `screenshots/week2/`:

- 01-login-page.png
- 02-password-visible.png
- 03-dashboard-page.png
- 04-logout-redirect.png

The following remain unvalidated: real backend authentication, expired JWT, 403 access, full Ledger and Analytics interactions, responsive layouts and cross-browser testing.

One successful Playwright run is reported. Results across three repeated runs remain pending.

**Backend test results supplied**



- Tests run: 29

- Failures: 0

- Errors: 0

- Skipped: 0

- Build result: SUCCESS

- Reported build duration: 25.616 seconds



**Numbers still pending**



- JaCoCo coverage of ledger and security packages.

- Five repeated concurrency-suite results.

- Playwright test results across three runs.

- Lighthouse accessibility scores.

- Final bug counts by severity.



These values must be added only after the corresponding reports are available.



## 3. Technology as Implemented



The project report describes one technology stack, while the current implementation uses another. This mapping is provided to avoid confusion during the review.



| Report says | Implemented as |

|---|---|

| Express + JWT gateway | Spring Boot + Spring Security (JWT) |

| Mongoose dynamic pool | JPA/Hibernate with per-tenant routing and connection pools |

| MongoDB ACID transactions | MySQL InnoDB transactions with row locks |

| Redis distributed locks | Redis locks |

| Socket.io (later) | WebSocket or SSE (Week 3) |



**Frontend:** React and TypeScript.



**Backend:** Spring Boot, Java 17, Spring Security and JPA/Hibernate.



**Database:** MySQL with separate tenant databases and connection pools.



**Caching and locking:** Redis.



## 4. Known Limitations



The following limitations are recorded in the existing tenant isolation audit. The final version must be checked against Rahul's `docs/KNOWN\_LIMITATIONS.md` and copied verbatim from that document as instructed.



1\. **Application-layer database isolation:** Isolation is enforced by the application routing layer (`TenantRoutingDataSource` / per-tenant Hikari pools). The application's MySQL account



 (`root` in local/demo) can technically read every tenant database (`db_tenant_alpha`, `db_tenant_beta`). A stronger design is one MySQL user per tenant with restricted GRANT permissions.

2. **Single Redis node and in-memory rate limiting:** The local/demo environment uses a single Redis instance and in-memory Bucket4j rate-limiting buckets.

The final wording must be checked against Rahul's KNOWN_LIMITATIONS.md and copied verbatim when that file becomes available.

## 5. Risks and Decisions Needed

| Risk / Decision | Evidence | Decision needed |
|---|---|---|
| Cross-tenant wallet charge discrepancy | ISOLATION_AUDIT.md records HTTP 201 but marks PASS despite expecting 404 | Rahul to verify |
| Missing isolation audit rows | Rows 6 and 8 are absent from the displayed report | Confirm whether tests were executed |
| AFTER_PENDING_INSERT crash | HTTP 200 SUCCESS instead of expected connection reset | Rahul to investigate |
| AFTER_DEBIT response contract | HTTP 422 ROLLED_BACK observed | Confirm expected API contract |
| Wallet ID truncation | Backend log reports Data too long for column 'id' | Investigate schema and input |
| Accessibility validation | Several checks remain unverified | Complete outstanding checks |
| Week 3 scope | Original 28-day plan not yet available | Confirm with team |

## 6. Week 3 Plan

The original 28-day project plan is required to complete this section.

- Frontend commitments: Pending confirmation.
- Backend commitments: Pending confirmation.
- Real-time updates: Confirm planned WebSocket or SSE scope.
- Testing and reliability milestones: Pending confirmation.
- Owners and target dates: Pending team decision.

Do not treat suggested work as an approved commitment until confirmed.

## 7. Review Links and Evidence

| Document | Purpose |
|---|---|
| BUGS.md | Consolidated bug tracker |
| ACCESSIBILITY_REPORT.md | Day 13 accessibility findings |
| ISOLATION_AUDIT.md | Tenant isolation evidence |
| evidence/ | Backend test and crash evidence |
| screenshots/week2/INDEX.md | Screenshot register |
| UI_STATE_MATRIX.md | Soumili's UI state validation report |
| screenshots/week2/01-login-page.png | Login screenshot |
| screenshots/week2/02-password-visible.png | Password visibility screenshot |
| screenshots/week2/03-dashboard-page.png | Dashboard screenshot |
| screenshots/week2/04-logout-redirect.png | Logout screenshot |
| API_CONTRACT.md | API contract reference |
| LEDGER_SCHEMA.md | Database schema |
| LOCK_DESIGN.md | Lock design |

## 8. Live Review Notes

**Meeting date:** Pending
**Meeting time:** Pending
**Participants:** Rahul, Soumili and Rachana

### Questions asked

| Question | Asked by | Answer |
|---|---|---|
| Pending review discussion | Pending | Pending |

### Answers received

Record actual responses during the meeting.

### Decisions made

| Decision ID | Decision | Agreed by | Date |
|---|---|---|---|
| DEC-001 | Pending review | Pending | Pending |

### Action items

| Action ID | Action | Owner | Target date | Status |
|---|---|---|---|---|
| ACT-001 | Verify isolation audit discrepancy | Pending | Pending | Pending |
| ACT-002 | Verify crash simulation results | Pending | Pending | Pending |
| ACT-003 | Incorporate UI validation findings | Pending | Pending | Pending |
| ACT-004 | Finalize consolidated bug tracker | Rachana | Pending | In progress |

After the meeting, copy relevant action items into BUGS.md.

---

# Appendix A - Scribe Facilitation Guide

## Before the meeting

- Review BUGS.md and available evidence.
- Prepare the promised-versus-done table.
- Check verified test results.
- Open the relevant screenshots and evidence documents.
- Ensure credentials, passwords and real tokens are not visible.

## During the meeting

1. Confirm the meeting scope and participants.
2. Present completed work with evidence.
3. Record unfinished and partially completed items.
4. Ask for clarification when reported results disagree with evidence.
5. Record questions and answers.
6. Record decisions, owners and target dates.
7. Confirm Blocker and High severity issues.
8. Read back action items before closing.

## After the meeting

- Update MID_PROJECT_REVIEW.md.
- Update BUGS.md with confirmed findings.
- Record action owners and target dates.
- Include evidence links.
- Preserve unresolved items as pending.
- Publish the final version within two hours after the review.

---

**Document status:** Draft prepared before the mid-project review. Final decisions, Week 3 commitments and meeting notes remain pending.
