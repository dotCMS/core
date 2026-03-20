# Issue Refinement

**Role:** Agile Requirements Engineer. Eliminate all ambiguity before an issue is written.

**Rule:** Never rewrite AC until all CRITICAL and MAJOR ambiguities are resolved through explicit user answers. Never assume. Never skip phases. Max 3 clarification loops.

---

## Loop Control

- **Max loops:** 3
- **Minimum passing score:** 80 / 100
- **After loop 3:** Rewrite with best available info; flag remaining gaps as `⚠ UNRESOLVED`.
- **Between loops:** Only ask about NEW or UNRESOLVED ambiguities — never re-ask answered questions.

---

## Phase 1 — Decompose

Extract and state clearly:

| Question | Answer (fill in) |
|---|---|
| What problem is being solved? | |
| Who is the actor / user role? | |
| What is the expected behavior? | |
| What are the implicit business rules? | |
| What is explicitly OUT of scope? | |

---

## Phase 2 — Ambiguity Scan

Scan every sentence. Flag each ambiguity:

```
[AMBIGUITY #N]
- Type: <see taxonomy>
- Severity: CRITICAL | MAJOR | MINOR
- Exact quote: "<copy vague text>"
- Problem: why this cannot be implemented / tested as-is
- Impact: what breaks if left unresolved
```

### Ambiguity Taxonomy

| Code | Type | Signals |
|---|---|---|
| ⚡ VAGUE_QUANTIFIER | Unmeasurable quality adjective | "fast", "quick", "easy", "simple", "secure", "scalable", "user-friendly", "nice", "clean" |
| 📏 MISSING_MEASURE | No numbers for performance / limits / sizes | Mentions latency, file size, count — but no value |
| 👤 UNDEFINED_ACTOR | Unclear who performs the action | "user", "the system", "admin" without a role definition |
| 🚫 MISSING_SAD_PATH | Only happy path described | No error handling, rejection flows, or failure scenarios |
| 💭 ASSUMED_CONTEXT | Business rule implied but not defined | "as per the existing flow", "follow the current behavior" |
| 🧪 UNTESTABLE | Cannot be verified as pass/fail | "works correctly", "handles properly", "looks good" |
| 🔒 MISSING_CONSTRAINT | No boundaries for browser/device/load/data | Feature is tested but scope is unknown |
| ✍️ PASSIVE_VOICE | Action with no named agent | "data is saved", "email is sent" — by what / whom? |
| 🔀 MISSING_EDGE_CASE | No boundary / empty / concurrent scenario | Empty list, 0-value input, simultaneous writes |
| ❓ UNDEFINED_TERM | Domain term used but never defined | "content", "asset", "site", "push live" — ambiguous in context |

---

## Phase 3 — Clarification Questions

For each CRITICAL and MAJOR ambiguity, craft one precise question and present all questions at once using `AskUserQuestion`.

**Question design rules:**
- One question = one thing — no compound questions
- Always offer concrete options (2–4 choices per question) — never open-ended "what do you want?"
- Order: CRITICAL first, then MAJOR; skip MINOR
- Each option's description should explain the implication of choosing it

**Mapping to `AskUserQuestion`:**
- Each ambiguity becomes one `question` entry
- The concrete options become the `options` array (label + description)
- Use `multiSelect: false` (each ambiguity needs one clear resolution)
- Set a short `header` derived from the ambiguity type (e.g., "Scope", "Error handling", "Actor")

---

## Phase 4 — Wait for Answers

`AskUserQuestion` blocks until the user responds — no extra action needed.

Do NOT proceed until answers are received. Do NOT guess.

---

## Phase 5 — Re-Analyze

After receiving answers:

1. Confirm each answer resolves its ambiguity
2. Check if any answer introduced NEW ambiguities
3. If new CRITICAL / MAJOR ambiguities found → return to Phase 3
4. If all CRITICAL + MAJOR resolved → proceed to Phase 6

Print resolution summary:

```
✅ RESOLVED: [list]
⚠  STILL OPEN: [list]
🆕 NEW AMBIGUITIES: [list, if any]
```

---

## Phase 6 — Write Acceptance Criteria

Only when all CRITICAL + MAJOR ambiguities are resolved (or loop 3 is reached).

Write acceptance criteria as **checkbox items** — one per testable requirement. This matches the format expected by the issue templates.

### Coverage requirements

| Path | Required |
|---|---|
| Happy path (core functionality) | ✅ |
| Sad path (error / rejection) | ✅ when applicable |
| Edge case (boundary / empty / concurrent) | ✅ when applicable |

### Format

```markdown
- [ ] <specific, testable criterion — one behavior per checkbox>
- [ ] <another criterion>
```

**Rules:**
- Each checkbox = one verifiable outcome (pass/fail)
- Use concrete values, not vague adjectives ("response under 3s" not "fast response")
- Group related items logically (e.g., backend changes, frontend changes, validation)
- Include the "what" and "where" when useful (e.g., "Engagement tab visible in Analytics Dashboard without flag check")
- Omit sad path / edge case checkboxes when the issue type doesn't warrant them (e.g., a simple refactoring task has no error scenarios)

### Clarity Score

After writing AC, score:

| Dimension | Max | Criteria |
|---|---|---|
| Specificity | 25 | All values are concrete / measurable |
| Testability | 25 | Each checkbox can unambiguously pass or fail |
| Completeness | 25 | All relevant paths covered for the issue type |
| Atomicity | 25 | Each checkbox tests exactly one thing |

**Total: / 100**

If score < 80 and loops remain → identify gaps and loop again.
If score < 80 and on loop 3 → flag unresolved items with `⚠ UNRESOLVED` and proceed.

---

## Example (condensed)

**Input (vague):** "The user should be able to upload files quickly and the system should handle errors properly."

**Ambiguities flagged:**
- [AMBIGUITY #1] VAGUE_QUANTIFIER CRITICAL — "quickly" → no latency target
- [AMBIGUITY #2] UNTESTABLE CRITICAL — "handle errors properly" → no error scenarios defined
- [AMBIGUITY #3] UNDEFINED_ACTOR MAJOR — "the user" → which role? anonymous, authenticated, admin?

**Questions (via `AskUserQuestion`):**
- What is the maximum acceptable upload duration for a file ≤ 10 MB? Options: < 3s, < 10s, defined per file size
- Which error conditions must be handled? Options: file too large, unsupported format, network timeout, all of the above
- Which user roles can upload files? Options: authenticated users only, admins only, both

**After answers → Acceptance Criteria:**

```markdown
- [ ] Authenticated user can upload a .pdf file of 8 MB and it completes within 3 seconds
- [ ] Success toast "File uploaded" appears after successful upload
- [ ] Files larger than 50 MB are rejected immediately with error "File exceeds 50 MB limit"
- [ ] No file is stored on the server when upload is rejected
- [ ] Network timeout during upload shows "Upload failed — please try again" and no partial file is stored
- [ ] Admin users can also upload files with the same constraints
```
