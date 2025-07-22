---
description: dotCMS development navigation and key reminders
globs: ["**/*"]
alwaysApply: true
---

# dotCMS Development Guide

## Primary Reference
**ALWAYS read and follow CLAUDE.md** - Complete development guide with all standards, patterns, and workflows.

## Documentation Navigation
For detailed patterns, reference:
- **Backend**: @docs/backend/JAVA_STANDARDS.md, @docs/backend/MAVEN_BUILD_SYSTEM.md
- **Frontend**: @docs/frontend/ANGULAR_STANDARDS.md, @docs/frontend/TESTING_FRONTEND.md
- **Architecture**: @docs/core/ARCHITECTURE_OVERVIEW.md
- **Git/CI**: @docs/core/GIT_WORKFLOWS.md, @docs/core/CICD_PIPELINE.md

## Critical Reminders Only
- **Java**: Use Config/Logger, never System.out/System.getProperty
- **Maven**: Versions in bom/application/pom.xml, never in dotCMS/pom.xml
- **Testing**: Always use data-testid, run tests before completing work
- **Security**: No secrets in code, validate all input

## 📝 Documentation Maintenance
**CRITICAL**: Follow the Documentation Maintenance System:
- Update `/docs/` when you discover gaps or incorrect information
- Keep cursor rules minimal - comprehensive details belong in `/docs/`
- Maintain single source of truth - never duplicate information
- Ensure both Claude and Cursor can follow the same patterns

**See**: [Documentation Maintenance System](documentation-maintenance.md)

All detailed patterns, examples, and workflows are in CLAUDE.md and /docs/ directory.