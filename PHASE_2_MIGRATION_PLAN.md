# Phase 2: Hibernate Migration Plan - Complete Implementation Strategy

## Executive Summary

Phase 2 migrates from repackaged Hibernate 2.1.7 to standard Hibernate 5.6.15 (last javax-compatible version). This involves updating 36 files with systematic import replacements and API compatibility updates.

**Estimated Effort**: 91 person-days  
**Risk Level**: High (core infrastructure changes)  
**Timeline**: 4-6 weeks for full implementation

## 1. Import Replacement Strategy

### 1.1 Core Package Mappings

| Hibernate 2.1.7 (Repackaged) | Hibernate 5.6.15 (Standard) |
|------------------------------|------------------------------|
| `com.dotcms.repackage.net.sf.hibernate.Session` | `org.hibernate.Session` |
| `com.dotcms.repackage.net.sf.hibernate.Query` | `org.hibernate.Query` |
| `com.dotcms.repackage.net.sf.hibernate.SessionFactory` | `org.hibernate.SessionFactory` |
| `com.dotcms.repackage.net.sf.hibernate.Transaction` | `org.hibernate.Transaction` |
| `com.dotcms.repackage.net.sf.hibernate.HibernateException` | `org.hibernate.HibernateException` |
| `com.dotcms.repackage.net.sf.hibernate.cfg.Configuration` | `org.hibernate.cfg.Configuration` |
| `com.dotcms.repackage.net.sf.hibernate.dialect.Dialect` | `org.hibernate.dialect.Dialect` |

### 1.2 Advanced/Deprecated Mappings

| Hibernate 2.1.7 (Repackaged) | Hibernate 5.6.15 (Standard) | Notes |
|------------------------------|------------------------------|-------|
| `com.dotcms.repackage.net.sf.hibernate.UserType` | `org.hibernate.usertype.UserType` | API changed |
| `com.dotcms.repackage.net.sf.hibernate.id.IdentifierGenerator` | `org.hibernate.id.IdentifierGenerator` | API changed |
| `com.dotcms.repackage.net.sf.hibernate.cache.CacheProvider` | `org.hibernate.cache.spi.RegionFactory` | Completely redesigned |
| `com.dotcms.repackage.net.sf.hibernate.impl.SessionFactoryImpl` | `org.hibernate.engine.spi.SessionFactoryImplementor` | Internal API |
| `com.dotcms.repackage.net.sf.hibernate.collection.*` | `org.hibernate.collection.spi.*` | Package restructured |

### 1.3 Automated Replacement Script

```bash
#!/bin/bash
# Phase 2 Import Replacement Script

# Core imports
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.Session/org.hibernate.Session/g' {} \;
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.Query/org.hibernate.Query/g' {} \;
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.SessionFactory/org.hibernate.SessionFactory/g' {} \;
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.HibernateException/org.hibernate.HibernateException/g' {} \;
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.Transaction/org.hibernate.Transaction/g' {} \;

# Configuration imports
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.cfg\.Configuration/org.hibernate.cfg.Configuration/g' {} \;
find . -name "*.java" -exec sed -i '' 's/com\.dotcms\.repackage\.net\.sf\.hibernate\.dialect\.Dialect/org.hibernate.dialect.Dialect/g' {} \;

# Note: Complex mappings require manual updates
```

## 2. HibernateUtil API Compatibility Strategy

### 2.1 Approach: Wrapper with Gradual Migration

**Strategy**: Create a compatibility wrapper that maintains the existing HibernateUtil API while using modern Hibernate 5.6.15 underneath.

```java
public class HibernateUtilV2 {
    // Keep existing method signatures
    public static void startTransaction() { /* modern impl */ }
    public static void commitTransaction() { /* modern impl */ }
    public static void rollbackTransaction() { /* modern impl */ }
    
    // Gradually migrate internals
    private static SessionFactory sessionFactory; // Hibernate 5.6.15
    private static ThreadLocal<Session> sessionHolder = new ThreadLocal<>();
}
```

### 2.2 Key API Changes to Handle

#### Session Management
- **Hibernate 2.1.7**: `Session.connection()` - direct connection access
- **Hibernate 5.6.15**: `Session.doWork(Work)` - callback pattern
- **Solution**: Wrap connection access in doWork callbacks

#### Query API  
- **Hibernate 2.1.7**: `Query.setMaxResults(int)` returns void
- **Hibernate 5.6.15**: `Query.setMaxResults(int)` returns Query (fluent API)
- **Solution**: Maintain void return types in wrapper

#### Transaction API
- **Hibernate 2.1.7**: Simple transaction model
- **Hibernate 5.6.15**: Enhanced transaction API with resource management
- **Solution**: Wrap modern transactions in legacy interface

### 2.3 Configuration Migration

#### Hibernate 2.1.7 Configuration
```xml
<!DOCTYPE hibernate-configuration PUBLIC 
  "-//Hibernate/Hibernate Configuration DTD 2.0//EN" 
  "http://hibernate.sourceforge.net/hibernate-configuration-2.0.dtd">
```

#### Hibernate 5.6.15 Configuration
```xml
<!DOCTYPE hibernate-configuration PUBLIC
  "-//Hibernate/Hibernate Configuration DTD 3.0//EN"
  "http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd">
```

## 3. HBM to Hibernate 5.x Conversion Strategy

### 3.1 HBM File Updates Required

Current HBM files need DTD and format updates:

**Files to Update:**
- `hibernate.cfg.xml` - Main configuration
- `DotCMSSeq.hbm.xml` - Entity mappings
- `DotCMSId.hbm.xml` - ID mappings
- `portal-hbm.xml` - Portal mappings

### 3.2 Key HBM Changes

#### DTD Updates
```xml
<!-- OLD: Hibernate 2.1.7 -->
<!DOCTYPE hibernate-mapping PUBLIC 
  "-//Hibernate/Hibernate Mapping DTD 2.0//EN" 
  "http://hibernate.sourceforge.net/hibernate-mapping-2.0.dtd">

<!-- NEW: Hibernate 5.6.15 -->
<!DOCTYPE hibernate-mapping PUBLIC 
  "-//Hibernate/Hibernate Mapping DTD 3.0//EN" 
  "http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd">
```

#### Generator Changes
```xml
<!-- OLD: Hibernate 2.1.7 -->
<generator class="com.dotmarketing.util.UUIDGenerator" />

<!-- NEW: Hibernate 5.6.15 -->
<generator class="uuid2" />
```

### 3.3 Custom Type Migration

#### UUIDGenerator Migration
```java
// OLD: Hibernate 2.1.7
public class UUIDGenerator implements IdentifierGenerator {
    public Serializable generate(SessionImplementor session, Object object) 
        throws HibernateException {
        return UUIDUtil.uuid();
    }
}

// NEW: Hibernate 5.6.15
public class UUIDGenerator implements IdentifierGenerator {
    public Serializable generate(SharedSessionContractImplementor session, Object object) 
        throws HibernateException {
        return UUIDUtil.uuid();
    }
}
```

## 4. Dependency Migration Order

### 4.1 Critical Path (Sequential)

1. **Maven Dependencies** (1 day)
   - Update `bom/application/pom.xml`
   - Update `dotCMS/pom.xml`
   - Add Hibernate 5.6.15.Final dependencies

2. **Core Infrastructure** (10 days)
   - `HibernateUtil.java` - Session factory and transaction management
   - `LocalTransaction.java` - Transaction handling
   - `DotSQLGeneratorTask.java` - SQL generation utilities

3. **Configuration Files** (2 days)
   - `hibernate.cfg.xml` - Main configuration
   - HBM mapping files - Entity definitions

4. **Custom Types** (15 days)
   - `UUIDGenerator.java` - Custom ID generator
   - `BooleanType.java` - Custom boolean type
   - `IntegerType.java` - Custom integer type
   - `NoCacheProvider.java` - Cache provider

### 4.2 Parallel Track (Can run simultaneously)

5. **Liferay Persistence** (20 days)
   - 11 Liferay persistence files
   - Systematic pattern replacement
   - Batch testing approach

6. **Business Logic** (8 days)
   - 10 business logic files
   - Mostly import replacements
   - Low complexity updates

### 4.3 Final Integration (Sequential)

7. **Testing Infrastructure** (10 days)
   - Update test files
   - Integration testing
   - Performance validation

8. **Documentation** (3 days)
   - Update API documentation
   - Migration guides
   - Architecture documentation

## 5. Testing Strategy

### 5.1 Test Categories

#### Unit Tests
- **Scope**: Individual component testing
- **Focus**: API compatibility, method behavior
- **Timeline**: Parallel with development

#### Integration Tests
- **Scope**: Cross-component interaction
- **Focus**: Transaction handling, session management
- **Timeline**: After core infrastructure updates

#### Performance Tests
- **Scope**: System-wide performance impact
- **Focus**: Database connection pooling, query performance
- **Timeline**: After full migration

### 5.2 Test Phases

#### Phase 1: Foundation Testing (Week 1-2)
- HibernateUtil functionality
- Transaction management
- Session handling
- Configuration loading

#### Phase 2: Integration Testing (Week 3-4)
- Cross-component interaction
- Cache invalidation
- Connection pooling
- Error handling

#### Phase 3: System Testing (Week 5-6)
- Full system integration
- Performance benchmarking
- Load testing
- User acceptance testing

### 5.3 Test Automation

```bash
# Automated test execution
./mvnw clean test -Dtest=HibernateUtil*Test
./mvnw clean test -Dtest=*PersistenceTest
./mvnw clean verify -Dcoreit.test.skip=false
```

## 6. Rollback Plan

### 6.1 Rollback Strategy

#### Git Branch Strategy
- Create feature branch: `feature/hibernate-5.6-migration`
- Maintain main branch stability
- Use feature flags for gradual rollout

#### Rollback Triggers
- **Performance degradation** > 20%
- **Critical functionality** failures
- **Data integrity** issues
- **Memory/resource** problems

#### Rollback Procedure
1. **Immediate**: Disable feature flags
2. **Short-term**: Revert to main branch
3. **Long-term**: Analyze issues and re-plan

### 6.2 Rollback Testing

#### Pre-Migration Baseline
- Performance metrics
- Functionality test results
- Memory usage patterns
- Error rates

#### Post-Migration Monitoring
- Real-time performance monitoring
- Error rate tracking
- Resource utilization alerts
- User experience metrics

## 7. Migration Execution Plan

### 7.1 Pre-Migration Phase (Week 0)

#### Environment Setup
- [ ] Create feature branch
- [ ] Set up testing environment
- [ ] Backup current system
- [ ] Performance baseline measurement

#### Team Preparation
- [ ] Code review assignments
- [ ] Testing responsibilities
- [ ] Communication plan
- [ ] Rollback procedures

### 7.2 Migration Phase (Week 1-4)

#### Week 1: Foundation
- [ ] Update Maven dependencies
- [ ] Migrate HibernateUtil.java
- [ ] Update configuration files
- [ ] Basic functionality testing

#### Week 2: Core Components
- [ ] Migrate custom types
- [ ] Update cache provider
- [ ] Migrate LocalTransaction
- [ ] Integration testing

#### Week 3: Persistence Layer
- [ ] Migrate Liferay persistence files
- [ ] Update business logic files
- [ ] Cross-component testing
- [ ] Performance testing

#### Week 4: Final Integration
- [ ] Complete remaining files
- [ ] Full system testing
- [ ] Performance validation
- [ ] Documentation updates

### 7.3 Post-Migration Phase (Week 5-6)

#### Validation
- [ ] Comprehensive testing
- [ ] Performance benchmarking
- [ ] User acceptance testing
- [ ] Documentation completion

#### Deployment
- [ ] Staged rollout
- [ ] Production monitoring
- [ ] Issue resolution
- [ ] Final validation

## 8. Success Criteria

### 8.1 Functional Criteria
- [ ] All existing functionality preserved
- [ ] No regression in user experience
- [ ] All tests passing
- [ ] No data integrity issues

### 8.2 Performance Criteria
- [ ] Response times within 10% of baseline
- [ ] Memory usage within acceptable limits
- [ ] No connection pool exhaustion
- [ ] Database query performance maintained

### 8.3 Code Quality Criteria
- [ ] Modern Hibernate API usage
- [ ] Eliminated repackaged dependencies
- [ ] Improved maintainability
- [ ] Updated documentation

## 9. Risk Mitigation

### 9.1 Technical Risks

#### API Compatibility Issues
- **Risk**: Hibernate 5.6.15 API changes break functionality
- **Mitigation**: Comprehensive wrapper layer with backward compatibility

#### Performance Degradation
- **Risk**: Modern Hibernate performs differently
- **Mitigation**: Extensive performance testing and tuning

#### Data Integrity Issues
- **Risk**: Transaction handling changes affect data consistency
- **Mitigation**: Thorough transaction testing and validation

### 9.2 Project Risks

#### Timeline Delays
- **Risk**: Complex migration takes longer than estimated
- **Mitigation**: Phased approach with fallback options

#### Resource Constraints
- **Risk**: Team availability impacts progress
- **Mitigation**: Clear task assignment and parallel workstreams

#### Integration Challenges
- **Risk**: Components don't integrate properly after migration
- **Mitigation**: Incremental integration with continuous testing

## 10. Conclusion

Phase 2 represents a significant modernization effort that will eliminate the last major repackaged Hibernate dependency. The systematic approach outlined above provides a clear path forward while minimizing risks and ensuring system stability.

The migration will result in:
- ✅ Modern Hibernate 5.6.15 infrastructure
- ✅ Elimination of repackaged dependencies
- ✅ Improved maintainability and performance
- ✅ Foundation for future jakarta migration

**Next Steps**: Approve plan and begin Week 0 preparation activities.