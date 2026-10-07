# Feature Specification: Network (Beta) Portlet

**Feature Branch**: `issue-37803-network-portlet-spec`

**Created**: 2026-10-07

**Status**: Draft

**Type**: New Feature

**Input**: User description: "Add a standalone Network (Beta) portlet that replaces the Network tab of the Configuration portlet with full feature parity: every cluster node with its identity, last contact and an Up / Lagging / Down status; the selected node's detail with its Cache Transport, Search Cluster and Assets health; Refresh in place; loading, empty, error and unlicensed states. Node status and section health are whatever the backend reports. Details: dotCMS/core#37801 (epic), #37802 (spike), #37803 (task)."

## Context

Today an administrator checks the health of a dotCMS cluster in the **Network** tab of the Configuration portlet (`/#/c/configuration`). That tab is built on legacy Dojo widgets and shows a table of nodes; clicking a row opens a floating panel with the node's cache transport, search cluster and shared assets details. The spike for this work (#37802) found that the tab also displays wrong values in several rows (Timed Out shows the cluster name, Number of Data Nodes shows the node count, Sent bytes are never shown).

This feature adds a standalone **Network (Beta)** portlet with a node list and a detail pane, following the new design. **Feature parity with the current tab is the primary requirement**: every value, status and action the tab exposes must be available in the new portlet, with the tab's display bugs fixed rather than copied. The old tab stays in place during the Beta so admins can fall back to it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See the health of every node at a glance (Priority: P1)

An administrator opens the Network portlet to find out whether every node in the cluster is up. They see one entry per node — including nodes that stopped answering — with the node's server ID, host name, how long ago it last checked in, and a status of Up, Lagging or Down. The node that served their own request is marked "This node".

**Why this priority**: This is the reason the screen exists. An admin investigating an incident needs to know in one look which nodes are healthy, which are falling behind and which are gone.

**Independent Test**: On a cluster with at least two nodes, open the portlet and compare the list against the nodes known to be running; stop one node and confirm it is still listed with a non-Up status.

**Acceptance Scenarios**:

1. **Given** a cluster of three running nodes, **When** an admin opens the Network portlet, **Then** three nodes are listed, each with its server ID, host name, time since last contact and an Up status.
2. **Given** the admin's request was served by node A, **When** the list loads, **Then** node A is marked "This node", is listed first and is selected.
3. **Given** a node stopped answering but checked in recently, **When** the list loads, **Then** that node is listed with a Lagging or Down status as reported by the backend, not left out.
4. **Given** the backend reports a node as Lagging, **When** the list loads, **Then** that node shows a Lagging status, visually distinct from Up and Down.

---

### User Story 2 - Inspect one node's detail (Priority: P1)

The administrator selects a node to see its full detail: server ID, status, host name, cluster name, IP address and the dotCMS version it runs, followed by three sections — Cache Transport, Search Cluster and Assets — each with its own health indicator and every value the old tab showed.

**Why this priority**: The list says *that* something is wrong; the detail says *what*. Without it the portlet is not at parity with the tab it replaces.

**Independent Test**: Select each node in turn and compare every value against the Configuration portlet's Network tab for the same node.

**Acceptance Scenarios**:

1. **Given** a node is selected, **When** the detail pane shows, **Then** it lists the node's server ID, status, host name, cluster, IP address and version.
2. **Given** a node is selected, **When** the admin reads the Cache Transport section, **Then** it shows cluster name, cache transport, number of nodes, channel open, cluster address, received and sent traffic, and cache port.
3. **Given** a node is selected, **When** the admin reads the Search Cluster section, **Then** it shows which search engine is in use (Elasticsearch or OpenSearch) and the cluster name, timed out, number of nodes, number of data nodes, active primary shards, active shards, relocating, initializing, unassigned and delayed unassigned shards, pending tasks, unfinished fetches, maximum task queue wait and active shards percent — each row showing its own value.
4. **Given** a node is selected, **When** the admin reads the Assets section, **Then** it shows the shared assets path and, separately, whether it can be read and whether it can be written.
5. **Given** any section, **When** it is displayed, **Then** it shows that section's health as reported by the backend (healthy, degraded, unhealthy, or unknown).
6. **Given** the admin selects a different node, **When** the selection changes, **Then** the header, summary and all three sections update to that node.

---

### User Story 3 - Refresh in place (Priority: P2)

While watching a recovering cluster, the administrator presses Refresh to get current data for every node without reloading the page and without losing the node they were looking at.

**Why this priority**: The old tab reloads the whole tab and drops the selection. Refreshing in place is what makes the screen usable while an incident unfolds, but the screen is still useful without it.

**Independent Test**: Select a node that is not "This node", press Refresh, and confirm values update, the same node stays selected and the page does not reload.

**Acceptance Scenarios**:

1. **Given** node B is selected, **When** the admin presses Refresh, **Then** data for every node is reloaded and node B stays selected.
2. **Given** a refresh is in progress, **When** the admin looks at the screen, **Then** the current data stays visible and the Refresh control shows it is working and cannot be pressed again until it finishes.
3. **Given** node B disappears from the cluster, **When** the refresh completes, **Then** the selection moves to the node that served the request.
4. **Given** a refresh fails, **When** the error is reported, **Then** the previously loaded data stays on screen.

---

### User Story 4 - Understand why there is nothing to show (Priority: P3)

When the portlet cannot show cluster data — no nodes found, the request failed, or the license does not include clustering — the administrator sees a clear message saying which of these happened, instead of empty cards.

**Why this priority**: These are uncommon states, but empty or broken-looking cards on a health screen are misleading during an incident.

**Independent Test**: Exercise each state (no nodes, a failed request, an install without the required license) and confirm a distinct message appears for each.

**Acceptance Scenarios**:

1. **Given** no nodes are reported, **When** the portlet loads, **Then** it shows a "No nodes found" message with a way to try again.
2. **Given** the first load fails, **When** the portlet loads, **Then** it shows an error message with a way to try again.
3. **Given** the install's license does not include cluster data, **When** the portlet loads, **Then** it shows the Clustering upsell — what the feature does, that it is an Enterprise feature, a contact link and a way to request a trial license — instead of any cluster data.
4. **Given** data is loading for the first time, **When** the portlet opens, **Then** it shows a loading placeholder rather than empty cards.

### Edge Cases

- **A node that did not answer**: it is still listed with its identity (server ID, host name, IP address, version, last contact) and its reported status; its sections show "no value" placeholders and an unknown health, never an error.
- **A node that never checked in** (no last-contact time): the list says so instead of showing a time.
- **Values that do not apply** to the node's cache transport (no port, no transport address): shown as "N/A", not as `-1`, `0` or blank.
- **A single-node install**: one entry, marked "This node", selected on load; every section is filled in.
- **A long host name or shared path**: truncated or wrapped without breaking the layout; the full value is still readable.
- **A slow status request** (the backend waits for nodes to answer, which can take several seconds when a node is down): the loading and refreshing indicators stay visible for the whole wait.
- **A refresh that fails** after data was already shown: the error is reported and the last good data stays visible.
- **The node selected before a refresh is gone afterwards**: the selection falls back to the node that served the request.
- **The server heartbeat feature is switched off** (`ENABLE_SERVER_HEARTBEAT=false`, which hides the old tab today): the portlet stays available and shows data as usual, plus a notice that the heartbeat feature is disabled, so dead servers are no longer cleaned up automatically.

## Requirements *(mandatory)*

### Functional Requirements

**Availability and access**

- **FR-001**: The system MUST offer a portlet named "Network (Beta)" that an administrator can add to a navigation section through the usual Add Portlet flow, served at `/#/c/network-beta`.
- **FR-002**: The portlet MUST NOT be added to any default layout or added automatically on upgrade; admins opt in.
- **FR-003**: Only backend users with access to the Network portlet (or the Configuration portlet, or CMS Administrators) MUST be able to load cluster data; anyone else MUST be refused.
- **FR-004**: The Configuration portlet's existing Network tab MUST keep working unchanged.

**Node list**

- **FR-005**: The portlet MUST list every node the backend reports, including nodes that did not answer the status request.
- **FR-006**: Each listed node MUST show its server ID, host name, time since its last contact (or that it never checked in), and its status: Up, Lagging or Down.
- **FR-007**: The status shown MUST be the status the backend reports for that node; the portlet MUST NOT derive it from other values.
- **FR-008**: The node that served the request MUST be marked "This node", listed first, and selected when the portlet loads.
- **FR-009**: The admin MUST be able to select any listed node to see its detail; exactly one node is selected at any time while nodes are shown.

**Node detail**

- **FR-010**: The detail MUST show the selected node's server ID, status, "This node" marker when it applies, host name, cluster name, IP address and dotCMS version.
- **FR-011**: The detail MUST show a Cache Transport section with: cluster name, cache transport, number of nodes, channel open, cluster address, received and sent traffic (both values), and cache port.
- **FR-012**: The detail MUST show a Search Cluster section that names the search engine in use (Elasticsearch or OpenSearch) and shows: cluster name, timed out, number of nodes, number of data nodes, active primary shards, active shards, relocating shards, initializing shards, unassigned shards, delayed unassigned shards, number of pending tasks, number of unfinished fetches, maximum task queue wait and active shards percent.
- **FR-013**: Every row MUST show its own value. In particular, Timed Out MUST show whether the search health request timed out (not the cluster name), and Number of Data Nodes MUST show the data-node count (not the total node count).
- **FR-014**: The detail MUST show an Assets section with the shared assets path and, as two separate rows, whether the path can be read and whether it can be written.
- **FR-015**: Each of the three sections MUST show its own health as reported by the backend: healthy, degraded, unhealthy, or unknown.
- **FR-016**: For a node that did not answer, every section value MUST show a "no value" placeholder and its health MUST show unknown; the node's identity MUST still show.
- **FR-017**: A value that does not apply to the node's cache transport (no port, no transport address) MUST be shown as "N/A".
- **FR-018**: The license ID, which the old tab showed under a "Server ID" heading, is intentionally not displayed; the node's actual server ID is shown instead. This is a signed-off drop for SC-001.

**Refresh and states**

- **FR-019**: The admin MUST be able to reload data for every node from the detail pane without a page reload.
- **FR-020**: Refresh MUST keep the current selection when that node is still reported, and otherwise select the node that served the request.
- **FR-021**: While a refresh is in progress the current data MUST stay visible and the Refresh control MUST show progress and not accept another press.
- **FR-022**: The portlet MUST show distinct states for: first load in progress, no nodes found, the request failed (with a way to retry), and the license does not include cluster data.
- **FR-023**: The unlicensed state MUST show the Clustering upsell: what the feature does, that it is available in the Enterprise edition, a contact link and a way to request a trial license.
- **FR-024**: When the server heartbeat feature is switched off, the portlet MUST stay available, show cluster data as usual, and show a notice that the heartbeat feature is disabled and dead servers are not cleaned up automatically.
- **FR-025**: A failed refresh MUST be reported to the admin and MUST leave the previously loaded data on screen.
- **FR-026**: Every user-facing text MUST be translatable.

### Key Entities

- **Cluster status**: one snapshot for the whole cluster — whether the license allows cluster data, which node served the request, the overall cluster health, and the list of nodes.
- **Node**: one dotCMS server seen in the last heartbeat window — server ID (full and short), friendly name, host name, IP address, dotCMS version, seconds since last contact, whether it answered the status request, and its status (Up, Lagging, Down).
- **Cache transport info** (per node): health, cluster name, transport type and provider, number of nodes, channel open, transport address, port, received and sent bytes and messages.
- **Search cluster info** (per node): health, engine (Elasticsearch or OpenSearch), cluster name, timed out, node and data-node counts, shard counts (active primary, active, relocating, initializing, unassigned, delayed unassigned), pending tasks, unfinished fetches, maximum task queue wait, active shards percent.
- **Assets info** (per node): health, shared path, can read, can write.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the values, statuses and actions available in the Configuration portlet's Network tab are available in the Network portlet, or are explicitly signed off as dropped.
- **SC-002**: For every node, every value shown matches what the backend reports for that node — including the rows the old tab got wrong (Timed Out, Number of Data Nodes, Sent traffic).
- **SC-003**: An admin can tell which nodes are Up, Lagging or Down from the node list alone, without opening any detail.
- **SC-004**: After pressing Refresh, the selected node stays selected in 100% of refreshes where that node is still reported.
- **SC-005**: Every non-data state (loading, no nodes, failed request, unlicensed) shows a distinct message; no state shows empty cards or raw placeholder values such as `undefined` or `-1`.
- **SC-006**: Removing the Beta portlet from a layout leaves the Configuration portlet's Network tab working, with no code change or redeploy.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: Cluster status in the admin UI, today only available in the Network tab of the legacy Configuration portlet (Dojo). That tab, its cluster status service and the clustering subsystem behind it are part of the older product surface.
- **Backward-compatibility expectations**: The Configuration portlet's Network tab and the cluster status service it uses must keep working unchanged; the Beta is purely additive. Layouts are not modified; nothing is deprecated in this feature. Promoting the Beta to the primary Network portlet and retiring the tab are separate work (#37805).
- **Known related decisions**: The spike (#37802) is the reference for current behavior, the tab's display bugs and the proposed data shape. The OpenSearch migration phases matter here: on installs where OpenSearch serves reads, the search section must describe OpenSearch, not Elasticsearch (#37876). The plan will formally consult `dotCMS/platform-adrs`.

## Dependencies

The data this portlet needs is not fully served by the backend today. The spike (#37802 §2.8) proposed a typed cluster status service (`GET /api/v1/cluster/nodes`) that returns every node in one consistent shape with its status computed on the server. Until it exists, the portlet can be built and reviewed against that proposed shape with stand-in data, but it cannot reach parity on a live cluster.

| Needed by this feature | Covered by |
|---|---|
| Network portlet users allowed to load cluster data; a clear refusal on unlicensed installs | #37875 |
| Search health that works on OpenSearch-only installs, and the engine name | #37876 |
| "This node" never reported down because of its own slow search check; concurrent refreshes not wiping each other's results | #37877 |
| Whether the server heartbeat feature is switched off (for the FR-024 notice) | **Not yet filed** — part of the typed cluster status service below |
| Server-computed Up / Lagging / Down status; whether each node answered; one consistent shape for answering and non-answering nodes; per-node section health; the real cache transport address | **Not yet filed** — the typed cluster status service proposed by the spike |

## Assumptions

- **Status rules belong to the backend.** What makes a node Lagging rather than Up or Down (the spike proposes thresholds based on whether the node answered and how fresh its heartbeat is) is decided and computed by the backend. This feature only displays it. Agreeing those thresholds is part of the backend work.
- **"Every node" means every node seen in the last heartbeat window.** A node that has not checked in within that window is no longer reported by the backend and therefore not listed.
- **Host name** shows the server's friendly name, as the old tab does; the separate host value is available if product prefers it.
- **Cache "Cluster Address"** stays as a row (parity with the old tab) and shows "N/A" for transports that have no address.
- **Dropped from the old tab**: the never-opened "Servers Not in Cache" dialog and the decorative server icon column. The remove-server, search-config, license-repo and test endpoints the tab does not use are out of scope. The license ID is no longer displayed (FR-018). The Clustering upsell copy from the old tab's orphaned unlicensed page is reused for the unlicensed state (FR-023).
- **Navigation placement**: the design places Network under Settings. That placement applies once the Beta is promoted (#37805); the Beta is reached through whatever section an admin adds it to.
- **Section health placement**: the design does not show section health; it is shown in each section's header.
