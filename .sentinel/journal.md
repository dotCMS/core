## 2026-05-12 — Insecure Deserialization in App Secrets Import [A08 / CWE-502]
**Pattern**: Use of `ObjectInputStream` without class filtering in a security-sensitive context (importing encrypted secrets).
**Root cause**: `VersionOverrideObjectInputStream` only handled version mismatches but did not restrict which classes could be instantiated, allowing attackers to trigger gadget chains.
**Prevention**: Always implement `resolveClass` with a strict FQCN allowlist and `resolveProxyClass` to reject dynamic proxies when using Java serialization for untrusted or user-supplied data.
