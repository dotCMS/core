# Security Policy

For more information regarding dotCMS security polices and known security issues see our documentation site:
https://dotcms.com/docs/latest/security-and-privacy

## Supported Versions

dotCMS is commited to backporting security fixes to our LTS version. For an up to date list of which versions are supported, please see:
https://www.dotcms.com/docs/latest/current-releases

When possible, dotCMS will also provide a workaround that can remediate a security issue without having to update you dotCMS version.

## Reporting a Vulnerability

Please see our responsible disclosure policy here:
https://dotcms.com/docs/latest/responsible-disclosure-policy

## Known Security Scanner False Positives

Some automated security scanners may report vulnerabilities that do not actually affect dotCMS. This section documents known false positives to help security teams during compliance reviews.

### CVE-2022-22965 (Spring4Shell)

**Status:** ❌ NOT VULNERABLE - False Positive

**CVE Details:**
- **Vulnerability:** Remote Code Execution in Spring Framework
- **Affected Versions:** Spring Framework 5.3.0-5.3.17, 5.2.0-5.2.19, and older
- **Attack Vector:** Spring MVC/WebFlux data binding vulnerability

**Why dotCMS is Not Vulnerable:**
- dotCMS does **not use Spring Framework**
- Spring libraries were **removed in version 22.06** (June 2022)
- All versions 22.06+ do not contain Spring dependencies
- dotCMS uses **JAX-RS (Jersey)** for REST APIs, not Spring MVC
- dotCMS uses **CDI (Weld)** for dependency injection, not Spring IoC

**Why Scanners Flag This:**
- URL patterns like `/api/v1/*` resemble Spring MVC endpoints
- Tomcat presence triggers heuristic matching (CVE mentions Tomcat)
- Pattern-based scanners cannot distinguish JAX-RS from Spring MVC

**Verification:**
```bash
# Verify no Spring JARs in distribution
find WEB-INF/lib/ -name "*spring*.jar"  # Returns nothing

# Verify no Spring dependencies
./mvnw dependency:tree | grep spring  # Returns nothing
```

**For Security Teams:**
See detailed verification steps and evidence package at [docs/security/SECURITY_SCANNER_FAQ.md](docs/security/SECURITY_SCANNER_FAQ.md)

---

**Have questions about a security scanner finding?** Contact security@dotcms.com
