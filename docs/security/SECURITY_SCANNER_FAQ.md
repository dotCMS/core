# Security Scanner FAQ

This document helps security teams and customers understand common security scanner false positives and how to verify dotCMS security posture.

## Table of Contents
- [Common False Positives](#common-false-positives)
- [Framework Architecture](#framework-architecture)
- [Verification Steps](#verification-steps)
- [For Security Teams](#for-security-teams)

---

## Common False Positives

### CVE-2022-22965 (Spring4Shell)

**Status:** ❌ NOT VULNERABLE - False Positive

**CVE Details:**
- **Affected:** Spring Framework 5.3.0-5.3.17, 5.2.0-5.2.19, and older versions
- **Vulnerability:** Remote Code Execution via Spring's data binding mechanism
- **Requirements:** Spring MVC/WebFlux + JDK 9+ + Tomcat

**Why dotCMS is Not Vulnerable:**
- dotCMS **does not use Spring Framework**
- Spring libraries were removed in **version 22.06** (June 2022)
- All current versions (22.06+) do not contain Spring dependencies

**Why Scanners Flag This:**
1. **URL Pattern Matching:** dotCMS REST APIs use patterns like `/api/v1/*` which superficially resemble Spring MVC patterns
2. **Tomcat Detection:** dotCMS runs on Tomcat, and the CVE mentions Tomcat in the attack vector
3. **Heuristic Analysis:** JAX-RS endpoints can look similar to Spring MVC endpoints in HTTP traffic
4. **Framework Confusion:** Pattern-based scanners cannot distinguish between JAX-RS and Spring MVC

**What dotCMS Actually Uses:**
- ✅ **JAX-RS (Jersey 2.47)** for REST APIs (not Spring MVC)
- ✅ **CDI/Weld 3.1.9** for dependency injection (not Spring IoC)
- ✅ **Jackson 2.17.2** for JSON processing (not Spring Data)
- ✅ **Tomcat 9.x** as servlet container (shared with Spring apps, but no Spring code)

**Framework Comparison:**

| Feature | Spring MVC | dotCMS (JAX-RS) |
|---------|-----------|----------------|
| REST Framework | `@RequestMapping` | `@Path`, `@GET`, `@POST` |
| Dependency Injection | `@Autowired` | `@Inject` (CDI) |
| URL Pattern | `/api/v1/users` | `/api/v1/users` *(looks same!)* |
| Data Binding | `WebDataBinder` | JAX-RS parameter binding |
| Vulnerable to CVE-2022-22965 | ✅ Yes (if version affected) | ❌ No (framework not present) |

---

## Framework Architecture

### REST API Architecture

dotCMS uses **JAX-RS (Java API for RESTful Web Services)** implemented by **Jersey**, not Spring MVC.

**Example Endpoint:**
```java
// dotCMS JAX-RS endpoint (NOT Spring)
@Path("/v1/users")
public class UserResource {

    @Inject  // CDI injection, NOT Spring @Autowired
    private UserAPI userAPI;

    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@PathParam("userId") String userId) {
        // Jersey handles request routing and parameter binding
        // No Spring framework involved
        return Response.ok(userAPI.loadUserById(userId)).build();
    }
}
```

**Contrast with Spring MVC:**
```java
// Spring MVC endpoint (NOT used in dotCMS)
@RestController
@RequestMapping("/v1/users")
public class UserController {

    @Autowired  // Spring DI, NOT used in dotCMS
    private UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable String userId) {
        // Spring MVC handles routing
        // This code does NOT exist in dotCMS
        return ResponseEntity.ok(userService.findById(userId));
    }
}
```

### Dependency Injection

dotCMS uses **CDI (Contexts and Dependency Injection)** with **Weld** implementation, not Spring IoC container.

**dotCMS Pattern:**
```java
@ApplicationScoped  // CDI scope
public class MyService {

    @Inject  // CDI injection
    private ContentletAPI contentletAPI;

    public void doWork() {
        // CDI manages lifecycle, not Spring
    }
}
```

### Spring Removal History

**Timeline:**
- **Before 22.06:** Spring libraries present but unused (legacy dependencies)
- **April 2022:** CVE-2022-22965 (Spring4Shell) disclosed
- **June 2022 (v22.06):** dotCMS removed all unused Spring libraries
- **Current (v26.x+):** Zero Spring dependencies

**What Was Removed:**
- `spring-core`
- `spring-beans`
- `spring-context`
- `spring-web`
- `spring-webmvc`
- All related Spring Framework JARs

**Why It Was Removed:**
- Defense-in-depth security practice
- Reduce attack surface
- Eliminate unused dependencies
- Prevent false security scanner alerts

---

## Verification Steps

### For DevOps/Security Teams

#### 1. Verify No Spring JARs in Deployment

**Location to check:**
```bash
# Navigate to your dotCMS installation
cd /path/to/dotcms/tomcat/webapps/ROOT/WEB-INF/lib/

# List all JAR files and search for Spring
ls -la | grep -i spring

# Expected result: NO spring-*.jar files found
```

**What you should NOT see:**
- `spring-core-*.jar`
- `spring-web-*.jar`
- `spring-beans-*.jar`
- `spring-context-*.jar`
- Any file starting with `spring-`

#### 2. Maven Dependency Tree Analysis

**From dotCMS source code:**
```bash
# Clone dotCMS repository
git clone https://github.com/dotCMS/core.git
cd core

# Generate dependency tree
./mvnw dependency:tree > dependencies.txt

# Search for Spring dependencies
grep -i spring dependencies.txt

# Expected result: NO matches (except possibly comments)
```

#### 3. Source Code Verification

**Check for Spring imports:**
```bash
# Search all Java files for Spring imports
grep -r "import org.springframework" dotCMS/src/

# Expected result: NO matches found
```

#### 4. Bill of Materials (BOM) Review

**Check dependency management:**
```bash
# Review the BOM file that manages all dependencies
cat bom/application/pom.xml | grep -i spring

# Expected result: NO Spring dependencies declared
```

### Automated Verification Script

```bash
#!/bin/bash
# verify-no-spring.sh - Verify dotCMS does not contain Spring Framework

echo "Checking dotCMS installation for Spring Framework..."
DOTCMS_HOME="/path/to/dotcms"

# Check 1: JAR files
echo "1. Checking for Spring JAR files..."
SPRING_JARS=$(find "$DOTCMS_HOME/tomcat/webapps/ROOT/WEB-INF/lib" -name "*spring*.jar" 2>/dev/null)
if [ -z "$SPRING_JARS" ]; then
    echo "   ✅ PASS: No Spring JAR files found"
else
    echo "   ❌ FAIL: Spring JAR files detected:"
    echo "$SPRING_JARS"
    exit 1
fi

# Check 2: Class files
echo "2. Checking for Spring class files..."
SPRING_CLASSES=$(find "$DOTCMS_HOME/tomcat/webapps/ROOT/WEB-INF/classes" -path "*/org/springframework/*" 2>/dev/null)
if [ -z "$SPRING_CLASSES" ]; then
    echo "   ✅ PASS: No Spring class files found"
else
    echo "   ❌ FAIL: Spring class files detected"
    exit 1
fi

echo ""
echo "✅ Verification Complete: Spring Framework NOT present in dotCMS"
echo "   Result: CVE-2022-22965 does NOT apply"
```

---

## For Security Teams

### Evidence Package for Compliance

When dealing with security compliance reviews or auditor questions:

**1. Dependency Declaration**
- **File:** `bom/application/pom.xml`
- **Evidence:** No Spring dependencies declared in Bill of Materials
- **Source:** https://github.com/dotCMS/core/blob/main/bom/application/pom.xml

**2. Architecture Documentation**
- **Framework:** JAX-RS (Jersey), not Spring MVC
- **DI Container:** CDI (Weld), not Spring IoC
- **Evidence:** `CLAUDE.md` and `docs/backend/` documentation

**3. Release Notes**
- **Version:** 22.06 (June 2022)
- **Change:** Removed unused Spring Framework libraries
- **Rationale:** Security best practices, reduce attack surface

**4. Source Code Review**
- **Search Pattern:** `org.springframework`
- **Result:** Zero imports found in codebase
- **Verification:** Can be audited via GitHub repository

### Scanner Configuration Recommendations

**Problem:** Pattern-based scanners flag false positives

**Solution:** Configure scanners for accurate detection

#### Option 1: Whitelist Specific CVEs
```yaml
# Example for common scanning tools
exceptions:
  - cve: CVE-2022-22965
    product: dotCMS
    rationale: "False positive - Spring Framework not present, uses JAX-RS"
    verified_by: "Security Team"
    verified_date: "YYYY-MM-DD"
    verification_method: "Manual JAR inspection + dependency tree analysis"
```

#### Option 2: Use File-Based Detection
Configure scanner to detect **actual JAR files**, not URL patterns:
- ✅ Accurate: Checks for `spring-*.jar` presence
- ❌ Inaccurate: Matches URL patterns like `/api/v1/*`

#### Option 3: Use Multiple Scanning Tools
Cross-validate with different tools to reduce false positives:
- **OWASP Dependency-Check:** Analyzes actual JARs and dependencies
- **Snyk:** Understands framework differences
- **Grype:** Accurate dependency scanning
- **Trivy:** Container and dependency scanning

**Recommended Command:**
```bash
# OWASP Dependency-Check (more accurate)
dependency-check --project dotCMS --scan /path/to/dotcms/WEB-INF/lib

# Expected result: No Spring-related vulnerabilities
```

### Audit Trail Documentation

For compliance documentation, include:

**✅ What to Document:**
1. Date of verification
2. Person/team who performed verification
3. Methods used (manual inspection, automated tools)
4. Findings (no Spring JARs present)
5. Screenshot of scanner exception/whitelist
6. Approval by security lead

**Example Audit Entry:**
```
CVE-2022-22965 False Positive Review

Date: 2026-02-24
Reviewer: Security Team Lead
Product: dotCMS 26.01.22
Finding: False Positive

Verification Performed:
1. Manual inspection of WEB-INF/lib - No spring-*.jar files
2. Maven dependency tree analysis - No Spring dependencies
3. Source code review - No org.springframework imports
4. Alternative tool scan (OWASP DC) - Confirmed no Spring

Decision: Whitelist CVE-2022-22965 for dotCMS
Rationale: Vulnerability requires Spring Framework which is not present
Approved By: [Security Lead Name]
Next Review: 2027-02-24 (annual review)
```

### Risk Assessment Template

```markdown
## CVE-2022-22965 Risk Assessment

**Vulnerability:** Spring4Shell Remote Code Execution
**Severity:** Critical (CVSS 9.8)
**Product:** dotCMS 26.01.22

### Applicability Analysis
| Requirement | Present in dotCMS? | Notes |
|-------------|-------------------|-------|
| Spring Framework 5.3.0-5.3.17 | ❌ No | Not present since v22.06 |
| JDK 9 or higher | ✅ Yes | Java 21 runtime |
| Apache Tomcat | ✅ Yes | Tomcat 9.x |
| Spring MVC/WebFlux | ❌ No | Uses JAX-RS (Jersey) |

### Risk Level
**NONE** - Required vulnerable component (Spring Framework) is not present

### Recommended Actions
1. ✅ Document false positive in scanner
2. ✅ Add to whitelist/exception list
3. ✅ Schedule annual review
4. ✅ Proceed with deployment

### Approval
- Security Team: Approved
- Compliance: Approved
- Date: YYYY-MM-DD
```

---

## Additional Resources

### dotCMS Security Resources
- **Security Policy:** https://github.com/dotCMS/core/security/policy
- **Security Advisories:** https://github.com/dotCMS/core/security/advisories
- **Support:** support@dotcms.com
- **Security Issues:** security@dotcms.com

### External References
- **CVE-2022-22965 Details:** https://nvd.nist.gov/vuln/detail/CVE-2022-22965
- **Spring Framework Advisory:** https://spring.io/security/cve-2022-22965
- **JAX-RS Specification:** https://jakarta.ee/specifications/restful-ws/

### Similar False Positives in Other Products

Other software that uses JAX-RS + Tomcat (not Spring) may experience similar false positives:
- WildFly/JBoss applications using RESTEasy
- Payara/GlassFish applications using Jersey
- Apache TomEE applications using CXF
- Any Java application using JAX-RS on Tomcat

This is a **known pattern-based scanner limitation**, not a dotCMS-specific issue.

---

## Contact

For questions about this documentation or security verification:
- **Technical Support:** support@dotcms.com
- **Security Inquiries:** security@dotcms.com
- **Emergency:** 1-800-509-1676 (Critical Care line)

**Last Updated:** 2026-02-24
**Applies to:** dotCMS 22.06 and later
