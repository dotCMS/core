#!/usr/bin/env python3
"""Find integration tests that fail without reporting why.

The pattern that just cost a CI run:

    try  { doThing(); }
    catch (Exception e) { Assert.assertTrue("No Exception should be thrown", false); }

The test fails, but the caught exception is discarded, so the CI log says nothing
about the actual cause. Retries just repeat the empty message.

Classifies each catch block whose body FAILS the test:
  SWALLOWED  - fails, never references the exception variable  (the bad case)
  REPORTED   - fails and passes the exception / its message along
Also flags empty catch blocks, which hide failures entirely.
"""
import os, re, os, glob, collections

ROOT = os.path.join(os.environ.get("REPO_ROOT", os.getcwd()), "dotcms-integration/src/test/java")

CATCH = re.compile(r'catch\s*\(\s*([\w.]+(?:\s*\|\s*[\w.]+)*)\s+(\w+)\s*\)\s*\{')
# something that makes the test fail
FAILS = re.compile(r'\b(fail|assertTrue|assertFalse|assertEquals|assertNotNull|Assert\.fail)\b')
# an unconditional failure, i.e. the catch exists only to fail
HARD_FAIL = re.compile(
    r'\bfail\s*\(|\bAssert\.fail\s*\(|assertTrue\s*\([^;]*,\s*false\s*\)|assertFalse\s*\([^;]*,\s*true\s*\)')


def body_of(src, open_idx):
    """Return the text inside the braces starting at open_idx (index of '{')."""
    depth, i = 0, open_idx
    while i < len(src):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return src[open_idx + 1:i]
        i += 1
    return ''


swallowed, empty, reported = [], [], []
for path in glob.glob(ROOT + "/**/*.java", recursive=True):
    src = open(path, errors='ignore').read()
    rel = path[len(ROOT) + 1:]
    for m in CATCH.finditer(src):
        var = m.group(2)
        body = body_of(src, m.end() - 1)
        line = src[:m.start()].count('\n') + 1
        stripped = re.sub(r'//[^\n]*|/\*.*?\*/', '', body, flags=re.S).strip()

        if not stripped:
            empty.append((rel, line, m.group(1)))
            continue
        if not HARD_FAIL.search(stripped):
            continue                      # catch doesn't unconditionally fail
        # does it carry the exception anywhere?
        carries = re.search(rf'\b{re.escape(var)}\b', stripped)
        (reported if carries else swallowed).append((rel, line, m.group(1), stripped[:90]))

print(f"scanned {len(glob.glob(ROOT + '/**/*.java', recursive=True))} integration test files\n")
print(f"SWALLOWED (fails without reporting the cause): {len(swallowed)}")
print(f"REPORTED  (fails and includes the exception):  {len(reported)}")
print(f"EMPTY catch blocks (hide failures entirely):   {len(empty)}\n")

by_file = collections.Counter(f for f, *_ in swallowed)
print("=== worst files (swallowed catches) ===")
for f, n in by_file.most_common(20):
    print(f"  {n:>3}  {f}")

print("\n=== every swallowed catch ===")
for f, line, exc, snippet in sorted(swallowed):
    one = ' '.join(snippet.split())
    print(f"  {f}:{line}  catch({exc})  ->  {one[:80]}")

if empty:
    print("\n=== empty catch blocks ===")
    for f, line, exc in sorted(empty)[:25]:
        print(f"  {f}:{line}  catch({exc}) {{}}")
