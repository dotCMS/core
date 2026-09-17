# Feature Specification: Content Drive browse scopes

**Feature Branch**: `37426-content-drive-browse-scopes`

**Created**: 2026-09-09

**Status**: Draft

**Type**: New Feature

**Input**: GitHub issue [#37426](https://github.com/dotCMS/core/issues/37426) — "[TASK] Content Drive: browse scopes for All, site root and System Host"

## User Scenarios & Testing *(mandatory)*

Content Drive can express only one browse scope today. Selecting a site lists everything on it, at every depth, with System Host content mixed in. There is no way to ask for the items that sit **at** the site root, and no way to reach System Host content on its own. This feature splits that single browse scope into three the user picks from the sidebar.

### User Story 1 - Browse the site root, and browse the whole site, as separate things (Priority: P1)

An editor opens Content Drive and wants to see what actually lives at the top of the site, not a flat list of every asset in every folder. Selecting the site in the hierarchy shows the site root: the items sitting there plus the site's top-level folders. When they do want the flat everything-on-this-site view, they select **All Site Content** at the top of the sidebar.

**Why this priority**: This is the defect at the heart of the request. "Browse the site" and "browse the site root" are the same request today, so one of the two views simply does not exist. Everything else in this feature builds on the sidebar having distinct, selectable browse scopes.

**Independent Test**: On a site with content at the root and content nested in folders, select the site row and confirm only root-level items and top-level folders are listed; select All Site Content and confirm the nested content appears and no folders do. Delivers the missing root view without any System Host work.

**Acceptance Scenarios**:

1. **Given** a site with an asset at its root and another asset inside a folder, **When** the user selects the site row in the hierarchy, **Then** the listing shows the root asset and the site's top-level folders, and does not show the asset that lives inside a folder.
2. **Given** the same site, **When** the user selects **All Site Content**, **Then** the listing shows both assets and shows no folders at all.
3. **Given** the user has selected **All Site Content**, **When** they select a folder in the hierarchy, **Then** the listing shows that folder's contents exactly as it does today.
4. **Given** the user has selected the site row, **When** they look at the listing, **Then** no System Host content appears in it regardless of any other setting.
5. **Given** any of the three sidebar entries is selected, **When** the user selects a different one, **Then** the previous selection is cleared, so exactly one entry is ever active.
6. **Given** the user has selected **All Site Content**, **When** they add content by any route — uploading, creating, or dropping files onto the listing — **Then** it is accepted and lands at the site root.
7. **Given** the user is on a later page of **All Site Content**, **When** they select a different sidebar entry, **Then** the listing starts again at its first page with no items still selected.
8. **Given** the site row is selected and a search is running, **When** the search is served by either of the product's two internal search paths, **Then** both return the same items, and neither admits content from inside a folder or from System Host.

---

### User Story 2 - See System Host content on its own (Priority: P2)

A user needs to find or manage assets shared across every site. Today those assets can only be seen mixed into a site's listing. Selecting **System Host** at the bottom of the sidebar lists System Host content and nothing else.

**Why this priority**: It is the second capability that does not exist today, and it is what makes the "Show System Host" toggle honest: shared content becomes reachable on its own instead of only ever appearing as an overlay on a site.

**Independent Test**: With shared content published to System Host and other content on a regular site, select System Host and confirm only the shared content is listed and no folders appear.

**Acceptance Scenarios**:

1. **Given** content exists on System Host and on the current site, **When** the user selects **System Host**, **Then** only the System Host content is listed.
2. **Given** the user has selected **System Host**, **When** they look at the listing, **Then** no folders are offered, because System Host has none.
3. **Given** the user has selected **All Site Content** with "Show System Host" on, **When** they look at the listing, **Then** System Host content appears alongside the current site's content.
4. **Given** the user has selected **All Site Content** with "Show System Host" off, **When** they look at the listing, **Then** no System Host content appears.
5. **Given** the user has selected the site row or a folder, **When** they look at the filter bar, **Then** the "Show System Host" control is not offered: it has nothing to decide outside **All Site Content**, because System Host content can only ever sit at the System Host root.
6. **Given** **System Host** is selected and a search is running, **When** the search is served by either of the product's two internal search paths, **Then** both return the same items, and neither admits content belonging to a site.

---

### User Story 3 - Move content to System Host by dropping it there (Priority: P3)

Having selected some content, a user drags it onto the **System Host** entry to share it across every site, the same gesture they already use to move content into a folder.

**Why this priority**: System Host is a real destination, so making it a drop target completes the interaction. It is separable from browsing: the browse scopes are useful before drag and drop is wired up.

**Independent Test**: Select an item on a site, drag it onto the System Host entry, and confirm it afterwards appears under System Host and no longer under the site.

**Acceptance Scenarios**:

1. **Given** the user has selected content on a site, **When** they drop it onto the **System Host** entry, **Then** the content is moved to System Host and the listing reflects the move.
2. **Given** the user is dragging content, **When** they drag it over the **All Site Content** entry, **Then** it is not offered as a drop target and nothing is moved. Dropping *files* onto the listing while this scope is selected is a different gesture and is accepted (FR-013); what is refused here is the **entry** as a move destination, because the site row directly beneath it already means the site root. Two adjacent rows that move content to the same place is a worse offer than one.
3. **Given** the user is dragging content, **When** they drag it over the site row or a folder, **Then** it behaves exactly as it does today.
4. **Given** the user lacks permission to add content to System Host, **When** they drag content over the **System Host** entry, **Then** it is not offered as a drop target.

---

### Edge Cases

- **A site with nothing at its root.** Selecting the site row shows the top-level folders and no content, or the empty state if the site has no folders either. It must not silently fall back to the everything view.
- **A search combined with a browse scope.** A text search narrows within the selected browse scope; it never widens it. Searching while the site row is selected must not start returning content from inside folders, and searching while System Host is selected must not start returning site content.
- **Restoring a shared link.** A link that names a browse scope reopens on it. A link saved before this feature names none and reopens on the view it produced before, so old links do not silently change meaning.
- **Switching sites while System Host is selected.** The selection survives, because System Host belongs to no site and its content does not change. The **hierarchy below re-renders for the newly chosen site**, so the switch visibly does something rather than appearing to fail, and the selected state MUST stay on the System Host entry: if the highlight drifts onto the new site's root, the sidebar claims the user is in two places at once.
- **A URL whose location is a reserved word rather than a path.** It selects that browse scope. The two cannot be confused, because every real folder path begins with `/` and no reserved word does, so no site can ever own a folder that collides with one.
- **A user without read access to System Host content.** Permission filtering applies to every browse scope, so the System Host entry can legitimately produce an empty listing for such a user.
- **A user who may browse System Host but not add to it.** The System Host entry lists content but refuses uploads, creation and drops, the same way a folder the user cannot add to already behaves.
- **Switching browse scope mid-page.** A user on page 4 of All Site Content who selects System Host lands on the first page of System Host, with nothing carried over from the previous selection.

## Requirements *(mandatory)*

### Functional Requirements

#### Sidebar structure

- **FR-001**: The sidebar MUST offer three kinds of selection: an **All Site Content** entry at the top, the **site hierarchy** (the site row and its folders), and a **System Host** entry at the bottom.
- **FR-001a**: The hierarchy MUST scroll within its own area rather than growing the sidebar, so that the **System Host** entry below it stays visible without hunting for it. The hierarchy loads lazily and grows as folders are expanded, so an entry that merely followed it in the page flow would drift further out of reach with every expansion. The entry above the hierarchy needs nothing special: scrolling back to the top is a cheap, known gesture.
- **FR-002**: **All Site Content** and **System Host** MUST be presented as plain sidebar sections, not as nodes of the site hierarchy: no expansion control, no children, and nothing beneath them to navigate into. This is about structure only. It does not stop System Host accepting content, which the rules below require of it.
- **FR-003**: Exactly one entry MUST be selected at any time; selecting one clears the previous selection.
- **FR-004**: The sidebar MUST NOT show item counts next to any entry.
- **FR-005**: Both sections MUST be reachable and selectable by keyboard, alongside the hierarchy they sit around.

#### What each browse scope lists

- **FR-006**: Selecting **All Site Content** MUST list the current site's content at any depth, and MUST NOT list folders.
- **FR-007**: Selecting the **site row** MUST list only the items that sit at the site root, including the site's top-level folders.
- **FR-008**: Selecting the **site row** MUST NOT include System Host content, under any setting.
- **FR-009**: Selecting a **folder** MUST list its contents exactly as it does today.
- **FR-010**: Selecting **System Host** MUST list System Host content only, and MUST NOT list folders.
- **FR-011**: Every browse scope MUST continue to respect the requesting user's read permissions.
- **FR-012**: A text search or a field filter MUST only ever remove items from the selected browse scope, never add items from outside it. Filtering narrows a browse scope; it never changes which browse scope was asked for. The answer MUST NOT depend on which of the product's internal search paths served the request.

#### Creating, uploading and moving

- **FR-013**: **All Site Content** MUST accept new content, which lands at the **site root**. Uploading, creating and dropping files onto the listing all behave as they do on the site row. The in-flight status deliberately does NOT name the destination: it was tried and removed on 2026-09-16 because the sentence had grown long enough that the part an author reads at a glance was lost behind the part they do not. The cost is knowingly accepted — in the System Host scope the site switcher still names a different site, so an upload there says only how many files are going, not where.

  This reverses an earlier version of this requirement, which made the view read-only on the grounds that a view spanning the whole site "names no single place to put anything". That is true of the view and false of the product: the site root is the obvious destination, and **this is the content search view, where uploading has always been possible and has always landed on the current site**. Refusing the upload prevented no mistake — it sent people to the site row to do the same thing one click later.

  The cost is accepted knowingly: because the listing carries no path column (see Assumptions), a file added here appears among everything else with nothing saying it sits at the root. That gap is inherited rather than introduced, the indicator naming the site is today's mitigation, and the location column that closes it properly is being handled separately.
- **FR-014**: The **site row**, a **folder**, and **System Host** MUST accept new content: uploads, creation, and content dropped onto them. For the site row and folders this is exactly today's behavior.
- **FR-015**: Dropping content onto the **System Host** entry MUST move it to System Host.
- **FR-016**: While **System Host** is selected, the permission check that gates creating and uploading MUST be evaluated against System Host itself, never against whichever site is selected in the site switcher.

#### The System Host toggle

- **FR-017**: The control that decides whether System Host content appears MUST live in a bar above the listing rather than among the filter chips, beside a sentence naming what the listing is currently showing. The sentence MUST agree with the control: with System Host included it reads "All Files in site (System Host shared files included)", and with it excluded, "…excluded". Links already in circulation that carry the control's value MUST keep restoring correctly. The Asset Picker keeps the chip it has always had, labelled "Show System Host"; this requirement governs Content Drive only.
- **FR-018**: The bar MUST be offered only while **All Site Content** is selected — the site root and System Host each answer the question it asks simply by being chosen, leaving its sentence nothing to qualify and its toggle nothing to decide. It MUST open and close by height rather than appearing and vanishing, so the listing is pushed down instead of jumping under the pointer. It MUST NOT show an item count: the listing endpoint pages by cursor and returns no total, so any number shown there would be invented.
- **FR-019**: The control MUST retain its value while another browse scope is selected, so returning to **All Site Content** restores the user's previous choice rather than resetting it.
- **FR-020**: With the control on, **All Site Content** MUST include System Host content alongside the site's; with it off, **All Site Content** MUST exclude it.

#### Persistence

- **FR-021**: The selection MUST be carried by the single URL value that already says where the drive is browsing, not by a second value beside it. Two values could disagree with each other, and then neither would be the answer.
- **FR-022**: That one value MUST be able to express all four selections: **absent** means All Site Content, `/` means the site root, a deeper path means that folder, and a reserved word means System Host. A reserved word MUST NOT be mistakable for a folder, which the leading `/` on every real path already guarantees.
- **FR-023**: Links made before this feature MUST keep meaning what they meant. A link carrying no location still lists the whole site, and a link to a folder still opens that folder.
- **FR-024**: A reload, a browser back or forward, and a shared link MUST all reopen the selection the sender was viewing.
- **FR-025**: Changing the selection MUST return the listing to its first page and clear the current item selection, since neither carries any meaning across selections.

#### Not breaking what exists

- **FR-026**: A content listing requested without a browse scope MUST behave as it does today, so other surfaces that share this listing (notably the Asset Picker) are unaffected by this feature.
- **FR-027**: No consumer of the shared content-listing service other than Content Drive may change behavior. Only Content Drive's own requests carry a browse scope; every other caller MUST keep producing exactly the results it produces today.
- **FR-028**: That claim MUST be demonstrated rather than assumed. Every one of those consumers reaches the listing through a single shared seam, so the seam itself MUST be pinned by a test, and each named consumer MUST be recorded as either covered by that test or checked by inspection. None may be left unaccounted for.

### Key Entities

- **Browse scope**: Which slice of content the listing is being asked for. One of: the whole current site at any depth, the current site's root only, or System Host only. Named in full throughout, because Content Drive is separately gaining a *search* scope that says which fields a search looks at. The two are independent: a browse scope says where you are, a search scope says how a search reads what is there.
- **Site root**: The level of a site that is not inside any folder. Home to both root-level content and the site's top-level folders.
- **System Host**: The site-independent container for content shared across every site. It holds no folders, and site content cannot live inside it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can list the items sitting at a site's root without any content from inside that site's folders appearing, which is not possible today.
- **SC-002**: A user can list System Host content on its own without any site's content appearing, which is not possible today.
- **SC-003**: In 100% of listings produced by the site-root and System Host browse scopes, every returned item belongs to the browse scope that was asked for.
- **SC-004**: A given browse scope returns the same set of items with a search active as without one, minus only the items the search legitimately excludes.
- **SC-005**: Opening a shared Content Drive link reproduces the browse scope the sender was viewing, every time.
- **SC-006**: Content is moved to System Host in a single drag, with no dialog and no intermediate step.
- **SC-007**: Surfaces other than Content Drive that browse content list exactly what they listed before this change, a listing asked for without a browse scope returns exactly what it returns today, and every consumer of that listing is accounted for by name rather than covered by a blanket claim.
- **SC-008**: Content added from the All Site Content view — by any of uploading, creating, or dropping files onto the listing — arrives at the site root.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Content Drive listing and the underlying content-browsing service it shares with the Asset Picker, which is long-standing code that predates Content Drive. The meaning of "the site is selected" changes deliberately: it becomes the site root rather than the whole site, with the whole-site view moving to its own **All Site Content** entry. That is a visible behavior change for existing Content Drive users and is the point of the feature.
- **Backward-compatibility expectations**: **Preserving existing behavior is a hard constraint, not a preference.** The content-listing service Content Drive uses is shared with the assets API, the older file browser and its deprecated tree endpoint, the legacy admin browser, a Velocity viewtool, and two internal callers. None of them will ask for a scope, so whatever a scope-less request means today it must mean afterwards, byte for byte, including how System Host content is treated. Shared Content Drive links created before this change must keep restoring, including the value of the toggle being renamed. No deprecation of existing admin workflows is intended.
- **A rename that now lands on one surface**: the "Show Shared Assets" label is a single shared translation that once served both the Content Drive toolbar and the Asset Picker toolbar. Content Drive no longer uses it — its control moved into the scope bar with wording of its own (FR-017) — so renaming it to "Show System Host" changes the Asset Picker alone. That is a visible change on a surface this feature does not otherwise touch, and it should be called out in review rather than discovered there.
- **Known related decisions**: The listing has two internal query paths that can disagree about whether System Host content is included; they must agree before any browse scope can be trusted, which is why the spec requires a scope to return the same items with or without a search rather than leaving it as an implementation concern. Only one of the two runs by default, and the other is reachable only by configuration, so proving they agree means deliberately exercising the site-root and System Host scopes under each rather than waiting for the non-default one to show up on its own. Moving content and browsing content also address System Host by different means, so support for one does not imply support for the other. Issue #37166 is related: it touches how Content Drive reports operations and surfaced this while examining what a move actually changes in the listing. The plan phase will formally consult `dotCMS/platform-adrs`.

## Assumptions

- **System Host accepts everything a folder accepts**: moved content, uploaded files, and newly created content. It is a real place to put things. **All Site Content** names no place of its own, but content added there lands at the site root, which is the destination the site row names — so the two agree rather than one of them refusing.
- **Selecting System Host survives a site switch**, because System Host belongs to no site and the listing would not change.
- **The site root browse scope shows the site's top-level folders.** They sit at the root, so they are part of what is "at" the root. This means the site-root and All Site Content browse scopes differ in their content, not in their folders, since All Site Content shows no folders at all.
- **Where an item lives is not shown in All Site Content, and that is accepted for now.** The listing carries no path column, so a flat view spanning the whole site cannot tell two files of the same name in different folders apart. The gap is inherited rather than introduced: today's site view is already this flat view. Naming the view and making it the default does raise the cost of it, and a location column shown when the listing spans more than one folder is the fix, but it is being handled separately and is not a defect in this work.
- **The Move dialog is not being changed to match the drop.** Its destination picker filters System Host out of the site list whenever a folder destination is required, which is the mode the dialog runs in, so dragging onto System Host will be the only route to that destination. Whether the dialog should offer it too is a product decision, deliberately left outside this feature.
- **"Children of the site root" is deliberately not a browse scope.** System Host has no folders and site content cannot live under System Host, so the site hierarchy already covers browsing below the root.
- **Item counts beside the sidebar entries are prototype-only** and are not part of this feature.
