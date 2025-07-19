# Repackaged Dependencies Analysis and Migration Plan

## Executive Summary

This analysis identifies 76 repackaged dependencies with groupId `com.dotcms.lib` in the dotCMS project. These dependencies were originally repackaged to avoid conflicts but create maintenance overhead and potential security vulnerabilities. This document outlines a comprehensive migration plan to replace them with standard Maven dependencies.

## Current State Analysis

### Repackaged Dependencies Overview
- **Total Found**: 76 dependencies with `com.dotcms.lib` groupId
- **Actively Used**: ~15-20 dependencies (based on import analysis)
- **Legacy Framework Dependencies**: 4 major ones (Struts, Portlets, DWR, Quartz)
- **Potentially Unused**: ~50+ dependencies
- **Successfully Migrated**: Hibernate (completed in Phase 1)

### Migration Status
- ✅ **Completed**: Hibernate Core & Validator migration to standard 5.6.15.Final
- 🔄 **In Progress**: Analysis and planning phase
- ⏳ **Pending**: 75 remaining dependencies

## Detailed Findings

### 1. High-Priority Dependencies (Actively Used)

| Repackaged Dependency | Standard Maven Coordinates | Version | Usage Level | Migration Complexity |
|----------------------|---------------------------|---------|-------------|---------------------|
| `dot.guava` | `com.google.guava:guava` | 27.0.1+ | **Very High** (219+ imports) | **Low** - Standard version in BOM |
| `dot.commons-io` | `commons-io:commons-io` | 2.11.0 | **Medium** | ✅ **Already Migrated** |
| `dot.commons-fileupload` | `commons-fileupload:commons-fileupload` | 1.5 | **Medium** | **Low** - Standard version in BOM |
| `dot.commons-httpclient` | `commons-httpclient:commons-httpclient` | 3.1+ | **Medium** (30+ imports) | **Medium** - Consider modern HTTP client |
| `dot.jsoup` | `org.jsoup:jsoup` | 1.15.3+ | **Low** (6+ imports) | **Low** - Simple migration |
| `dot.slf4j-api` | `org.slf4j:slf4j-api` | 1.7.36+ | **Unknown** | **Low** - Standard version in logging BOM |

### 2. Legacy Framework Dependencies (Major Migration Effort)

| Repackaged Dependency | Standard Maven Coordinates | Migration Complexity | Impact |
|----------------------|---------------------------|---------------------|--------|
| `dot.struts` | `org.apache.struts:struts-core` | **Very High** | Legacy portlet system dependent |
| `dot.portlet` | `javax.portlet:portlet-api` | **Very High** | Core portlet functionality |
| `dot.dwr` | `org.directwebremoting:dwr` | **High** | Ajax functionality |
| `dot.quartz-all` | `org.quartz-scheduler:quartz` | ✅ **Already Replaced** | Scheduling system |

### 3. Database/JEE Dependencies

| Repackaged Dependency | Standard Maven Coordinates | Replacement Available | Priority |
|----------------------|---------------------------|----------------------|----------|
| `dot.commons-dbcp` | `org.apache.commons:commons-dbcp2` | ✅ Modern DBCP2 or HikariCP | **Medium** |
| `dot.commons-pool` | `org.apache.commons:commons-pool2` | ✅ Available in BOM | **Medium** |
| `dot.jta` | `javax.transaction:javax.transaction-api` | ✅ Standard JTA | **Medium** |
| `dot.persistence-api` | `javax.persistence:javax.persistence-api` | ✅ Standard JPA | **Medium** |
| `dot.validation-api` | `javax.validation:validation-api` | ✅ Available in BOM | **Medium** |

### 4. XML/Data Processing Dependencies

| Repackaged Dependency | Standard Maven Coordinates | Status | Priority |
|----------------------|---------------------------|--------|----------|
| `dot.jaxb-api` | `jakarta.xml.bind:jakarta.xml.bind-api` | ✅ Available in BOM | **Medium** |
| `dot.jaxb-core` | `org.glassfish.jaxb:jaxb-core` | ✅ Available in BOM | **Medium** |
| `dot.jaxb-impl` | `org.glassfish.jaxb:jaxb-runtime` | ✅ Available in BOM | **Medium** |
| `dot.stax2-api` | `org.codehaus.woodstox:stax2-api` | ⚠️ Needs mapping | **Low** |
| `dot.woodstox-core-lgpl` | `com.fasterxml.woodstox:woodstox-core` | ⚠️ Needs migration | **Low** |
| `dot.xpp3-min` | `xpp3:xpp3_min` | ⚠️ Consider newer XML parsers | **Low** |

### 5. Utility Libraries

| Repackaged Dependency | Standard Maven Coordinates | Priority | Notes |
|----------------------|---------------------------|----------|-------|
| `dot.commons-cli` | `commons-cli:commons-cli` | **Low** | Limited usage |
| `dot.commons-net` | `commons-net:commons-net` | **Low** | Limited usage |
| `dot.twitter4j-core` | `org.twitter4j:twitter4j-core` | **Low** | Social media integration |
| `dot.snappy-java` | `org.xerial.snappy:snappy-java` | **Medium** | Compression |
| `dot.trove` | `net.sf.trove4j:trove4j` | **Low** | Collections library |

### 6. Potentially Unused Dependencies (Candidates for Removal)

These dependencies appear in the POM but show no active code usage:

#### Image/Media Processing
- `dot.gif89`
- `dot.wbmp`
- `dot.webp-imageio-core`
- `dot.com.dotmarketing.jhlabs.images.filters`

#### Build/Testing Tools
- `dot.asm`
- `dot.cactus.integration.ant`
- `dot.cargo-ant`
- `dot.cargo-core-uberjar`
- `dot.commons-jci-core`
- `dot.commons-jci-eclipse`

#### Legacy Libraries
- `dot.bsf` (Bean Scripting Framework)
- `dot.bsh` (BeanShell)
- `dot.concurrent`
- `dot.core-renderer-modified`
- `dot.cos`
- `dot.counter-ejb`
- `dot.fileupload-ext`
- `dot.google`
- `dot.httpbridge`
- `dot.httpclient`
- `dot.iText`
- `dot.jamm`
- `dot.javacsv`
- `dot.javax.annotation-api`
- `dot.jazzy-core`
- `dot.jboss-logging`
- `dot.jempbox`
- `dot.jep`
- `dot.jstl`
- `dot.jxl`
- `dot.ldap`
- `dot.lesscss`
- `dot.mail-ejb`
- `dot.maxmind-db`
- `dot.msnm`
- `dot.myspell`
- `dot.odmg`
- `dot.platform`
- `dot.rhino`
- `dot.secure-filter`
- `dot.slf4j-jcl`
- `dot.sslext`
- `dot.Tidy`
- `dot.txtmark`
- `dot.util-taglib`

#### Eclipse Mylyn Dependencies
- `dot.org.eclipse.mylyn.wikitext.confluence.core`
- `dot.org.eclipse.mylyn.wikitext.core`
- `dot.org.eclipse.mylyn.wikitext.mediawiki.core`
- `dot.org.eclipse.mylyn.wikitext.textile.core`
- `dot.org.eclipse.mylyn.wikitext.tracwiki.core`
- `dot.org.eclipse.mylyn.wikitext.twiki.core`

#### Compression/Utility
- `dot.axiom-api`
- `dot.axiom-impl`
- `dot.compression-filter`

## Migration Strategy

### Phase 1: Foundation & Cleanup (Completed)
- ✅ **Migrate Hibernate**: Replace repackaged Hibernate with standard 5.6.15.Final
- ✅ **Migrate Hibernate Validator**: Replace repackaged validator with standard version

### Phase 2: Low-Risk Migrations (2-3 weeks)
- **Remove unused dependencies** from POM after verification
- **Migrate simple libraries** (JSoup, Commons CLI)
- **Update import statements** systematically
- **Verify no runtime issues**

### Phase 3: Medium-Risk Migrations (4-6 weeks)
- **Replace Commons HTTPClient** with modern `org.apache.httpcomponents:httpclient`
- **Migrate XML processing libraries** (JAXB, Woodstox)
- **Update utility libraries** (Snappy, Twitter4J)
- **Migrate database connection libraries**

### Phase 4: High-Risk Migrations (Major effort - 3-6 months)
- **Modernize Ajax functionality** (replace DWR with modern alternatives)
- **Portlet system evaluation** (consider architectural changes)
- **Struts migration planning** (consider Spring MVC or JAX-RS)

## Risk Assessment

### Low Risk
- Unused dependencies removal
- Simple utility libraries (JSoup, Commons CLI)
- Libraries with direct standard replacements

### Medium Risk
- HTTP client migration (might affect integrations)
- XML processing libraries (could impact data processing)
- Database connection libraries (connection pooling changes)

### High Risk
- Legacy framework dependencies (Struts, Portlets, DWR)
- Core infrastructure changes
- Large-scale import statement updates

## Success Metrics

- **Dependency Count**: Reduce from 76 to <20 repackaged dependencies
- **Security**: Eliminate known vulnerabilities in repackaged libraries
- **Maintainability**: Use standard Maven dependencies with regular updates
- **Performance**: Potentially improved performance with modern libraries
- **Build Time**: Reduced build complexity

## Testing Strategy

1. **Unit Tests**: Ensure all existing tests pass
2. **Integration Tests**: Run full integration test suite
3. **Performance Tests**: Verify no performance regressions
4. **Security Scanning**: Run dependency vulnerability scans
5. **Compatibility Testing**: Test with different environments

## Timeline Estimates

- **Phase 1**: ✅ **Completed** (Hibernate migration)
- **Phase 2**: 2-3 weeks (Low-risk migrations)
- **Phase 3**: 4-6 weeks (Medium-risk migrations)  
- **Phase 4**: 3-6 months (High-risk migrations)

**Total Estimated Timeline**: 4-7 months for complete migration

## Conclusion

The repackaged dependencies cleanup represents a significant technical debt reduction opportunity. With 76 dependencies identified, approximately 50+ can be safely removed, and the remaining can be migrated to standard versions. The phased approach minimizes risk while delivering incremental value.

The completed Hibernate migration in Phase 1 demonstrates the feasibility and benefits of this approach. The remaining phases will modernize the dependency management and improve long-term maintainability of the dotCMS platform.