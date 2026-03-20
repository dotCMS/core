---
paths:
  - "**/pom.xml"
  - "bom/**/*"
  - "parent/**/*"
---

# Maven Build System

## Dependency management (CRITICAL)

```
parent/pom.xml              # Global properties, plugin management
  bom/application/pom.xml   # Dependency management (ALL versions go here)
    dotCMS/pom.xml           # Module dependencies (NO versions — inherited from BOM)
```

- **Versions** go ONLY in `bom/application/pom.xml` as properties + `<dependencyManagement>`.
- **Dependencies** in `dotCMS/pom.xml` and other modules reference groupId:artifactId without `<version>`.
- **Never** add a `<version>` tag directly in a module pom.xml.

## Build commands
```bash
just build                     # full build (~8-15 min)
just build-quicker             # core only (~2-3 min)
```

## On-demand
- `docs/backend/MAVEN_BUILD_SYSTEM.md` -- full BOM patterns, plugin management, examples
