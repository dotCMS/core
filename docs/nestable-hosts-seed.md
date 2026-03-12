
  goal: >
    Implement nestable hosts in dotCMS, allowing hosts to have parent hosts or folders,
    with URL resolution via path segments, permission inheritance through the host hierarchy,
    and full admin UI integration.

  constraints:
    - Java 11 syntax compatibility in core modules (Java 21 runtime)
    - No new columns in the Identifier table — reuse existing host_inode, parent_path, asset_name
    - Identifier.asset_name must equal hostname for all host records
    - Hosts identified by asset_type='contentlet' AND asset_subtype='Host'
    - Hostname/segment labels must be globally unique (existing constraint)
    - Content paths are host-relative — reparenting does not require content reindex
    - No license gating — available on all license levels including Community
    - No automatic URL redirects on reparent — operator's responsibility
    - Cache invalidation via PostgreSQL LISTEN/NOTIFY (no Hazelcast)
    - Caches built lazily on each node after invalidation
    - Full workflow applies to reparent operations (no special-casing)
    - Angular 19+ modern syntax (@if, input(), signals)

  acceptance_criteria:
    - A host can be created with a parent host or folder via HostFolderField on the Host content type
    - URL https://dotcms.com/en/nestedHost1/page resolves to host=nestedHost1, URI=/page
    - Multi-level nesting works: dotcms.com → nestedHost1 → nestedHost2
    - Host.getParentPermissionable() returns parent host (not System Host) for nested hosts
    - Host.getAbsoluteBaseUrl() returns full path-based URL (e.g., https://dotcms.com/en/nestedHost1)
    - NestedHostPatternCache per-top-level-host with longest-first regex matching
    - Cache-first resolution in HostResolver — pattern check before normal resolution
    - CMS_FILTER_URI_OVERRIDE overwritten with remaining URI after host prefix stripped
    - CMS_RESOLVED_HOST request attribute set for downstream consumers
    - HostWebAPIImpl.getCurrentHost() checks CMS_RESOLVED_HOST first
    - REST Page API internally invokes HostResolver for nested host resolution
    - Deleting a host with descendants is blocked with error reporting descendant count
    - Archiving a host cascades to all descendants; unarchive is manual
    - Push publish auto-includes ancestor hosts; import sorts HOST+FOLDER by path length ascending
    - Site selector shows flat list with indentation derived from parentPath
    - BrowserResource/ContentDriveResource support showSubHosts parameter
    - Nested host nodes appear in folder tree with host icon; click switches local context
    - Content editor accepts ?hostId= query param for HostFolderField pre-fill
    - Startup task adds HostFolderField to Host content type and syncs Identifier.asset_name=hostname
    - Cycle detection in ancestor chain traversal throws DotRuntimeException
    - Vanity URLs and search are fully isolated per-host (no inheritance)
    - Reparent fires HostReparentPayload with oldTopLevelHostId and newTopLevelHostId
    - renameFolderChildren SP cascades parent_path updates on reparent
    - Sitemap, preview, canonical URLs all use getAbsoluteBaseUrl()

  ontology_schema:
    name: NestableHostHierarchy
    description: Domain model for hierarchical host nesting in dotCMS
    fields:
      - name: hostId
        type: string
        description: UUID of the host (from Identifier.id)
      - name: hostname
        type: string
        description: Segment label / hostname (globally unique, stored in Identifier.asset_name)
      - name: parentHostId
        type: string
        description: UUID of parent host (from Identifier.hostId); SYSTEM_HOST_ID for top-level
      - name: parentPath
        type: string
        description: Folder path within parent host (e.g., /en/); / for root placement
      - name: depth
        type: number
        description: Nesting depth derived from parentPath segment count
      - name: absoluteBaseUrl
        type: string
        description: Computed full URL prefix (e.g., https://dotcms.com/en/nestedHost1)
      - name: topLevelHostId
        type: string
        description: UUID of the root domain host in the ancestor chain
      - name: nestedHostPatterns
        type: array
        description: Per-top-level-host ordered regex patterns for all descendant hosts
      - name: hostResolutionResult
        type: object
        description: Tuple of (resolvedHost, remainingUri) from HostResolver

  evaluation_principles:
    - name: backward_compatibility
      description: Existing hosts with hostId=SYSTEM_HOST_ID continue to work unchanged
      weight: 1.0
    - name: url_correctness
      description: All URL construction produces valid path-based URLs for nested hosts
      weight: 0.9
    - name: permission_integrity
      description: Permission inheritance follows the host hierarchy correctly
      weight: 0.9
    - name: cache_consistency
      description: NestedHostPatternCache invalidates and rebuilds correctly on all host changes
      weight: 0.8
    - name: push_publish_reliability
      description: Bundles include ancestors; import ordering prevents referential integrity violations
      weight: 0.8
    - name: ui_discoverability
      description: Nested hosts are visible and navigable in site selector and folder tree
      weight: 0.7

  exit_conditions:
    - name: all_acceptance_criteria_pass
      description: Every acceptance criterion is implemented and verified
      criteria: All 25 acceptance criteria pass integration tests
    - name: no_regression
      description: Existing host operations work unchanged for non-nested hosts
      criteria: Existing host integration tests pass without modification
    - name: cycle_safety
      description: Circular host references are detected and rejected
      criteria: Cycle detection throws DotRuntimeException in all ancestor traversal paths
