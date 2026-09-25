# Feature Specification: Section management endpoints for the Tools portlet

**Feature Branch**: `37353-layouts-section-endpoints`

**Created**: 2026-09-18

**Status**: Draft

**Type**: New Feature

**Related GitHub Issue**: [dotCMS/core#37353](https://github.com/dotCMS/core/issues/37353). Sibling backend task: [#37574](https://github.com/dotCMS/core/issues/37574) (Tools portlet id, tools catalog, custom-tool read and gate), which delivers the `tools` portlet id this feature gates on. Access is decided by the id string, so the two can merge in either order.

**Input**: User description: "37353"

## Context

A **section** is a named, ordered group of tools that appears in the admin left navigation.
Every backend user sees the sections granted to their roles, in the sections' stored order,
and inside each section the tools in the section's stored order. Sections are also the way
access to tools is granted: a user can open a tool because a section they hold contains it.

Today the only place to create, rename, re-icon, delete or reorder a section is a tab inside
the legacy **Roles & Tools** screen. That tab talks to the server through a remoting layer the
new Angular Tools portlet cannot call. The one modern read of the section list lives under the
Roles resource and requires the Roles portlet. Nothing on the modern API can create, change,
delete or reorder a section. The legacy dialog does let an admin drag tools into order, but
only inside a modal that saves name, icon, position and the whole tool list in one step.

This feature delivers the section side of the Tools portlet backend: a read of every section
under `/v1/layouts`, and the writes the portlet needs to create, rename, re-icon, delete and
reorder sections and to replace the ordered set of tools inside one section. The tools catalog,
the custom-tool read and the portlet id are the sibling task #37574 and are out of scope here.

The frontend already exists as draft PR #37481, running against a mocked service whose method
list matches the endpoints below one for one. Its section model is
`{ id, name, icon, tabOrder, portletIds }` and this feature must match it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the navigation as it really is (Priority: P1)

An admin opens the Tools portlet and the sections panel shows every section in the system, in
the order they appear in the left navigation, each with its icon, and the selected section
expands to show its tools in their stored order, with a readable title for each tool.

**Why this priority**: Every other action on the screen starts from this list. It is the one
call that unblocks the frontend from its mock, and on its own it already gives users granted
the Tools portlet a faithful read-only view of the navigation.

**Independent Test**: With a backend user who holds the Tools (Beta) portlet, request the
section list and compare it to the legacy Roles & Tools grid, which lists every section. Same
sections, same order, same tools in the same order inside each. Then compare the order to the
left navigation of a user granted every section: it matches.

**Acceptance Scenarios**:

1. **Given** an instance with several sections, **When** a user with the Tools (Beta) portlet
   requests the list, **Then** every section is returned once, in navigation order, each with
   its id, name, icon, position, its ordered tool ids and one localized tool title per tool id
   in the same order.
2. **Given** a section whose tools were saved in a specific order, **When** the list is
   requested, **Then** that section's tool ids come back in exactly that order.
3. **Given** the Getting Started section exists, **When** the list is requested, **Then** it is
   present like any other section, in the position its stored order gives it.
4. **Given** a tool whose title has no translation for the caller's language, **When** the list
   is requested, **Then** its title is a readable name, never a raw translation key.
5. **Given** a section holding a tool that the tools catalog does not list, such as the old
   Languages tool while the configuration that hides it is on, **When** the list is requested,
   **Then** the tool is still returned in that section, because the list reflects what is
   stored, not what is offered.

---

### User Story 2 - Create, rename, re-icon and delete a section (Priority: P2)

An admin clicks **New Section**, gives it a name and picks an icon, and the section appears at
the end of the sections panel with no tools in it. It shows up in the left navigation once it
holds at least one tool and is granted to a role. Later the admin opens the section's menu, picks
**Edit** to change its name or icon, or **Delete** to remove it after confirming.

**Why this priority**: These are the actions the legacy tab exists for. Without them the new
portlet is a viewer, not a replacement.

**Independent Test**: Create a section, confirm it is last in the list with no tools, rename it,
change its icon, confirm nothing else about it changed, delete it, confirm it is gone from the
list and from every role and user that held it.

**Acceptance Scenarios**:

1. **Given** a name no section uses, **When** a CMS Administrator with the Tools (Beta) portlet
   creates a section with that name and an icon, **Then** the saved section is returned with a
   new id, the given name and icon, a position after every existing section, and no tools.
2. **Given** a section already named "Marketing", **When** another section is created or
   renamed to "Marketing", **Then** the request is rejected as invalid with a message the screen
   can display, and nothing is written.
3. **Given** an existing section, **When** its name and icon are changed, **Then** the saved
   section is returned with the new name and icon, and its position and its tools are exactly
   as before.
4. **Given** a section granted to two roles and to one user, **When** it is deleted, **Then**
   it disappears from the list, from the navigation of every user who reached it through those
   grants, and no grant pointing at it remains.
5. **Given** a section id no section has, **When** it is renamed, re-iconed or deleted,
   **Then** the response is not found and nothing changes.
6. **Given** a blank name, a name over 255 characters or an icon over 255 characters, **When**
   a section is created or updated with it, **Then** the request is rejected as invalid.
7. **Given** the Getting Started section, **When** it is renamed or its icon is changed,
   **Then** the change is saved like any other section's and is still there after any user
   switches Getting Started on or off; **When** it is deleted, **Then** the request is rejected
   as invalid and the section is intact.
8. **Given** a section is created, renamed, re-iconed or deleted, **When** an admin has another
   session open, **Then** that session's navigation reflects the change without a hard reload.

---

### User Story 3 - Choose and order the tools in a section (Priority: P3)

An admin selects a section, ticks tools in the Available Tools panel to add them, unticks or
removes them to take them out, and drags a tool within the section to change its order. Each of
those gestures persists as one save of the section's full ordered tool list.

**Why this priority**: Choosing and ordering tools is the second half of what the legacy tab
does. The new screen saves each gesture on its own instead of one modal save.

**Independent Test**: Save the ordered list `[a, b, c]` to a section, read the list back and
confirm the order; save `[c, a]`, confirm `b` is gone and the order is `c, a`; send a list with
an unknown id and confirm the section is untouched.

**Acceptance Scenarios**:

1. **Given** an existing section, **When** a CMS Administrator with the Tools (Beta) portlet
   saves a full ordered list of tool ids to it, **Then** the section's tools become exactly
   that list in that order, every other section is unchanged, and the full section list is
   returned in navigation order.
2. **Given** the saved list, **When** the sections are read again, from any node, **Then** the
   tool ids come back in the order they were sent.
3. **Given** a list containing an id that names no registered tool, a tool the product marks as
   not placeable in a section, or the same id twice, **When** it is saved, **Then** the request
   is rejected as invalid, the message names the offending id, and the section is unchanged.
4. **Given** a list containing a registered tool that the tools catalog does not currently
   offer, such as the hidden old Languages tool, **When** it is saved, **Then** it is accepted,
   so an existing section that holds such a tool can still be reordered.
5. **Given** an empty list, **When** it is saved to a section, **Then** the section is kept with
   no tools and drops out of the menu until it holds a tool again; **When** it is saved to the
   Getting Started section, **Then** the request is rejected as invalid and the section keeps
   its tools, because a user who switches Getting Started on must find something in it.
6. **Given** a section id no section has, **When** a tool list is saved to it, **Then** the
   response is not found.
7. **Given** a tool list is saved, **When** an admin has another session open, **Then** that
   session's navigation reflects the change without a hard reload.

---

### User Story 4 - Reorder the sections (Priority: P4)

An admin drags a section to a new position in the sections panel and the whole navigation
takes that order, for every user, without a reload.

**Why this priority**: Position exists today as a numeric field that admins have to keep
consistent by hand. Dragging is a usability gain, not a new capability, so it ranks after the
actions that have no modern equivalent.

**Independent Test**: Send the ids of every section in a new order, read the list back and
confirm it matches; send a list missing one id and confirm nothing moved.

**Acceptance Scenarios**:

1. **Given** the full set of section ids in a new order, **When** a CMS Administrator with the
   Tools (Beta) portlet saves it, **Then** the sections' positions are rewritten so that the
   navigation order is exactly the order sent, with no two sections sharing a position, and the
   full section list is returned in that order.
2. **Given** a list that omits an existing section, contains an id no section has, or repeats
   an id, **When** it is saved, **Then** the request is rejected as invalid, the message names
   the problem, and no position changes.
3. **Given** the Getting Started section, **When** it is placed anywhere in the order, **Then**
   its new position is saved like any other section's and is still there after any user
   switches Getting Started on or off.
4. **Given** the order is changed, **When** an admin has another session open, **Then** that
   session's navigation reflects the new order without a hard reload, after a single refresh.

---

### User Story 5 - Only the right people change the navigation, and every change is on record (Priority: P5)

A backend user who has been granted the Tools (Beta) portlet can see the sections. Only a CMS
Administrator can change them. Every change, and every attempt by a non-administrator Tools
holder to change something, is written to the security log with who did it.

**Why this priority**: Sections decide which tools each role can reach, so a section write is
an access change. The rule must be in place before the writes ship, but it has no value on its
own, so it is listed last.

**Independent Test**: Three backend users: a non-admin holding a section that contains Tools
(Beta), a non-admin holding neither Tools nor Tools (Beta), and a CMS Administrator with no
explicit grant. The first reads but is refused on every write, the second is refused on
everything, the third passes everything.

**Acceptance Scenarios**:

1. **Given** a non-admin backend user whose granted sections contain the Tools (Beta) or the
   Tools portlet, **When** they request the section list, **Then** it is returned.
2. **Given** the same user, **When** they attempt any write, **Then** the request is refused as
   forbidden, nothing changes, and the security log records the user and the attempt.
3. **Given** a backend user whose granted sections contain neither the Tools nor the Tools
   (Beta) portlet, **When** they request the list or attempt any write, **Then** the request is
   refused as unauthorized with no section data returned.
4. **Given** a CMS Administrator, with or without an explicit Tools grant, **When** they perform
   any read or write, **Then** it succeeds.
5. **Given** an unauthenticated caller, **When** they call any endpoint here, **Then** the
   request is refused as unauthorized with no section data returned.
6. **Given** any successful write, **When** the security log is inspected, **Then** it holds the
   acting user, the operation and the section affected.

---

### Edge Cases

- **Getting Started is the product's own onboarding section.** It is identified by its fixed
  id only; a section an admin creates and names "Getting Started" is an ordinary section here
  and can be renamed, emptied or deleted. The product resolves it
  whenever a user switches Getting Started on or off; that lookup must find the section as the
  admin left it and must not rewrite it, otherwise every edit made here would be undone at the
  next switch. Delete and an empty tool list are refused, and the legacy screen can no longer
  delete it either, so the section always exists and always holds something to show. Rename, icon, position and tool changes are allowed and
  persist.
- **Two admins saving the same name at once.** The application check and the database
  uniqueness rule both guard the name; whichever one trips, the loser gets the same invalid
  response as an ordinary duplicate.
- **Reorder is one step.** Every position is written together, so a failure changes nothing and
  open sessions receive a single refresh rather than one per section.
- **Tools in a section that the catalog does not offer.** A section may hold a registered tool
  the catalog hides (the old Languages tool under its hiding configuration) or an id whose tool
  is no longer registered (a removed plugin). The read returns both. The tool-list write accepts
  the former and rejects the latter, so the screen must drop an unregistered id before it can
  reorder that section. The rejection message names the id so the admin knows why.
- **Sections with no tools exist here but not in the menu.** The legacy dialog requires at
  least one tool; this feature allows a section to be created empty and emptied later, so the
  panel can build a section before filling it. The left menu already skips empty sections, so
  such a section is visible in the Tools portlet and absent from every user's menu until it
  holds a tool.
- **Positions may collide today.** Two sections can share a position number, and the navigation
  then orders them arbitrarily. The reorder write rewrites every position so they are strictly
  increasing in the order sent, which removes the ambiguity. Create places the new section after
  the current last one.
- **Name uniqueness is exact.** "Marketing" and "marketing" are different names today and stay
  so, matching the legacy screen. The incoming name is trimmed, then compared with stored names
  as they are stored. A legacy section saved as " Marketing" with a leading space therefore does
  not block a new "Marketing"; that is accepted.
- **Deleting a section removes access.** A user whose only path to a tool was the deleted
  section loses that tool from their navigation. Deleting the section that carries the Tools
  (Beta) portlet removes the Tools screen from every non-admin who held it; administrators keep
  it through their fallback. This is the intended meaning of deleting a section, not an error.
- **Two admins editing at once.** The reorder and tool-list writes replace the whole list, so
  two admins editing the same section or the section order concurrently overwrite each other,
  last writer wins. Accepted as a known limitation for an administrator-only surface; no version
  check is added.
- **Rejection statuses.** A missing portlet grant is reported as 401 Unauthorized, as every
  portlet-gated endpoint in the product does. A portlet holder who is not a CMS Administrator is
  reported as 403 Forbidden, as the modern operations that grant sections to roles do. The
  frontend must not depend on the distinction beyond "refused".
- **Legacy screen untouched, except deleting Getting Started.** The Roles & Tools tab and its
  remoting methods keep working unchanged, except that deleting Getting Started is refused with
  a message naming the section. The role-scoped read of one role's sections and the system-wide read under the
  Roles resource are not changed; the new read is the Tools-gated replacement for the latter.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST expose a read of every section in the system, ordered as the
  navigation orders them, each carrying its id, name, icon, position, ordered tool ids and one
  localized tool title per tool id in the same order.
- **FR-002**: When a tool's title has no translation for the caller's language, the title MUST
  be the name the tool was registered with, and if that is absent, its id. It MUST never be the
  raw translation key.
- **FR-003**: The system MUST allow a section to be created from a name and an icon. The new
  section MUST be placed after every existing section and MUST contain no tools. The response
  MUST be the saved section.
- **FR-004**: The system MUST allow a section's name and icon to be changed together, leaving
  its position and its tools unchanged. The response MUST be the saved section.
- **FR-005**: A section name MUST be present after trimming, at most 255 characters, and unique
  among sections by exact comparison. An icon MUST be at most 255 characters and MAY be empty. A
  violation MUST be rejected as invalid with a message the screen can display, and nothing MUST
  be written. A duplicate detected by the database's own uniqueness rule, when two writes race,
  MUST be reported as the same invalid response, never as a server error.
- **FR-006**: The system MUST allow a section to be deleted. Deletion MUST remove the section,
  its tool list and every grant of it to a role or a user, leaving no reference behind.
- **FR-007**: The system MUST allow the position of every section to be rewritten from one full
  ordered list of section ids. The list MUST contain every existing section id exactly once;
  otherwise the request MUST be rejected as invalid, the message MUST name the problem, and no
  position MUST change. Positions MUST be written strictly increasing in the order sent, as
  one step: either every position changes or none does, and open sessions MUST receive one
  refresh for the whole reorder.
- **FR-008**: The system MUST allow the ordered set of tools in one section to be replaced from
  one full ordered list of tool ids. Every id MUST name a registered tool that the product allows
  in a section and MUST appear once; otherwise the request MUST be rejected as invalid, the
  message MUST name the offending id, and the section MUST be unchanged. An empty list MUST be
  accepted for every section other than Getting Started. Registered tools the catalog hides
  MUST be accepted.
- **FR-009**: The Getting Started section MUST be returned by the read and MUST accept every
  write except delete and an empty tool list, both refused as invalid, leaving the section
  intact. For these refusals Getting Started is identified by its fixed id only.
- **FR-018**: The product's own Getting Started lookup, used when a user switches Getting
  Started on or off, MUST resolve the section by its fixed id; if no section has that id, by
  the name "Getting Started", adopting that section as it is; and only if neither exists MUST
  it create the section with its default name, icon, position and welcome tool. When the
  resolved section holds no tools, the lookup MUST restore the welcome tool and change nothing
  else. The lookup MUST NOT change the name, icon or position of an existing section.
- **FR-010**: Any write that names a section id no section has MUST respond not found and change
  nothing.
- **FR-011**: The delete, reorder and tool-list writes MUST respond with the full section list
  as the read would return it after the write.
- **FR-012**: After every successful write, open admin sessions MUST reflect the change in their
  navigation without a hard reload, as they do after the legacy screen's writes, with one
  refresh per write.
- **FR-013**: The read MUST require an authenticated backend user who either holds a granted
  section containing `tools` or `tools-beta`, or is a CMS Administrator. Any other caller MUST
  be refused as unauthorized with no section data.
- **FR-014**: Every write MUST require the same portlet condition as FR-013 and, in addition,
  the CMS Administrator role. A caller who passes the portlet condition but is not a CMS
  Administrator MUST be refused as forbidden with nothing written.
- **FR-015**: Every successful write, and every write refused because the caller passed the
  portlet condition but is not a CMS Administrator, MUST be recorded in the security log with
  the acting user, the operation and, where one exists, the section id. A refusal for a missing
  portlet grant is answered by the shared gate and is not separately logged, as for every other
  portlet-gated endpoint.
- **FR-016**: Every endpoint added MUST be described in the generated API documentation with
  response schemas that match what is actually returned.
- **FR-017**: The legacy Roles & Tools screen, its remoting methods, and the existing modern
  reads of sections under the Roles resource MUST continue to behave exactly as today, except
  that the legacy screen's delete MUST refuse the Getting Started section (its fixed id) and
  leave it intact.

### Key Entities

- **Section**: A named, ordered group of tools shown in the admin left navigation. Has an id, a
  name unique among sections, an icon, a position in the navigation, and an ordered list of tool
  ids. Getting Started is the product's own onboarding section, identified by its fixed id;
  it cannot be deleted or emptied here.
- **Tool reference**: A tool id held by a section, in a position within that section. Refers to
  a tool that ships with the product, a custom content tool, or occasionally a tool that is no
  longer registered.
- **Grant**: The link that gives a role, or a user through their own role, a section and
  therefore every tool in it. Owned by Roles & Tools; touched here only when a section is
  deleted, which removes every grant of it.
- **Portlet id `tools`**: The identifier a section must contain for a user to pass the Tools
  gate. `tools-beta` is its Beta-period alias in every gate here. Delivered by #37574.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: For any instance, the section list returned to the Tools portlet is identical in
  membership, order, and per-section tool order to the legacy Roles & Tools grid, and, once no
  two sections share a position, its order matches the left navigation of a user granted every
  section.
- **SC-002**: An admin completes create, rename, re-icon, reorder sections, set tools, reorder
  tools and delete on a section entirely from the Tools portlet, with every step visible in the
  admin navigation of a second open session without a reload.
- **SC-003**: 100% of tool lists saved through this feature are read back in the order they were
  sent, across every node.
- **SC-004**: 100% of invalid writes (duplicate name, blank or over-long name, unknown id,
  non-placeable or duplicated tool id, incomplete or duplicated reorder list, deleting or
  emptying Getting Started, a duplicate name that reaches the database) leave every section
  exactly as it was.
- **SC-009**: 100% of rename, icon, position and tool changes made to Getting Started here are
  still in place after a user switches Getting Started on or off.
- **SC-005**: 100% of writes by a caller who is not a CMS Administrator, and 100% of reads and
  writes by a caller with neither Tools nor Tools (Beta) nor the administrator fallback, are
  refused with nothing written and no section data returned, and every write refused for lack
  of the administrator role appears in the security log.
- **SC-006**: Zero grants point at a deleted section, verified by inspection after every delete
  in the acceptance run.
- **SC-007**: The frontend PR #37481 swaps its mocked section read and five section writes to
  the real endpoints with no change to its section model. Its delete, reorder and set-tools
  mocks return nothing, while the real calls return the full section list; the store may consume
  or ignore that body.
- **SC-008**: The legacy Roles & Tools tab still creates, edits, deletes and grants sections
  after this feature ships, with no change in behaviour other than refusing to delete Getting
  Started.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The section management surface, which today is part of the
  older Roles & Tools admin area, and the section services in the legacy package tree that the
  Dojo tab and the modern Roles endpoints already share. Those services are called, with two
  small additions: the Getting Started lookup resolves by fixed id instead of by name and no
  longer rebuilds an existing section (FR-018), and a single-step reorder that writes every
  position together and notifies once (FR-007). The Dojo tab's delete remoting method refuses
  Getting Started (FR-017); the rest of the Dojo tab, its remoting methods, and the modern Roles
  reads of sections are not modified. The new resource is added alongside them.
- **Backward-compatibility expectations**: No existing endpoint or screen changes contract. No
  stored data changes shape; the reorder write only rewrites position numbers that already exist,
  and the tool-list write uses the same storage the legacy screen uses. Rolling back is safe: a
  section created or reordered here is an ordinary section to the older release, which would
  merely resume resetting Getting Started to its defaults on the next switch. The Getting
  Started lookup change also makes the legacy screen's edits to that section persist, which is
  what that screen always appeared to do. The behaviours this feature is stricter about than
  the legacy screen are refusing to delete or empty Getting Started. Sections with no tools are
  otherwise new: the legacy dialog requires at least one tool. The legacy screen's only change
  is refusing to delete Getting Started. Rolling back restores that delete.
- **Known related decisions**: On 2026-09-17 it was decided, and recorded on #37353,
  that section writes require the CMS Administrator role in addition to the Tools portlet,
  following the modern operations that grant sections to roles and users, while reads and the
  custom-tool operations in #37574 stay portlet-gated. The gating precedent for a missing portlet
  answering 401 is #37259 / PR #37323, which gated the system-wide section read on the Roles
  portlet. Both `tools` and `tools-beta` are
  accepted by every gate during the Beta, with the move to `tools` alone deferred to #37356. The
  spike (#37352) raised concurrent-edit safety on the full-replace writes; the decision here is
  to document last-writer-wins rather than add versioning. The plan phase formally consults
  `dotCMS/platform-adrs`.

## Assumptions

- **Wire names follow the frontend model**: `id`, `name`, `icon`, `tabOrder`, `portletIds`,
  plus `portletTitles` aligned by index with `portletIds`. `icon` is the section's stored
  description and `tabOrder` its stored position, as the issue's field mapping states.
- **Getting Started is editable and durable** (decided 2026-09-23 after spec review): listed
  like any section; rename, icon, position and tools editable; delete and an empty tool list
  refused. The product's lookup is corrected to resolve by fixed id so those edits survive
  (FR-018). Its name is not reserved: identity is the id only, both for these refusals and in the
  legacy screen's delete, and the ordinary uniqueness rule
  already prevents two sections sharing a name. The My Account and Users toggles keep their
  static "Show Getting Started" label whatever the section is renamed to.
- **Positions are rewritten, not preserved.** Reorder assigns strictly increasing positions in
  list order and create appends after the current maximum. The absolute numbers are not part of
  the contract; only their order is.
- **Name uniqueness stays exact and case-sensitive**, as the existing duplicate check behaves.
  Making it case-insensitive would reject sections that coexist today. Trimming the name before
  the check is a small addition; the legacy screen does not trim.
- **Icon is any string up to 255 characters**, including empty. The curated icon list is owned
  by the frontend; the server does not validate against it, so the legacy screen's free-text
  icons remain valid.
- **Tool ids are validated for registration and placeability, not catalog membership.** The
  catalog additionally hides the old Languages tool under a configuration flag; that is a
  presentation rule of the catalog, not a placement rule.
- **Title fallback order is translation, then the tool's registered name, then id**, the same
  rule #37574 sets for the catalog, so the two lists never disagree on a tool's title.
- **No licence check.** Access is decided by portlet grants and the administrator role alone,
  matching every sibling admin endpoint.
- **`tools-beta` registration is delivered by the frontend PR #37481** together with the Angular
  route. This feature gates on the id string, so it works whether or not that PR has merged.
- **The `tools` portlet id constant is delivered by #37574.** If this feature lands first, the
  gate uses the literal id and the plan notes the constant to adopt.
- **Concurrency is last-writer-wins**, as recorded under Known related decisions.
- **Tests are written first**, per the constitution: integration tests covering the read's
  order and titles, each write's happy path, every invalid-input rejection leaving data
  untouched, the delete removing grants, the refused Getting Started delete and empty list, the
  Getting Started lookup (edits survive a switch, adoption by name, creation when missing,
  welcome tool restored when empty), the single-refresh reorder, the order round-trip of
  tools and of sections, and every gate combination in User Story 5, all registered in a
  `MainSuite` so they run in CI. Which suite and the exact fixtures are decided in the plan.
