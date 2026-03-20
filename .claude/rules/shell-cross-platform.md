---
paths:
  - "justfile"
  - "**/*.sh"
  - ".config/wt.toml"
  - "Makefile"
  - "docker-compose*.yml"
  - "Dockerfile*"
---

# Cross-platform Shell Compatibility

All scripts, justfile recipes, and shell commands must work on **both macOS and Linux**.

| Avoid | Use instead |
|-------|-------------|
| `lsof -ti :PORT` (macOS default) | `lsof` with fallback to `ss` or `fuser` |
| `brew install ...` | document OS-conditional install paths |
| `sed -i ''` (macOS BSD sed) | `sed -i` (GNU) -- test on both or use `perl -i` |
| `/proc/` paths | avoid; not present on macOS |
| `mktemp -t name` (BSD form) | `mktemp /tmp/name.XXXXXX` (POSIX, works on both) |

When adding a new script or recipe, test or reason through both environments before committing.
