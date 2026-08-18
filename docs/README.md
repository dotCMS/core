# dotCMS Documentation Structure

## Overview
This documentation is organized into specialized sections to reduce cognitive load and improve maintainability.

## Structure

### `/docs/core/` - Universal Principles
Standards that apply to all development:
- **ARCHITECTURE_OVERVIEW.md** - System architecture and integration points
- **SECURITY_PRINCIPLES.md** - Security rules and patterns
- **PROGRESSIVE_ENHANCEMENT.md** - Legacy vs modern pattern guidance
- **[SPEC_KIT_QUICK_START.md](core/SPEC_KIT_QUICK_START.md)** - Running spec-driven development: sizing the flow, the two-PR review protocol, the TDD and ADR gates
- **[SPEC_KIT_TEAM_SESSION.md](core/SPEC_KIT_TEAM_SESSION.md)** - Run-of-show for teaching Spec-Kit to a team or new-hire cohort

### `/docs/backend/` - Java/Maven Development
Backend-specific patterns and standards:
- **JAVA_STANDARDS.md** - Java development patterns and API usage
- **MAVEN_BUILD_SYSTEM.md** - Dependency and plugin management

### `/docs/frontend/` - Angular/TypeScript Development
Frontend-specific patterns and standards. **Start at the index: [frontend/README.md](frontend/README.md)** — it lists every doc and when to load it.
- **[ANGULAR_STANDARDS.md](frontend/ANGULAR_STANDARDS.md)** - Single source of truth: components, signals, change detection, forms, icons
- **[COMPONENT_ARCHITECTURE.md](frontend/COMPONENT_ARCHITECTURE.md)** - Component structure, file layout, data flow
- **[STATE_MANAGEMENT.md](frontend/STATE_MANAGEMENT.md)** - NgRx Signal Store, rxMethod, patchState
- **[STYLING_STANDARDS.md](frontend/STYLING_STANDARDS.md)** - Tailwind, PrimeNG theme, BEM, SCSS
- **[TYPESCRIPT_STANDARDS.md](frontend/TYPESCRIPT_STANDARDS.md)** - Strict types, as const, `#` private
- **[TESTING_FRONTEND.md](frontend/TESTING_FRONTEND.md)** - Writing tests: Spectator, Jest, byTestId
- **[TESTING_REVIEW_RULES.md](frontend/TESTING_REVIEW_RULES.md)** - Reviewing tests: violation checklist
- **[BREADCRUMBS.md](frontend/BREADCRUMBS.md)** - GlobalStore breadcrumb trail

### `/docs/claude/` - AI Assistant Guidance
Workflow optimization for AI assistants:
- **WORKFLOW_PATTERNS.md** - Task patterns and information gathering
- **DOCUMENTATION_MAINTENANCE.md** - Keeping docs current and accurate

## Usage

### For Human Developers
1. Read **core principles** for universal standards
2. Read **domain-specific** docs for your area (backend/frontend)
3. Reference **architecture** for system understanding

### For AI Assistants
1. Start with **CLAUDE.md** for task routing
2. Read **core principles** for all tasks
3. Read **domain-specific** docs based on task context
4. Follow **workflow patterns** for efficient task completion
5. **Update documentation** when discovering incorrect/missing information

## Documentation Quality Standards

### Include:
- Specific commands with exact syntax
- File paths and directory structures
- Prerequisites and dependencies
- Troubleshooting for common issues

### Exclude:
- Obvious programming concepts
- Information repeated in other documents
- Generic software development advice
- Outdated or deprecated patterns

## Maintenance
Documentation should be updated immediately when:
- Commands don't work as documented
- Missing information causes extra work
- New patterns are discovered
- Code changes affect documented procedures

See: [Documentation Maintenance System](claude/DOCUMENTATION_MAINTENANCE.md)