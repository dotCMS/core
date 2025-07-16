# GitHub Epic: Remove Repackaged Dependencies

## Epic Description

**Title**: Migrate from Repackaged Dependencies to Standard Maven Dependencies

**Description**:
The dotCMS project currently uses 76 repackaged dependencies with the groupId `com.dotcms.lib`. These dependencies were originally repackaged to avoid conflicts but now create maintenance overhead, security vulnerabilities, and complicate dependency management.

This epic aims to systematically migrate from repackaged dependencies to standard Maven dependencies, improving security, maintainability, and reducing technical debt.

**Business Value**:
- **Security**: Eliminate known vulnerabilities in outdated repackaged libraries
- **Maintainability**: Use standard Maven dependencies with regular security updates
- **Build Performance**: Reduce build complexity and potentially improve performance
- **Developer Experience**: Simplify dependency management and IDE integration

**Scope**:
- Migrate 76 repackaged dependencies to standard Maven coordinates
- Remove unused dependencies (estimated 50+ candidates)
- Update import statements throughout the codebase
- Ensure backward compatibility and no functional regressions

**Success Criteria**:
- Reduce repackaged dependencies from 76 to <20
- All tests pass (unit, integration, postman)
- No performance regressions
- Security vulnerability count reduced
- Build time maintained or improved

**Epic Timeline**: 4-7 months

---

## Subtasks

### Phase 1: Foundation & Cleanup ✅ (Completed)

#### Task 1.1: Migrate Hibernate Dependencies ✅
**Status**: ✅ **COMPLETED**
- **Title**: Replace repackaged Hibernate with standard Hibernate 5.6.15.Final
- **Description**: 
  - Remove `dot.hibernate` repackaged dependency
  - Add standard `org.hibernate:hibernate-core:5.6.15.Final`
  - Update all import statements from `com.dotcms.repackage.net.sf.hibernate.*` to `org.hibernate.*`
  - Test all Hibernate functionality
- **Acceptance Criteria**:
  - [x] Repackaged Hibernate dependency removed from POM
  - [x] Standard Hibernate 5.6.15.Final added to BOM
  - [x] All import statements updated
  - [x] All tests pass
  - [x] No performance regressions

#### Task 1.2: Migrate Hibernate Validator ✅
**Status**: ✅ **COMPLETED**
- **Title**: Replace repackaged Hibernate Validator with standard version
- **Description**:
  - Remove `dot.hibernate-validator` repackaged dependency
  - Add standard `org.hibernate:hibernate-validator:5.4.3.Final`
  - Update validation annotations and imports
  - Test all validation functionality
- **Acceptance Criteria**:
  - [x] Repackaged Hibernate Validator removed from POM
  - [x] Standard Hibernate Validator added to BOM
  - [x] All validation imports updated
  - [x] All validation tests pass

---

### Phase 2: Low-Risk Migrations (2-3 weeks)

#### Task 2.1: Remove Unused Dependencies Analysis
- **Title**: Identify and Remove Unused Repackaged Dependencies
- **Description**:
  - Analyze the 50+ potentially unused dependencies identified
  - Create test build without unused dependencies
  - Verify no runtime failures
  - Remove unused dependencies from POM
- **Acceptance Criteria**:
  - [ ] List of confirmed unused dependencies created
  - [ ] Test build passes without unused dependencies
  - [ ] Integration tests pass
  - [ ] Dependencies removed from BOM and dotCMS POM
- **Estimated Effort**: 3-5 days

#### Task 2.2: Migrate JSoup Dependency
- **Title**: Replace dot.jsoup with standard org.jsoup:jsoup
- **Description**:
  - Update 6+ import statements from repackaged JSoup to standard
  - Remove `dot.jsoup` from POM
  - Add standard `org.jsoup:jsoup` dependency
  - Test HTML parsing functionality
- **Acceptance Criteria**:
  - [ ] All JSoup imports updated
  - [ ] Standard JSoup dependency added to BOM
  - [ ] All HTML parsing tests pass
  - [ ] No functional regressions
- **Estimated Effort**: 2-3 days

#### Task 2.3: Migrate Commons CLI
- **Title**: Replace dot.commons-cli with standard commons-cli:commons-cli
- **Description**:
  - Update import statements (limited usage)
  - Remove `dot.commons-cli` from POM
  - Add standard `commons-cli:commons-cli` dependency
  - Test command-line interface functionality
- **Acceptance Criteria**:
  - [ ] All Commons CLI imports updated
  - [ ] Standard Commons CLI dependency added to BOM
  - [ ] CLI functionality tests pass
- **Estimated Effort**: 1-2 days

#### Task 2.4: Migrate SLF4J Dependencies
- **Title**: Replace dot.slf4j-api with standard SLF4J
- **Description**:
  - Verify current SLF4J usage patterns
  - Remove `dot.slf4j-api` and `dot.slf4j-jcl` from POM
  - Ensure standard SLF4J from logging BOM is used
  - Test logging functionality
- **Acceptance Criteria**:
  - [ ] Repackaged SLF4J dependencies removed
  - [ ] Standard SLF4J from logging BOM confirmed
  - [ ] All logging functionality works
  - [ ] No logging level changes
- **Estimated Effort**: 2-3 days

---

### Phase 3: Medium-Risk Migrations (4-6 weeks)

#### Task 3.1: Migrate Google Guava (High Usage)
- **Title**: Replace dot.guava with standard com.google.guava:guava
- **Description**:
  - Update 219+ import statements from repackaged Guava
  - Remove `dot.guava` from POM
  - Ensure standard Guava 27.0.1-android is used from BOM
  - Test all Guava functionality (especially @VisibleForTesting annotations)
- **Acceptance Criteria**:
  - [ ] All 219+ Guava imports updated
  - [ ] Standard Guava dependency confirmed in BOM
  - [ ] All unit tests pass
  - [ ] @VisibleForTesting annotations work correctly
- **Estimated Effort**: 5-7 days

#### Task 3.2: Migrate Commons HTTP Client
- **Title**: Replace dot.commons-httpclient with modern HTTP client
- **Description**:
  - Analyze 30+ import statements of Commons HTTPClient
  - Evaluate migration to `org.apache.httpcomponents:httpclient`
  - Update HTTP client usage patterns
  - Test all HTTP integrations
- **Acceptance Criteria**:
  - [ ] All HTTP client imports updated
  - [ ] Modern HTTP client dependency added
  - [ ] All HTTP integration tests pass
  - [ ] No performance regressions in HTTP calls
- **Estimated Effort**: 7-10 days

#### Task 3.3: Migrate JAXB Dependencies
- **Title**: Replace dot.jaxb-* with standard Jakarta XML Bind
- **Description**:
  - Remove `dot.jaxb-api`, `dot.jaxb-core`, `dot.jaxb-impl`
  - Ensure standard Jakarta XML Bind dependencies from BOM
  - Update XML binding functionality
  - Test XML serialization/deserialization
- **Acceptance Criteria**:
  - [ ] All repackaged JAXB dependencies removed
  - [ ] Standard Jakarta XML Bind confirmed
  - [ ] All XML processing tests pass
  - [ ] No XML binding functionality lost
- **Estimated Effort**: 5-7 days

#### Task 3.4: Migrate Database Connection Libraries
- **Title**: Replace dot.commons-dbcp and dot.commons-pool
- **Description**:
  - Remove `dot.commons-dbcp` and `dot.commons-pool`
  - Migrate to `org.apache.commons:commons-dbcp2` and `commons-pool2`
  - Test database connection pooling
  - Verify no connection leaks
- **Acceptance Criteria**:
  - [ ] Repackaged DBCP/Pool dependencies removed
  - [ ] Standard Commons DBCP2/Pool2 dependencies added
  - [ ] Database connection tests pass
  - [ ] No connection pool regressions
- **Estimated Effort**: 5-7 days

#### Task 3.5: Migrate Utility Libraries
- **Title**: Replace remaining utility dependencies
- **Description**:
  - Migrate `dot.commons-net`, `dot.twitter4j-core`, `dot.snappy-java`
  - Remove repackaged versions from POM
  - Add standard versions to BOM
  - Test utility functionality
- **Acceptance Criteria**:
  - [ ] All utility library imports updated
  - [ ] Standard dependencies added to BOM
  - [ ] Utility functionality tests pass
- **Estimated Effort**: 4-6 days

---

### Phase 4: High-Risk Migrations (3-6 months)

#### Task 4.1: Evaluate DWR Migration
- **Title**: Replace dot.dwr with modern Ajax solution
- **Description**:
  - Analyze 42+ DWR import statements
  - Evaluate migration to modern Ajax alternatives (JAX-RS, WebSocket)
  - Create migration plan for DWR functionality
  - Implement proof-of-concept
- **Acceptance Criteria**:
  - [ ] DWR usage analysis completed
  - [ ] Migration strategy documented
  - [ ] Proof-of-concept implemented
  - [ ] Performance comparison completed
- **Estimated Effort**: 3-4 weeks

#### Task 4.2: Portlet System Evaluation
- **Title**: Evaluate dot.portlet migration strategy
- **Description**:
  - Analyze 361+ portlet import statements
  - Evaluate migration to standard `javax.portlet:portlet-api`
  - Assess impact on legacy portlet system
  - Create long-term modernization plan
- **Acceptance Criteria**:
  - [ ] Portlet usage analysis completed
  - [ ] Migration complexity assessment done
  - [ ] Modernization roadmap created
  - [ ] Risk assessment documented
- **Estimated Effort**: 2-3 weeks

#### Task 4.3: Struts Migration Planning
- **Title**: Create Struts migration strategy
- **Description**:
  - Analyze 311+ Struts import statements
  - Evaluate migration to standard Struts or modern alternatives
  - Assess impact on legacy portlet system
  - Create phased migration plan
- **Acceptance Criteria**:
  - [ ] Struts usage analysis completed
  - [ ] Migration options evaluated
  - [ ] Phased migration plan created
  - [ ] Resource requirements estimated
- **Estimated Effort**: 2-3 weeks

---

### Phase 5: Validation & Cleanup (2-3 weeks)

#### Task 5.1: Comprehensive Testing
- **Title**: Execute full test suite and performance validation
- **Description**:
  - Run complete unit test suite
  - Execute integration tests
  - Run Postman API tests
  - Perform performance benchmarking
  - Execute security vulnerability scans
- **Acceptance Criteria**:
  - [ ] All unit tests pass
  - [ ] Integration tests pass
  - [ ] Postman tests pass
  - [ ] No performance regressions
  - [ ] Security scan shows reduced vulnerabilities
- **Estimated Effort**: 1-2 weeks

#### Task 5.2: Documentation Update
- **Title**: Update dependency documentation and migration guide
- **Description**:
  - Update CLAUDE.md with new dependency patterns
  - Create migration guide for future reference
  - Update developer documentation
  - Document lessons learned
- **Acceptance Criteria**:
  - [ ] CLAUDE.md updated
  - [ ] Migration guide created
  - [ ] Developer docs updated
  - [ ] Lessons learned documented
- **Estimated Effort**: 3-5 days

#### Task 5.3: Final Cleanup
- **Title**: Remove remaining repackaged dependencies
- **Description**:
  - Remove any remaining unused repackaged dependencies
  - Clean up BOM file
  - Update version management
  - Final verification
- **Acceptance Criteria**:
  - [ ] All unused dependencies removed
  - [ ] BOM file cleaned up
  - [ ] Version management updated
  - [ ] Final build verification passes
- **Estimated Effort**: 2-3 days

---

## Epic Labels
- `epic`
- `technical-debt`
- `dependencies`
- `security`
- `maintenance`

## Epic Priority
**High** - Technical debt reduction with security implications

## Epic Assignee
To be assigned to senior developer or team lead

## Epic Dependencies
- Access to CI/CD pipeline for testing
- Security scanning tools for vulnerability assessment
- Performance testing environment