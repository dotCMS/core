# Feature Specification: Publishing Environments modal in Publishing Queue

**Feature Branch**: `issue-37813-publishing-environments-spec`

**Created**: 2026-10-14

**Status**: Draft — implementation waits on the endpoint status service (dotCMS/core#38027)

**Type**: New Feature

**Input**: User description: "Publishing Environments modal in Publishing Queue — epic dotCMS/core#37813 (implementation task #37815, spike #37814). Migrate the Configuration portlet's Publishing Environments tab to a modal opened from the Publishing Queue (Beta) toolbar, with full feature parity. Every action saves immediately. Access is limited to CMS Admins and users with the Configuration tool, and the known server-side access gaps must be closed."

## Context

Publishing Environments are the destinations push publishing sends bundles to: each environment groups one or more endpoints (remote dotCMS servers, AWS S3 buckets or static targets), has a push mode, and lists the users and roles allowed to send to it. Today they are managed in a tab of the legacy Configuration tool. This feature brings that management into the Publishing Queue (Beta) as a modal, so the people who push content can configure where it goes without leaving the queue. It is not a tool of its own: there is no separate menu entry or address for it.

**Feature parity with the current Configuration tab is the primary requirement.** The Configuration tab stays in place and keeps working.

Design reference: the Publishing Queue design (Publishing Environments dialog), which reuses the earlier Publishing Environments portlet design inside a dialog.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Open Publishing Environments from the queue and browse them (Priority: P1)

An administrator working in the Publishing Queue opens Publishing Environments from the toolbar, sees every environment they are allowed to see, picks one and sees its push mode, who can send to it and its endpoints, then closes the modal and is back in the queue exactly where they were.

**Why this priority**: Without the entry point and the read view nothing else in this feature is reachable; it is also the smallest slice that already replaces "go to Configuration to look at environments".

**Independent Test**: With a few environments configured, open Publishing Queue (Beta) as an admin, open the modal, select each environment and check its detail, then close the modal.

**Acceptance Scenarios**:

1. **Given** a CMS Admin or a user with access to the Configuration tool is on Publishing Queue (Beta) with no bundles selected, **When** they look at the toolbar, **Then** they see an outlined **Publishing Environments** button next to Refresh.
2. **Given** a Publishing Queue user without access to the Configuration tool who is not an admin, **When** they look at the toolbar, **Then** the Publishing Environments button is not shown.
3. **Given** bundles are selected in the queue, **When** the user looks at the toolbar, **Then** the Publishing Environments button is not shown (the toolbar shows the bulk actions instead).
4. **Given** the button is visible, **When** the user clicks it, **Then** a modal titled "Publishing Environments" opens with the environment list on the left and the selected environment's detail on the right; the first environment is selected.
5. **Given** the modal is open, **When** the user looks at the environment list, **Then** each environment shows its name, its endpoint count ("No endpoints", "1 endpoint", "N endpoints") and its push mode ("Push to One Endpoint" / "Push to All Endpoints").
6. **Given** an environment is selected, **When** the user looks at the detail, **Then** it shows the environment name, its push mode, "Who can send: …" with the assigned users and roles (or "No users or roles assigned"), an **Add Endpoint** button, a more-actions menu, and the endpoints table.
7. **Given** there are no environments the user can see, **When** the modal opens, **Then** it shows "No environments configured", a short explanation and an **Add Environment** button instead of the two panes.
8. **Given** the selected environment has no endpoints, **When** the detail shows, **Then** the endpoints area shows "No endpoints in this environment" with a hint that nothing can be pushed there until an endpoint is added.
9. **Given** the modal is open with search text, a status filter or a page set in the queue behind it, **When** the user closes the modal with its X button, the **Close** button or ESC, **Then** the modal closes and the queue keeps its search, filter and page.

---

### User Story 2 - Create, edit and delete environments (Priority: P1)

An administrator creates an environment, sets its push mode and who can send to it, edits it later, clears its push history when they need to resend everything, and deletes it when it is no longer used.

**Why this priority**: Environment management is the core of the old tab; without it the modal is read-only and admins still need Configuration.

**Independent Test**: Create an environment, edit its name, push mode and who-can-send, delete its push history, then delete it — each change visible immediately without reloading the page and still there after reopening the modal.

**Acceptance Scenarios**:

1. **Given** the modal is open, **When** the user clicks **New** (or **Add Environment** in the empty state), **Then** an Add Environment dialog opens on top with: Environment name (required), Push mode as two selectable cards ("Push to One Endpoint — each bundle goes to a single endpoint", "Push to All Endpoints — each bundle goes to every endpoint"; One is preselected), and "Who can send to this environment" as a searchable users-and-roles picker.
2. **Given** the Add Environment dialog, **When** the user saves with an empty name, **Then** the dialog stays open and shows "Environment name is required."
3. **Given** valid values, **When** the user saves, **Then** the environment is created on the server, the dialog closes, the new environment appears in the list and is selected.
4. **Given** the name is already used by another environment, **When** the user saves, **Then** the dialog stays open and shows the server's message; nothing is created.
5. **Given** an environment is selected, **When** the user opens the more-actions menu (or right-clicks the environment in the list), **Then** they see Edit, Delete Push History and Delete (Delete styled as dangerous, separated from the others).
6. **Given** the user chooses Edit, **When** the dialog opens, **Then** it is titled Edit Environment and is filled with the current name, push mode and who-can-send; saving updates the list and the detail immediately.
7. **Given** the user chooses Delete Push History, **When** they confirm "Delete push history for {name}?", **Then** the push history is deleted and a success banner says the next push to that environment will resend all content; cancelling does nothing.
8. **Given** the user chooses Delete, **When** they confirm "Delete environment {name}?" (which warns that its endpoints will be removed and it cannot be undone), **Then** the environment and its endpoints are deleted, it disappears from the list and the first remaining environment is selected (or the empty state shows).
9. **Given** any of these server calls fails, **When** the response arrives, **Then** the user sees the server's message and the list and detail stay as they were before the action.

---

### User Story 3 - Add, edit, enable/disable and delete endpoints (Priority: P1)

An administrator adds the servers or buckets an environment pushes to, choosing the endpoint type and filling the fields that type needs, gets an authorization token from a receiving server without leaving the dialog, switches endpoints on and off, and removes endpoints that are no longer used.

**Why this priority**: An environment without endpoints cannot receive anything; endpoint management is the other half of parity.

**Independent Test**: In one environment, add an HTTP endpoint using Get Token, add an HTTPS endpoint with a pasted token, and (with a Platform license) an AWS S3 and a Static endpoint; toggle one off; edit one; delete one. Reopen the modal and confirm every change persisted.

**Acceptance Scenarios**:

1. **Given** an environment is selected, **When** the user clicks **Add Endpoint**, **Then** an Add Endpoint dialog opens on top, with the subtitle "Environment: {name}", Endpoint name (required), Type (required), and an Enabled switch that is on by default.
2. **Given** the Type list, **When** the install has a Platform license, **Then** it offers Dynamic dotCMS HTTP, Dynamic dotCMS HTTPS, Static AWS S3 and Static; **When** it does not, **Then** only the two Dynamic types are offered.
3. **Given** a Dynamic type is chosen, **When** the dialog shows its fields, **Then** it asks for Address (required), Port (required; prefilled 80 for HTTP and 443 for HTTPS) and "Authorization token or file path", with a **Get Token** button.
4. **Given** a Static or Static AWS S3 type is chosen, **When** the dialog shows its fields, **Then** it asks for Properties as one `key=value` per line, prefilled with that type's template, with the hint that `{hostname}` and `{languageIso}` are replaced at push time.
5. **Given** the user clicks **Get Token**, **When** they enter the receiving server's login, password and an expiration date and request it, **Then** the token is requested from the receiving server through dotCMS and, on success, fills the token field; on failure the server's message is shown and the field is unchanged.
6. **Given** required fields are missing, **When** the user saves, **Then** the dialog stays open and marks each missing field.
7. **Given** valid values, **When** the user saves, **Then** the endpoint is saved on the server, the dialog closes and the endpoints table and the environment's endpoint count update immediately.
8. **Given** the endpoints table, **When** the user looks at a row, **Then** it shows the endpoint name with its address underneath (`protocol://address:port` for Dynamic types, the bucket or publish target for Static types), its type, its status, and an Enabled switch; a row actions menu appears on hover.
9. **Given** an endpoint row, **When** the user flips its Enabled switch, **Then** the change is saved immediately; if saving fails, the switch returns to its previous position and the server's message is shown.
10. **Given** an endpoint row, **When** the user opens its actions menu, **Then** they see Check Integrity (only where allowed — see User Story 4), Edit and Delete (Delete styled as dangerous).
11. **Given** the user chooses Edit, **When** the dialog opens, **Then** it is titled Edit Endpoint and is filled with the endpoint's current values; an authorization token already stored is not displayed, and leaving the token field empty keeps the stored one.
12. **Given** the user chooses Delete, **When** they confirm "Delete endpoint {name}?", **Then** the endpoint is deleted and disappears from the table; cancelling does nothing.

---

### User Story 4 - Check integrity against an endpoint and resolve conflicts (Priority: P2)

Before trusting a receiving server, an administrator checks whether it and this server agree on content types, sites, folders, pages, files and roles, sees the conflicts if any, and fixes them on either side or discards them.

**Why this priority**: Important for keeping environments healthy and part of parity, but environments and endpoints can be managed and used without it.

**Independent Test**: Run Check Integrity against a reachable receiver with no conflicts (success), against an unreachable one (could-not-start error), and against one with a known conflict (results dialog; fix locally, fix remotely and discard each work).

**Acceptance Scenarios**:

1. **Given** an environment in Push to One mode, **When** the user opens endpoint row menus, **Then** Check Integrity is offered only on the first endpoint.
2. **Given** an environment in Push to All mode, **When** the user opens endpoint row menus, **Then** Check Integrity is offered on every endpoint.
3. **Given** an AWS S3 or Static endpoint, **When** the user opens its row menu, **Then** Check Integrity is never offered.
4. **Given** the user chooses Check Integrity, **When** the check starts, **Then** a dismissible "Checking integrity…" banner names the endpoint and that endpoint's status shows Checking until the check finishes; progress keeps being followed while the modal is open.
5. **Given** a check was already running for an endpoint when the modal is opened, **When** that environment's detail shows, **Then** the endpoint shows Checking and its progress is followed as if it had been started now.
6. **Given** the check cannot start (for example the receiver is unreachable), **When** the server answers, **Then** a red banner titled "Integrity check could not start" shows the server's message.
7. **Given** the check finishes with no conflicts, **When** the result arrives, **Then** a green banner says the endpoint is in sync.
8. **Given** the check finishes with conflicts, **When** the result arrives, **Then** an Integrity results dialog opens listing the conflicts grouped by type — Content Types (Structures), Sites, Folders, HTML Pages, File Assets, Roles — showing only the types that have conflicts.
9. **Given** the results dialog, **When** the user chooses where to fix a type's conflicts (this server or the remote one) and clicks **Fix**, **Then** the fix runs, its outcome is reported, and the fixed type is removed from the dialog.
10. **Given** the results dialog, **When** the user clicks **Discard**, **Then** the stored conflicts for that endpoint are discarded and the dialog closes.

---

### User Story 5 - Only authorized users can read or change environments (Priority: P1)

The rule that decides who sees the button is also enforced by the server for every operation the modal performs, so nobody can manage environments, run or fix integrity checks, delete push history or read endpoint configuration by calling the server directly.

**Why this priority**: Moving management next to the queue widens who can reach it; today several of these server operations accept any logged-in user. This must be closed before the feature ships.

**Independent Test**: As a backend user without the Configuration tool who is not an admin, call each server operation the modal uses and confirm every one is refused; repeat as a Configuration user and as an admin and confirm they succeed.

**Acceptance Scenarios**:

1. **Given** a backend user who is not a CMS Admin and does not have the Configuration tool, **When** they create, edit or delete an environment or endpoint, **Then** the server refuses (this already holds today).
2. **Given** the same user, **When** they start, follow, read the results of, fix or discard an integrity check, **Then** the server refuses.
3. **Given** the same user, **When** they delete an environment's push history, **Then** the server refuses.
4. **Given** the same user, **When** they list endpoints or list an environment's endpoints, **Then** the server refuses.
5. **Given** a non-admin user with the Configuration tool, **When** they list environments, **Then** they see only the environments their roles are allowed to send to; a CMS Admin sees every environment.
6. **Given** a user who is allowed, **When** they perform any of the operations above, **Then** they succeed as today.

---

### Edge Cases

- **Unlicensed install**: below the Professional license level the modal shows the unlicensed state instead of the environment list (Publishing Queue already requires the license, so this is a fallback).
- **Non-admin creates an environment that excludes their own roles**: with the current server behavior, it may disappear from their list right after saving. The modal must not appear broken: the user is told the environment was created and why it is not listed, or the behavior is changed to keep it visible (decided in planning after confirming how the Configuration tab behaves).
- **Integrity check still running when the modal is closed**: the check keeps running on the server; reopening the modal resumes following it (User Story 4, scenario 5).
- **Deleting the selected environment while a check is running on one of its endpoints**: the banner is dismissed and nothing keeps following the deleted endpoint.
- **Switching environments while a banner is shown**: the banner belongs to the environment it was raised for and is cleared when another environment is selected.
- **Two admins editing the same environment**: last save wins, as in the Configuration tab; the modal shows server errors (for example the environment was deleted) rather than failing silently.
- **Server error on load**: the modal shows an error state with the server's message instead of an empty list that looks like "No environments configured".
- **Status check unavailable or slow**: rows show "—" (or Failed with the reason once the bounded wait expires) and the rest of the modal stays usable.
- **Long names and addresses**: environment names truncate in the list; endpoint addresses wrap rather than overflow the table.
- **"Receive From" (non-JWT receiving servers)**: not shown in the modal (see Assumptions).

## Requirements *(mandatory)*

### Functional Requirements

**Entry point and access**

- **FR-001**: The Publishing Queue (Beta) toolbar MUST show an outlined **Publishing Environments** button next to Refresh when no bundles are selected, and only to users who are CMS Admins or have access to the Configuration tool.
- **FR-002**: The button MUST NOT be shown while bundles are selected, nor to users who are neither CMS Admins nor have the Configuration tool.
- **FR-003**: Clicking the button MUST open a modal titled "Publishing Environments", sized for a two-pane layout (about 1220 × 840 px, never larger than the browser window), that closes with its X button, a **Close** button in its footer, and ESC.
- **FR-004**: Closing the modal MUST leave the Publishing Queue's search, status filter, selection and page unchanged.
- **FR-005**: The feature MUST NOT add a menu entry, tool or address of its own; it is reachable only from the Publishing Queue (Beta) toolbar.

**Environments**

- **FR-006**: The modal MUST list every environment the user can see, each with name, endpoint count and push mode, and show the selected environment's detail beside the list.
- **FR-007**: The modal MUST show an empty state with **Add Environment** when the user can see no environments, and an error state with the server's message when environments cannot be loaded.
- **FR-008**: Users MUST be able to create an environment with a required name, a push mode (Push to One Endpoint, default; Push to All Endpoints) and any number of users and roles allowed to send to it, picked through a search.
- **FR-009**: Users MUST be able to edit an environment's name, push mode and who-can-send from the more-actions menu or by right-clicking it in the list.
- **FR-010**: Users MUST be able to delete an environment's push history and to delete an environment, each only after confirming, with the outcome reported.
- **FR-011**: The environment detail MUST show "Who can send" with the assigned users and roles, or "No users or roles assigned".

**Endpoints**

- **FR-012**: Users MUST be able to add an endpoint to the selected environment with a required name, a required type, the fields that type requires, and an Enabled switch (on by default).
- **FR-013**: The endpoint types MUST be Dynamic dotCMS HTTP, Dynamic dotCMS HTTPS, Static AWS S3 and Static; the two Static types MUST be offered only with a Platform license.
- **FR-014**: Dynamic types MUST require Address and Port (prefilled 80 / 443) and accept an authorization token or a file path; Static types MUST accept their configuration as `key=value` properties, prefilled with the type's template.
- **FR-015**: Users MUST be able to request an authorization token from a receiving server by entering its login, password and an expiration date; on success the token fills the token field.
- **FR-016**: Editing an endpoint MUST NOT display an already stored authorization token; leaving the token field empty MUST keep the stored token.
- **FR-017**: The endpoints table MUST show, per endpoint, name with address, type, status and an Enabled switch, plus a row actions menu with Check Integrity (where allowed), Edit and Delete.
- **FR-018**: Flipping an endpoint's Enabled switch MUST save immediately and revert the switch if saving fails.
- **FR-019**: Users MUST be able to delete an endpoint only after confirming.
- **FR-020**: The Status column MUST show each endpoint's current health, fetched with a lightweight status check (no integrity check, no side effects) when the environment is shown: **Success** when the receiver is reachable and accepts the stored token; **Failed** when it is unreachable or the token is invalid or expired, with the reason available to the user; **Checking…** while the status is loading or an integrity check is running on that endpoint; and a neutral "—" for endpoint types the status check does not cover. A slow or unreachable receiver MUST NOT delay the rest of the modal.

**Integrity**

- **FR-021**: Check Integrity MUST be offered on the first endpoint of a Push to One environment, on every endpoint of a Push to All environment, and never on Static or Static AWS S3 endpoints.
- **FR-022**: While a check runs, the modal MUST show a dismissible "Checking integrity…" banner naming the endpoint and a Checking status on its row, and MUST follow the check's progress until it finishes, including checks already running when the modal opens.
- **FR-023**: When a check cannot start, the modal MUST show an error banner with the server's message; when it finishes without conflicts, a success banner.
- **FR-024**: When a check finds conflicts, the modal MUST open an Integrity results dialog grouping them by Content Types, Sites, Folders, HTML Pages, File Assets and Roles, and let the user fix each type on this server or on the remote one, or discard the conflicts, reporting each outcome.

**Saving behavior**

- **FR-025**: Every change MUST be saved to the server as soon as the user confirms it (a dialog's Save, a confirmation, or the Enabled switch). The modal MUST NOT hold unsaved changes or offer a "save all" action.
- **FR-026**: After any successful change the list, detail, endpoint counts and endpoints table MUST reflect it without reloading the page; after a failed change they MUST stay as they were and show the server's message.
- **FR-027**: Below the Professional license level the modal MUST show the unlicensed state instead of the environment list.

**Server-side access (backend)**

- **FR-028**: Every server operation the modal uses MUST be allowed only for CMS Admins and users with the Configuration tool. This already holds for creating, editing and deleting environments and endpoints.
- **FR-029**: Starting, following, reading results of, fixing and discarding integrity checks MUST be refused for other users. Today these only require a logged-in user.
- **FR-030**: Deleting an environment's push history MUST be refused for other users. Today it only requires a backend user.
- **FR-031**: Listing all endpoints and listing an environment's endpoints MUST be refused for other users. Today any backend user receives every endpoint.
- **FR-032**: Non-admin users MUST continue to see only the environments their roles may send to; CMS Admins MUST see every environment.

**Compatibility**

- **FR-033**: The Configuration tool's Publishing Environments tab MUST keep working unchanged, so rolling back this feature requires no code change or redeploy.
- **FR-034**: Every action the Configuration tab offers today MUST be available in the modal, except "Receive From" (see Assumptions).

### Key Entities

- **Environment**: a named push-publishing destination. Has a push mode (to one endpoint or to all), the users and roles allowed to send to it, and its endpoints.
- **Endpoint**: a receiver inside an environment. Has a name, a type (Dynamic HTTP / HTTPS, Static AWS S3, Static), an address and port or a set of properties, an authorization token or file path (Dynamic types), an enabled flag, and a current status (reachable and token accepted, or not, with a reason).
- **Authorization token**: a credential issued by the receiving server for a given user and expiration date, stored on the endpoint and used to authenticate pushes. Secret: never shown once stored.
- **Integrity check**: a comparison between this server and one endpoint, run in the background; has a state (running, finished, could not start) and, when finished, a set of conflicts.
- **Integrity conflict**: a difference found by a check, grouped by type (Content Types, Sites, Folders, HTML Pages, File Assets, Roles), that can be fixed on either side or discarded.
- **Push history**: the record of what has already been pushed to an environment; deleting it makes the next push resend everything.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the actions available in the Configuration tab's Publishing Environments section (except Receive From) can be completed from the modal, verified against the parity matrix produced by the spike.
- **SC-002**: An administrator can open the modal from the queue, create an environment and add a working Dynamic endpoint using Get Token in under 3 minutes, without leaving the Publishing Queue.
- **SC-003**: Every change made in the modal is visible in the modal immediately and in the Configuration tab after a refresh, with no page reload needed in the queue.
- **SC-004**: 0 server operations used by the modal succeed for a backend user who is neither a CMS Admin nor has the Configuration tool.
- **SC-005**: Users who should not manage environments never see the Publishing Environments button.
- **SC-006**: Closing the modal returns the user to the queue with search, filter, selection and page unchanged in 100% of cases.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: push-publishing environment and endpoint management, today in the legacy Configuration tool (server-rendered pages and legacy actions), and the Publishing Queue (Beta) toolbar, which gains one button. The server operations for integrity checks and push-history deletion are older endpoints that gain the admin / Configuration access check; the environment and endpoint operations already have it.
- **Backward-compatibility expectations**: the Configuration tab keeps working unchanged and remains the place for "Receive From". Environments, endpoints and tokens created in either place are the same data and appear in both. Tightening access on the integrity, push-history and endpoint-listing operations changes behavior only for users who are neither admins nor have the Configuration tool; anything that relied on those users calling them (for example custom scripts) will be refused. No data or schema change.
- **Known related decisions**: the Publishing Queue (Beta) migration (dotCMS/core#34734) hosts this feature; Dojo-to-Angular migrations keep the legacy screen available until the new one is promoted. The plan will consult `dotCMS/platform-adrs`.

## Dependencies

The frontend work does not start until the first row is available; the others must land before the feature is considered done.

| Dependency | What it provides | Blocks | Issue |
|---|---|---|---|
| Endpoint status service | A lightweight, side-effect-free check of whether each receiver is reachable and accepts its token | The Status column (FR-020) and the start of implementation | dotCMS/core#38027 (assigned) |
| Server-side access hardening | The admin / Configuration check on integrity, push-history deletion and endpoint listing | FR-029 to FR-031, User Story 5 | To be filed by the spike, dotCMS/core#37814 |
| Parity matrix | Every action of the Configuration tab mapped to the modal, with any drop signed off | SC-001, FR-034 | Spike, dotCMS/core#37814 |

## Assumptions

- The rule for who may manage environments is the one the server already applies to environment and endpoint changes: CMS Admin or access to the Configuration tool. No new permission or role is introduced.
- "Receive From" (the list of receiving servers used when token-based push authentication is turned off) is not part of the modal. Token-based authentication is on by default; installs that turn it off keep managing receiving servers in the Configuration tab, which stays available.
- The Integrity results dialog has no visual design yet; its behavior follows the Configuration tab's six-type dialog, and its layout will follow the rest of the modal.
- The users-and-roles picker searches the same users and roles the Configuration tab offers today.
- Publishing Queue already requires the Professional license; the unlicensed state in the modal is a fallback, not a new gate.
- The lightweight status check behind the Status column (FR-020) is a new server capability designed and delivered in its own task (dotCMS/core#38027); this feature depends on it. Integrity checks are never run automatically, because they make the receiver export all of its content metadata.
- The backend access changes (FR-029 to FR-031) are delivered as separate tasks filed by the spike (dotCMS/core#37814) and must be merged before this feature is considered done.
