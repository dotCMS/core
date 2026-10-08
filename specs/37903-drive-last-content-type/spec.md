# Feature Specification: Open Content Drive filtered to the last content type opened in the editor

**Feature Branch**: `issue-37903-drive-last-content-type`

**Created**: 2026-10-07

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue dotCMS/core#37903. After a user opens any content type in the Content
Types editor, the next time they open Content Drive in that tab it starts filtered to that content
type, once. This mirrors what Content Search already does.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Content Drive picks up the content type I was just working on (Priority: P1)

A developer or content architect opens any content type in the Content Types editor to check or
change its fields. Then they open Content Drive to look at that type's content. Today the Drive
opens unfiltered and they have to find the same content type in the Content Type filter again.
With this feature, the Drive opens already filtered to the content type they just opened, the same
way Content Search does today.

**Why this priority**: This is the whole point of the feature. It removes a repeated manual step
between editing a content type and looking at its content.

**Independent Test**: Open any existing content type in the Content Types editor, then open
Content Drive from the menu. The Content Type filter shows that content type and the list shows
only its content.

**Acceptance Scenarios**:

1. **Given** the user opened an existing content type in the Content Types editor,
   **When** they open Content Drive from the menu in the same tab,
   **Then** the Drive's Content Type filter is set to that content type and the list shows only
   its content.
2. **Given** the user created a new content type and saved it (so the editor now shows it),
   **When** they open Content Drive in the same tab,
   **Then** the Drive is filtered to the new content type.
3. **Given** the Drive applied the content type filter this way,
   **When** the user reloads the page,
   **Then** the filter is still applied, because it is now part of the Drive's own address like
   any filter the user picks by hand.
4. **Given** the user opened one content type in the editor and then a different one,
   **When** they open Content Drive,
   **Then** the Drive is filtered to the second one, the last one opened.
5. **Given** the user opened a content type in the editor,
   **When** they follow a link that opens Content Drive in a specific folder but with no filters,
   **Then** the Drive opens that folder with the content type filter applied.
6. **Given** the user did not open any content type in the Content Types editor, but opened a
   contentlet in Edit Content (which also reads its content type),
   **When** they open Content Drive from the menu,
   **Then** the Drive opens with no content type filter.

---

### User Story 2 - The remembered content type is used once (Priority: P2)

As soon as Content Drive loads and picks up the remembered content type, it is wiped. The Drive
doesn't watch what the user does with the filter afterwards. The next time the user opens the
Drive, nothing is remembered, unless they open a content type in the editor again.

**Why this priority**: A filter that keeps reappearing on every visit looks like a bug and hides
content. Content Search avoids this by forgetting the type after its first use, and the Drive has
to do the same.

**Independent Test**: Open a content type in the editor, open the Drive (filtered to that type),
then go to another screen and open the Drive from the menu again. It opens with no content type
filter.

**Acceptance Scenarios**:

1. **Given** Content Drive already loaded and applied the remembered content type,
   **When** the user opens Content Drive again from the menu,
   **Then** the Drive opens with no content type filter.

---

### User Story 3 - When the Drive arrives with its own instructions, the remembered type is dropped (Priority: P3)

The remembered content type is only for opening Content Drive directly. Sometimes the Drive opens
with its own instructions instead: a bookmark or a shared link that carries filters, another screen
that sends the user to the Drive with a filter already set (for example Content Search redirecting
to the Drive when Content Search is not in the user's menu), or the Drive opening an item straight
into the editor. In those cases the Drive does exactly what it was asked, does not apply the
remembered content type, and still wipes it.

**Why this priority**: Overriding an explicit filter or an edit request would make links and
redirects unreliable. Wiping the remembered type anyway keeps it from surfacing on some later,
unrelated visit. The accepted cost: if the user opens a content type, then takes a detour through
a filtered link or an edit request before opening the Drive directly, the remembered type is gone
and the direct visit opens unfiltered.

**Independent Test**: Open a content type in the editor, then reach the Drive through a link or a
redirect that already sets a filter. The Drive shows only that filter. Open the Drive again from
the menu: it opens with no content type filter.

**Acceptance Scenarios**:

1. **Given** the user opened a content type in the editor,
   **When** they open a Content Drive link that already includes any filter,
   **Then** the Drive applies only the link's filters, the remembered content type is not added,
   and it is wiped.
2. **Given** the user opened a content type in the editor,
   **When** another screen sends them to Content Drive with a filter already set (for example a
   redirect from Content Search),
   **Then** the Drive applies only that filter, and the remembered content type is not added and
   is wiped.
3. **Given** the user opened a content type in the editor,
   **When** Content Drive opens straight into editing an item,
   **Then** the remembered content type is not applied and is wiped.
4. **Given** any of the cases above already happened,
   **When** the user later opens Content Drive from the menu,
   **Then** the Drive opens with no content type filter.

---

### User Story 4 - Content Drive stops offering a Rename action that doesn't work (Priority: P3)

When exactly one item is selected, Content Drive offers a Rename action. It doesn't work, and the
Drive isn't meant to rename items from there. Offering it only leads users into a dead end.

**Why this priority**: Small cleanup, carried in this change at the developer's request. It's
independent of the content type handoff.

**Independent Test**: Select exactly one item in Content Drive. No Rename action is offered.

**Acceptance Scenarios**:

1. **Given** the user selected exactly one item in Content Drive,
   **When** they look at the actions offered for it,
   **Then** there is no Rename action.

---

### User Story 5 - The content row menu only offers what the user may do (Priority: P3)

The folder row menu already hides what the user has no permission for. The content row menu does
not: a user who can only read an item is still offered Edit, Push Publish and Add to Bundle. Edit
opens an editor that cannot save, and the other two lead to actions the server refuses.

**Why this priority**: Small cleanup, carried in this change at the developer's request. It
matches the folder menu and Content Search, and removes dead ends from normal use.

**Independent Test**: As a user with only read permission on a content item, open its row menu.
It offers View instead of Edit, and no Push Publish or Add to Bundle.

**Acceptance Scenarios**:

1. **Given** the user can read but not edit a content item,
   **When** they open its row menu,
   **Then** the first entry is "View Content" instead of "Edit Content", and it opens the item the
   same way.
2. **Given** the user can edit a content item,
   **When** they open its row menu,
   **Then** the entry still reads "Edit Content".
3. **Given** the user opens the row menu of a page,
   **When** the menu shows,
   **Then** it offers two entries: "Edit Page" (opens the page editor) and "Edit Properties" (opens
   the page's content form in the side panel), or "View Page" and "View Properties" when the user
   can't edit the page.
4. **Given** the user has no publish permission on a content item,
   **When** they open its row menu,
   **Then** Push Publish and Add to Bundle are not offered.
5. **Given** the user has publish permission on a content item,
   **When** they open its row menu,
   **Then** Push Publish and Add to Bundle are offered as today.

---

### Edge Cases

- **Other screens load content types too.** Opening a contentlet in Edit Content, or any other
  screen that reads a content type, does not count. Only opening a type in the Content Types
  editor does.
- **Creating a type that is never saved.** Opening the "new content type" form and leaving without
  saving does not count, because there is no type yet.
- **The remembered content type was deleted, or the user has no permission on it.** If that
  content type was deleted, or the user has no permission on that content type, by the time the
  Drive opens, the Drive drops that filter on its own: it disappears from the address and the
  filter chip, and the list loads unfiltered. The Drive's existing "Something went wrong loading
  options" notice may show once, as it does today for any Drive link naming a deleted content
  type. The remembered content type is already forgotten.
- **The user goes somewhere else first.** If the user opens a type in the editor, visits other
  screens, and only later opens the Drive in the same tab, the Drive still applies the type,
  because nothing has used it yet.
- **Switching site.** After the Drive applied the remembered type, switching site does not bring
  it back: it was used once, so the Drive treats the site change like any other visit with nothing
  remembered.
- **Another tab.** A content type opened in the editor in one tab is not picked up by the Drive in
  a different, newly opened tab.
- **Content Search is unaffected.** Content Search keeps its own existing behavior. Opening the
  Drive does not change what Content Search pre-selects, and the other way around.

## Requirements *(mandatory)*

### Terms

- **Opening Content Drive**: entering the Drive, either by navigating to it from another screen or
  by loading or reloading its page. Moving around inside the Drive (changing folders, applying or
  clearing filters, opening and closing items) is not opening it.
- **Filters**: any of the Drive's list filters, such as content type, base type, language,
  workflow, status, search text, shared assets or search scope, when they are part of the address
  the Drive was opened with.
- **Not filters**: the folder being browsed, whether the folder tree is expanded, and any defaults
  the Drive fills in by itself after opening (such as its default language). These don't stop the
  remembered type from being applied.

### Functional Requirements

- **FR-001**: When the Content Types editor opens an existing content type, the system MUST
  remember that content type as the "last opened type" for the current browser tab, replacing
  any type remembered before.
- **FR-002**: Only the Content Types editor MUST set the remembered type. Other screens that read
  content types MUST NOT change it.
- **FR-003**: When Content Drive opens without any filters in its address, and a type is
  remembered, the Drive MUST apply a Content Type filter for that type. Any folder in the address
  MUST still be applied.
- **FR-004**: When Content Drive opens with its own instructions (filters already in its address,
  whether from a link or from another screen redirecting to it, or a request to open an item in the
  editor), the Drive MUST follow only those instructions and MUST NOT apply the remembered type.
- **FR-005**: The system MUST forget the remembered type every time Content Drive opens, whether
  or not the type was applied, including when the Drive opened with its own instructions (FR-004).
- **FR-006**: Once applied, the Content Type filter MUST behave like a filter the user picked by
  hand: it shows in the filter bar, appears in the Drive's address, survives a reload, and can be
  cleared.
- **FR-007**: The remembered type MUST be kept in the browser's session storage
  (`sessionStorage`), so it belongs to a single browser tab. Other tabs, including new ones, MUST
  NOT see it. It MUST NOT be kept on the server or in storage shared across tabs.
- **FR-008**: If the remembered content type can't be used as a filter when the Drive opens
  (the content type was deleted, the user has no permission on that content type, or the Drive's
  Content Type filter doesn't list it), the Drive MUST end up without that filter (not in the
  address, not in the filter chip, list unfiltered) and MUST forget the remembered content type.
  The Drive's existing notice about field options failing to load may show once for a deleted
  type; this change does not alter how the Drive reacts to an unknown content type.
- **FR-009**: Content Drive MUST NOT offer a Rename action, including when exactly one item is
  selected.
- **FR-010**: In a content row's menu, the Drive MUST label the open-in-editor entry "View Content"
  when the user lacks edit permission on that item, and "Edit Content" when they have it. Either
  way it opens the item in the editor, as Content Search does. A page's menu MUST offer two
  entries, "Edit Page" (page editor) and "Edit Properties" (the page's content form in the side
  panel), each labelled "View …" instead when the user lacks edit permission.
- **FR-011**: In a content row's menu, the Drive MUST offer Push Publish and Add to Bundle only when
  the user has publish permission on that item, the same rule the folder row menu already applies.
  When the item's permissions are not known, the Drive MUST behave as it does today (offer them).

### Key Entities

- **Remembered content type**: the identifier (variable name) of the last content type opened in
  the Content Types editor, held for one browser tab until the Drive next opens (applied or not, per FR-005) or the
  tab closes.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After opening a content type in the editor, a user reaches a Drive list filtered to
  that type in one step (opening the Drive), down from three today (open the Drive, open the
  Content Type filter, find and pick the type).
- **SC-002**: In every scenario under User Story 2, the Drive opens with no content type filter:
  the remembered type is applied at most once per time a type is opened in the editor.
- **SC-003**: Every Drive link that carries its own filters opens with exactly those filters,
  whatever type was opened in the editor before.
- **SC-004**: None of the edge cases above produces a broken Drive screen. The only message allowed
  is the Drive's existing field-options notice for a deleted content type (FR-008).

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Types editor (Angular admin) and Content Drive. It
  copies a behavior from the legacy Content Search portlet, where the server remembers the last
  content type read and Content Search pre-selects it once. That legacy behavior is not changed.
- **Backward-compatibility expectations**: Existing Drive links, bookmarks and filter behavior keep
  working unchanged. No APIs, server data or content are changed. Content Search's pre-selection
  stays as it is.
- **Known related decisions**: Content Search forgets the remembered type after its first search,
  which is the one-time behavior this feature copies. The plan phase will consult
  `dotCMS/platform-adrs`.

## Assumptions

- **Storage is decided: `sessionStorage`.** The remembered type is held in the browser's session
  storage, not on the server and not in `localStorage`, so it needs no backend change. The plan
  MUST use it and not reopen this choice.
  The server's own remembered type, used by Content Search, is never returned to the browser, so
  the Drive can't reuse it.
- **Logging out deliberately does not clear it.** Logout reloads the same tab, so the remembered
  type survives until the next Drive visit uses it. If someone else logs in on that tab, their
  first Drive visit may open filtered to the previous user's content type, once: FR-008 covers a
  content type they have no permission on, and otherwise it's a visible filter they can clear. Clearing it would
  make the login code depend on a Content Drive detail for a one-time, harmless effect.
- "Opening a type" means the Content Types editor loading an existing type. A newly created type
  counts after its first save, because the editor then reopens it as an existing type.
- The Drive's other defaults, such as its default language, still apply next to the content type
  filter.
- Out of scope: changing Content Search, adding a server endpoint for the server's remembered
  type, and sharing the last browsed site or folder between the Drive and the asset picker
  (noted for later).
