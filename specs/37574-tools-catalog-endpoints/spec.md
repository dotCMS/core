# Feature Specification: Tools portlet id, tools catalog endpoints and custom-tool gate

**Feature Branch**: `37574-tools-catalog-endpoints`

**Created**: 2026-09-17

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [dotCMS/core#37574](https://github.com/dotCMS/core/issues/37574). Sibling backend task: [#37353](https://github.com/dotCMS/core/issues/37353) (section endpoints under `/v1/layouts`), which depends on the portlet id delivered here.

**Input**: User description: "37574"

## Context

The Tools portlet is a new Angular admin screen that owns the master list of
backend navigation sections and the tools inside each one. Its right-hand panel lists every
tool that can be placed in a section, lets an admin search that list, and offers **New Tool**,
**Edit** and **Delete** for custom content tools. Today that catalog and the custom-tool
management behind it exist in two places that the new screen cannot use as they stand:

- The catalog is served only through the legacy Dojo remoting layer used by the Roles & Tools
  screen. The Angular portlet has no way to call it.
- Custom content tools can be created, updated and deleted over the modern API, but every one
  of those operations is reserved to users who hold the **Roles** portlet, because the button
  historically lived inside the Roles screen. Two existing screens use those operations today:
  the legacy Roles & Tools screen (create and delete) and the Angular **Add to menu** dialog on
  the Content Types listing (create, then add to a section the caller holds). A per-tool read
  exists, but it is gated on holding that very tool and returns the raw portlet definition,
  neither of which suits an Edit dialog opened from a catalog.
- The existing delete of a custom content tool never checks that its target is custom. Given
  the id of a shipped tool it removes that tool from every section on the instance.
- The product has no **Tools** portlet id at all. The surface has always ridden on the Roles
  id, so there is nothing for the new endpoints to be gated on.

This feature delivers the portlet-side backend for the Tools portlet: the tools catalog, the
single custom-tool read, the new `tools` portlet id, and the access change that lets an admin
who has been granted Tools but not Roles manage custom content tools. The section endpoints
under `/v1/layouts` are the sibling task #37353 and are out of scope here. The two touch
disjoint files and are meant to be reviewed in parallel, with #37353 depending on the portlet
id delivered here for its gate.

The frontend for this surface already exists as draft PR #37481, running against a mocked
service. Its catalog and custom-tool create, update and delete methods correspond to the
endpoints below; the Edit prefill is currently a constant and becomes the single-tool read.
Its data models define the response shapes this feature must match: a catalog row is `{ id, title, isCustom }` and a
custom tool's configuration is `{ portletId, portletName, baseTypes, contentTypes,
dataViewMode }`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse the tools catalog from the Tools portlet (Priority: P1)

An admin opens the Tools portlet, selects a section, and sees in the Available Tools panel
every tool that can be added to a section, sorted by name, with a readable title for each.
Custom content tools are distinguishable from the tools that ship with the product, so the
panel can offer Edit and Delete only where those actions are valid.

**Why this priority**: Without the catalog the right half of the Tools screen has no data.
Every other action on that panel starts from this list. It is the single call that unblocks
the frontend from its mock.

**Independent Test**: With a backend user who holds the Tools (Beta) portlet, request the
catalog and compare it to the list the legacy Roles & Tools tool picker shows the same user.
Same tools, same order, and each custom tool flagged as custom. Delivers a working Available
Tools panel on its own.

**Acceptance Scenarios**:

1. **Given** an instance with shipped tools and at least one custom content tool, **When** a
   user with the Tools (Beta) portlet requests the catalog, **Then** every tool that can be
   added to a section is returned once, sorted by title without regard to letter case, each
   with its id, its localized title and a `isCustom` flag.
2. **Given** the same instance, **When** the catalog is returned, **Then** `isCustom` is true
   for every tool an admin created through New Tool and false for every tool that ships with
   the product, Language Variables included. The flag does not depend on how the tool's id is
   spelled.
3. **Given** the configuration that hides the old Languages tool is on, **When** the catalog
   is requested, **Then** the old Languages tool is absent; when that configuration is off,
   it is present.
4. **Given** a tool the product marks as not placeable in a section, **When** the catalog is
   requested, **Then** that tool is absent, exactly as the legacy picker omits it.
5. **Given** a custom content tool created a moment ago, **When** the catalog is requested,
   **Then** the new tool is in the list with the title the admin gave it.
6. **Given** a tool whose title has no translation for the caller's language, **When** the
   catalog is requested, **Then** its title is a readable name, not the raw translation key.

---

### User Story 2 - Manage custom content tools with Tools access alone (Priority: P2)

An admin who has been granted the Tools portlet but not the Roles portlet clicks **New Tool**,
fills in a name, an id, the content to display and a data view mode, and the tool is created.
The same admin can later edit or delete it. An admin who has only the Roles portlet keeps
every ability they have today, in the legacy screen and in the Add to menu dialog alike.

**Why this priority**: Without this, the Tools screen shows a New Tool button that fails for
exactly the people the portlet is for, which is a broken product state. Roles-only admins
must not lose anything the day the Beta ships.

**Independent Test**: Create two non-admin backend users, one granted a section containing
Tools (Beta) only, one granted a section containing Roles only. Each creates, updates and
deletes a custom content tool. Both succeed. A third user with neither is rejected.

**Acceptance Scenarios**:

1. **Given** a non-admin backend user whose only relevant grant is a section containing the
   Tools (Beta) portlet, **When** they create a custom content tool, **Then** the tool is
   created and appears in the catalog.
2. **Given** the same user, **When** they update that tool's name, content types or data view
   mode, or delete it, **Then** the operation succeeds and the catalog reflects it.
3. **Given** a non-admin backend user whose only relevant grant is a section containing the
   Roles portlet, **When** they create, update or delete a custom content tool, **Then** the
   operation succeeds exactly as before this change.
4. **Given** a backend user with neither the Tools nor the Roles portlet in any granted
   section, **When** they attempt any custom-tool write, **Then** the request is rejected as
   unauthorized and nothing changes.
5. **Given** a CMS Administrator with no explicit grant, **When** they perform any of the above,
   **Then** the operation succeeds, as administrators pass every portlet gate today.
6. **Given** a custom content tool is deleted, **When** the sections are inspected, **Then**
   the tool is gone from every section that contained it, and open admin sessions refresh
   their navigation without a hard reload. This is existing behaviour and must not regress.
7. **Given** the id of a tool that ships with the product, Language Variables included, **When**
   any authorised user attempts to delete it through the custom-tool delete or to overwrite it
   through the custom-tool update, **Then** the response is not found, its configuration is
   unchanged and the tool is still present in every section that had it.
8. **Given** a non-admin backend user whose only relevant grant is a section containing the
   Tools (Beta) portlet, **When** they attempt to add a tool to a section through the existing
   add-to-section operation, **Then** the request is rejected as unauthorized, exactly as
   today; a user holding Roles still succeeds.

---

### User Story 3 - Edit an existing custom content tool (Priority: P3)

An admin picks **Edit** on a custom content tool in the Available Tools panel. The dialog opens
prefilled with the tool's current name, id, selected base types and content types, and data
view mode, so the admin changes only what they mean to change.

**Why this priority**: Edit is on the design and in the frontend PR, but the catalog rows carry
only id and title. Without a way to read one tool's configuration, the dialog can only be
prefilled with the name and id, and saving would silently blank the content selection.

**Independent Test**: Create a custom content tool with specific base types, content types and
the Card view mode, then request that tool by id. The response carries exactly the values that
were saved. Requesting a shipped tool's id or an unknown id is answered with not found.

**Acceptance Scenarios**:

1. **Given** a custom content tool exists, **When** a user with the Tools (Beta) portlet
   requests it by id, **Then** the response carries its id, its name, its base types, its
   content types and its data view mode, matching what was saved.
2. **Given** the id of a tool that ships with the product, **When** it is requested through the
   custom-tool read, **Then** the response is not found, because the tool has no custom
   configuration to edit.
3. **Given** an id no tool has, **When** it is requested, **Then** the response is not found.
4. **Given** a backend user without the Tools portlet in any granted section, including one who
   holds only Roles, **When** they request a custom tool by id, **Then** the request is rejected
   as unauthorized. The Roles-only user still edits custom tools through the legacy screen.

---

### User Story 4 - A Tools portlet id exists for gating (Priority: P4)

The product recognises `tools` as a portlet id in the same way it recognises `roles`, `users`
or `tags`, so the section endpoints in #37353 and the endpoints here can be gated on it, and so
the eventual promotion of the Beta portlet (#37356) has an id to promote to.

**Why this priority**: It is a prerequisite for #37353 and a one-line change, but it has no
user-visible effect by itself, so it ranks last.

**Independent Test**: The portlet id registry contains an entry that resolves to `tools`, and a
gate configured on it behaves like every other portlet gate. A section may reference the id
before any portlet named `tools` is registered, which is how the test grants it.

**Acceptance Scenarios**:

1. **Given** the change is deployed, **When** a gate is configured on the `tools` id, **Then**
   a backend user whose granted sections contain a `tools` portlet passes and one without it
   is rejected, with the CMS Administrator fallback applying as for every other portlet.

---

### Edge Cases

- **Beta id versus final id.** During Beta the portlet admins add to a section is `tools-beta`,
  not `tools`. Access is decided by exact membership of an id in the user's granted sections,
  so a gate on `tools` alone would admit only CMS Administrators. Every gate introduced here
  accepts both `tools` and `tools-beta`. The same latent gap exists today between `roles-beta`
  and `roles` and is not addressed here.
- **Rejection status.** A missing portlet grant is reported as 401 Unauthorized, not 403. That
  is how every portlet-gated endpoint in the product responds today and this feature follows
  it. The frontend must not depend on the status code beyond "rejected".
- **Title fallback.** The title lookup returns the raw key when nothing matches. Shipped tools
  have keys in the base bundle, and custom tools receive a stored title in every language that
  exists when they are saved, so the realistic gap is a custom tool viewed in a language added
  later. The catalog must still show a readable name in that case.
- **Custom-ness is not the id prefix.** Custom tools conventionally carry a `c_` id prefix, but
  the flag must come from how the tool is registered in the system, so a future change to the
  id scheme cannot mislabel tools.
- **A built-in tool stored like a custom one.** Language Variables ships with the product but is
  stored as a database row in the same shape as a hand-made tool. "Registered in the database"
  alone would call it custom. The product therefore declares it in its portlet id registry, and
  a custom tool is one registered in the database **and** not declared by the product. A
  hand-made tool an admin names Language Variables, with a different id, remains custom.
- **Legacy screen and a protected tool.** The legacy screen still offers Delete on Language
  Variables, since it goes by id prefix. The call answers not found and the screen shows its
  error message. Expected.
- **Catalog freshness.** A tool created or deleted in one request must be present or absent in
  the very next catalog read, from any node.
- **Deleting a tool that sits in sections.** Existing behaviour removes the tool from every
  section and notifies open sessions. This feature widens who may trigger it and, for the first
  time, restricts it to custom tools. There is no other single operation that removes a shipped
  tool from every section: the existing per-portlet remove is role-scoped, editing only the
  sections one role holds. Editing a section's tools directly is the legacy layout editor today
  and the section endpoint in #37353 tomorrow, both administrator-controlled.
- **Who may change sections.** Sections carry portlet access: a user reaches a tool because a
  section they hold contains it. Changing a section therefore changes what roles can reach.
  The section writes are owned by #37353 and are restricted to CMS Administrators, matching the
  modern operations that grant sections to roles and users. Nothing this feature widens can change
  what a role can reach, which is why those operations stay portlet-gated. The one existing
  operation that can, adding a tool to a section the caller holds, accepts any placeable tool
  and is therefore left on its Roles gate (FR-009a).
- **Unauthenticated callers** receive 401 on every endpoint touched here, with no catalog data
  in the response.
- **Legacy screen untouched.** The Roles & Tools Dojo screen and its remoting methods keep
  working exactly as before, including creating custom tools from that screen.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose a tools catalog listing every tool that can be placed in a
  navigation section, applying the same inclusion rules as the legacy Roles & Tools picker:
  tools the product marks as not placeable are excluded, and the old Languages tool is excluded
  while the configuration that hides it is on.
- **FR-002**: Each catalog row MUST carry the tool's id, its title localized to the caller's
  language, and an `isCustom` boolean.
- **FR-003**: The catalog MUST be sorted by title, ignoring letter case, and MUST contain each
  tool exactly once.
- **FR-004**: `isCustom` MUST be true exactly when the tool is registered in the database and its
  id is not one the product declares in its portlet id registry. It MUST NOT depend on the
  spelling of the id.
- **FR-005**: When a tool's title has no translation for the caller's language, the catalog
  MUST return the name the tool was registered with, when it has one, and otherwise its id. It
  MUST never return the raw translation key.
- **FR-006**: The system MUST expose a read of one custom content tool by id returning its id,
  its name, its base types and content types as lists, and its data view mode as stored, in
  lowercase (`list` or `card`). For an unknown id, or for a tool that is not a custom content
  tool, it MUST respond not found.
- **FR-007**: The product's portlet id registry MUST contain an entry resolving to `tools`.
- **FR-007a**: The product's portlet id registry MUST declare Language Variables
  (`c_Language-Variables`), so FR-004 and FR-013 treat it as a product tool.
- **FR-008**: The catalog and the single-tool read MUST require an authenticated backend user
  who either holds a granted section containing `tools` or `tools-beta`, or is a CMS
  Administrator. Any other caller MUST receive 401 Unauthorized with no tool data.
- **FR-009**: The existing operations to create, update and delete a custom content tool MUST
  accept a caller who holds `roles`, `tools` or `tools-beta` in a granted section, or is a CMS
  Administrator. Their request and response contracts MUST NOT change, except that the update
  refuses a target that is not a custom content tool (FR-013).
- **FR-009a**: The existing operation that adds a tool to a section the caller holds MUST keep
  its current Roles-only gate. It accepts any placeable tool, not only custom ones, so widening
  it would let a Tools holder add the Roles or Users tool to their own section. The new portlet
  does not use it; the Add to menu dialog does, and its users hold Roles today.
- **FR-010**: A custom content tool created, updated or deleted through any of the operations
  in FR-009 MUST be reflected in the next catalog read.
- **FR-011**: The legacy Roles & Tools screen, the Angular Add to menu dialog, their remoting
  and REST calls, and the role-scoped per-portlet remove MUST continue to behave exactly as
  today.
- **FR-012**: Every endpoint added or changed MUST be described in the generated API
  documentation with response schemas that match what is actually returned.
- **FR-013**: The delete and the update of a custom content tool MUST refuse, with not found
  and without changing any data, any id that is not a custom content tool under FR-004.
  Language Variables is therefore refused by both.

### Key Entities

- **Tool (catalog entry)**: A placeable unit of the backend navigation. Identified by its id;
  presented by a localized title; flagged as custom or shipped. Shipped tools are declared by the
  product, in its configuration or its portlet id registry; custom tools are registered in the
  database at runtime and are not declared.
- **Custom content tool configuration**: The editable definition of a custom tool: its id, its
  display name, the base types and content types whose content it lists, and whether that
  content is shown as a list or as cards.
- **Navigation section**: A named, ordered group of tools shown in the admin left menu. Owned
  by #37353; referenced here only because access is decided by which sections a user holds.
- **Portlet id `tools`**: The identifier that a section must contain for a user to pass the
  Tools gate. `tools-beta` is its Beta-period alias in every gate introduced here.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any given backend user and configuration, the tools catalog returned to the
  Tools portlet is identical in membership and order to the tool picker shown to that user in
  the legacy Roles & Tools screen.
- **SC-002**: 100% of tools created through New Tool are flagged custom, and 0% of shipped
  tools are, across an instance that has both.
- **SC-003**: A non-admin backend user granted only Tools (Beta) completes create, edit and
  delete of a custom content tool end to end with no rejection.
- **SC-004**: A non-admin backend user granted only Roles completes the same three operations
  with no change in outcome compared to the release before this feature, measured by the
  existing Postman collection for this resource staying green and by the legacy screen and the
  Add to menu dialog completing their flows.
- **SC-005**: 100% of requests that lack the gate for that operation, Tools for the catalog and
  the single-tool read, Tools or Roles for the custom-tool writes, and all unauthenticated
  requests, are rejected without returning tool data.
- **SC-006**: The frontend PR #37481 swaps its mocked catalog, custom-tool create, update,
  delete and single-read calls to the real endpoints with no change to its data models beyond
  lowercasing the two data view mode values and pointing its delete at the custom-tool
  delete.
- **SC-007**: No catalog title is a raw translation key in a default install with all
  configured languages.
- **SC-008**: Zero shipped tools can be removed from any section, or have their configuration
  rewritten, through the custom-tool delete or update, for any caller including administrators.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The custom content tool management surface, which is part of
  the older Roles & Tools admin area and is also driven by the Angular Add to menu dialog, and
  the portlet id registry, which gains two entries and is the single place where the product
  declares a tool as its own, which lives in the legacy
  package tree. The catalog logic being lifted onto the modern API is the legacy remoting
  method the Dojo picker uses. The Dojo screen itself, its remoting methods and the legacy
  layout and portlet services are read, not modified, except for the one-line addition of the
  new portlet id.
- **Backward-compatibility expectations**: Every existing operation keeps its contract. Existing endpoints
  admit more callers, never fewer. The one narrowing is the custom-tool delete and update
  refusing ids that are not custom tools. Removing or rewriting a shipped tool was never these
  operations' purpose; the role-scoped per-portlet remove and the layout editor keep serving that need. Rolling back
  is safe: a section that contains `tools-beta` simply has a portlet the older release does not
  gate on, and the frontend PR ships the `tools-beta` portlet registration independently. No
  stored data changes shape. The legacy Dojo screen continues to work for anyone who has not
  adopted the Beta.
- **Known related decisions**: The access decision recorded on #37574 is to accept Roles or
  Tools during the Beta and to move the custom-tool operations to Tools alone in the promotion
  ticket #37356, when the Dojo tab that creates tools from the Roles screen is retired. The
  gating precedent is #37259 / PR #37323, which gated the system-wide section catalog on the
  Roles portlet and established that a missing grant answers 401. The modern operations that
  grant sections to roles and to users additionally require the CMS Administrator role; on
  2026-09-17 it was decided that the section writes in #37353 follow that precedent, while the
  reads and the custom-tool operations here remain portlet-gated. The plan phase formally
  consults `dotCMS/platform-adrs`.

## Assumptions

- **No licence check.** Access is decided by portlet grants alone, matching the existing
  custom-tool operations. The spike (#37352) asked whether a licence check is needed; none of
  the sibling admin endpoints has one, so none is added.
- **`tools-beta` registration is delivered by the frontend PR #37481**, together with the
  Angular route. This feature gates on the id string regardless of whether that PR has
  merged, so the two can land in either order.
- **The response shapes follow the frontend models already defined**: catalog row `{ id,
  title, isCustom }` and custom tool `{ portletId, portletName, baseTypes, contentTypes,
  dataViewMode }`. The write side stores base types and content types as comma-separated
  text; the read returns them as lists. The data view mode is `list` or `card` in lowercase on
  the wire in both directions, because every existing writer, the legacy screen and Add to
  menu, sends lowercase and the value is stored without normalisation. The frontend maps its
  display labels to those values.
- **Language Variables is a product tool.** Admins did not create it and cannot easily recreate
  it, so it is protected like every other product tool. Declaring it in the registry is
  code-only; a new stored marker was rejected because it would need a data change on every
  install for one row.
- **Title fallback order is translation, then the tool's registered name, then id.**
  This replaces the frontend's current recovery of a label from the raw key, which becomes
  unnecessary but harmless.
- **A Tools grant gives a non-administrator a read-only view of the navigation plus custom
  content tool management.** It does not let them change which tools a section contains, so it
  is not an administrator-equivalent grant. Custom-tool delete is bounded to custom tools by
  FR-013, so it cannot be used to strip access from anyone.
- **The `roles-beta` versus `roles` gap** is out of scope. It is noted so the plan does not
  "fix" it as a side effect.
- **Concurrency** is not addressed. Two admins editing the same custom tool at once is
  last-writer-wins, as it is today.
- **Tests are written first**, per the constitution: integration tests covering catalog
  membership and order, `isCustom`, title fallback, the single-tool read including its two
  not-found cases, the delete refusing a shipped tool, and every gate combination in User
  Story 2 and 3, all registered in a
  `MainSuite` so they run in CI. Which suite, and the exact fixtures, are decided in the plan.
