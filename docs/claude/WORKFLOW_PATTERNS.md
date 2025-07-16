# Claude Workflow Patterns

## Task Initiation Pattern

### 1. Context Detection
```bash
# Backend context indicators
- File paths: /dotCMS/, /bom/, /parent/
- File types: .java, pom.xml
- Commands: ./mvnw, docker

# Frontend context indicators  
- File paths: /core-web/
- File types: .ts, .html, .scss, package.json
- Commands: nx, yarn
```

### 2. Documentation Reading Priority
```markdown
# For ALL tasks (read first)
1. docs/core/ARCHITECTURE_OVERVIEW.md
2. docs/core/SECURITY_PRINCIPLES.md
3. docs/core/PROGRESSIVE_ENHANCEMENT.md

# For Backend tasks (add these)
4. docs/backend/JAVA_STANDARDS.md
5. docs/backend/MAVEN_BUILD_SYSTEM.md

# For Frontend tasks (add these)
4. docs/frontend/ANGULAR_STANDARDS.md
5. docs/frontend/STYLING_STANDARDS.md
```

### 3. TodoWrite Usage (Required for Multi-Step Tasks)
```markdown
# Always use TodoWrite when task involves:
- Multiple files or directories
- Sequential build/test/deploy steps
- Cross-domain changes (backend + frontend)
- Complex troubleshooting
- Documentation updates
```

## Information Gathering Pattern

### Use Task Tool When:
- Searching for patterns across multiple files
- Finding examples of specific implementations
- Locating configuration files
- Understanding codebase structure

### Use Read Tool When:
- You know specific file paths
- Reviewing specific implementations
- Checking current configuration
- Validating existing patterns

### Use Grep Tool When:
- Searching for specific code patterns
- Finding usage of particular classes/methods
- Locating configuration properties
- Identifying security patterns

## Code Change Pattern

### 1. Before Making Changes
- Read relevant documentation sections
- Understand progressive enhancement rules
- Check for existing patterns in codebase
- Validate security requirements

### 2. During Changes
- Follow domain-specific standards
- Apply progressive enhancement safely
- Maintain existing interfaces
- Add proper error handling

### 3. After Changes
- Run appropriate tests
- Update documentation if needed
- Verify build succeeds
- Check for security compliance

## Documentation Update Pattern

### Immediate Updates Required
```markdown
# When you discover:
- Commands that don't work as documented
- Missing prerequisites or dependencies
- Incorrect file paths or directory structures
- Outdated patterns or approaches

# Update process:
1. Identify correct document (backend/frontend/core)
2. Add specific, actionable information
3. Remove outdated information
4. Add cross-references if needed
5. Validate changes work correctly
```

### Documentation Quality Standards
```markdown
# Include:
- Specific commands with exact syntax
- File paths and directory structures
- Prerequisites and dependencies
- Error solutions and troubleshooting

# Exclude:
- Obvious programming concepts
- Information repeated in other documents
- Generic software development advice
- Outdated or deprecated patterns
```

## Cross-Domain Task Pattern

### When Task Involves Both Backend and Frontend
```markdown
1. Read core principles first
2. Read both backend and frontend documentation
3. Plan changes with integration in mind
4. Implement backend changes first
5. Implement frontend changes second
6. Test integration thoroughly
7. Update both domain documentation if needed
```

### Integration Points to Consider
- REST API contracts
- Data model consistency
- Security validation on both sides
- Error handling consistency
- Testing coverage across domains

## Error Handling Pattern

### When Commands Fail
```markdown
1. Check documentation for correct syntax
2. Verify prerequisites are met
3. Check file paths and directory structure
4. Look for recent changes that might affect command
5. Update documentation with solution
```

### When Documentation is Incorrect
```markdown
1. Identify what was wrong
2. Find correct approach
3. Update relevant document immediately
4. Add troubleshooting section if needed
5. Verify correction works
```

## Testing Pattern

### Backend Testing
```bash
# Unit tests
./mvnw test -pl :dotcms-core

# Integration tests
./mvnw -pl :dotcms-integration verify -Dcoreit.test.skip=false

# Postman tests
./mvnw -pl :dotcms-postman verify -Dpostman.test.skip=false
```

### Frontend Testing
```bash
# Unit tests
cd core-web && nx run dotcms-ui:test

# E2E tests (if available)
cd core-web && nx run dotcms-ui:e2e
```

### Cross-Domain Testing
```bash
# Full build with tests
./mvnw clean install

# Docker integration testing
./mvnw -pl :dotcms-core -Pdocker-start -Dtomcat.port=8080
# Then test frontend against running backend
```

## Build Pattern

### Backend Build Sequence
```bash
# Development iteration
./mvnw install -pl :dotcms-core -DskipTests

# Docker image update (when needed)
./mvnw clean install -DskipTests

# Full build with tests
./mvnw clean install
```

### Frontend Build Sequence
```bash
# Development server
cd core-web && nx run dotcms-ui:serve

# Build check
cd core-web && nx build dotcms-ui

# Test run
cd core-web && nx run dotcms-ui:test
```

## Maintenance Pattern

### Regular Maintenance Tasks
- Validate documented commands still work
- Check file paths are correct
- Verify dependencies are up to date
- Test documented workflows
- Update patterns based on code changes

### Signs Documentation Needs Updates
- Repeated troubleshooting for same issues
- Commands that fail on first try
- Missing information that causes extra work
- Outdated patterns being followed