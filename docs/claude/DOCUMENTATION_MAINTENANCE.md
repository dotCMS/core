# Documentation Maintenance System

## When to Update Documentation

### 1. Wrong Assumptions or Outdated Information
**ALWAYS update immediately when you discover:**
- Commands that don't work as documented
- Patterns that are no longer valid
- Missing dependencies or setup steps
- Incorrect file paths or directory structures

### 2. Extra Work Due to Missing Information
**Update when you had to:**
- Search extensively for information that should be documented
- Trial-and-error to find the correct approach
- Read multiple files to understand a pattern
- Discover undocumented requirements or dependencies

### 3. Code Changes That Affect Documentation
**Update when changes involve:**
- New build commands or processes
- New development patterns or standards
- Changes to existing APIs or interfaces
- New security requirements or validations
- Changes to testing approaches or frameworks

## Documentation Update Process

### Step 1: Identify the Right Document
```bash
# Backend code changes
docs/backend/          # Java, Maven, REST APIs, Database

# Frontend code changes  
docs/frontend/         # Angular, TypeScript, Testing

# Universal principles
docs/core/             # Security, Architecture, Progressive Enhancement

# AI workflow improvements
docs/claude/           # Task patterns, context switching
```

### Step 2: Update with Specific Information
**DO:**
- Add specific commands that work
- Include exact file paths and locations
- Document dependencies and prerequisites
- Add troubleshooting for common issues

### Step 3: Mark Essential Information
**IMPORTANT**: When adding information that's critical for system operation, mark it clearly:

#### Essential Information Markers
Use clear markers for information that should not be removed:
- **⚠️ CRITICAL**: For information absolutely required for system function
- **🚨 ESSENTIAL**: For information that prevents known failure modes
- **💡 IMPORTANT**: For information that significantly improves outcomes

#### Examples
```markdown
⚠️ **CRITICAL**: Claude must understand these patterns to properly manage epics and subtasks.

🚨 **ESSENTIAL**: This section contains proven patterns from real-world implementation.

💡 **IMPORTANT**: These templates ensure consistent formatting across all issues.
```

### Step 4: Verify Command Information
**CRITICAL**: Always verify command-line tool information by checking --help output:

#### Command Verification Process
```bash
# Always check --help for accurate options and functionality
git-issue-create --help
git-smart-switch --help
git-issue-pr --help
git-issue-branch --help

# Document actual options, not assumed ones
# Include automation-specific options like --json, --yes, --dry-run
```

#### When to Verify Commands
- **Before documenting**: Check --help before adding command examples
- **During updates**: Verify commands when utilities are updated
- **When debugging**: Check --help when commands aren't working as documented
- **Regular maintenance**: Periodic verification of documented commands

#### Interactive vs Non-Interactive Commands
**CRITICAL**: When documenting commands for Claude automation, always ensure non-interactive usage:

```bash
# ❌ WRONG - Will prompt for user input even with --dry-run
git issue-create "Title" --team Platform --type Task --dry-run

# ✅ CORRECT - Non-interactive pattern for automation
git issue-create "Title" --team Platform --type Task --repo dotCMS/core --dry-run
```

**Testing Interactive Commands:**
- **Test both modes**: Interactive (for developers) and non-interactive (for Claude)
- **Identify required flags**: Find flags that prevent interactive prompts
- **Document both patterns**: Show developer-friendly and automation-friendly versions
- **Update when behavior changes**: Commands may become more/less interactive with updates

**DON'T:**
- Repeat information from other documents
- Add obvious or generic information
- Include information that doesn't help with development

### Step 3: Maintain Cross-References
When updating documents, check for references in:
- `CLAUDE.md` (main router)
- Related domain documents
- Core principle documents

## Update Examples

### Wrong Assumption Example
```markdown
# BEFORE (incorrect)
Run tests with: `npm test`

# AFTER (correct)
Run tests with: `nx run dotcms-ui:test`
Note: Uses Nx, not npm scripts
```

### Missing Information Example
```markdown
# BEFORE (incomplete)
Use @Value.Immutable for data objects

# AFTER (complete)
Use @Value.Immutable for data objects
CRITICAL: Run `./mvnw compile` after creating @Value.Immutable classes to generate implementations
```

### Information Placement Examples

#### ❌ WRONG: Class-specific details in domain docs
```markdown
# docs/backend/JAVA_STANDARDS.md - TOO SPECIFIC
"The UserAPI.findByEmail() method throws DotDataException when user not found"
"ContentletAPI.checkin() requires EDIT permission on the contentlet"
"WorkflowAPI.findStepByName() returns null if step doesn't exist"
```

#### ✅ CORRECT: Class-specific details in code
```java
// com/dotcms/api/UserAPI.java - WHERE IT BELONGS
/**
 * Finds user by email address
 * @throws DotDataException when user not found
 * @throws DotSecurityException when access denied
 */
public User findByEmail(String email) throws DotDataException, DotSecurityException;
```

#### ❌ WRONG: General patterns in task-specific docs
```markdown
# docs/testing/BACKEND_UNIT_TESTS.md - TOO GENERAL
"Always use APILocator for service access"
"Use @Value.Immutable for data objects"
"Log errors with Logger.error()"
```

#### ✅ CORRECT: General patterns in domain docs
```markdown
# docs/backend/JAVA_STANDARDS.md - RIGHT PLACE
"Always use APILocator for service access"
"Use @Value.Immutable for data objects"
"Log errors with Logger.error()"
```

### Avoiding Duplication Example

#### ❌ WRONG: Repeating build commands across multiple files
```markdown
# docs/backend/JAVA_STANDARDS.md
"Run ./mvnw clean install"

# docs/backend/MAVEN_BUILD_SYSTEM.md
"Run ./mvnw clean install"

# docs/testing/BACKEND_UNIT_TESTS.md
"Run ./mvnw clean install"
```

#### ✅ CORRECT: Single source with cross-references
```markdown
# CLAUDE.md - SINGLE SOURCE
### Backend Development
./mvnw clean install

# docs/backend/JAVA_STANDARDS.md - CROSS-REFERENCE
> **Build Commands**: See [Quick Commands](../../CLAUDE.md#quick-commands)
```

## Maintenance Responsibilities

### For AI Development
- **Immediately update** when discovering wrong information
- **Add missing context** that caused extra work
- **Maintain information hierarchy** (core → domain → task-specific)
- **Use cross-references** instead of duplicating content
- **Place information** where it's most relevant to users

### For Human Developers
- **Follow information placement guidelines** when adding new documentation
- **Update relevant docs** when changing patterns or standards
- **Maintain single source of truth** for each piece of information
- **Use core documentation** as navigation aid, not implementation guide

## Quality Standards

### Information Must Be:
- **Specific**: Exact commands, file paths, configurations
- **Actionable**: Clear steps that can be followed
- **Current**: Validated against current codebase
- **Necessary**: Helps with development tasks

### Information Should NOT Be:
- **Obvious**: Basic programming concepts
- **Repeated**: Available in other documents
- **Generic**: Applies to all software projects
- **Outdated**: No longer relevant to current system
- **Misplaced**: Class-specific details in domain docs, domain details in core docs
- **Irrelevant**: Information not needed for the current task context

## Cross-Reference Maintenance

### When Adding New Information
1. **Check existing documents** for related information
2. **Add cross-references** instead of duplicating
3. **Update CLAUDE.md** if it affects task workflows
4. **Link related documents** for comprehensive understanding

### Cross-Reference Format
```markdown
<!-- Reference to related information -->
> **Related**: For Maven build patterns, see [Maven Build System](../backend/MAVEN_BUILD_SYSTEM.md)

<!-- Reference to core principles -->
> **Security**: All input validation must follow [Security Principles](../core/SECURITY_PRINCIPLES.md)
```

## Documentation Health Check

### Regular Maintenance Tasks
- **Command validation**: Verify all documented commands work
- **Path verification**: Check all file paths are correct
- **Link checking**: Ensure all cross-references are valid
- **Completeness review**: Identify missing information based on development tasks

### Signs Documentation Needs Updates
- **Repeated questions**: Same issues raised multiple times
- **Development delays**: Time lost searching for information
- **Incorrect implementations**: Following documentation leads to errors
- **Outdated patterns**: Documentation doesn't match current codebase