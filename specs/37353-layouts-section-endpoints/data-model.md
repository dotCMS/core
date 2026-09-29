# Data Model: Section management endpoints (#37353)

No new tables or columns. Existing storage, reached only through `LayoutAPI`:

| Table | Columns used | Role in this feature |
|---|---|---|
| `cms_layout` | `id` (uuid), `layout_name` varchar(255) not null, `description` varchar(255) → wire `icon`, `tab_order` int → wire `tabOrder` | one row per section |
| `cms_layouts_portlets` | `layout_id`, `portlet_id`, `portlet_order` (1..n) | ordered tools of a section; replaced wholesale by `setPortletIdsToLayout` |
| `layouts_cms_roles` | `layout_id`, `role_id` | grants; deleted by `removeLayout`, never written here |

## Wire entities

### SectionView (response)

| Field | Type | Source | Notes |
|---|---|---|---|
| `id` | string | `Layout.id` | |
| `name` | string | `Layout.name` | |
| `icon` | string | `Layout.description` | may be empty |
| `tabOrder` | int | `Layout.tabOrder` | absolute value not part of the contract; only order |
| `portletIds` | string[] | `Layout.portletIds` | stored order |
| `portletTitles` | string[] | title fallback per id | same length and order as `portletIds` |

### SectionForm (POST /v1/layouts, PUT /v1/layouts/{id})

| Field | Rule |
|---|---|
| `name` | required; trimmed; non-blank; ≤ 255 chars; unique among sections by exact comparison (enforced by `LayoutAPI.saveLayout`) |
| `icon` | optional; ≤ 255 chars; may be empty |

### SectionOrderForm (PUT /v1/layouts/_reorder)

| Field | Rule |
|---|---|
| `layoutIds` | required; must equal the set of all existing section ids, each exactly once |

### SectionToolsForm (PUT /v1/layouts/{id}/portlets)

| Field | Rule |
|---|---|
| `portletIds` | required; may be empty; each id names a registered portlet (`findPortlet` non-null) that `canAddPortletToLayout` accepts; no duplicates |

## Validation outcomes

| Condition | Exception | HTTP |
|---|---|---|
| blank / over-long name, over-long icon, bad reorder list, bad tool list, delete Getting Started, empty tool list on Getting Started | `BadRequestException` | 400 |
| duplicate name, including a database uniqueness violation when two writes race | `LayoutNameAlreadyExistsException` (DotStateException) | 400 |
| unknown section id on any write | `DoesNotExistException` | 404 |
| no `tools` / `tools-beta` / admin | REST `SecurityException` from `InitBuilder` | 401 |
| portlet holder, not CMS Administrator, on a write | `DotSecurityException` | 403 |

## State transitions

- **Create**: `tabOrder = max + 1`, `portletIds = []`.
- **Update**: `name`, `description` change; `tabOrder`, `portletIds` unchanged.
- **Reorder**: every section's `tabOrder` becomes its 1-based index in the sent list, in one transaction, one refresh event.
- **Getting Started lookup** (toggle endpoints): resolve by fixed id, else adopt by name, else create with defaults; restore `[starter]` when the tool list is empty; never rewrite name, icon or position.
- **Set tools**: `cms_layouts_portlets` rows for the section replaced with `portlet_order` 1..n.
- **Delete**: rows in all three tables removed; role layout cache cleared per affected role.

Every transition ends with `SystemEventsAPI.pushAsync(UPDATE_PORTLET_LAYOUTS)` from inside
`LayoutAPIImpl`.
