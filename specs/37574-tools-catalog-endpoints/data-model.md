# Data Model: Tools portlet id, tools catalog endpoints and custom-tool gate (#37574)

**Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md)

No stored data changes shape. This feature adds read models over the existing `Portlet` object and
three constants to the portlet id registry.

## Existing entities (read, not modified)

### Portlet (`com.liferay.portal.model.Portlet`)

| Field / init param | Source | Used for |
|---|---|---|
| `portletId` | XML `<portlet-name>` or `portlet.portletid` | catalog `id`, custom `portletId`, gate ids |
| `initParams["portletSource"]` | `"db"` when written by `PortletAPIImpl.savePortlet` or the Language Variables startup task; absent (defaults to `"xml"`) for XML and OSGi portlets | `isCustom` (with the registry check) |
| `initParams["name"]` | custom tools only | catalog title fallback, custom `portletName` |
| `initParams["baseTypes"]` | comma-joined `BaseContentType.name()` values, may be `""` | custom `baseTypes` |
| `initParams["contentTypes"]` | comma-joined content type variables, may be `""` | custom `contentTypes` |
| `initParams["dataViewMode"]` | `"list"` or `"card"` as sent by the writer | custom `dataViewMode` |

Source of the full set: `PortletAPI.findAllPortlets()` (XML files plus the `portlet` table, keyed
by id, cached cluster-wide).

### Layout (`com.dotmarketing.business.Layout`)

A navigation section. `getPortletIds()` decides portlet access through
`LayoutAPI.doesUserHaveAccessToPortlet(id, user)`. Owned by #37353; read only here.

## Registry additions

### `PortletID` (`com.dotmarketing.util.PortletID`)

| Constant | `toString()` | Note |
|---|---|---|
| `TOOLS` | `tools` | FR-007. Final id of the Tools portlet. |
| `TOOLS_BETA("tools-beta")` | `tools-beta` | Beta-period alias accepted by every Tools gate. Remove at promotion (#37356). |
| `LANGUAGE_VARIABLES("c_Language-Variables")` | `c_Language-Variables` | FR-007a. Declares the product's DB-stored tool as shipped. |

Rule: a tool is **declared by the product** when some `PortletID` constant's `toString()` equals
its id.

## New read models (`com.dotcms.rest.api.v1.portlet`)

### `ToolCatalogEntryView` (record)

| Component | Type | Derivation | Rule |
|---|---|---|---|
| `id` | `String` | `portlet.getPortletId()` | unique within the list |
| `title` | `String` | `LanguageUtil.get(user, "com.dotcms.repackage.javax.portlet.title." + id)`; if the result equals the key → `initParams["name"]` if set → else `id` | never a raw key (FR-005) |
| `isCustom` | `boolean` | `PortletAPI.isCustomContentPortlet(portlet)` | FR-004 |

Serialized as `{ "id": "...", "title": "...", "isCustom": true|false }`.

### Catalog (list of `ToolCatalogEntryView`)

Membership and order, applied in this sequence to `findAllPortlets()`:

1. Drop the portlet whose id equals `PortletID.LANGUAGES.toString()` (ignoring case) when
   `Config.getBooleanProperty(FEATURE_FLAG_LOCALES_HIDE_OLD_LANGUAGES_PORTLET, true)` is true.
2. Drop every portlet for which `PortletAPI.canAddPortletToLayout(portlet)` is false.
3. Map to `ToolCatalogEntryView`.
4. Sort by `title.toLowerCase()` ascending, then by `id` for a stable order between equal titles.

Wrapper: `ResponseEntityToolCatalogView extends ResponseEntityView<List<ToolCatalogEntryView>>`.

### `CustomToolView` (record)

| Component | Type | Derivation |
|---|---|---|
| `portletId` | `String` | stored id, `c_` prefix included |
| `portletName` | `String` | `initParams["name"]` |
| `baseTypes` | `List<String>` | `initParams["baseTypes"]` split on `,`, trimmed, empties removed; `[]` when blank |
| `contentTypes` | `List<String>` | `initParams["contentTypes"]` split the same way |
| `dataViewMode` | `String` | `initParams["dataViewMode"]` as stored |

Existence rule: produced only when `findPortlet(id)` is non-null **and**
`isCustomContentPortlet(portlet)`; otherwise the read answers 404.

Wrapper: `ResponseEntityCustomToolView extends ResponseEntityView<CustomToolView>`.

## Predicates

### `PortletAPI.isCustomContentPortlet(Portlet)` (new)

```
"db".equals(portlet.getInitParams().get("portletSource"))
  AND  no PortletID p with p.toString().equals(portlet.getPortletId())
```

| Portlet | portletSource | Declared | isCustom |
|---|---|---|---|
| `roles`, `content`, `tools-beta` (XML) | xml | yes / no | false (marker absent) |
| OSGi JSP/Velocity plugin portlet stored in DB | xml | no | false |
| `c_Language-Variables` | db | yes | false |
| `c_press-releases` (New Tool) | db | no | **true** |
| a hand-made tool named "Language Variables" with another id | db | no | **true** |

### Gate sets (`WebResource.InitBuilder.requiredPortlet(...)`, any-match, 401 on miss)

| Set | Ids | Applied to |
|---|---|---|
| Tools gate | `tools`, `tools-beta` | `GET /_catalog`, `GET /custom/{id}` |
| Custom-tool write gate | `roles`, `tools`, `tools-beta` | `POST /custom`, `PUT /custom`, `DELETE /custom/{id}` |
| Roles gate (unchanged) | `roles` | `PUT /custom/{id}/_addtolayout/{layoutId}`, per-role removes |

CMS Administrators pass every set through `LayoutAPIImpl.doesUserHaveAccessToPortlet`.

## State transitions

None new. Create/update/delete of custom tools keep their existing transitions; the update and the
delete gain a precondition (`isCustomContentPortlet` on the existing portlet) before
`PortletAPI.savePortlet` / `PortletAPI.deletePortlet` run, so a shipped tool stored in the database
can be neither rewritten nor removed through them.

## Validation rules carried over (unchanged)

From `PortletAPIImpl.savePortlet`: id, name and data view mode required; at least one base type or
content type; content types must exist; `Host` is not allowed. Ids are normalised to the `c_`
prefix by `portletIdPrefixCleaner`.
