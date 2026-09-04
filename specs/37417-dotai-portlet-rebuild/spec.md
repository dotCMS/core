# Feature Specification: dotAI Portlet Rebuild

**Feature Branch**: `fmontes/dot-ai-portlet`

**Issue**: [dotCMS/core#37417](https://github.com/dotCMS/core/issues/37417)

**Created**: 2026-09-04

**Status**: Draft

**Type**: New Feature

**Input**: User description: "Rebuild the dotAI portlet on the modern admin stack, reorganizing the legacy four tabs into the five-tab approved redesign (Search, Chat, Image, Embeddings, Config Values) with no new functionality, swapping it in place while keeping the old screen reachable as a legacy twin."

## Overview

Dojo → Angular migration of the dotAI portlet, following an approved redesign. The legacy four tabs become five — Search, Chat, Image, Embeddings, Config Values — with a shared retrieval-settings panel on Search and Chat and a guided index dialog on Embeddings.

No new functionality: every screen maps onto behavior that already ships. The new screen takes over the existing menu entry so upgrades need no manual step; the old screen stays reachable at an unlisted fallback address for rollback.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Search indexed content semantically (Priority: P1)

Enter a natural-language query, get ranked content back with match passages, closeness scores and deep links. A settings panel beside the results narrows index, site, content types, threshold and distance measure.

**Why this priority**: The portlet's primary screen and the fastest replacement of the legacy tool.

**Independent Test**: Build a small index, search it, verify results and deep links; change settings and confirm the next run reflects them.

**Acceptance Scenarios**:

1. **Given** an index with content, **When** a query is submitted, **Then** results appear ranked by closeness, each showing content type, modification date, matching passage, match count and closeness — with a header showing result count, time taken and threshold applied.
2. **Given** nothing matches above the threshold, **When** results return, **Then** the user is told why rather than shown a blank panel.
3. **Given** a result with no modification date, **When** it renders, **Then** the date is omitted cleanly.
4. **Given** the inner-product distance measure is selected, **When** the search runs, **Then** it uses inner product (see FR-024).
5. **Given** settings were adjusted, **When** the user goes to Chat and back, **Then** they are unchanged.
6. **Given** the portlet is reloaded, **When** Search opens, **Then** the last query is restored.

---

### User Story 2 - Ask questions and read the answer as it streams (Priority: P2)

Ask in prose; the answer renders progressively and can be stopped mid-flight. The same settings panel governs what the answer draws on.

**Why this priority**: Second half of the legacy tool's core; depends on the same settings as Search.

**Independent Test**: Send a question and confirm incremental rendering; press Stop mid-answer and confirm generation halts with the partial answer intact.

**Acceptance Scenarios**:

1. **Given** a configured provider and selected index, **When** a question is sent, **Then** a busy indicator shows and the answer renders progressively until complete.
2. **Given** an answer is streaming, **When** Stop is pressed, **Then** generation halts, no further text arrives, and the partial answer stays readable.
3. **Given** an answer is streaming, **When** a second question is sent, **Then** the first is abandoned.
4. **Given** the answer fails partway, **When** it fails, **Then** the error shows inline in the conversation — no blocking dialog over a stream the user is watching.
5. **Given** the composer has text, **When** Enter is pressed, **Then** it sends; **When** Shift+Enter, **Then** a newline is inserted.

---

### User Story 3 - Manage embeddings indexes (Priority: P3)

List every index with its counts, coverage, cost estimate and build status. Create, add to, delete from, delete outright, or rebuild the store — destructive actions behind confirmations.

**Why this priority**: Foundational but administrative, and it requires a role Search does not.

**Independent Test**: Create an index from a query, watch it build then settle, delete part of it by query, then delete it entirely.

**Acceptance Scenarios**:

1. **Given** existing indexes, **When** Embeddings opens, **Then** each shows name, covered content types, chunk and content counts, token total, tokens-per-chunk, estimated cost and status.
2. **Given** a build is accepted, **When** it starts, **Then** that index reports as building and returns to ready once its counts stop moving.
3. **Given** the index dialog is open, **When** the user toggles between add and delete mode, **Then** the submit action and label change, and delete mode submits the query as a deletion criterion.
4. **Given** an index is deleted, **When** confirmed, **Then** it disappears from the list and from the retrieval index picker with no page reload.
5. **Given** two indexes are deleted in quick succession, **When** both are confirmed, **Then** both complete; neither cancels the other.
6. **Given** Rebuild is triggered, **When** the confirmation appears, **Then** it states the action discards the embeddings store, and nothing happens unless accepted.
7. **Given** the list is filtered by name or status, **When** the filter changes, **Then** rows update with no server round trip.

---

### User Story 4 - Generate an image and keep it (Priority: P4)

Describe an image, pick an orientation, generate. Preview it, read the provider's rewritten prompt, then save to assets or download — two separate actions.

**Why this priority**: Part of the legacy tool but independent of the retrieval pipeline.

**Independent Test**: Generate, confirm the preview and rewritten prompt, save to assets, then download.

**Acceptance Scenarios**:

1. **Given** a prompt and orientation, **When** generating, **Then** a placeholder shows and the finished image replaces it.
2. **Given** a generated image, **When** it renders, **Then** the rewritten prompt is visible and copyable.
3. **Given** Save has not been pressed, **When** generation finishes, **Then** nothing has been published.
4. **Given** Save is double-clicked, **When** both clicks register, **Then** the image publishes once.
5. **Given** saving fails, **When** the error is reported, **Then** the image stays on screen.

---

### User Story 5 - See what dotAI is configured with (Priority: P5)

Every resolved setting as key, value and source, secrets masked, searchable and copyable. The full provider configuration opens as formatted structured text.

**Why this priority**: Diagnostic — high value when something is wrong, and it must stay reachable when nothing else is.

**Independent Test**: Open Config Values on a configured instance; confirm sources, masked secrets and the formatted provider view.

**Acceptance Scenarios**:

1. **Given** a configured instance, **When** Config Values opens, **Then** each setting shows key, value and source — app configuration when explicitly set, default otherwise.
2. **Given** a credential setting, **When** it renders, **Then** it is labelled a secret and shown as a fixed mask; neither the stored value nor the server's own mask string is displayed.
3. **Given** the provider configuration cannot be safely presented, **When** that happens, **Then** the screen says so instead of rendering the failure marker as a value.
4. **Given** the user searches, **When** they type, **Then** rows filter with no server round trip.

---

### User Story 6 - Never hit a silent dead end (Priority: P6)

A user with no provider configured, or with portlet access but not the administrator role index operations require, currently gets empty controls and no explanation. Each state must state what is missing.

**Why this priority**: Closes a real dead end neither the legacy screen nor the design addresses.

**Independent Test**: Open with no provider configured, then as a non-administrator; confirm both states explain themselves.

**Acceptance Scenarios**:

1. **Given** no provider is configured, **When** the portlet opens, **Then** a persistent notice explains it and Search, Send, Generate and Build are unavailable.
2. **Given** no provider is configured, **When** the user navigates, **Then** Config Values and Embeddings stay reachable — the portlet is never blanked.
3. **Given** the user lacks the administrator role, **When** Embeddings or the index picker loads, **Then** each states the role requirement instead of showing an empty result.
4. **Given** either state, **When** it renders, **Then** no error dialog interrupts.

---

### User Story 7 - Fall back if the new screen misbehaves (Priority: P7)

The old screen stays reachable at a separate documented address, absent from every menu, so a regression is fixable without an outage.

**Why this priority**: Rollout insurance.

**Independent Test**: After the swap, confirm the menu opens the new screen and the fallback address opens the old one intact.

**Acceptance Scenarios**:

1. **Given** an upgraded installation, **When** dotAI is opened from the menu, **Then** the rebuilt screen opens with no manual configuration or layout edit.
2. **Given** the same installation, **When** support navigates to the fallback address, **Then** the previous screen loads and works as before.
3. **Given** the fallback exists, **When** any user browses their menus, **Then** it does not appear.

---

### Edge Cases

- A requested index no longer exists — the user is told which index was missing, not given a generic failure.
- Answer data arrives split mid-token across transport boundaries — the answer still renders as continuous, correct text.
- The user leaves Chat while an answer streams — generation is cancelled, not left running unseen.
- A stored preference names an index or model no longer offered — defaults apply.
- Very long content-type coverage or configuration values — rows truncate; the page never scrolls sideways.
- An empty index list, or an index with zero chunks — the table explains the state.
- A build is started for an index already building — status stays accurate rather than flickering.
- The provider rewrites a prompt — the rewritten prompt is always shown.

## Requirements *(mandatory)*

### Functional Requirements

**Placement and rollout**

- **FR-001**: The rebuilt screen MUST open from the existing dotAI menu entry with no upgrade step, layout change or manual configuration.
- **FR-002**: The previous screen MUST remain reachable at a separate documented address, behavior unchanged.
- **FR-003**: The fallback screen MUST NOT appear in any menu.
- **FR-004**: The rebuilt screen MUST render inside the admin shell, inheriting theme, navigation and site context — not in an embedded legacy frame.
- **FR-005**: The portlet MUST present five tabs — Search, Chat, Image, Embeddings, Config Values — each addressable by its own URL.
- **FR-006**: Opening the portlet without naming a tab MUST land on Search.

**Search**

- **FR-007**: Users MUST be able to submit a query by Enter or by the search action and receive results ranked by semantic closeness.
- **FR-008**: Each result MUST show title as a link to the content, content type, modification date when one exists, matching passage, match count and closeness.
- **FR-009**: The results header MUST show result count, time taken and the threshold applied.
- **FR-010**: The last search query MUST be restored on return.

**Chat**

- **FR-011**: Users MUST be able to send a question and see the answer render progressively.
- **FR-012**: Users MUST be able to stop a response in flight; stopping MUST halt production, not merely hide it, and MUST leave the partial answer readable.
- **FR-013**: Sending a new question while one is in flight MUST abandon the earlier one.
- **FR-014**: A generation failure MUST be reported inline in the conversation, never as a blocking dialog.
- **FR-015**: Leaving the Chat tab mid-response MUST cancel it.

**Shared retrieval settings**

- **FR-016**: Search and Chat MUST share one settings panel governing index, site, content types, response length, model, temperature, threshold and distance measure.
- **FR-017**: Settings MUST survive navigation between Search and Chat unchanged.
- **FR-018**: Settings MUST be restored on the next visit; a restored value no longer offered MUST fall back to the current default.
- **FR-019**: The panel MUST be dismissible or resizable, and that choice MUST persist.
- **FR-020**: An empty content-type selection MUST mean "all content types" and MUST NOT be sent as an explicit empty restriction.
- **FR-021**: A cleared site selection MUST mean "all sites".
- **FR-022**: Temperature MUST be constrained to its supported range.
- **FR-023**: Response length MUST enforce the platform's true minimum of 128 tokens. *(The legacy screen advertises 10, which the server rejects.)*
- **FR-024**: Selecting inner product MUST perform an inner-product search. *(The legacy screen sends an unrecognized value and silently falls back to cosine.)*

**Embeddings**

- **FR-025**: Users MUST see every index with name, covered content types, chunk count, content count, token total, tokens-per-chunk, estimated cost and status.
- **FR-026**: Cost MUST be shown for every index and labelled as an estimate based on one provider's published pricing.
- **FR-027**: Build status MUST be per index — building from the moment a build is accepted, ready once its counts stop changing.
- **FR-028**: Users MUST be able to filter by name and status with no server round trip, and sort by the numeric columns.
- **FR-029**: Users MUST be able to create an index or add to one from a content query, optionally restricted to specific fields and shaped by a template.
- **FR-030**: The same dialog MUST support deleting matching content from an index, with its action and label changing to match the mode.
- **FR-031**: Users MUST be able to delete an entire index, behind a confirmation.
- **FR-032**: Users MUST be able to rebuild the embeddings store, behind a confirmation stating the action is destructive.
- **FR-033**: Every index mutation MUST refresh the list, and every consumer of it MUST reflect the change with no page reload.
- **FR-034**: Concurrent deletions of different indexes MUST all complete.
- **FR-035**: Repeated activation of a build, save or rebuild MUST perform the action once.

**Image**

- **FR-036**: Users MUST be able to generate an image from a prompt and orientation, with a visible in-progress state.
- **FR-037**: Generating MUST NOT publish. Saving to assets MUST be a separate explicit action.
- **FR-038**: Users MUST be able to download a generated image without publishing it.
- **FR-039**: The provider's rewritten prompt MUST be shown and copyable.
- **FR-040**: A failed save MUST leave the image on screen.

**Config Values**

- **FR-041**: Users MUST see every resolved setting as key, value and source — app configuration when explicitly set, default otherwise.
- **FR-042**: Credential settings MUST be labelled secrets and rendered as a fixed mask; neither the stored value nor the server's own mask string may be displayed.
- **FR-043**: Keys MUST be the platform's real configuration keys, so a value found here can be searched for elsewhere.
- **FR-044**: Users MUST be able to search the list with no server round trip and copy any value.
- **FR-045**: The full provider configuration MUST be viewable as formatted structured text in a read-only view.
- **FR-046**: If the provider configuration cannot be safely presented, the screen MUST say so rather than render the failure marker as a value.

**Availability, permissions and errors**

- **FR-047**: With no provider configured, a persistent notice MUST explain this and Search, Send, Generate and Build MUST be unavailable.
- **FR-048**: With no provider configured, Config Values and Embeddings MUST remain reachable — the portlet MUST NOT be blanked.
- **FR-049**: Where the user lacks the administrator role index operations require, the Embeddings tab and the index picker MUST each state that requirement instead of showing an empty result.
- **FR-050**: Neither the unconfigured nor the insufficient-permission state may surface as an error dialog.
- **FR-051**: Recoverable failures MUST leave the surrounding screen usable — a failed refresh must not strand the user in a permanent loading state.

**Presentation, text and accessibility**

- **FR-052**: All user-visible text MUST come from the platform's language bundle; no hardcoded strings.
- **FR-053**: The portlet MUST use the installation's configured theme colors.
- **FR-054**: Every data-presenting area MUST distinctly handle loading, empty, error and loaded states.
- **FR-055**: The portlet MUST NOT scroll horizontally at standard desktop widths; long values MUST truncate.
- **FR-056**: Interactive elements MUST be reachable and labelled for assistive technology; in-progress states MUST be announced, not conveyed by motion alone.

**Scope discipline**

- **FR-057**: This feature MUST NOT introduce new server capability.

### Out of Scope

- **Sources shown alongside chat answers** — only the non-streaming mode carries them, and progressive rendering was chosen instead. The empty-state copy must not promise sources.
- **Sorting search results in the UI** — results arrive ordered and no alternative ordering exists, so a control would misrepresent what is possible.
- **App-level AI configuration editing** (providers, credentials, connection tests) — stays on the existing dotAI app configuration screens.
- **Any change to indexing, retrieval, generation or storage behavior.**
- **The legacy raw structured-response mode** — the provider's own response payload and total time become unreachable from the admin. Deliberate.
- **The legacy recent-image-prompts list** — browser-local, never portable, and could not have carried over to a new storage key regardless. Deliberate.
- **Migrating the legacy screen's stored preferences** — its stored query served both search and chat, now separate prompts. The old entry is left untouched and continues to serve the fallback screen.

### Key Entities

- **Embeddings Index**: A named collection of embedded content — chunk count, content count, token total, tokens-per-chunk, covered content types, derived cost estimate and derived build status. One index is the default. A reserved internal cache index appears in the list but is not selectable as a retrieval target.
- **Search Result**: Content returned by a semantic search — identity, title, content type, modification date where available, and one or more matching passages.
- **Matching Passage**: An excerpt that matched the query, with a closeness score.
- **Retrieval Settings**: The shared criteria governing what a search or answer draws on — index, site, content types, threshold, distance measure, model, temperature, response length.
- **Chat Message**: One turn, authored by the user or produced by the assistant, the latter possibly incomplete while in flight or stopped.
- **Generated Image**: An image from a prompt — the prompt as written, the provider's rewritten prompt, and a temporary location it can be viewed and downloaded from until deliberately saved.
- **Resolved Configuration Value**: A dotAI setting as the platform resolves it — key, effective value, and source (app configuration, default, or secret).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every capability of the previous screen is reachable in the rebuilt screen except the removals listed in Out of Scope — verified item by item.
- **SC-002**: The portlet is indistinguishable in shell, theme and navigation from the other Developer Tools screens, and a branded installation renders it in its brand colors.
- **SC-003**: 100% of user-visible text is externalized; no hardcoded strings remain.
- **SC-004**: A first-time user can run a useful search without changing any setting.
- **SC-005**: A streaming answer stops within one second of being asked to, with no further text after.
- **SC-006**: An index build or removal is reflected everywhere the list appears within one refresh cycle, with no page reload.
- **SC-007**: Zero silently empty or inert surfaces — every state where the portlet cannot do what was asked gives a stated reason.
- **SC-008**: The previous screen answers at its fallback address on an upgraded installation and appears in no menu.
- **SC-009**: An existing installation shows the rebuilt screen at its usual menu entry immediately after upgrade, zero manual steps.
- **SC-010**: No horizontal page scroll at 1280px and above with the longest realistic values present.
- **SC-011**: Behavior is verified by tests written, approved and confirmed failing before implementation; the tests guarding existing shared behavior pass unchanged.
- **SC-012**: FR-023 and FR-024 are demonstrably fixed and covered by tests.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The dotAI portlet in Developer Tools — currently Dojo/JSP in an embedded legacy frame. Also the shared AI client layer used by the block editor, the content editor's AI features and the dotAI app configuration screens: reorganized so the portlet can build on it, with no behavior change for those consumers. Portlet registration is edited to add the fallback twin. No server-side AI behavior, storage or contract changes.
- **Backward-compatibility expectations**: The menu entry, portlet identity and existing layouts keep working untouched, so upgrades need no migration step. AI features outside this portlet must behave identically, and the tests guarding them must pass unchanged. The previous screen keeps working at its fallback address, with its stored preferences deliberately left alone. Nothing is deprecated in this release; retiring the fallback is a later decision.
- **Known related decisions**: Two sibling Developer Tools screens — ES Search (#34733) and Velocity Playground (#34737) — were migrated this same way, and this follows that precedent. Index operations have long required an administrator role while portlet access does not, which is the source of the dead end FR-049 closes. The admin theme's primary color is customer-configurable at runtime, which is why FR-053 requires theme colors rather than the design's palette. The plan phase will consult `dotCMS/platform-adrs`.

## Assumptions

- The approved redesign is the visual and structural authority. Where it shows something the platform cannot supply, the closest honest equivalent is used and the difference stated.
- The design's specific brand color is not adopted (FR-053) — that color is customer-configurable, so hardcoding it would desynchronize this screen from the rest of the admin.
- Persisting the settings panel between visits (FR-018) is **new behavior** — the legacy screen never stored those controls. Included because a ten-control panel that resets on reload is a daily annoyance.
- Build status is derived by observing whether an index's counts are still changing, seeded by the fact a build was just requested. The platform stores no status field.
- Index cost is an estimate from one provider's published token pricing, already inaccurate for the other supported providers — hence FR-026's labelling.
- The design's "Updated \<date\>" line for an index cannot be honored — no timestamp is stored anywhere. Covered content types occupy that slot instead: real data, previously buried in a tooltip.
- The internal cache index continues to appear in the list but not in the retrieval picker, preserving legacy behavior.
- Anyone reaching the portlet has layout access to it; role gaps are handled per FR-049 rather than by hiding the portlet.
- Three capabilities are removed on purpose, not overlooked — chat sources, the raw response mode, and recent image prompts. All remain on the fallback screen. A reviewer who disagrees should say so here rather than at implementation.
