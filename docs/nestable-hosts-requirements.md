# Nestable Hosts — Requirements Document

**Feature branch:** `allow-host-nesting`
**Status:** Requirements finalized
**Audience:** Java backend developers, Angular frontend developers

---

## Table of Contents

1. [Overview](#1-overview)
2. [Data Model](#2-data-model)
3. [URL Structure and Resolution](#3-url-structure-and-resolution)
4. [Host API Changes](#4-host-api-changes)
5. [NestedHostPatternCache](#5-nestedhostpatterncache)
6. [Host Resolution Service](#6-host-resolution-service)
7. [CMSFilter and Request Lifecycle](#7-cmsfilter-and-request-lifecycle)
8. [Reparenting](#8-reparenting)
9. [Permissions](#9-permissions)
10. [Delete and Archive](#10-delete-and-archive)
11. [findDescendantHostIdentifiers](#11-finddescendanthostidentifiers)
12. [Push Publishing](#12-push-publishing)
13. [REST API — /api/v1/site](#13-rest-api----apiv1site)
14. [Browser Tree and ContentDrive](#14-browser-tree-and-contentdrive)
15. [Angular UI — DotBrowserSelectorStore](#15-angular-ui----dotbrowserselectorstore)
16. [Content Editor Integration](#16-content-editor-integration)
17. [Isolation and Sharing](#17-isolation-and-sharing)
18. [Background Tasks](#18-background-tasks)
19. [Startup Task](#19-startup-task)
20. [Cycle Detection](#20-cycle-detection)
21. [License](#21-license)
22. [Content Import](#22-content-import)
23. [Files and Components — Created or Modified](#23-files-and-components--created-or-modified)
24. [Out of Scope](#24-out-of-scope)

---

## 1. Overview

Today every host in dotCMS lives directly under System Host. This feature allows a host to have another host (or a folder within a host) as its parent, creating an arbitrary-depth hierarchy. The hierarchy surfaces in URLs as path segments, drives permission inheritance, and enables multi-tenant / regional site hierarchies within a single dotCMS installation.

### Key concepts

| Term | Definition |
|---|---|
| **Top-level host** | A host whose `Identifier.hostId` equals `SYSTEM_HOST_ID`. Has a real domain name. |
| **Nested host** | A host whose `Identifier.hostId` refers to another host (not System Host). Its "hostname" is a path-segment label, not a domain. |
| **Segment label** | The value returned by `getHostname()` for a nested host; used as a URL path segment. |
| **parentPath** | The folder path within the parent host under which this host lives (e.g., `/en/`). |
| **Absolute base URL** | The fully-qualified URL prefix for a host, computed by `getAbsoluteBaseUrl()`. |

### Non-goals (explicitly out of scope)

- Automatic redirects when a host is reparented
- Additional audit instrumentation beyond the standard contentlet audit trail
- Any new license gating — the feature is available on all license levels including Community

---

## 2. Data Model

### 2.1 Identifier table changes

No new columns are added. The existing columns are reused with extended semantics.

| Column | Existing meaning | Extended meaning for nested hosts |
|---|---|---|
| `host_inode` (`Identifier.hostId`) | UUID of the containing site | For a nested host's own Identifier row: UUID of the **parent host** |
| `parent_path` (`Identifier.parentPath`) | Folder path within the host | For a nested host: full ancestor folder path within the parent host (e.g., `/en/`) |
| `asset_name` (`Identifier.assetName`) | Filename or folder name | For hosts: the **hostname** (segment label). Must be kept in sync with the Host contentlet's hostname field. |
| `asset_type` | Type discriminator | `'contentlet'` for hosts (same as all contentlets) |
| `asset_subtype` | Content type discriminator | `'Host'` — this is how you identify a host Identifier row |

### 2.2 Identifier.asset_name for hosts

The `asset_name` column on the `Identifier` row for a host must equal the host's hostname (segment label). This is critical for:

- **NestedHostPatternCache**: patterns are built from `parentPath + asset_name`.
- **URL resolution**: the segment label in the URL path must match `asset_name`.
- **`getAbsoluteBaseUrl()`**: reads `parentPath` + `asset_name` to construct the full path.
- **Push publish import ordering**: sorts by `(parent_path + asset_name).length()`.

The startup task (see [Section 19](#19-startup-task)) must also update all existing host Identifier rows to set `asset_name = hostname` for hosts where this is not already the case.

When a host is renamed, `asset_name` on the Identifier must be updated to match the new hostname.

### 2.3 Top-level detection

A host is top-level if and only if:

```
Identifier.hostId == SYSTEM_HOST_ID
```

### 2.4 Existing host rows

Existing hosts have `hostId = SYSTEM_HOST_ID` and `parentPath = /`. The startup task updates `Identifier.asset_name` to match the hostname for all existing host records (see [Section 19](#19-startup-task)).

### 2.5 Uniqueness constraint

The global uniqueness constraint on hostname/segment label is unchanged. No two hosts may share a label regardless of their position in the hierarchy.

---

## 3. URL Structure and Resolution

### 3.1 URL shape for nested hosts

A nested host contributes its ancestor folder path and its own segment label to the URL:

```
https://<top-level-domain>/<parentPath-segments>/<segment-label>/<content-path>
```

Example hierarchy:

```
dotcms.com                         (top-level, Identifier.hostId = SYSTEM_HOST_ID)
  └── /en/nestedHost1              (parentPath = /en/, hostname = nestedHost1)
        └── /en/nestedHost1/nestedHost2  (parentPath = /en/nestedHost1/, hostname = nestedHost2)
```

Resulting URLs:

| Host | Example page URL |
|---|---|
| `dotcms.com` | `https://dotcms.com/about` |
| `nestedHost1` | `https://dotcms.com/en/nestedHost1/about` |
| `nestedHost2` | `https://dotcms.com/en/nestedHost1/nestedHost2/about` |

### 3.2 Aliases

Aliases (alternate server names) are used only for top-level domain resolution via `serverName`. They are not used in path-segment matching for nested hosts.

### 3.3 Content paths are host-relative

A page stored at `/about.html` in `nestedHost2` has `parent_path = /` and `asset_name = about.html` relative to `nestedHost2`'s UUID. The absolute URL is computed at render time by `getAbsoluteBaseUrl()`. This means reparenting a host does not require reindexing content.

---

## 4. Host API Changes

### 4.1 `getHostname()` — unchanged

Returns the segment label only. Semantics are unchanged.

### 4.2 New method: `Host.getAbsoluteBaseUrl()`

Returns the scheme + full path prefix for the host.

**Algorithm:**

1. Load `Identifier` for this host.
2. If `Identifier.hostId == SYSTEM_HOST_ID`: return `"https://" + getHostname()`.
3. Else:
   a. Call `IdentifierAPI.getTopLevelHostId(this)` to find the root domain host.
   b. Load the top-level host and call `getHostname()` on it to get the domain.
   c. Read `Identifier.parentPath` for this host (e.g., `/en/nestedHost1/`).
   d. Strip the leading `/` from `parentPath`, then append `getHostname()`.
   e. Return `"https://" + topLevelDomain + "/" + strippedParentPath + getHostname()`.

**Example:**

```
parentPath  = /en/nestedHost1/
hostname    = nestedHost2
top domain  = dotcms.com

result      = https://dotcms.com/en/nestedHost1/nestedHost2
```

### 4.3 New method: `IdentifierAPI.getTopLevelHostId(Host host)`

Traverses the `Identifier.hostId` chain until it finds an entry where `hostId == SYSTEM_HOST_ID`. Returns the UUID of that entry (the top-level host).

- Uses `IdentifierCache` (no additional caching required).
- Throws `DotRuntimeException` if a cycle is detected (see [Section 20](#20-cycle-detection)).

### 4.4 Call-site updates for `getAbsoluteBaseUrl()`

All locations that currently construct an absolute URL with `"https://" + host.getHostname()` must be updated to call `host.getAbsoluteBaseUrl()`. Known call sites include:

- Sitemap generation
- Preview URL construction
- Canonical tag rendering
- `SiteSearchAPI`
- `HTMLPageAssetRenderedResource`

A codebase-wide search for `"https://" + ` concatenated with any host name accessor should be performed during implementation to ensure full coverage.

---

## 5. NestedHostPatternCache

### 5.1 Purpose

Provides fast path-prefix matching during request resolution without a database query per request.

### 5.2 Structure

- Keyed by **top-level host UUID**.
- Each value is an ordered list of compiled regex patterns for all descendant hosts at all depths.
- Patterns are ordered **longest-first** to ensure the most-specific match wins.

### 5.3 Pattern format

For a descendant host with `parentPath + hostname`:

```
^/<parentPath-without-leading-slash><hostname>(/.*)?$
```

Example: parentPath = `/en/nestedHost1/`, hostname = `nestedHost2`

```
^/en/nestedHost1/nestedHost2(/.*)?$
```

### 5.4 Cache lifecycle

| Aspect | Behavior |
|---|---|
| Scope | JVM-local |
| Build trigger | Lazy — built on first request after invalidation |
| Data source | `findDescendantHostIdentifiers` CTE (see [Section 11](#11-finddescendanthostidentifiers)) |
| Invalidation events | `SAVE_SITE`, `UPDATE_SITE`, `DELETE_SITE`, `PUBLISH_SITE`, `UN_PUBLISH_SITE`, `ARCHIVE_SITE` |
| Invalidation granularity | Only the affected top-level host's cache entry is dropped |
| Cluster propagation | PostgreSQL `LISTEN/NOTIFY` broadcasts invalidations; each node rebuilds lazily |

### 5.5 Reparent invalidation

When a host is reparented, a `HostReparentPayload` event is fired containing `oldTopLevelHostId` and `newTopLevelHostId`. Both cache entries are invalidated. If a host moves within the same top-level tree, both fields are identical — the double-invalidation is harmless.

---

## 6. Host Resolution Service

### 6.1 New service: `HostResolver`

`HostResolver` is a new dedicated service. It encapsulates all logic for determining which host handles a given HTTP request.

**Method signature:**

```java
HostResolutionResult resolve(String serverName, String fullUri);
```

**Return type `HostResolutionResult`:**

| Field | Type | Description |
|---|---|---|
| `resolvedHost` | `Host` | The host that owns the request |
| `remainingUri` | `String` | The URI after stripping the nested-host prefix |

### 6.2 Resolution algorithm

1. Look up the top-level host by `serverName` (existing logic).
2. Check `NestedHostPatternCache` for the top-level host UUID using `fullUri`.
3. If a pattern matches:
   a. Identify the matched nested host.
   b. Strip the matched prefix from `fullUri` to produce `remainingUri`.
   c. If `fullUri` exactly equals the nested host prefix with no trailing slash, set `remainingUri = "/"`.
   d. Return `(nestedHost, remainingUri)`.
4. If no pattern matches, return `(topLevelHost, fullUri)`.

### 6.3 No walk-up fallback

If content is not found on the resolved host, the result is a 404. There is no fallback to the parent host.

---

## 7. CMSFilter and Request Lifecycle

### 7.1 CMSFilter changes

`CMSFilter` invokes `HostResolver` at the start of request processing and stores the result in request attributes:

| Attribute key | Value |
|---|---|
| `CMS_FILTER_URI_OVERRIDE` | Overwritten with `remainingUri` from `HostResolutionResult` |
| `CMS_RESOLVED_HOST` | The `Host` object from `HostResolutionResult` (new attribute) |

### 7.2 HostWebAPIImpl changes

`HostWebAPIImpl.getCurrentHost()` updated to:

1. Check request attribute `CMS_RESOLVED_HOST` first.
2. Fall back to existing `serverName`-based lookup if attribute is absent.

### 7.3 REST Page API

`/api/v1/page/render` invokes `HostResolver` internally. Clients may pass a full hierarchical URL containing the top-level host; the API will resolve the correct nested host and remaining path transparently.

---

## 8. Reparenting

### 8.1 HostFolderField on the Host content type

A `HostFolderField` is added to the Host content type at startup (see [Section 19](#19-startup-task)). This field allows editors to select a parent host or folder. At save time, `HostAPIImpl` reads this field to compute the new `Identifier.hostId` and `parentPath`.

### 8.2 Reparent detection in HostAPIImpl

Before calling `checkin`, `HostAPIImpl` reads the current `Identifier.hostId` from the database. After reading the new `HostFolderField` value, it compares old vs. new `hostId`. If they differ, a reparent is detected.

### 8.3 Reparent workflow

All steps execute within the same transaction as `checkin`:

1. Execute `renameFolderChildren` stored procedure to cascade `parent_path` updates to all descendant host assets.
2. Build a `HostReparentPayload` with `oldTopLevelHostId` and `newTopLevelHostId`.
3. Fire `SystemEventsAPI` event with the payload.

`oldTopLevelHostId` is captured by calling `IdentifierAPI.getTopLevelHostId()` on the host **before** the save.

### 8.4 renameFolderChildren SP

The existing `renameFolderChildren(old_path, new_path, hostInode)` stored procedure is adapted for host reparenting. It cascades `parent_path` updates to all descendant host assets within the moved host's identifier tree.

Because content paths are host-relative, page content (e.g., `/about.html` in `nestedHost2`) requires no update and no reindex.

### 8.5 HostReparentPayload

New event payload type:

| Field | Type | Description |
|---|---|---|
| `oldTopLevelHostId` | `String` | UUID of the top-level host before the move |
| `newTopLevelHostId` | `String` | UUID of the top-level host after the move |

### 8.6 Reparenting a live top-level host

Reparenting a currently-live top-level host into a nested position is supported in the initial release. Redirects for old URLs are the operator's responsibility; no automatic redirects are generated.

---

## 9. Permissions

### 9.1 Permission inheritance chain

```
System Host → top-level host → nested child host → child host's content
```

### 9.2 Host.getParentPermissionable()

Updated to return the parent host resolved via `Identifier.hostId`, instead of the current hardcoded call to `findSystemHost()` for all non-system hosts.

**Logic:**

- If `Identifier.hostId == SYSTEM_HOST_ID`: return System Host (existing behavior).
- Else: load and return the host identified by `Identifier.hostId`.

No other changes to the permission subsystem are required.

---

## 10. Delete and Archive

### 10.1 Delete

Deleting a host is blocked if any descendants exist at any depth.

- `findDescendantHostIdentifiers` CTE is called before delete.
- If the result set is non-empty, the delete is rejected with an error message reporting the total descendant count.

### 10.2 Archive

Archiving a host cascades automatically to all descendants at all depths. Order is not significant; each `ARCHIVE_SITE` event independently invalidates the relevant `NestedHostPatternCache` entry.

### 10.3 Unarchive

Unarchive is manual. There is no cascade for unarchive — each host must be unarchived individually.

---

## 11. findDescendantHostIdentifiers

### 11.1 New method in IdentifierFactoryImpl

**Signature:**

```java
List<Identifier> findDescendantHostIdentifiers(String topLevelHostId)
```

### 11.2 Implementation

A `WITH RECURSIVE` CTE query on the `identifier` table:

- Selects all rows where `asset_type = 'contentlet'` and `asset_subtype = 'Host'`.
- Walks the `host_inode` chain recursively starting from `topLevelHostId`.
- Returns all descendant host Identifier records at every depth.

### 11.3 Consumers

| Consumer | Usage |
|---|---|
| `NestedHostPatternCache` | Build the pattern list for a top-level host |
| Delete validation | Block delete if result is non-empty |
| Cascade archive | Drive the cascade archive workflow |

---

## 12. Push Publishing

### 12.1 Dependency bundle — DependencyManager

When a nested host is added to a push-publish bundle, `DependencyManager` automatically adds all ancestor hosts to the bundle. This ensures that ancestor hosts are present on the receiving environment before the nested host is imported.

### 12.2 Import ordering — PushPublishHandler

Assets of type `HOST` and `FOLDER` must be sorted and imported before all other asset types. Sort key:

```
(parent_path + asset_name).length() ascending
```

This ensures shallower ancestors are imported before deeper descendants. The sort depth is computed from the bundle's own Identifier records — no external data source is needed.

After all `HOST` and `FOLDER` assets have been imported in order, all remaining asset types can be imported without ordering constraints.

---

## 13. REST API — /api/v1/site

### 13.1 Response model changes

The site list response is extended with a `parentPath` field. This field is populated via a JOIN on the `identifier` table in `HostFactoryImpl.findAll()`.

`parentPath` is stored as a transient field on `Host` objects (not persisted separately; derived from `Identifier`).

### 13.2 Depth computation — client-side

The Angular site selector computes display depth from `parentPath`:

```typescript
depth = parentPath.split('/').filter(Boolean).length
```

### 13.3 Site selector rendering

The existing `p-dropdown` component is used. No new tree component is needed. Nested hosts are rendered as a flat list with indented labels:

| Depth | Label prefix |
|---|---|
| 0 (top-level) | (none) |
| 1 | `— ` |
| 2 | `—— ` |
| n | n × `—` + space |

### 13.4 No hierarchical endpoint

A separate `?hierarchical=true` endpoint is not needed. The flat list with `parentPath` is sufficient for the site selector. Tree queries are handled by the browser/drive endpoints (see [Section 14](#14-browser-tree-and-contentdrive)).

---

## 14. Browser Tree and ContentDrive

### 14.1 New parameter: showSubHosts

A new boolean query parameter `showSubHosts` is added to the existing `BrowserResource` and `ContentDriveResource` endpoints.

When `showSubHosts=true`:

- Identifiers with `asset_subtype='Host'` are included in results alongside folders.
- Nested host nodes are returned with `baseType=HOST`.

### 14.2 Frontend rendering

The frontend uses the `isHost` flag returned by `FolderSearchResultView`:

- `isHost=true` nodes render with the host icon and `type='nested-host'`.
- **Clicking** a `nested-host` node navigates into the host's root _without_ switching the
  global site context.  The global site selector always shows the top-level site the user
  originally selected.  This is intentional: nested hosts are folders in the UX, not separate
  site contexts.

#### Content Drive — `browseHostname` state

`DotContentDriveStore` keeps a `browseHostname` field alongside `path`.  Both are updated
whenever a tree node is selected (`setSelectedNode`).  The `assetPath` fed to `drive/search`
is built as:

```
//${browseHostname || currentSite.hostname}${path || '/'}
```

Selecting a top-level _site_ node (`type='site'`) still calls `switchToHost`, which resets
`browseHostname` to `undefined` so the fallback to `currentSite.hostname` takes effect.

### 14.3 Inline subtree expansion

When a nested host node is expanded in the folder tree:

- The `nodeExpand` event handler reads `node.data.hostname` (the nested host's own hostname).
- It calls `POST /api/v1/folder/byPath` with `path=//nestedHostname/`.
- The nested host's folder tree is loaded inline as children of that node.

Deep-linking is supported: expanding a nested host node always shows its full subfolder tree inline.

---

## 15. Angular UI — DotBrowserSelectorStore

### 15.1 New state field

A new field `resolvedSite: Site` is added to `DotBrowserSelectorStore`. It tracks the currently active host within the folder browser component (which may be a nested host or a top-level host).

### 15.2 Scope

`resolvedSite` is a local override scoped to the folder browser component instance. It does not affect other portlets.

- The global site selector continues to represent the application-wide top-level site context.
- The user must change the global site selector to switch app-wide context.

### 15.3 Content operations

Content create and save operations initiated from within the folder browser use `resolvedSite` as the target host.

---

## 16. Content Editor Integration

### 16.1 hostId query parameter

When the folder browser opens the content editor for a nested host, it passes the nested host's UUID as the `hostId` query parameter:

```
/c/content?hostId=<nestedHost-uuid>
```

### 16.2 Content editor behavior

The content editor reads `queryParams.hostId` via `ActivatedRoute` and pre-fills the `HostFolderField` with the specified host.

If the `hostId` value is invalid or the user is unauthorized for that host, the editor silently falls back to the global current site.

---

## 17. Isolation and Sharing

| Concern | Behavior |
|---|---|
| Search / Lucene index | Isolated per-host. Each nested host is indexed under its own UUID. |
| Vanity URLs | Fully isolated per-host. No inheritance from parent hosts. |
| Templates and containers | Existing permission-based sharing model is unchanged. |
| Content types | Existing `HostFolderField` scoping is unchanged. Nested hosts are valid host selections. |
| Language resolution | No changes. Each nested host configures languages independently. |
| Sitemaps | Per-host. Each nested host gets its own sitemap. |

---

## 18. Background Tasks

`ReindexThread`, `SiteSearchAPI`, `MaintenanceUtil`, and all other background tasks that iterate over sites treat nested hosts identically to top-level hosts. Each nested host receives its own reindex pass, sitemap generation, and maintenance sweep.

No changes are required to the background task infrastructure.

---

## 19. Startup Task

An idempotent startup task performs two operations:

### 19.1 Add HostFolderField to Host content type

If-not-exists check before adding the field. No distributed lock is needed; any DB unique constraint violation from a concurrent startup on another node is a harmless no-op.

### 19.2 Sync Identifier.asset_name for existing hosts

Updates all existing host Identifier rows to set `asset_name = hostname`:

```sql
UPDATE identifier i
SET asset_name = c.text1  -- text1 holds the hostname field value
FROM contentlet c
JOIN contentlet_version_info cvi ON cvi.working_inode = c.inode
JOIN structure s ON c.structure_inode = s.inode
WHERE s.structuretype = 1  -- Host type
  AND i.id = c.identifier
  AND i.asset_name <> c.text1
```

This is idempotent — rows already matching are skipped via the `WHERE` clause.

---

## 20. Cycle Detection

`IdentifierAPI.getTopLevelHostId()` traverses the `Identifier.hostId` chain. If a cycle is detected (a UUID appears twice in the traversal), it throws `DotRuntimeException` immediately.

This is a defensive measure. The application does not permit saving a cycle, but the detection prevents an infinite loop if the database state is corrupted.

---

## 21. License

This feature is available on all license levels including Community. No `ParentProxy` wrapper or license-gated code paths are needed.

---

## 22. Content Import

The `hostName` field in the content import API continues to accept a segment label only. Because segment labels are globally unique, path-based disambiguation is unnecessary. No changes to the import API or import processing logic are required.

---

## 23. Files and Components — Created or Modified

### 23.1 New files (create)

| File | Description |
|---|---|
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/HostResolver.java` | New service: resolves `(serverName, fullUri)` → `HostResolutionResult` |
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/HostResolutionResult.java` | Value object holding `resolvedHost` and `remainingUri` |
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/NestedHostPatternCache.java` | JVM-local per-top-level-host pattern cache |
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/HostReparentPayload.java` | Event payload: `oldTopLevelHostId`, `newTopLevelHostId` |

### 23.2 Modified Java files

| File | Change |
|---|---|
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/model/Host.java` | Add `getAbsoluteBaseUrl()` |
| `dotCMS/src/main/java/com/dotcms/contenttype/business/IdentifierAPI.java` | Add `getTopLevelHostId(Host host)` to interface |
| `dotCMS/src/main/java/com/dotcms/contenttype/business/IdentifierAPIImpl.java` | Implement `getTopLevelHostId()` with cycle detection |
| `dotCMS/src/main/java/com/dotcms/contenttype/business/IdentifierFactoryImpl.java` | Add `findDescendantHostIdentifiers(topLevelHostId)` CTE method |
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/HostAPIImpl.java` | Read `HostFolderField` at save time; detect reparent; execute cascade; fire event |
| `dotCMS/src/main/java/com/dotmarketing/filters/CMSFilter.java` | Call `HostResolver`; set `CMS_FILTER_URI_OVERRIDE` and `CMS_RESOLVED_HOST` |
| `dotCMS/src/main/java/com/dotcms/cms/urlmap/HostWebAPIImpl.java` | Check `CMS_RESOLVED_HOST` before serverName fallback |
| `dotCMS/src/main/java/com/dotmarketing/portlets/contentlet/business/web/HostWebAPIImpl.java` | Same as above (confirm single vs. dual impl) |
| `dotCMS/src/main/java/com/dotmarketing/portlets/htmlpageasset/business/HTMLPageAssetRenderedResource.java` | Update URL construction to `getAbsoluteBaseUrl()` |
| `dotCMS/src/main/java/com/dotcms/enterprise/publishing/sitesearch/SiteSearchAPI.java` | Update URL construction to `getAbsoluteBaseUrl()` |
| `dotCMS/src/main/java/com/dotcms/enterprise/publishing/remote/handler/PushPublishHandler.java` | Sort HOST/FOLDER assets by path length before import |
| `dotCMS/src/main/java/com/dotcms/enterprise/publishing/bundlers/DependencyManager.java` | Add ancestor hosts to bundle when nested host is included |
| `dotCMS/src/main/java/com/dotcms/rest/api/v1/site/HostFactoryImpl.java` | JOIN on identifier table; populate transient `parentPath` field |
| `dotCMS/src/main/java/com/dotcms/rest/api/v1/browser/BrowserResource.java` | Add `showSubHosts` parameter; include host identifiers in results |
| `dotCMS/src/main/java/com/dotcms/rest/api/v1/contentdrive/ContentDriveResource.java` | Add `showSubHosts` parameter |
| Sitemap generation class(es) | Update URL construction to `getAbsoluteBaseUrl()` |
| Canonical tag rendering class(es) | Update URL construction to `getAbsoluteBaseUrl()` |
| Preview URL construction class(es) | Update URL construction to `getAbsoluteBaseUrl()` |
| Startup task registration class | Register new `HostFolderField` startup task |

### 23.3 New/modified SQL

| Artifact | Change |
|---|---|
| `renameFolderChildren` stored procedure | Adapt to accept host reparent arguments and cascade `parent_path` across descendant host assets |
| PostgreSQL `LISTEN/NOTIFY` setup | Ensure channel is established for `NestedHostPatternCache` invalidation broadcasts |

### 23.4 Modified Angular files

| File | Change |
|---|---|
| `core-web/libs/dotcms-models/src/lib/navigation/menu-item.model.ts` (or site model) | Add `parentPath: string` to site/host model |
| Site selector component (`p-dropdown`) | Compute depth from `parentPath`; render indented labels |
| `DotBrowserSelectorStore` | Add `resolvedSite: Site` field |
| Folder browser `nodeExpand` handler | Detect `baseType=HOST`; call `BrowserAPI` with nested host as root |
| Content editor component | Read `queryParams.hostId` via `ActivatedRoute`; pre-fill `HostFolderField` |
| `BrowserAPI` service | Pass `siteId` and `folderPath` parameters for nested host subtree queries |

---

## 24. Out of Scope

The following items were explicitly discussed and excluded from this feature:

| Item | Decision |
|---|---|
| Automatic URL redirects on reparent | Operator's responsibility |
| Additional audit instrumentation | Standard contentlet audit trail is sufficient |
| `findAllDepthFirst` site endpoint | Dropped; flat list with `parentPath` is used instead |
| `?hierarchical=true` REST endpoint | Not needed |
| Walk-up fallback on content not found | Explicitly rejected; 404 is returned |
| Per-host vanity URL inheritance | No inheritance; full isolation |
| Cascade unarchive | Manual; no cascade |
| License gating | Feature available on all license levels |
| Content reindex on reparent | Not needed; content paths are host-relative |
| Changes to language resolution | No changes |
