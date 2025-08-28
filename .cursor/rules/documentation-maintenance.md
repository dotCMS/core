---
description: Documentation Maintenance Rules for dotCMS Development
globs: ["**/*.md", "**/*.mdc", "CLAUDE.md", "docs/**/*"]
alwaysApply: true
---

# Documentation Maintenance System

## 🎯 Core Principle: Single Source of Truth

**DRY Documentation**: Each piece of information exists in exactly ONE authoritative location.

### Documentation Hierarchy
1. **CLAUDE.md** - Navigation hub and quick reference roadmap
2. **`/docs/` directory** - Comprehensive implementation details organized by domain
3. **`.cursor/rules/`** - Minimal navigation and critical reminders only

## 📋 When to Update Documentation

### Always Update When You:
- Discover incorrect information in any documentation
- Find missing patterns or examples while coding
- Learn new implementation details not covered
- Encounter outdated technology references
- Find contradictions between different docs

### Documentation Location Guide

#### Backend Java Changes → Update:
- `docs/backend/JAVA_STANDARDS.md` - Coding patterns, frameworks
- `docs/backend/REST_API_PATTERNS.md` - API development
- `docs/backend/CONFIGURATION_PATTERNS.md` - Config management
- `docs/backend/DATABASE_PATTERNS.md` - Database access

#### Frontend Angular Changes → Update:
- `docs/frontend/ANGULAR_STANDARDS.md` - Component architecture, modern syntax
- `docs/frontend/TESTING_FRONTEND.md` - All testing patterns (Spectator, Jest)
- `docs/frontend/STYLING_STANDARDS.md` - CSS/SCSS patterns
- `docs/frontend/COMPONENT_ARCHITECTURE.md` - Structure patterns

#### Build/CI Changes → Update:
- `docs/core/CICD_PIPELINE.md` - Pipeline processes
- `docs/backend/MAVEN_BUILD_SYSTEM.md` - Build configuration
- `docs/infrastructure/DOCKER_BUILD_PROCESS.md` - Docker patterns

## ✅ Documentation Update Process

### 1. Identify the Right Location
```bash
# Ask yourself: Where does this information belong?
- Is it a general principle? → CLAUDE.md (navigation only)
- Is it domain-specific implementation? → docs/{domain}/
- Is it a quick reminder for specific file types? → .cursor/rules/
```

### 2. Update with Actionable Information
```markdown
# ✅ Good: Specific, actionable, with examples
## Java Configuration Pattern
Use `Config.getStringProperty()` for all configuration access:
```java
// ✅ Correct
String timeout = Config.getStringProperty("database.timeout", "30");

// ❌ Incorrect  
String timeout = System.getProperty("database.timeout");
```

# ❌ Bad: Vague, no examples
## Configuration
Use the Config class properly.
```

### 3. Cross-Reference, Don't Duplicate
```markdown
# ✅ Good: Reference to authoritative source
See comprehensive patterns: [Java Standards](../backend/JAVA_STANDARDS.md)

# ❌ Bad: Duplicating information
// Long explanation that already exists elsewhere
```

### 4. Keep Cursor Rules Minimal
```markdown
# ✅ Good .cursor/rules pattern:
**Critical Reminders:**
- ALWAYS use `Config.getStringProperty()` not `System.getProperty()`
- Use `@Value.Immutable` for new data classes

**Comprehensive Documentation:** [Java Standards](../docs/backend/JAVA_STANDARDS.md)

# ❌ Bad: Long detailed explanations in cursor rules
```

## 🔄 Progressive Enhancement Pattern

**Always improve existing code while working:**

### When Editing Java Code:
1. Add generics to raw types: `List<String>` not `List`
2. Replace `System.out.println()` with `Logger.info(this, "message")`
3. Replace `System.getProperty()` with `Config.getStringProperty()`
4. Add `@Override` annotations where missing

### When Editing Angular Code:
1. Convert to modern template syntax: `@if` not `*ngIf`
2. Use `input()` and `output()` functions not decorators
3. Add `data-testid` attributes for testable elements
4. Use `ChangeDetectionStrategy.OnPush`

### When Editing Tests:
1. Use `spectator.setInput()` not direct property assignment
2. Use `byTestId()` not CSS selectors
3. Test user interactions, not implementation details

## 📝 Documentation Quality Standards

### Required Elements:
- **Code examples** with ✅ correct and ❌ incorrect patterns
- **Specific file paths** and line references when relevant
- **Cross-references** to related documentation
- **Context** - when/why to use each pattern

### Avoid:
- Duplicating information that exists elsewhere
- Vague guidance without concrete examples
- Implementation details in navigation files
- Outdated technology versions or patterns

## 🤝 Collaboration Between Claude and Cursor

### Both Systems Must:
1. **Check documentation first** before making implementation decisions
2. **Update docs when discovering gaps** or incorrect information
3. **Follow the same patterns** - DRY principle applies to both
4. **Use cross-references** instead of duplicating content

### Update Workflow:
```bash
# 1. Discover issue while coding
# 2. Identify correct documentation location
# 3. Update with specific, actionable information
# 4. Cross-reference from other locations if needed
# 5. Keep cursor rules minimal with navigation only
```

## 🚨 Critical Rules

### NEVER:
- Duplicate detailed implementation in multiple places
- Update only cursor rules without updating primary docs
- Leave documentation contradictions unresolved
- Create new documentation files without checking existing structure

### ALWAYS:
- Update the authoritative source (docs/ directory)
- Add concrete code examples
- Cross-reference related information
- Keep information DRY across both systems
- Ensure both Claude and Cursor can follow the same patterns

## 📍 Quick Reference

**Documentation Locations:**
- **Navigation**: `CLAUDE.md` - roadmap to find information
- **Implementation**: `docs/{domain}/` - comprehensive patterns
- **Context**: `.cursor/rules/` - minimal navigation and reminders

**Update Process:**
1. Discover → 2. Locate → 3. Update → 4. Cross-reference → 5. Verify consistency

**Quality Check:**
- Is information in the right place?
- Are examples concrete and actionable?
- Are cross-references correct?
- Would both Claude and Cursor users find this helpful?