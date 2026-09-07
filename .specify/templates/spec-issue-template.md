# Issue Resolution Specification: [ISSUE TITLE]

**Feature Branch**: `[###-issue-name]`

**Created**: [DATE]

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#NNNNN or link, if any]

**Input**: User description: "$ARGUMENTS"

<!--
  This is the dotCMS ISSUE-RESOLUTION spec (used by /speckit-specify-fix). Unlike the
  feature spec, it is framed around a defect: what is wrong, how to reproduce it, and how
  we will know it is fixed. It still flows into /speckit-plan, where the Legacy Impact and
  ADR Alignment gates apply. Keep this technology-light — root-cause and fix details are
  refined in the plan.
-->

## Problem Statement *(mandatory)*

[Plain-language description of what is broken and the impact on users. What is happening that
should not, or not happening that should?]

**Severity / Impact**: [Who is affected, how badly, how often]

## Reproduction *(mandatory)*

**Environment**: [Version / build, browser or server, tenant/site, relevant config]

**Steps to Reproduce**:

1. [Step]
2. [Step]
3. [Step]

**Expected Behavior**: [What should happen]

**Actual Behavior**: [What actually happens — include error messages / IDs if known]

**Reproducibility**: [Always / intermittent / specific data or state required]

## Scope of Investigation *(mandatory)*

<!--
  Keep to WHAT is affected, not the code-level fix (that is the plan's job). But DO name the
  product area, since dotCMS mixes modern and legacy surfaces — this drives Legacy Impact in
  the plan.
-->

- **Affected area**: [Which dotCMS capability/subsystem — e.g. content editing, page render,
  search, workflows, REST API, admin UI]
- **Suspected surface**: [Modern (`com.dotcms.*`) vs legacy (`com.dotmarketing.*`) — best
  current understanding; confirmed during planning]
- **Related known decisions**: [Any ADR or documented behavior this fix must respect? The
  plan formally consults `dotCMS/platform-adrs`.]

## Root-Cause Hypothesis

[Current best hypothesis for the underlying cause. Mark unknowns with
[NEEDS CLARIFICATION: question] — max 3. The plan phase confirms or replaces this.]

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- [What this change will fix]

**Explicitly out of scope / non-goals**:

- [Related problems this will NOT address — avoids scope creep and unintended legacy rewrites]

## Regression Risk *(mandatory)*

- **Blast radius**: [Other features/areas that share this code path and could regress]
- **Backward compatibility**: [Content, APIs, serialized state, DB/ES mappings that must keep
  working — flag rollback-unsafe changes]
- **Data considerations**: [Migration/repair of existing bad data, if any]

## Acceptance & Verification *(mandatory)*

<!-- Measurable, so the fix is provably done. -->

- **AC-001**: The reproduction steps above no longer produce the actual behavior; they produce
  the expected behavior.
- **AC-002**: [Regression check for the identified blast radius]
- **Verification method**: [Specific test(s) to add/run — e.g. integration
  `-Dit.test=SomeTest#method`, Postman collection, Jest/Spectator spec, or manual steps]

## Assumptions

- [Assumptions about environment, data, or scope made where the report was ambiguous]
