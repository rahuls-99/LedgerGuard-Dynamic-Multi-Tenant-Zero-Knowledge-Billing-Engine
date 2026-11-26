

# LedgerGuard - Consolidated Bug Tracker



**Project:** LedgerGuard - Dynamic Multi-Tenant Zero-Knowledge Billing Engine

**Prepared by:** Rachana

**Review:** Day 14 - Mid-Project Review

**Status:** Working draft - pending team triage

**Last updated:** 26 November 2026



---



## 1. Severity definitions



| Severity | Meaning | Rule |

|---|---|---|

| Blocker | Money is incorrect, tenant data leaks, or data is lost | Fix before the review |

| High | A main flow is broken or inaccessible | Fix before Day 15 |

| Medium | Incorrect behaviour with a workaround | Schedule for Day 21 buffer |

| Low | Cosmetic or minor issue | Backlog |



Severity assignments marked provisional must be confirmed during team triage.



## 2. Consolidated bug list



| ID | Area | Title | Steps to reproduce | Expected | Actual | Severity | Found by | Evidence | Owner | Status | Target day |

|---|---|---|---|---|---|---|---|---|---|---|---|

| BUG-001 | Security | Cross-tenant wallet charge audit discrepancy | Use an orgA token and attempt a charge with an orgB wallet ID | 404 WALLET\_NOT\_FOUND; no row created | ISOLATION\_AUDIT.md records HTTP 201 but marks PASS | Blocker (provisional) | Rahul's isolation audit | docs/ISOLATION\_AUDIT.md | Pending team confirmation | Needs verification | Before review |

| BUG-002 | Security | Tenant isolation audit cases missing from report | Review the generated isolation audit table | All required attack-matrix cases documented | Test cases 6 (shared email login) and 8 (cross-tenant history search) are absent from the displayed table | Medium | Rachana, document review | docs/ISOLATION\_AUDIT.md | Pending team confirmation | Needs evidence | Day 14 |

| BUG-003 | API / Demo | AFTER\_PENDING\_INSERT crash injection not demonstrated | Run scripts/demo/crash-test.sh for AFTER\_PENDING\_INSERT and inspect the request result | JVM should halt at the armed crash point | Evidence shows HTTP 200 SUCCESS and CURL\_EXIT 0 instead of a connection reset | Medium | Rahul's crash evidence | docs/evidence/crash-AFTER\_PENDING\_INSERT-20261122-203733.txt | Pending team confirmation | Needs investigation | Day 14 |

| BUG-004 | API | AFTER\_DEBIT rollback response differs from task specification | Arm AFTER\_DEBIT:CRASH, restart backend and retry the same idempotency key | Expected rollback response according to the Day 14 specification | HTTP 409 initially, followed by HTTP 422 ROLLED\_BACK with STUCK\_PENDING\_RECOVERED | Medium | Rahul's crash evidence | docs/evidence/crash-AFTER\_DEBIT-20261122-204633.txt | Pending team confirmation | Needs API contract verification | Day 14 |

| BUG-005 | DB / API | Wallet ID data truncation reported during wallet creation | Reproduce the wallet creation request associated with the backend log | Invalid input should receive a clear validation response | MySQL reports Data too long for column 'id'; the request returns HTTP 409 | Medium (provisional) | Rahul's backend log | docs/evidence/backend-demo.log | Pending team confirmation | Needs investigation | Day 14 |



**Important:** BUG-001 is a potential security blocker, not yet a confirmed tenant data leak. Rahul must verify the exact token, wallet ID and database used.



BUG-005 also needs confirmation that the request was not an intentional invalid-input test.



## 3. Accessibility findings - Day 13 history



Source: docs/ACCESSIBILITY\_REPORT.md



The following issues were recorded as fixed or already present:



| Page | Finding | Previous status |

|---|---|---|

| Login | Email autocomplete | Fixed |

| Login | Password autocomplete | Fixed |

| Login | Accessible required-field indicators | Fixed |

| Login | Screen-reader login error announcements | Fixed |

| Login | Loading status announcements | Fixed |

| Login | Visible keyboard focus | Fixed |

| Dashboard | Accessible navigation label | Fixed |

| Ledger | Loading announcements | Fixed |

| Ledger | Accessible error alerts | Fixed |

| Ledger | Long invoice data wrapping on narrow screens | Fixed |

| Application | HTML language attribute | Already present |

| Application | Descriptive browser title | Fixed |



These are resolved historical findings and should not be counted as open bugs.



## 4. Accessibility validation still pending



The existing accessibility report does not confirm completion of these checks:



| ID | Validation item | Status |

|---|---|---|

| A11Y-001 | Full keyboard navigation | Pending |

| A11Y-002 | Screen-reader testing | Pending |

| A11Y-003 | 200% zoom | Pending |

| A11Y-004 | 320px viewport | Pending |

| A11Y-005 | Color contrast audit | Pending |

| A11Y-006 | Reduced-motion audit | Pending |

| A11Y-007 | Forced-colors audit | Pending |

| A11Y-008 | Automated axe/Lighthouse audit | Pending |

| A11Y-009 | Component accessibility tests | Pending |



These are validation gaps, not confirmed failures.



## 5. Test evidence collected



### Frontend



| Check | Result |

|---|---|

| Vitest test files | 12 passed |

| Individual tests | 90 passed |

| Frontend production build | Passed |



### Backend evidence supplied



| Check | Result |

|---|---|

| Maven tests run | 29 |

| Failures | 0 |

| Errors | 0 |

| Skipped | 0 |

| Maven build | SUCCESS |

| Total build time | 25.616 seconds |



Source: docs/evidence/test-summary.txt and uploaded backend test output.



JaCoCo package coverage and five repeated concurrency-suite results have not yet been confirmed from the available evidence.



## 6. Other team findings pending



### Soumili - Frontend validation



Awaiting:

\- docs/UI\_STATE\_MATRIX.md

\- UI validation failures

\- Screenshots

\- Playwright test results

\- Real-backend transaction-state results

\- Lost-response and rollback test findings



### Rahul - Backend validation



Awaiting confirmation of:

\- Cross-tenant charge discrepancy (BUG-001)

\- Missing audit rows 6 and 8

\- AFTER\_PENDING\_INSERT crash injection

\- AFTER\_DEBIT response contract

\- Wallet ID truncation issue

\- Remaining crash evidence and concurrency results



Add confirmed findings to this tracker after reviewing their evidence.



## 7. Known limitations



The existing isolation audit records these limitations:



1\. Tenant isolation is enforced at the application routing layer.

2\. The application's MySQL account can technically read multiple tenant databases.

3\. Database-level per-tenant GRANT hardening remains future work.

4\. The local/demo environment uses a single Redis node.

5\. Rate-limiting buckets are held in memory.



These are documented architectural limitations, not automatically classified as bugs.



## 8. Bug summary



### Counts by severity



| Severity | Current provisional count |

|---|---:|

| Blocker | 1 |

| High | 0 |

| Medium | 4 |

| Low | 0 |

| Total | 5 |



Counts must be updated following team triage.



### Counts by status



| Status | Count |

|---|---:|

| Needs verification / investigation | 5 |

| Confirmed open | 0 |

| Fixed | 0 |

| Total | 5 |



### Top five risks requiring review



1\. Possible cross-tenant wallet charge acceptance.

2\. Missing tenant isolation attack-matrix evidence.

3\. AFTER\_PENDING\_INSERT crash simulation not triggering.

4\. AFTER\_DEBIT rollback response contract mismatch.

5\. Wallet ID database truncation error.



## 9. Duplicate-check checklist



Before adding a new bug:



\- \[ ] Check whether the same issue already exists.

\- \[ ] Compare the affected page or API endpoint.

\- \[ ] Compare reproduction steps and expected behaviour.

\- \[ ] Compare actual response codes and error messages.

\- \[ ] Check whether the issue is already fixed.

\- \[ ] Attach evidence such as screenshot, test output or log.

\- \[ ] Keep related issues separate when they have different root causes.

\- \[ ] Link duplicate reports instead of creating another ID.



## 10. Team triage notes



**Participants:** Rachana, Rahul and Soumili

**Meeting date:** Pending confirmation



| Bug ID | Confirmed severity | Owner | Target day | Decision | Notes |

|---|---|---|---|---|---|

| BUG-001 | Pending | Pending | Pending | Pending | |
| BUG-002 | Pending | Pending | Pending | Pending | Verify missing audit cases |
| BUG-003 | Pending | Rahul - confirmation needed | Pending | Pending | Verify crash injection |
| BUG-004 | Pending | Rahul - confirmation needed | Pending | Pending | Confirm API response contract |
| BUG-005 | Pending | Rahul - confirmation needed | Pending | Pending | Investigate wallet ID truncation |







Any issue marked Won't Fix must include a one-line reason.



---



**Document status:** Initial consolidated draft. Final bug count, ownership, severity and target dates must be confirmed with Rahul and Soumili.
