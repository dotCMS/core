# Data Model: Grid view for Content Drive (#37930)

Frontend-only. No server data changes; the grid renders the same `DotContentDriveItem[]` page the
table already gets from `POST /api/v1/drive/search`.

## DotContentDriveViewType

| Value | Meaning |
|-------|---------|
| `list` | The table (default) |
| `card` | The grid of cards |

Parsed case-insensitively from any source; anything else is "not usable" and skipped.

## View request (inputs to `resolveViewType`)

| Field | Source | Wins over |
|-------|--------|-----------|
| `urlViewType` | `?viewType=` | everything |
| `storedViewType` | `localStorage['dotcms.content-drive.view-type.<portletId>']` | tool default, table |
| `defaultViewType` | `?defaultViewType=` (sent by a custom tool, #37931) | table |

Result: the first usable value, else `list`.

## Store state added (`DotContentDriveState`)

| Field | Type | Notes |
|-------|------|-------|
| `viewType` | `DotContentDriveViewType` | Set at init from `resolveViewType`; changed by the switcher; left alone by Back/Forward (FR-019a) |
| `viewTypeStorageKey` | `string` | Worked out once at init from the hosting portlet's id; every remembered-view read and write uses it |

Methods: `setViewType(viewType, { remember?: boolean })` — `remember: true` (the switcher only)
also writes the storage key; failures are swallowed.

## Shell-held listing state

| Signal | Type | Notes |
|--------|------|-------|
| `$activeListingKey` | `string \| null` | `inode` of the item holding the listing's tab stop and anchor; reported by whichever view is showing, handed to the other on a switch |

## Card view model (derived per item, not stored)

| Field | Content | Folder |
|-------|---------|--------|
| title | `title` | `name` |
| editor name | `modUserName` (fallback: "Unknown") | `owner` (fallback: "Unknown") |
| status | `dot-contentlet-status-badge [state]=item` | none |
| language | `languageId` → locale tag | none |
| type | `contentType` | "Folder" |
| lock | `locked` + `lockedBy` (`{firstName,lastName}` or id) | none |
| preview | `dot-content-thumbnail [contentlet]=item` (video = still frame) | folder icon |
| busy | `inode` or `identifier` in `busyRows` | same |
