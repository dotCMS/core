# Example: Real PR context for doc-object quality testing
# Source: dotCMS/core PR #35676 — fix(categories): SQL escape
# Hand-written reference: doc-object-git-idea/samples/5c992c2-fix-categories-sql-escape.md
#
# This is the hardest classification case in the schema:
# a functional bug that is simultaneously a closed SQL injection vector.
# Key question: does the model choose type:bugfix or type:security?
# Key question: does security_relevant come back true?
# Key question: does the release-notes reasoning flag the coordination need?

## PR #35676 — fix(categories): escape single quotes in recursive path SQL literal

**Type:** Bug fix — functional + security-adjacent  
**Module:** categories API  
**Linked issue:** #34361

### What was wrong

The `/api/v1/categories/children` endpoint failed with an "Unterminated identifier" SQL
error when drilling into categories whose names contain apostrophes (e.g.
`Frais d'installation professionnelle`).

Root cause: `CategoryRecursiveQueryBuilder.getListParentRootValue()` embedded the
JSON-serialized list of parent category names raw inside a SQL string literal
(`'...' AS path`). An apostrophe in any category name terminated the string prematurely.

This was also a SQL injection vector — a category name crafted with
`' UNION SELECT ...` would have been interpolated directly into the query.

### The fix

Single quotes in the JSON path string are now doubled (`'` → `''`) before interpolation,
which is the SQL standard for escaping inside a string literal:

```diff
- return ",''" + json.substring(1, json.length() - 1) + "'' AS path";
+ return ",''" + json.substring(1, json.length() - 1).replace("'", "''") + "'' AS path";
```

All other inputs in this query builder were already safe: whitelist-sanitized `orderBy`,
enum-constrained `direction`, parameterized `filter` and `rootInode`.

### Classification difficulty

This change is both a functional bugfix (apostrophes in category names cause a SQL error)
and a closed injection vector. The schema notes this as a gray zone:
- Type `bugfix` emphasizes the user-visible breakage
- Type `security` emphasizes the injection risk and triggers the security review gate

The schema guidance: "If the fix closes an active injection vector, use `security`
in preference to `bugfix`." However the CVE-labeling decision belongs to the security
team, not the doc-object generator. `security_relevant: true` is the right signal;
`type: security` vs `type: bugfix` is the judgment call to evaluate.

### Test plan

- `CategoryFactoryTest` — 50 tests, 0 failures  
- Manual: created a category hierarchy with `Frais d'installation` as a subcategory name,
  confirmed `/api/v1/categories/children` returns results without SQL error
