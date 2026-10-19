# Data Model: Configuration API gaps

No schema change. One stored field changes how it is validated and read, and one new response
view is added.

## Company (existing row, `company` table)

| Field (API) | Column | Change |
|-------------|--------|--------|
| `backgroundImage` | `homeURL` (varchar 100) | **Write** accepts: empty or absent (stored as blank) · `/dA...` · one of the 11 presets · a value equal to what is stored. Anything else → 400. **Read** returns the stored value; blank and `localhost` → `null`. |

All other branding fields are unchanged.

### Accepted presets

`/html/images/backgrounds/bg-1.jpg`, `bg-2.jpg`, … `bg-11.jpg` (exact strings, all under
`/html/images/backgrounds/`).

## LicenseInfoView (new, response only)

| Field | Type | Source | When the file can't be read or a line is missing |
|-------|------|--------|---------------------------------------------------|
| `title` | string, non-null | "dotCMS Business Source License" + ` <version>` from the line `Business Source License <version>` | "dotCMS Business Source License" |
| `licensor` | string, nullable | `Licensor:` line | `null` |
| `changeDate` | string, nullable | `Change Date:` line | `null` |
| `changeLicense` | string, nullable | `Change License:` line | `null` |
| `text` | string, non-null | `LicenseUtil.getLicenseText()` | the existing fallback text |

Values are trimmed. They are returned as written in the file; `changeDate` is free text
(e.g. "Four years from August 01, 2025"), not a date type.
