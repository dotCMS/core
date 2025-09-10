---
description: Continuous documentation maintenance automation
globs: ["**/*.md", "CLAUDE.md", "docs/**/*"]
alwaysApply: true
---

# Documentation Maintenance Automation

## 🤖 Continuous Maintenance System

### Auto-Update Triggers
When working on code, AUTOMATICALLY check for documentation updates:

#### Java File Changes → Check:
- Are new Config properties documented in `@docs/backend/CONFIGURATION_PATTERNS.md`?
- Are new API patterns covered in `@docs/backend/JAVA_STANDARDS.md`?
- Do Maven changes need `@docs/backend/MAVEN_BUILD_SYSTEM.md` updates?

#### Angular File Changes → Check:
- Are new component patterns in `@docs/frontend/ANGULAR_STANDARDS.md`?
- Are testing patterns current in `@docs/frontend/TESTING_FRONTEND.md`?
- Do style changes need `@docs/frontend/STYLING_STANDARDS.md` updates?

#### Test File Changes → Check:
- Are new testing patterns documented in appropriate test guides?
- Are data-testid conventions up to date?
- Do integration patterns need documentation updates?

### Progressive Documentation Enhancement

#### When You See These Patterns, Auto-Update Docs:
```java
// If you see Config.getProperty() usage → document pattern
// If you see new @Value.Immutable classes → document pattern  
// If you see new REST endpoints → document in API patterns
```

```typescript
// If you see new Angular features → document in standards
// If you see new testing patterns → document in testing guide
// If you see new component architectures → document patterns
```

### Quality Assurance Automation

#### Before Completing Any Task:
1. **Documentation Consistency Check**:
   - Are all code examples using current syntax?
   - Are tech stack versions current?
   - Are cross-references working?

2. **Context Window Optimization**:
   - Is essential information in immediate context?
   - Are detailed examples in on-demand docs?
   - Is duplicate information eliminated?

3. **Both-System Compatibility**:
   - Does Claude have navigation via CLAUDE.md?
   - Does Cursor have context via .cursor/rules/?
   - Do both reference the same authoritative docs?

### Maintenance Workflow Automation
```bash
# When making ANY code change:
1. Code Change → 2. Pattern Recognition → 3. Doc Location → 4. Update → 5. Cross-Reference

# Context window optimization:
- Keep immediate context under 100 lines
- Load detailed docs on-demand only
- Eliminate redundancy across files
```

## 🔄 Self-Healing Documentation

### Automated Pattern Detection
When AI tools encounter:
- **Missing patterns**: Auto-suggest documentation location
- **Outdated examples**: Auto-suggest updates with current syntax
- **Broken references**: Auto-suggest corrections
- **New technologies**: Auto-suggest where to document

### Validation Rules
```markdown
# Every code example MUST have:
✅ Current syntax (Java 11 for core, Angular 18.2.3)
✅ Concrete working examples  
✅ Both correct (✅) and incorrect (❌) patterns
✅ Specific file paths when relevant

# Every documentation file MUST:
✅ Single responsibility (one domain)
✅ Cross-references instead of duplication
✅ Context-window optimized structure
✅ Both Claude and Cursor compatibility
```

### Error Prevention
- **Before adding new config**: Check if documentation location exists
- **Before new testing patterns**: Check if testing guides cover it
- **Before architectural changes**: Check if core docs need updates
- **Before tech stack updates**: Check all version references

## 📊 Maintenance Metrics

### Success Indicators:
- Documentation stays current with code changes
- Context window usage remains optimized
- Both Claude and Cursor users find information quickly
- No duplicate information across files
- Cross-references remain valid

### Warning Signs:
- Documentation examples using outdated syntax
- Context files growing too large (>100 lines essential content)
- Information duplicated across multiple files
- Claude and Cursor getting different guidance
- Developers can't find patterns quickly

## 🎯 Automation Goals

1. **Zero-Maintenance Documentation**: Updates happen automatically during development
2. **Context-Aware**: Right information loads at right time
3. **Self-Healing**: Broken references auto-detected and fixed  
4. **Progressive**: Always improving existing patterns during development
5. **Unified**: Both AI systems work from same source of truth

**Result**: Documentation that stays current, efficient, and useful without manual maintenance overhead.