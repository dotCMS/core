# Feature Specification: Network (Beta) Portlet

**Feature Branch**: `issue-37803-network-portlet-spec`

**Issue**: [dotCMS/core#37803](https://github.com/dotCMS/core/issues/37803) (epic [#37801](https://github.com/dotCMS/core/issues/37801), spike [#37802](https://github.com/dotCMS/core/issues/37802))

**Created**: 2026-10-07

**Status**: Draft

**Type**: New Feature

**Input**: User description: "Add a standalone Network (Beta) portlet that replaces the Network tab of the Configuration portlet with full feature parity: every cluster node with its identity, last contact and an Up / Lagging / Down status; the selected node's detail with its Cache Transport, Search Cluster and Assets health; Refresh in place; loading, empty, error and unlicensed states. Node status and section health are whatever the backend reports. Details: dotCMS/core#37801 (epic), #37802 (spike), #37803 (task)."

## Scope Note *(read this first)*

Today an administrator checks the health of a dotCMS cluster in the **Network** tab of the Configuration portlet (`/#/c/configuration`). The tab is built on legacy Dojo widgets: a table of nodes, and a floating panel that opens when a row is clicked, showing that node's cache transport, search cluster and shared assets details.

This feature adds a standalone **Network (Beta)** portlet — a node list on the left, the selected node's detail on the right — following the new design. **Feature parity with the current tab is the primary requirement**: every value, status and action the tab exposes must be available, with the tab's display bugs fixed rather than copied. The [Parity map](#parity-map) below lists every feature of the old tab and where it lands.

Three boundaries are worth stating up front.

**This is a screen, not a backend change.** The portlet displays what the cluster status service reports. It does not decide whether a node is Up, Lagging or Down, does not compute section health, and does not change how nodes are discovered or contacted. Everything it needs from the backend is listed under [Dependencies](#dependencies); some of it is not built yet.

**This is a Beta beside the old tab, not a replacement of it.** The Configuration portlet's Network tab stays exactly as it is, so an admin who hits a problem in the Beta still has the old view. Promoting the Beta to the primary Network portlet, adding it to default layouts and retiring the tab are separate work ([#37805](https://github.com/dotCMS/core/issues/37805)).

**This is read-only.** The old tab's backend also offers removing a server from the cluster, reading search configuration properties, license repository totals and a cache ping. The tab never exposes them and neither does this portlet.

---

## State of the code as found

The epic and the design describe the current tab in several places. Some of those descriptions are incomplete or no longer match the code. They are recorded here because they change what the work is. Requirements below are written against the code, not against the epic. Each row was checked in the source on `main`.

| # | Epic or design says | Code says | Effect on scope |
|---|---|---|---|
| 1 | The tab has two display bugs: Timed Out and Number of Data Nodes | There are **five**. Timed Out shows the search cluster *name*; Number of Data Nodes shows the *total* node count; the row labelled "Received/Sent" shows only received bytes; Read and Write are drawn in one cell that reads `true/true` (the read flag arrives with a trailing slash); and the Assets row labelled "Cluster Address" shows the shared *path* | All five are fixed, not copied. See FR-021, FR-027, FR-031, FR-032 |
| 2 | The tab's "Server ID" column shows the server ID | It shows the **license ID** (a masked license serial). The real short server ID only appears in the floating panel's title | The new list shows the real server ID. The license ID is intentionally not displayed — see FR-019 |
| 3 | On installs whose license does not allow cluster data, node data comes back as empty maps | On those installs the cache transport has no cluster support, its cluster check returns nothing, and the status request **fails with a server error**. The tab shows "An unexpected error occurred" | The portlet cannot tell "unlicensed" from "broken" by itself. The backend must say so explicitly (#37875). See FR-045 |
| 4 | "Every node in the cluster is listed" | Only servers that wrote a heartbeat in the last heartbeat timeout (600 seconds by default) are considered. A server silent for longer is not listed, not even as Down | "Every node" means "every node seen in the heartbeat window". See FR-008 and Assumptions |
| 5 | The design's Version tile may need a new backend field | The value the tab shows as "Version" is already the dotCMS release version of each node | No backend work. See FR-016 |
| 6 | Each node has a status (green / red) | The per-node status reflects **only** the shared assets check, not whether the node is reachable. There is no "Lagging" state anywhere | Up / Lagging / Down must come from new backend work. See FR-011 and Dependencies |
| 7 | — | Refresh in the old tab reloads the whole tab and **loses the selection**; it falls back to the node serving the request | The new Refresh keeps the selection. See FR-037 |
| 8 | — | The status request waits for every node to answer: at least 2 seconds, and up to one second per node, **whenever any node does not answer** | Loading and refreshing states must hold for several seconds without looking broken. See FR-039 and FR-042 |
| 9 | — | Each status request makes **every node write a test file** to the shared assets volume to check it is writable | Refresh is not free. The portlet must not issue overlapping or background requests. See FR-039 and FR-053 |
| 10 | — | The tab is hidden when the server heartbeat feature is switched off (`ENABLE_SERVER_HEARTBEAT=false`), but the heartbeat job keeps running, so the data stays valid | The portlet stays available and explains the consequence. See FR-047 |

---

## Parity map

Every feature of the old tab, where it lands in the new portlet, and the requirement that covers it. **Fix** means carried over with the old tab's bug corrected; **Drop** means intentionally not carried over.

| Old tab | New portlet | Class | Requirement |
|---|---|---|---|
| Node table, one row per node, including nodes that did not answer | Node list, one entry per node | Carry | FR-008 |
| "Server ID" column (license ID) | Real server ID on each entry | Fix | FR-009, FR-019 |
| "Version" column | Version in the detail summary | Carry | FR-016 |
| "Site" column (actually the server's friendly name) | Host name on each entry and in the detail header | Fix (label) | FR-009, FR-015 |
| "IP Address" column | IP address in the detail summary | Carry | FR-016 |
| "Contacted" column | "Contacted … ago" on each entry | Carry | FR-010 |
| Status dot (green / red) | Up / Lagging / Down status on each entry | Carry, extended | FR-011 |
| User icon on the node serving the request | "This node" marker | Carry | FR-012 |
| Opens on the node serving the request | That node is listed first and selected on load | Carry | FR-013 |
| Click a row to open its floating panel | Select an entry to show its detail | Carry | FR-014 |
| Panel title `{name} - {server ID}` | Detail header: server ID, status, name | Carry | FR-015 |
| Cache Transport rows and status dot | Cache Transport section | Fix | FR-020 – FR-024 |
| ES Cluster Health rows and status dot | Search Cluster section, Elasticsearch or OpenSearch | Fix | FR-025 – FR-029 |
| Assets rows and status dot | Assets section | Fix | FR-030 – FR-033 |
| "Refresh Status" link (reloads the tab) | Refresh control (reloads data in place) | Carry, improved | FR-036 – FR-041 |
| "No Nodes found." | Empty state with retry | Carry | FR-043 |
| "An unexpected error occurred: …" (English only) | Error state with retry, translated | Fix | FR-044 |
| Loading spinner | Loading placeholders | Carry | FR-042 |
| Hidden when the heartbeat feature is off | Available, with a notice | Changed | FR-047 |
| Orphaned Clustering upsell page (never shown) | Unlicensed state | New | FR-045, FR-046 |
| Decorative server icon column | — | Drop | — |
| "Servers Not in Cache" dialog (never opened) | — | Drop | — |
| License ID as "Server ID" | — | Drop | FR-019 |

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the health of every node at a glance (Priority: P1)

An administrator opens the Network portlet to find out whether every node in the cluster is up. They see one entry per node — including nodes that stopped answering — with its server ID, host name, how long ago it last checked in, and a status of Up, Lagging or Down. The node that served their own request is marked "This node".

**Why this priority**: This is the reason the screen exists. An admin investigating an incident needs to know in one look which nodes are healthy, which are falling behind and which are gone.

**Independent Test**: On a cluster with at least two nodes, open the portlet and compare the list against the nodes known to be running; stop one node and confirm it stays listed with a non-Up status.

**Acceptance Scenarios**:

1. **Given** a cluster of three running nodes, **When** an admin opens the Network portlet, **Then** three entries are listed, each with its server ID, host name, time since last contact and an Up status.
2. **Given** the admin's request was served by node A, **When** the list loads, **Then** node A is marked "This node", is listed first and is selected.
3. **Given** the backend reports node B as Lagging and node C as Down, **When** the list loads, **Then** B and C are listed with those statuses, each distinguishable from Up and from each other by its label as well as its colour.
4. **Given** a node stopped answering but is still within the heartbeat window, **When** the list loads, **Then** it is listed with the status the backend reports, not left out.
5. **Given** a node checked in 4 seconds ago, **When** the list loads, **Then** its entry reads "Contacted 4 seconds ago"; **Given** 6 minutes ago, **Then** "Contacted 6 minutes ago".
6. **Given** a node has never checked in, **When** the list loads, **Then** its entry says no heartbeat was recorded instead of showing a time.
7. **Given** a single-node install, **When** the list loads, **Then** one entry is listed, marked "This node" and selected.
8. **Given** more nodes than fit on screen, **When** the admin scrolls the list, **Then** the list scrolls on its own while the detail pane stays in place.

---

### User Story 2 - Inspect one node's detail (Priority: P1)

The administrator selects a node to see its full detail: server ID, status, host name, cluster, IP address and the dotCMS version it runs, followed by three sections — Cache Transport, Search Cluster and Assets — each with its own health and every value the old tab showed, correctly labelled.

**Why this priority**: The list says *that* something is wrong; the detail says *what*. Without it the portlet is not at parity with the tab it replaces.

**Independent Test**: Select each node in turn and compare every value with the cluster status service's response for that node, and with the old tab for the rows it shows correctly.

**Acceptance Scenarios**:

1. **Given** a node is selected, **When** the detail shows, **Then** the header shows the node's server ID, status and host name, with a "This node" marker when it applies.
2. **Given** a node is selected, **When** the detail shows, **Then** a summary shows its server ID, cluster name, IP address and dotCMS version.
3. **Given** a node is selected, **When** the admin reads Cache Transport, **Then** it shows cluster name, cache transport, number of nodes, channel open, cluster address, received and sent traffic as two numbers, and cache port.
4. **Given** the node's cache transport has no port and no transport address, **When** Cache Transport shows, **Then** Cache Port and Cluster Address read "N/A", not `-1`, `0` or blank.
5. **Given** a node is selected, **When** the admin reads Search Cluster, **Then** it names the search engine in use (Elasticsearch or OpenSearch) and shows all fourteen rows listed in FR-026, each with its own value.
6. **Given** the search health request did not time out and the search cluster has 3 nodes of which 2 hold data, **When** Search Cluster shows, **Then** Timed Out reads "false", Number of Nodes reads 3 and Number of Data Nodes reads 2.
7. **Given** a node is selected, **When** the admin reads Assets, **Then** it shows the shared assets path, and on two separate rows whether the path can be read and whether it can be written.
8. **Given** any section, **When** it shows, **Then** its header carries that section's health as reported by the backend: Healthy, Degraded, Unhealthy or Unknown.
9. **Given** the admin selects another node, **When** the selection changes, **Then** the header, summary and all three sections show the newly selected node.

---

### User Story 3 - Read a node that did not answer (Priority: P1)

When a node does not answer the status request, the administrator still sees who it is and when it was last heard from, and the detail makes it clear that its health values are unavailable rather than broken.

**Why this priority**: A node that stops answering is exactly the case an admin opens this screen for. Showing it as an error, or hiding it, defeats the purpose.

**Independent Test**: Stop one node of a multi-node cluster; within the heartbeat window, select it and confirm its identity shows, its sections show placeholders with Unknown health, and nothing on screen reports an error.

**Acceptance Scenarios**:

1. **Given** node C did not answer, **When** it is selected, **Then** its server ID, host name, IP address, version and last contact are shown.
2. **Given** node C did not answer, **When** it is selected, **Then** a notice explains that the node did not answer and that its health values are unavailable.
3. **Given** node C did not answer, **When** its sections show, **Then** every value shows a "no value" placeholder and every section's health reads Unknown.
4. **Given** node C did not answer, **When** the detail shows, **Then** no error message, error dialog or broken value (such as `undefined`) appears.

---

### User Story 4 - Refresh in place (Priority: P2)

While watching a recovering cluster, the administrator presses Refresh to get current data for every node without reloading the page and without losing the node they were looking at.

**Why this priority**: The old tab reloads the whole tab and drops the selection. Refreshing in place is what makes the screen usable while an incident unfolds, but the screen is still useful without it.

**Independent Test**: Select a node that is not "This node", press Refresh, and confirm values update, the same node stays selected and the page does not reload.

**Acceptance Scenarios**:

1. **Given** node B is selected, **When** the admin presses Refresh, **Then** data for every node is reloaded and node B stays selected.
2. **Given** a refresh is in progress, **When** the admin looks at the screen, **Then** the current data stays visible and the Refresh control shows that it is working.
3. **Given** a refresh is in progress, **When** the admin presses Refresh again, **Then** nothing happens; no second request is made.
4. **Given** node B is gone after the refresh, **When** the refresh completes, **Then** the node that served the request is selected.
5. **Given** a refresh fails, **When** the failure is reported, **Then** the previously loaded data stays on screen and Refresh can be pressed again.
6. **Given** a node changed from Up to Down between refreshes, **When** the refresh completes, **Then** its entry and, if selected, its detail show Down.

---

### User Story 5 - Understand why there is nothing to show (Priority: P3)

When the portlet cannot show cluster data — the first load is still running, no nodes were found, the request failed, or the license does not include clustering — the administrator sees a clear message saying which of these happened, instead of empty cards. When the heartbeat feature is switched off, they see the data with a notice explaining what that means.

**Why this priority**: These are uncommon states, but empty or broken-looking cards on a health screen are misleading during an incident.

**Independent Test**: Exercise each state — slow first load, no nodes, failed request, unlicensed install, heartbeat feature off — and confirm each shows its own message.

**Acceptance Scenarios**:

1. **Given** data is loading for the first time, **When** the portlet opens, **Then** it shows placeholders in the shape of the list and the detail, not empty cards.
2. **Given** no nodes are reported, **When** the portlet loads, **Then** it shows "No nodes found" with a way to try again.
3. **Given** the first load fails, **When** the portlet loads, **Then** it shows an error message with a way to try again, and trying again reloads the data.
4. **Given** the install's license does not include cluster data, **When** the portlet loads, **Then** it shows the Clustering upsell — what the feature does, that it is available in the Enterprise edition, a contact link and a way to request a trial license — and no cluster data.
5. **Given** the server heartbeat feature is switched off, **When** the portlet loads, **Then** cluster data shows as usual and a notice explains that the heartbeat feature is disabled and dead servers are not removed from the cluster automatically.

---

### User Story 6 - Fall back to the old tab (Priority: P3)

If the Beta misbehaves, an administrator can go back to the Configuration portlet's Network tab, which works exactly as before.

**Why this priority**: It is the rollback path for the Beta. It costs nothing to keep, but it has to be verified.

**Independent Test**: With the Beta added to a layout, open the Configuration portlet's Network tab and confirm it behaves as before; remove the Beta from the layout and confirm the tab still works.

**Acceptance Scenarios**:

1. **Given** the Beta is in a layout, **When** the admin opens Configuration → Network, **Then** the tab shows and behaves as it did before this feature.
2. **Given** the Beta is removed from every layout, **When** the admin opens Configuration → Network, **Then** the tab still works, with no code change or redeploy.

### Edge Cases

- **A node that did not answer** is listed with its identity and reported status; its sections show placeholders and Unknown health, never an error (User Story 3).
- **A node that never checked in** has no last-contact time; the entry says so (FR-010).
- **Values that do not apply** to the node's cache transport — no port, no transport address — show "N/A", not `-1`, `0` or blank (FR-024).
- **A value missing from a node that did answer** shows the "no value" placeholder, not "N/A"; "N/A" is reserved for values that cannot exist for that transport (FR-024, FR-052).
- **The node serving the request is not in the list** (it should always be; the backend builds it): the first listed node is selected instead (FR-013).
- **A long host name or shared path** is shortened or wrapped without breaking the layout, and the full value stays readable (FR-050).
- **A slow status request** — several seconds when a node does not answer — keeps the loading or refreshing indicator visible for the whole wait (FR-039, FR-042).
- **A refresh that fails** after data was shown reports the failure and keeps the last good data (FR-041).
- **The selected node disappears** after a refresh; the selection falls back to the node serving the request (FR-037).
- **Search health is degraded but the node is up** (for example a single-node search cluster with replica shards unassigned): the node stays Up and only the Search Cluster section shows Degraded (FR-018).
- **An install where OpenSearch serves reads**: the Search Cluster section names OpenSearch and shows OpenSearch's values (FR-025).
- **The license does not include cluster data**: the upsell shows; no request failure or empty list is shown instead (FR-045).
- **The heartbeat feature is off**: the data shows, plus the notice (FR-047).
- **The admin lacks access** to the Network, Configuration or administrator role: the data is refused by the backend and the portlet shows its error state (FR-003).

## Requirements *(mandatory)*

### Availability and access

- **FR-001**: The system MUST offer a portlet named "Network (Beta)" that an administrator can add to a navigation section through the usual Add Portlet flow. It MUST be served at `/#/c/network-beta`.
- **FR-002**: The portlet MUST NOT be added to any default layout and MUST NOT be added to existing layouts on upgrade; admins opt in.
- **FR-003**: Cluster data MUST be available only to backend users who have the Network portlet, the Network (Beta) portlet or the Configuration portlet in a layout, and to CMS Administrators. Anyone else MUST be refused.
- **FR-004**: The Configuration portlet's Network tab, and the cluster status service it uses, MUST keep working unchanged.
- **FR-005**: The portlet MUST NOT offer any action that changes the cluster (such as removing a server); it is read-only.

### Node list

- **FR-006**: The portlet MUST show the list of nodes and the selected node's detail side by side. The list MUST scroll independently of the detail.
- **FR-007**: The list MUST show how many nodes it contains.
- **FR-008**: The list MUST contain every node the backend reports, including nodes that did not answer the status request. It MUST NOT add, hide or merge nodes.
- **FR-009**: Each entry MUST show the node's short server ID and its host name (the server's friendly name).
- **FR-010**: Each entry MUST show the time since the node's last heartbeat as "Contacted … ago", in whole seconds under a minute, whole minutes under an hour, whole hours under a day, and whole days beyond that, using singular wording for 1. A node with no recorded heartbeat MUST say so instead.
- **FR-011**: Each entry MUST show the node's status — Up, Lagging or Down — exactly as the backend reports it. The portlet MUST NOT derive the status from heartbeat age, section health or any other value.
- **FR-012**: The entry for the node that served the request MUST carry a "This node" marker. No other entry may carry it.
- **FR-013**: The node that served the request MUST be listed first and selected when data first loads. The remaining nodes MUST keep the order the backend sent. If that node is not in the list, the first listed node MUST be selected.
- **FR-014**: The admin MUST be able to select any entry, with the pointer or the keyboard, to show its detail. Exactly one entry MUST be selected whenever nodes are shown; selecting the already-selected entry MUST NOT clear the selection.

### Node detail — header and summary

- **FR-015**: The detail header MUST show the selected node's short server ID, its status, a "This node" marker when it applies, and its host name.
- **FR-016**: Below the header, a summary MUST show the node's short server ID, cluster name (the cache transport's cluster name), IP address and the dotCMS release version it runs.
- **FR-017**: The Refresh control MUST be in the detail header.
- **FR-018**: A section's health MUST NOT change the node's status. A node can be Up while one of its sections is Degraded or Unhealthy.
- **FR-019**: The license ID MUST NOT be displayed. The old tab showed it under a "Server ID" heading; the real server ID replaces it. This is a signed-off drop.

### Cache Transport section

- **FR-020**: A Cache Transport section MUST show, in this order: Cluster Name, Cache Transport, Number of Nodes, Channel Open, Cluster Address, Received/Sent, Cache Port.
- **FR-021**: Received/Sent MUST show both the bytes received and the bytes sent, as two whole numbers separated by a slash. The old tab showed only the received value.
- **FR-022**: Channel Open MUST show the reported true / false value.
- **FR-023**: The section header MUST show the cache cluster name beside the title when it is known.
- **FR-024**: Cluster Address and Cache Port MUST show "N/A" when the node answered and its transport has no address or port. They MUST show the "no value" placeholder when the node did not answer.

### Search Cluster section

- **FR-025**: A Search Cluster section MUST name the search engine reporting the values — Elasticsearch or OpenSearch — beside its title. When the backend does not report the engine, the name MUST be omitted rather than guessed.
- **FR-026**: The section MUST show, in this order: Cluster Name, Timed Out, Number of Nodes, Number of Data Nodes, Active primary shards, Active shards, Relocating shards, Initializing shards, Unassigned shards, Delayed unassigned shards, Number of pending tasks, Number of unfinished fetches, Maximum task queue wait, Active shards percent.
- **FR-027**: Every row MUST show its own value. Timed Out MUST show whether the search health request timed out; Number of Data Nodes MUST show the data-node count. The old tab showed the cluster name and the total node count in those two rows.
- **FR-028**: Maximum task queue wait MUST show a duration with its unit.
- **FR-029**: Active shards percent MUST show a percentage with at most one decimal place.

### Assets section

- **FR-030**: An Assets section MUST show, in this order: Shared path, Read, Write. Its header MUST say it describes the shared volume.
- **FR-031**: Shared path MUST show the shared assets path, labelled as a path. The old tab labelled it "Cluster Address", and the design shows an IP address there.
- **FR-032**: Read and Write MUST be two rows, each showing its own true / false value. The old tab drew both in one cell.
- **FR-033**: The section's health MUST reflect the backend's assets check (read, write and test-file creation together).

### Section health and nodes that did not answer

- **FR-034**: Each section MUST show its health in its header, as one of: Healthy, Degraded, Unhealthy, Unknown — exactly as the backend reports it. Health MUST be conveyed by text as well as colour.
- **FR-035**: For a node that did not answer, the detail MUST show the node's identity (FR-015, FR-016), a notice that the node did not answer and its health values are unavailable, the "no value" placeholder for every section value, and Unknown health for every section. It MUST NOT show an error.

### Refresh

- **FR-036**: Refresh MUST reload the data of every node without reloading the page or leaving the portlet.
- **FR-037**: After a refresh, the node that was selected MUST stay selected if it is still reported. Otherwise the node that served the request MUST be selected (FR-013).
- **FR-038**: The node list order and "This node" marker MUST be recomputed from each refresh's data.
- **FR-039**: Only one status request MUST be in flight at a time. While a request is in flight, Refresh MUST show progress and MUST NOT start another.
- **FR-040**: While a refresh is in flight, the data already on screen MUST stay visible and usable; selecting another node MUST still work.
- **FR-041**: A failed refresh MUST be reported to the admin through the admin UI's standard error notification, MUST leave the data already on screen, and MUST leave Refresh usable for another attempt.

### States

- **FR-042**: During the first load the portlet MUST show placeholders in the shape of the node list and the detail. The placeholders MUST stay until the load finishes, however long it takes.
- **FR-043**: When the backend reports no nodes, the portlet MUST show "No nodes found", a short explanation, and a way to try again.
- **FR-044**: When the first load fails, the portlet MUST show an error message, a short explanation and a way to try again. The message MUST be translated.
- **FR-045**: When the backend reports that the license does not include cluster data, the portlet MUST show the unlicensed state and no cluster data. The portlet MUST rely on the backend's report, not infer it from a failure or an empty list.
- **FR-046**: The unlicensed state MUST show the Clustering upsell: what the feature does, that it is available in the Enterprise edition, a link to contact dotCMS and a way to request a trial license. It reuses the copy of the old tab's unused unlicensed page.
- **FR-047**: When the backend reports that the server heartbeat feature is switched off, the portlet MUST stay available, show cluster data as usual, and show a notice that the heartbeat feature is disabled and dead servers are not removed from the cluster automatically.
- **FR-048**: Exactly one of these states MUST show at a time: first-load placeholders, unlicensed, error, empty, or the node list with detail.

### Cross-cutting

- **FR-049**: Every user-facing text MUST be translatable, including status and health labels, relative times, "N/A" and units.
- **FR-050**: Long values (host names, paths, cluster names) MUST be shortened or wrapped without breaking the layout, and the full value MUST stay readable.
- **FR-051**: The node list MUST be usable with the keyboard alone and announced to screen readers as a single-selection list labelled "Nodes". The Refresh control MUST be reachable and operable by keyboard.
- **FR-052**: The portlet MUST NOT show raw placeholder values such as `undefined`, `null`, `-1` or empty strings anywhere.
- **FR-053**: The portlet MUST make one status request per load or refresh, and none in the background.

### Out of Scope

- **Deciding node status or section health.** Up / Lagging / Down thresholds and health rules are backend behaviour (Dependencies).
- **Promotion to primary**, default layouts, navigation placement under Settings, and retiring the old tab (#37805).
- **Cluster-changing actions**: removing a server, editing cache or search settings.
- **Search configuration properties, license repository totals and the cache ping** — offered by the old tab's backend, never shown by the tab.
- **Automatic refresh** on a timer.
- **History or trends** of node status over time.
- **Uptime** of each node: reported by the backend today but not shown by the old tab or the design.

### Key Entities

- **Cluster status**: one snapshot for the whole cluster — whether the license allows cluster data, whether the heartbeat feature is on, which node served the request, the overall cluster health, and the list of nodes.
- **Node**: one dotCMS server seen in the last heartbeat window — server ID (full and short), friendly name, host name, IP address, dotCMS version, seconds since last heartbeat, whether it answered the status request, and its status (Up, Lagging, Down).
- **Cache transport info** (per node): health, cluster name, transport type and provider, number of nodes, channel open, transport address, port, received and sent bytes and messages.
- **Search cluster info** (per node): health, engine (Elasticsearch or OpenSearch), cluster name, timed out, node and data-node counts, shard counts (active primary, active, relocating, initializing, unassigned, delayed unassigned), pending tasks, unfinished fetches, maximum task queue wait, active shards percent.
- **Assets info** (per node): health, shared path, can read, can write.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the rows in the [Parity map](#parity-map) are either available in the new portlet or marked Drop with sign-off.
- **SC-002**: For every node and every row, the value shown matches the cluster status service's response for that node, including the five rows the old tab got wrong.
- **SC-003**: An admin can identify every node that is not Up from the node list alone, without opening any detail.
- **SC-004**: In 100% of refreshes where the selected node is still reported, it stays selected.
- **SC-005**: Pressing Refresh repeatedly during one refresh produces exactly one status request.
- **SC-006**: No screen state shows empty cards or a raw placeholder value (`undefined`, `null`, `-1`, blank) — verified for each of: first load, empty, error, unlicensed, heartbeat off, a node that did not answer, and a normal node.
- **SC-007**: A node that did not answer is listed and selectable in 100% of loads where the backend reports it, and selecting it never shows an error.
- **SC-008**: Every state and every label in the portlet appears in the admin's selected language when a translation exists.
- **SC-009**: An admin can move through the node list and select a node using only the keyboard.
- **SC-010**: Removing the Beta from every layout leaves the Configuration portlet's Network tab working, with no code change or redeploy.
- **SC-011**: On an install where OpenSearch serves reads, the Search Cluster section names OpenSearch and its values match OpenSearch's cluster health.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Cluster status in the admin UI, available today only in the Network tab of the legacy Configuration portlet (Dojo). That tab, the cluster status service behind it, and the clustering subsystem (heartbeats, cache transport, shared assets volume) are part of the older product surface.
- **Backward-compatibility expectations**: The Configuration portlet's Network tab and its cluster status service keep working unchanged; the Beta is purely additive. No layout is modified and nothing is deprecated by this feature.
- **Known related decisions**: The spike (#37802) is the reference for current behaviour, the old tab's display bugs and the proposed data shape. The OpenSearch migration matters: on installs where OpenSearch serves reads, the Search Cluster section must describe OpenSearch (#37876). The plan will formally consult `dotCMS/platform-adrs`.

## Dependencies

The data this portlet needs is not fully served by the backend today. The spike (#37802 §2.8) proposed a typed cluster status service (`GET /api/v1/cluster/nodes`) that returns every node in one consistent shape with its status computed on the server. Until it exists, the portlet can be built and reviewed against that proposed shape with stand-in data, but it cannot reach parity on a live cluster.

| Needed by this feature | Requirement | Covered by |
|---|---|---|
| Network and Network (Beta) users allowed to load cluster data | FR-003 | #37875 |
| An explicit "unlicensed" answer instead of a server error | FR-045 | #37875 |
| Search health that works when OpenSearch serves reads, and the engine name | FR-025, SC-011 | #37876 |
| The node serving the request never reported as not answering because of its own slow search check; concurrent refreshes not wiping each other's results | FR-013, FR-035 | #37877 |
| Whether the heartbeat feature is switched off | FR-047 | Requested on #37875; may move to the typed service below |
| Server-computed Up / Lagging / Down status | FR-011 | **Not yet filed** — typed cluster status service |
| Whether each node answered the status request | FR-035 | **Not yet filed** — typed cluster status service |
| One consistent shape for nodes that answered and nodes that did not | FR-008, FR-052 | **Not yet filed** — typed cluster status service |
| Per-node section health (cache health is cluster-wide today) | FR-034 | **Not yet filed** — typed cluster status service |
| The real cache transport address (overwritten by the node's IP today) | FR-024 | **Not yet filed** — typed cluster status service |

## Assumptions

- **Status rules belong to the backend.** What makes a node Lagging rather than Up or Down is decided and computed by the backend. The spike proposes: Up when the node answered and its heartbeat is at most three heartbeat intervals old; Lagging when it answered with an older heartbeat, or did not answer but its heartbeat is still fresh; Down otherwise. Agreeing those thresholds is part of the backend work.
- **"Every node" means every node seen in the heartbeat window** (600 seconds by default). A node silent for longer is no longer reported by the backend and therefore not listed.
- **Host name** is the server's friendly name, as in the old tab. In containerised installs it usually equals the pod's host name.
- **Cache "Cluster Address"** stays as a row for parity and shows "N/A" for transports without an address (PubSub, the default).
- **Section health placement**: the design does not show section health; it goes in each section's header.
- **Navigation placement**: the design places Network under Settings. That applies once the Beta is promoted (#37805); the Beta lives wherever an admin adds it.
- **True / false values** (Channel Open, Timed Out, Read, Write) are shown as the reported true / false, as in the old tab.

### Where the design prototype and this spec disagree

The design ("Network Portlet", variant 1d, 1-node and 3-node toggles) is the visual reference. Where it conflicts with parity or with the code, this spec wins, and the design should be updated.

| Design shows | This spec requires | Why |
|---|---|---|
| Timed Out = the search cluster name | Whether the request timed out (FR-027) | The design copies the old tab's bug |
| Received/Sent = one number | Both numbers (FR-021) | The design copies the old tab's bug |
| Cache Cluster Address = the node's IP, same as the IP tile | The transport address, or "N/A" (FR-024) | The backend overwrites the address with the IP today |
| Assets "Cluster Address" = an IP | "Shared path" = the shared assets path (FR-031) | The value is a filesystem path |
| Cache Port = `-1` | "N/A" (FR-024) | `-1` means "no port" |
| No "This node" marker | Marker on the node serving the request (FR-012) | Epic acceptance criterion |
| Only Up and Lagging nodes | Down nodes too, with the not-answering notice (FR-035) | Epic acceptance criterion |
| No section health | Health in each section header (FR-034) | Epic acceptance criterion |
| "Search Cluster" with no engine | Engine named beside the title (FR-025) | Elasticsearch and OpenSearch both possible |
| No loading, empty, error, unlicensed or heartbeat-off states; no Refresh in progress | All of them (FR-039, FR-042 – FR-047) | Epic acceptance criteria and the product decisions on the unlicensed and heartbeat-off states |
