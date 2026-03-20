---
paths:
  - "**/*.java"
  - "dotCMS/src/**/*"
---

# Java Backend Context

## Imports
```java
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.util.CollectionsUtils;
```

## Patterns
- Config: `Config.getStringProperty("key", "default")` -- never System.getProperty.
- Logging: `Logger.info(this, "msg")` -- never System.out.
- Null: `UtilMethods.isSet(s)` before use.
- APIs: `APILocator.getUserAPI()`, etc.
- Exceptions: DotDataException, DotSecurityException.
- Data classes: `@Value.Immutable` + builder.

## Java
- Core: Java 11 release compatibility. CLI: Java 21 ok. Runtime: Java 21. Migrating to Java 25 (parallel CI workflows validate forward compatibility).

## Build
```bash
just build                           # full build (~8-15 min)
just build-quicker                   # core only (~2-3 min)
```

## On-demand
- `docs/backend/JAVA_STANDARDS.md`
- `docs/backend/REST_API_PATTERNS.md`
- `docs/backend/MAVEN_BUILD_SYSTEM.md`
- `docs/backend/CONFIGURATION_PATTERNS.md`
- `docs/backend/DATABASE_PATTERNS.md`
