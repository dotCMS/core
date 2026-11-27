# Data Model: New Edit Contentlet UI polish (#37958)

No persisted data, API payloads, store state or in-code shapes change.

After the scope change of 2026-10-15, neither `DateOptions.icon` (`calendar-field.util.ts`)
nor the `icon` input of `dot-chip-filter` (`libs/ui`) changes: both keep their PrimeIcons
class strings, as on `main`.
