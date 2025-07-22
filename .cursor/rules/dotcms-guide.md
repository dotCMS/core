---
description: dotCMS development navigation and key reminders
globs: ["**/*"]
alwaysApply: true
---

# dotCMS Development Guide

## Primary Reference
**CLAUDE.md** provides optimized context-window efficient guidance for both Claude and Cursor.

## Domain-Specific Context (Loads Automatically)
- **Java files**: `java-context.md` - Essential Java patterns, Maven rules, build commands
- **Angular files**: `typescript-context.md` - Modern syntax, testing, SCSS standards  
- **Test files**: `test-context.md` - Spectator patterns, data-testid, user-centric testing

## Critical Reminders Only
- **Java**: Use Config/Logger, never System.out/System.getProperty
- **Maven**: Versions in bom/application/pom.xml, never in dotCMS/pom.xml
- **Angular**: Use @if/@for, input()/output(), spectator.setInput()
- **Testing**: Always use data-testid, run tests before completing work
- **Security**: No secrets in code, validate all input

## 📝 Documentation Maintenance
**CRITICAL**: Follow the Documentation Maintenance System:
- Update `/docs/` when you discover gaps or incorrect information
- Keep cursor rules minimal - comprehensive details belong in `/docs/`
- Maintain single source of truth - never duplicate information
- Ensure both Claude and Cursor can follow the same patterns

**See**: [Documentation Maintenance System](documentation-maintenance.md)

**Context Strategy**: Essential patterns immediately available, detailed docs loaded on-demand via `/docs/` directory.