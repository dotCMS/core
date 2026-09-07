# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`

**Created**: [DATE]

**Status**: Draft

**Type**: New Feature

**Input**: User description: "$ARGUMENTS"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
-->

### User Story 1 - [Brief Title] (Priority: P1)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [How this can be tested independently and the value it delivers]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]
2. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 2 - [Brief Title] (Priority: P2)

[Describe this user journey in plain language]

**Why this priority**: [Explain the value and why it has this priority level]

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- What happens when [boundary condition]?
- How does system handle [error scenario]?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific capability]
- **FR-002**: System MUST [specific capability]
- **FR-003**: Users MUST be able to [key interaction]

*Mark ambiguities with [NEEDS CLARIFICATION: question] — max 3.*

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: [What it represents, key attributes without implementation]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Measurable, technology-agnostic outcome]
- **SC-002**: [Measurable, technology-agnostic outcome]

## Legacy Considerations *(dotCMS-specific — mandatory)*

<!--
  Kept technology-light on purpose: name the AREA of the product, not the implementation.
  The plan phase (Legacy Impact + ADR Alignment) does the technical depth.
-->

- **Existing behavior touched**: [Which existing dotCMS capability/area does this change or
  extend? Is that area part of the older/legacy product surface?]
- **Backward-compatibility expectations**: [Must existing content, APIs, or admin workflows
  keep working unchanged? Any deprecations intended?]
- **Known related decisions**: [Any ADRs, prior features, or long-standing behavior the
  reader should know this must respect? The plan will formally consult `dotCMS/platform-adrs`.]

## Assumptions

- [Assumption about users / scope boundaries / dependencies on existing systems]
