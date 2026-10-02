# Contract: Content Drive URL params for panels and folder dialogs

Route: `/dotAdmin/#/content-drive`. These params are a **supported entry point** (FR-023). Part 1's
redirect builds `editContent`/`editContentLang` and `createContent`, and other screens may link to
them. Existing params (`path`, `filters`, `isTreeExpanded`) are unchanged.

| Param | Value | Meaning | Written while |
|---|---|---|---|
| `editContent` | content identifier | Open that content in the panel the content type chose | an edit panel is open (either editor) |
| `editContentLang` | language id (positive integer) | Language of the version to open. Optional on read: falls back to the list's language filter, then the default, then any language | with `editContent` |
| `createContent` | content type **variable** | Open the create form for that type, in the folder `path` names (or the site root), in the language FR-003 picks | a create panel is open with nothing saved yet |
| `createFolder` | `true` | Open New Folder; parent is `path` (or the site root) | New Folder is open |
| `editFolder` | folder identifier | Open Folder Settings for that folder (needs EDIT) | Folder Settings is open |
| `folderPermissions` | folder identifier | Open Edit Permissions for that folder (needs EDIT_PERMISSIONS) | Edit Permissions is open |

## Rules

- **At most one** of the panel or folder-dialog params is set. Read on load, a URL that sets
  `editContent` with `createContent`, a panel param with a folder-dialog param, or two folder-dialog
  params opens **nothing** and has all six params removed (`replaceState`).
- `editContent=new` is the old create marker. It is ignored on read and never written.
- `CD_`-prefixed params are no longer produced or read.
- Opening a panel or folder dialog adds **one** history entry. Closing it for any reason removes its
  params with `replaceState`. Browser Back while one is open goes through its close path: the
  unsaved-changes prompt for a panel, a plain close for a folder dialog.
- First save of a create rewrites `createContent=<type>` as `editContent=<id>&editContentLang=<lang>`
  with `replaceState`. Exception: a new HTML page created in the legacy panel, whose first save closes
  the panel and opens the page editor.
- A language switch inside the legacy panel rewrites `editContentLang` with `replaceState`.
- Params are read **once**, on load. Changing them on an open Content Drive (other than through Back)
  does nothing until the next load, as with `editContent` today.

## Error handling on read

| Case | Result |
|---|---|
| `editContent` names nothing the user can read | Today's handling in `openEditByIdentifier`: standard error, nothing opens |
| content type lookup fails | standard error, nothing opens |
| `createContent` names a type that doesn't exist or the user can't create | standard error, nothing opens, the user stays on Content Drive |
| `editFolder` / `folderPermissions` names a folder that is gone, out of reach, or a bad id; or the user lacks the permission | standard error, no dialog, the param is removed |

## Examples

```text
/content-drive?path=/blog/&editContent=4b1d...&editContentLang=1
/content-drive?path=/blog/&createContent=webPageContent
/content-drive?createContent=htmlpageasset                  # site root
/content-drive?path=/blog/&createFolder=true
/content-drive?editFolder=8f2c...
/content-drive?folderPermissions=8f2c...
/content-drive?editContent=4b1d...&createContent=Blog        # conflict: nothing opens, both removed
```
