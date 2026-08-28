import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, firstValueFrom, forkJoin, of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotRoleDeletionResult,
    DotRolesService,
    DotRoleUserGrantResult,
    DotRoleUsersRemovalResult
} from '@dotcms/data-access';

import {
    appendChildToParent,
    collectAncestorChain,
    findRoleInTree,
    mergeTreesPreferParent,
    patchNodeChildren,
    patchNodeInPlace,
    patchNodeUserCount,
    removeNodeFromTree
} from './dot-roles.tree-utils';

import {
    DotRoleDetail,
    DotRoleFormValue,
    DotRoleMember,
    DotRoleNode,
    DotRoleTab,
    DotRolesStatus,
    DotRoleToolGroupRow
} from '../../models/dot-roles.models';

export interface DotRolesState {
    /**
     * Nested role tree as returned by `GET /v1/roles?loadChildrenRoles=true`
     * — root roles with their direct children populated. Grandchildren come
     * back empty from the initial call and are lazy-loaded per node when
     * the user expands them (see `loadRoleChildren`).
     */
    roles: DotRoleNode[];
    /** Free-text filter typed into the `Filter roles` input. */
    filter: string;
    /**
     * Server-side role search results (from
     * `/api/role/loadbyname/name/{q}/`). `null` = no active search →
     * the tree shows the normal `roles` cache. Non-null (including
     * `[]`) = a search is running/done → the tree shows this list.
     * Populated by `searchRoles` when the filter has 3+ chars.
     */
    searchResults: DotRoleNode[] | null;
    searchStatus: DotRolesStatus;
    /** Currently selected role id (drives the right-hand detail area). */
    selectedRoleId: string | null;
    /** Detail of the currently selected role (name/desc/can-grant flags/parent). */
    selectedRole: DotRoleDetail | null;
    /** Load status for the selected role's detail — drives header skeleton. */
    selectedRoleStatus: DotRolesStatus;
    /** Active tab on the right-hand detail area. */
    activeTab: DotRoleTab;
    /** Members of the selected role. */
    members: DotRoleMember[];
    /**
     * Tool groups rendered by the Tools tab: the full catalog, each row
     * annotated with whether the selected role gets it and from where.
     */
    toolGroups: DotRoleToolGroupRow[];
    toolGroupsStatus: DotRolesStatus;
    /** In-flight flag for the grant/revoke POST, so the grid can lock. */
    toolGroupsSaving: boolean;
    status: DotRolesStatus;
    membersStatus: DotRolesStatus;
    error: string | null;
}

const initialState: DotRolesState = {
    roles: [],
    filter: '',
    searchResults: null,
    searchStatus: 'INIT',
    selectedRoleId: null,
    selectedRole: null,
    selectedRoleStatus: 'INIT',
    activeTab: 'users',
    members: [],
    toolGroups: [],
    toolGroupsStatus: 'INIT',
    toolGroupsSaving: false,
    status: 'INIT',
    membersStatus: 'INIT',
    error: null
};

export const DotRolesStore = signalStore(
    withState<DotRolesState>(initialState),

    withComputed(({ roles, searchResults, selectedRoleId, selectedRole, members, toolGroups }) => ({
        /** Alias for `roles` — the tree comes nested from the wire response. */
        roleTree: computed(() => roles()),

        /**
         * Tree rendered by the tree component. When a server-side search
         * is active (`searchResults` non-null) the tree switches to the
         * search results — which include the full ancestor path of every
         * matching role, so grandchildren that were never lazy-loaded
         * still surface. Otherwise the normal `roles` cache is used.
         */
        filteredRoles: computed(() => {
            const results = searchResults();

            return results !== null ? results : roles();
        }),

        /** True while the tree is showing search results (filter length >= 3). */
        isSearching: computed(() => searchResults() !== null),

        /** Total users granted this role. */
        memberCount: computed(() => members().length),

        /**
         * Tool groups effectively granted — direct plus inherited. Drives the
         * "N tools granted" line in the detail header.
         *
         * Counts tool *groups*, not the individual portlets inside them. The
         * header copy says "tools" because that is the user-facing name for a
         * group in this screen; summing `portletIds` instead would report a
         * different number than the rows the tab shows.
         */
        toolGroupCount: computed(() => toolGroups().filter((group) => group.granted).length),

        /** True when the selected role is a system role (locked / immutable). */
        isSystemRole: computed(() => selectedRole()?.system ?? false),

        // System AND locked roles are rejected by the BE for edit/delete
        // (RoleHelper.updateRole / deleteRole return 403). Header + tree
        // context menu + edit dialog all key off this so the disabled state
        // stays consistent across entry points. Per-domain flags
        // (`editUsers`, `editPermissions`, `editLayouts`) are exposed as
        // separate computeds — the BE gates each independently so a role
        // may be editable overall but block user grants, etc.
        canModifyRole: computed(() => {
            const role = selectedRole();

            return !!role && !role.system && !role.locked;
        }),

        /**
         * Grant / revoke users on this role.
         *
         * Gated on `editUsers` ALONE, matching `RoleHelper` — the backend
         * rejects those calls only when the flag is false, and says nothing
         * about `system` or `locked`. Adding those here disabled the tab for
         * roles the backend (and the legacy portlet) happily accept, CMS
         * Administrator among them.
         *
         * `system` / `locked` DO gate updating and deleting the role itself —
         * that is `canModifyRole`, a different contract.
         */
        canEditRoleUsers: computed(() => {
            const role = selectedRole();

            return !!role && (role.editUsers ?? true);
        }),

        /**
         * Grant / revoke tool groups on this role.
         *
         * `POST /v1/roles/layouts` places no restriction on the target role at
         * all — its only gate is that the CALLER holds the CMS Admin role, which
         * surfaces as a 403 if unmet. The legacy portlet still greys the grid
         * out on `editLayouts`, so that check is kept as the UI contract; the
         * `system` / `locked` conditions that were here are not, since neither
         * the backend nor the legacy screen applies them.
         */
        canEditRoleLayouts: computed(() => {
            const role = selectedRole();

            return !!role && (role.editLayouts ?? true);
        }),

        /** True when the selected role can accept user grants. */
        canGrantUsers: computed(() => selectedRole()?.editUsers ?? true),

        /** True when the selected role has children (folder icon in the header). */
        selectedRoleIsParent: computed(() => (selectedRole()?.roleChildren?.length ?? 0) > 0),

        /** Selected role id used by consumers that need to correlate. */
        selectedIdForCorrelation: computed(() => selectedRoleId())
    })),

    withMethods((store) => {
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const rolesService = inject(DotRolesService);

        const loadRootRoles = rxMethod<void>(
            pipe(
                tap(() => patchState(store, { status: 'LOADING', error: null })),
                switchMap(() =>
                    rolesService.getRoots(true).pipe(
                        tap((roles) => {
                            patchState(store, { roles, status: 'LOADED' });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'ERROR' });

                            return EMPTY;
                        })
                    )
                )
            )
        );

        const loadRoleDetail = rxMethod<string>(
            pipe(
                tap(() => patchState(store, { selectedRoleStatus: 'LOADING' })),
                switchMap((roleId) =>
                    rolesService.getById(roleId, true).pipe(
                        tap((selectedRole) =>
                            patchState(store, { selectedRole, selectedRoleStatus: 'LOADED' })
                        ),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { selectedRoleStatus: 'ERROR' });

                            return EMPTY;
                        })
                    )
                )
            )
        );

        /**
         * Server-side role search backing the tree filter input.
         *
         * `switchMap` cancels prior in-flight searches when the user keeps
         * typing (or hits Backspace), matching the canonical Dojo portlet's
         * `filterRolesHandle` timeout gate. Queries under 3 characters
         * clear the results — the tree falls back to the normal `roles`
         * cache — matching legacy behavior (`view_roles_js_inc.jsp:311`).
         */
        const runSearch = rxMethod<string>(
            pipe(
                switchMap((filter) => {
                    const q = filter.trim();
                    if (q.length < 3) {
                        patchState(store, { searchResults: null, searchStatus: 'INIT' });

                        return EMPTY;
                    }
                    patchState(store, { searchStatus: 'LOADING' });

                    return rolesService.searchTree(q).pipe(
                        tap((results) => {
                            patchState(store, {
                                searchResults: results,
                                searchStatus: 'LOADED'
                            });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { searchStatus: 'ERROR' });

                            return EMPTY;
                        })
                    );
                })
            )
        );

        // Ancestor-walk + parallel fan-out that populates `members`. Wrapped
        // in `rxMethod` so `switchMap` cancels prior invocations when the
        // user switches roles quickly — otherwise an earlier chain could
        // resolve *after* a later one and overwrite `members` with stale
        // data. Also used post-grant / post-remove to refresh the list.
        const loadMembers = rxMethod<{ id: string }>(
            pipe(
                tap(() => patchState(store, { membersStatus: 'LOADING' })),
                switchMap((role) => {
                    // Under active search, ancestors of the picked role may
                    // live only in `searchResults` (the lazy tree hasn't
                    // loaded that branch yet). Merge both — prefer the copy
                    // that has `parent` populated so the ancestor walk can
                    // actually climb, since `unwrapLegacySearchNode` nodes
                    // don't carry `parent`.
                    const searchTree = store.isSearching() ? (store.searchResults() ?? []) : [];
                    const chain = collectAncestorChain(
                        mergeTreesPreferParent(store.roles(), searchTree),
                        role
                    );
                    if (chain.length === 0) {
                        patchState(store, { members: [], membersStatus: 'LOADED' });

                        return EMPTY;
                    }

                    const requests = chain.map((node) =>
                        rolesService.getUsers(node.id).pipe(
                            map((users) => ({
                                failed: false,
                                members: users.map<DotRoleMember>((u) => ({
                                    userId: u.userId,
                                    firstName: u.firstName ?? '',
                                    lastName: u.lastName ?? '',
                                    emailAddress: u.emailAddress ?? '',
                                    grantedFromRoleId: node.id,
                                    grantedFromRoleName: node.name
                                }))
                            })),
                            catchError((error) => {
                                httpErrorManager.handle(error);

                                // Unknown, not empty — see the `failed` check below.
                                return of({ failed: true, members: [] as DotRoleMember[] });
                            })
                        )
                    );

                    return forkJoin(requests).pipe(
                        tap((batches) => {
                            const byUserId = new Map<string, DotRoleMember>();
                            for (const batch of batches) {
                                for (const member of batch.members) {
                                    if (!byUserId.has(member.userId)) {
                                        byUserId.set(member.userId, member);
                                    }
                                }
                            }
                            // Same guard as loadToolGroups: a response for a
                            // role the admin already navigated away from must
                            // not repaint the current role's member list.
                            if (store.selectedRoleId() !== role.id) {
                                return;
                            }

                            // Same rule as loadToolGroups: an ancestor we could
                            // not query is not an ancestor with no members. A
                            // silently short roster is worse than a visible
                            // failure when the admin is auditing who has access.
                            const unverified = batches.some((batch) => batch.failed);
                            const members = Array.from(byUserId.values());

                            // Keep the tree badge honest. `userCount` ships with
                            // the role payload and is never refreshed by a grant
                            // or a revoke, so without this it stays at whatever
                            // it was when the tree loaded — most visibly, a role
                            // that just got its first user keeps showing no badge
                            // at all, since the badge hides at zero.
                            //
                            // Counts DIRECT grants only, matching what the
                            // backend puts in that field. Skipped when a check
                            // failed: writing a count derived from a partial
                            // answer would replace a stale number with a wrong
                            // one.
                            const directCount = unverified
                                ? null
                                : members.filter((m) => m.grantedFromRoleId === role.id).length;

                            patchState(store, {
                                members,
                                membersStatus: unverified ? 'ERROR' : 'LOADED',
                                ...(directCount === null
                                    ? {}
                                    : {
                                          roles: patchNodeUserCount(
                                              store.roles(),
                                              role.id,
                                              directCount
                                          )
                                      })
                            });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { membersStatus: 'ERROR' });

                            return EMPTY;
                        })
                    );
                })
            )
        );

        /**
         * Populate the Tools tab.
         *
         * Two reads, composed client-side because neither endpoint alone
         * answers the question the tab asks:
         *   - the catalog (`/v1/roles/layouts`) is the only source of
         *     `portletTitles`, so it drives every row and its display;
         *   - `/v1/roles/{id}/layouts` returns direct grants only (the BE
         *     query is `where role_id = ?`, no hierarchy walk), so effective
         *     grants are assembled by walking the ancestor chain, exactly as
         *     `loadMembers` does for users.
         *
         * Each row is tagged with the CLOSEST ancestor that grants it (the
         * chain is ordered `[role, parent, grandparent, ...]`), so a direct
         * grant always wins over an inherited one and the Granted From chip
         * names the role the admin has to edit to revoke it.
         */
        const loadToolGroups = rxMethod<{ id: string; silent?: boolean }>(
            pipe(
                // `silent` reconciles in the background after a grant/revoke.
                // Flipping to LOADING there would swap the whole table for the
                // skeleton on every checkbox click — the flicker users see.
                tap(({ silent }) => {
                    if (!silent) {
                        patchState(store, { toolGroupsStatus: 'LOADING' });
                    }
                }),
                switchMap((role) => {
                    const searchTree = store.isSearching() ? (store.searchResults() ?? []) : [];
                    const chain = collectAncestorChain(
                        mergeTreesPreferParent(store.roles(), searchTree),
                        role
                    );

                    const grants$ =
                        chain.length === 0
                            ? of<Array<{ node: DotRoleNode; ids: Set<string>; failed: boolean }>>(
                                  []
                              )
                            : forkJoin(
                                  chain.map((node) =>
                                      rolesService.getToolGroups(node.id).pipe(
                                          map((groups) => ({
                                              node,
                                              ids: new Set(groups.map((group) => group.id)),
                                              failed: false
                                          })),
                                          catchError((error) => {
                                              httpErrorManager.handle(error);

                                              // Keep the other ancestors usable,
                                              // but remember this one is unknown —
                                              // see the `failed` handling below.
                                              return of({
                                                  node,
                                                  ids: new Set<string>(),
                                                  failed: true
                                              });
                                          })
                                      )
                                  )
                              );

                    return forkJoin({
                        catalog: rolesService.getAllToolGroups(),
                        grants: grants$
                    }).pipe(
                        tap(({ catalog, grants }) => {
                            const toolGroups = catalog.map<DotRoleToolGroupRow>((group) => {
                                const source = grants.find((grant) => grant.ids.has(group.id));

                                return {
                                    ...group,
                                    granted: !!source,
                                    grantedFromRoleId: source?.node.id ?? null,
                                    grantedFromRoleName: source?.node.name ?? null
                                };
                            });

                            // The role may have changed while this was in
                            // flight — writing here would show one role's tool
                            // groups under another's name, and the next toggle
                            // would POST this role's grants onto that one.
                            if (store.selectedRoleId() !== role.id) {
                                return;
                            }

                            // If an ancestor check failed we cannot tell "not
                            // granted" from "could not verify". Rendering the
                            // grid anyway would show inherited groups unchecked,
                            // and an admin trusting it would create a redundant
                            // direct grant for something the role already has.
                            const unverified = grants.some((grant) => grant.failed);

                            patchState(store, {
                                toolGroups,
                                toolGroupsStatus: unverified ? 'ERROR' : 'LOADED'
                            });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { toolGroupsStatus: 'ERROR' });

                            return EMPTY;
                        })
                    );
                })
            )
        );

        return {
            loadRootRoles,
            loadRoleDetail,

            /**
             * Update the free-text filter and (for queries with 3+ chars)
             * kick off the server-side search via `runSearch`. The component
             * already debounces user input, so we forward the value as-is.
             */
            setFilter(filter: string): void {
                patchState(store, { filter });
                runSearch(filter);
            },

            /** Select a role and load its detail + members. */
            selectRole(roleId: string | null): void {
                patchState(store, {
                    selectedRoleId: roleId,
                    // Clear the previous role's detail so the header can
                    // render its skeleton on `selectedRoleStatus === 'LOADING'`
                    // instead of showing stale data while the new fetch is
                    // in flight.
                    selectedRole: null,
                    selectedRoleStatus: roleId ? 'LOADING' : 'INIT',
                    members: [],
                    membersStatus: 'INIT',
                    toolGroups: [],
                    toolGroupsStatus: 'INIT',
                    // A save still in flight belongs to the role we are leaving.
                    // Leaving this true locks every checkbox on the role we are
                    // switching TO, which has no save of its own.
                    toolGroupsSaving: false
                });

                if (roleId) {
                    loadRoleDetail(roleId);
                    // Loaded on selection rather than when the Tools tab opens:
                    // the detail header shows the granted count on every tab, so
                    // deferring this would leave it reading 0 until the admin
                    // happened to click Tools.
                    loadToolGroups({ id: roleId });
                }
            },

            /**
             * Load members for the currently selected role — including
             * users granted to any ancestor of the role (dotCMS inherits
             * grants downward, so a Reviewer's user list includes users
             * granted Publisher / Legal even though those users were never
             * granted Reviewer directly).
             *
             * Implementation: walks the ancestor chain in `state.roles` via
             * `parent` id, fires one `GET /v1/roles/{roleId}/users` per role
             * in parallel, and merges the results. Each user is tagged with
             * the closest ancestor where they were directly granted (selected
             * role first, then parent, grandparent, ...), so the Users tab can
             * render the "Granted From" chip and disable removal on inherited
             * rows.
             *
             * The fan-out is permanent, not a stopgap. #37070 shipped
             * `/roles/{id}/users` as a *direct grants only* resource and
             * explicitly declined to resolve inheritance or denormalize
             * granted-from metadata into the response, so composing effective
             * membership is the client's job by design. What the endpoint did
             * remove is the `roleKey`-vs-id branching and the email-less
             * `/rolehierarchyanduserroles` fallback: one call shape now works
             * for every role and carries `emailAddress`.
             */
            loadMembers(role: { id: string }): void {
                loadMembers({ id: role.id });
            },

            loadToolGroups(role: { id: string }): void {
                loadToolGroups({ id: role.id });
            },

            /**
             * Persist the tool groups granted directly to the selected role.
             *
             * `toolGroupIds` must be the COMPLETE set of direct grants after
             * the toggle, not a delta — the endpoint is a full replace and
             * drops anything missing from the payload. Inherited grants are
             * deliberately excluded: they belong to the ancestor, and sending
             * them here would silently promote them to direct grants on this
             * role.
             *
             * Reloads on success so the Granted From chips and the header
             * count reflect what the backend actually stored.
             */
            async saveToolGroups(toolGroupIds: string[]): Promise<boolean> {
                const roleId = store.selectedRoleId();
                if (!roleId) {
                    return false;
                }

                const previous = store.toolGroups();
                const wanted = new Set(toolGroupIds);
                const selectedRoleName = store.selectedRole()?.name ?? null;

                // Paint the toggle immediately. The row already knows enough to
                // show the right thing: a checked group is granted by the role
                // we are editing. Un-checking a group an ancestor ALSO grants
                // is the one case we cannot resolve locally — the row only
                // keeps its closest source — so the reconcile below restores
                // the inherited chip a moment later.
                patchState(store, {
                    toolGroupsSaving: true,
                    toolGroups: previous.map((group) => {
                        if (wanted.has(group.id)) {
                            return {
                                ...group,
                                granted: true,
                                grantedFromRoleId: roleId,
                                grantedFromRoleName: selectedRoleName
                            };
                        }

                        return group.grantedFromRoleId === roleId
                            ? {
                                  ...group,
                                  granted: false,
                                  grantedFromRoleId: null,
                                  grantedFromRoleName: null
                              }
                            : group;
                    })
                });

                try {
                    await firstValueFrom(
                        rolesService.saveToolGroups(roleId, toolGroupIds).pipe(take(1))
                    );
                    patchState(store, { toolGroupsSaving: false });
                    // Silent: reconciles inherited chips and the header count
                    // without the table ever leaving the loaded state.
                    loadToolGroups({ id: roleId, silent: true });

                    return true;
                } catch (error) {
                    httpErrorManager.handle(error);
                    // Roll the optimistic patch back — the grid must not keep
                    // showing a state the backend rejected.
                    patchState(store, { toolGroups: previous, toolGroupsSaving: false });

                    return false;
                }
            },

            /**
             * Lazy-load a node's children when the user expands it in the
             * roles tree. Fetches `/v1/roles/{roleId}?loadChildrenRoles=true`
             * and splices the returned children into the state tree.
             *
             * `firstValueFrom(...pipe(take(1)))` disposes the HTTP
             * subscription after one emission; spam-expanding tree nodes
             * no longer leaks.
             */
            async loadRoleChildren(roleId: string): Promise<void> {
                try {
                    const loaded = await firstValueFrom(
                        rolesService.getById(roleId, true).pipe(take(1))
                    );
                    patchState(store, {
                        roles: patchNodeChildren(store.roles(), roleId, loaded.roleChildren ?? [])
                    });
                } catch (error) {
                    httpErrorManager.handle(error);
                }
            },

            /**
             * On-demand fetch of a role's full detail (no state mutation).
             * Callers that need the full form payload (`roleKey`, `parent`,
             * `description`, `editUsers/Permissions/Layouts`) must resolve
             * it here before opening the Edit dialog — search-result nodes
             * only carry `{id, name, locked}`, and PUT is a full replace.
             */
            /**
             * One-shot deep search that RETURNS its results instead of writing
             * them to state.
             *
             * `setFilter` drives the left-hand tree through `searchResults`, so
             * a dialog reusing it would visibly re-filter the tree behind it.
             * This shares the same endpoint and the same ancestor-path payload
             * without touching what the page is showing.
             */
            async searchRoleTree(query: string): Promise<DotRoleNode[]> {
                try {
                    return await firstValueFrom(rolesService.searchTree(query).pipe(take(1)));
                } catch (error) {
                    httpErrorManager.handle(error);

                    return [];
                }
            },

            async fetchRoleDetail(roleId: string): Promise<DotRoleDetail | null> {
                try {
                    return await firstValueFrom(rolesService.getById(roleId, false).pipe(take(1)));
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            setActiveTab(activeTab: DotRoleTab): void {
                patchState(store, { activeTab });
            },

            /**
             * Create a role. Returns a promise so the calling component
             * can close its dialog on success and surface errors on failure.
             *
             * Efficiency: the POST response carries the fully-hydrated new
             * role (id, parent, roleKey, DBFQN, FQN...). We splice it into
             * the parent's `roleChildren` in state — no follow-up fetch,
             * no loss of lazy-loaded branches elsewhere in the tree.
             *
             * Fallback: when the target parent isn't currently in state
             * (edge case — parent picker showed a role we haven't lazy-
             * loaded above), we scope the refresh to just that parent's
             * subtree via `loadRoleChildren` instead of reloading the whole
             * tree.
             */
            async createRole(form: DotRoleFormValue): Promise<DotRoleDetail | null> {
                try {
                    const response = await firstValueFrom(rolesService.create(form).pipe(take(1)));

                    // `POST /v1/roles` answers with `Role.toMap()`, not a
                    // `RoleView`, so the payload carries no `childCount`. The
                    // tree treats an absent count as "unknown" and falls back
                    // to its chevron heuristic, which renders a brand-new role
                    // as a folder. A role that was just created has no
                    // children by definition, so state that outright.
                    // (`PUT` does return a RoleView, so update needs none of
                    // this.)
                    const created: DotRoleDetail = { ...response, childCount: 0 };

                    const parentId = form.parentRoleId ?? null;
                    if (!parentId) {
                        // Root role — append to state.roles.
                        patchState(store, { roles: [...store.roles(), created] });
                    } else if (findRoleInTree(store.roles(), parentId)) {
                        // Parent is loaded — splice in place, keep the rest untouched.
                        patchState(store, {
                            roles: appendChildToParent(store.roles(), parentId, created)
                        });
                    } else {
                        // Parent isn't in the loaded tree — await the
                        // sub-tree refresh so the tree is coherent before
                        // we resolve. A bare `subscribe` here used to race
                        // the resolve.
                        try {
                            const parentDetail = await firstValueFrom(
                                rolesService.getById(parentId, true).pipe(take(1))
                            );
                            patchState(store, {
                                roles: patchNodeChildren(
                                    store.roles(),
                                    parentId,
                                    parentDetail.roleChildren ?? []
                                )
                            });
                        } catch (error) {
                            httpErrorManager.handle(error);
                        }
                    }

                    // The POST response is already a hydrated `RoleView` —
                    // seed `selectedRole` directly instead of firing a
                    // follow-up GET, which would leave the header showing
                    // a skeleton / stale role for the round-trip.
                    patchState(store, {
                        selectedRoleId: created.id,
                        selectedRole: created,
                        selectedRoleStatus: 'LOADED'
                    });

                    return created;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Update a role (PUT /v1/roles/{roleId}). Returns the hydrated
             * updated role on success, or `null` on failure with the
             * error routed through `httpErrorManager`.
             *
             * State reconciliation:
             *   - `selectedRole` refreshed with the response
             *   - If the `parent` didn't change, the tree node is patched in place
             *   - If the `parent` changed (reparent), the node is spliced out of
             *     the old parent and appended to the new one — no full-tree reload
             *
             * The response is trusted for parent because the BE encodes root as
             * `parent === role.id`. `null` `parentRoleId` in the request becomes
             * that self-referential shape in the response.
             */
            async updateRole(
                roleId: string,
                form: DotRoleFormValue
            ): Promise<DotRoleDetail | null> {
                try {
                    const updated = await firstValueFrom(
                        rolesService.update(roleId, form).pipe(take(1))
                    );

                    const previous = findRoleInTree(store.roles(), roleId);
                    const previousParentId = previous?.parent ?? null;
                    const nextParentId =
                        updated.parent && updated.parent !== updated.id ? updated.parent : null;

                    if (previousParentId === nextParentId) {
                        patchState(store, {
                            roles: patchNodeInPlace(store.roles(), roleId, updated),
                            selectedRole: updated
                        });
                    } else {
                        const detached = removeNodeFromTree(store.roles(), roleId);
                        const inserted = nextParentId
                            ? appendChildToParent(detached, nextParentId, updated)
                            : [...detached, updated];
                        patchState(store, { roles: inserted, selectedRole: updated });
                    }

                    return updated;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Delete a role (DELETE /v1/roles/{roleId}). Returns the
             * deletion result on success (including `usersAffected`, the
             * cascade blast radius) so the caller can surface it in a toast.
             *
             * State reconciliation: the node is removed from the tree; if it
             * was the selected role, selection + members + selectedRole are
             * cleared. On BE rejection (403 system/locked, 404, 409 has
             * children/workflow), the error routes through `httpErrorManager`
             * and this method returns `null`.
             */
            async deleteRole(roleId: string): Promise<DotRoleDeletionResult | null> {
                try {
                    const result = await firstValueFrom(rolesService.delete(roleId).pipe(take(1)));

                    // Only prune from the tree when the BE actually deleted.
                    // A 200 with `deleted:false` means the server rejected on
                    // its own terms (hierarchy constraint / workflow ref) —
                    // keeping the node in the tree matches reality.
                    if (result?.deleted) {
                        patchState(store, {
                            roles: removeNodeFromTree(store.roles(), roleId)
                        });

                        if (store.selectedRoleId() === roleId) {
                            patchState(store, {
                                selectedRoleId: null,
                                selectedRole: null,
                                members: [],
                                membersStatus: 'INIT'
                            });
                        }
                    }

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Grant a user membership in the currently-selected role
             * (POST /v1/roles/{roleId}/users/{userId}).
             *
             * Refreshes members on success so the Users tab reflects the grant
             * (and inherited-vs-direct labelling stays correct). The BE is
             * idempotent — re-granting an already-held role returns `granted:
             * true` and no error.
             *
             * Returns the granted result on success, `null` on failure.
             */
            async grantUserToRole(userId: string): Promise<DotRoleUserGrantResult | null> {
                const role = store.selectedRole();
                if (!role) {
                    return null;
                }

                try {
                    const result = await firstValueFrom(
                        rolesService.grantUser(role.id, userId).pipe(take(1))
                    );

                    // Reload members so the new row lands in the table with the
                    // correct grantedFromRoleId/Name (matching the selected role).
                    loadMembers({ id: role.id });

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Bulk-remove users from the currently-selected role
             * (DELETE /v1/roles/{roleId}/users). Returns the
             * partial-success report so the caller can surface which rows
             * were skipped (typically inherited memberships that can only
             * be revoked from the ancestor that grants them).
             *
             * State reconciliation: on any success, prune the removed users
             * out of `members` immediately (optimistic), clear the current
             * bulk-selection, and refetch members to reconcile with any BE
             * changes we didn't observe. Returns `null` when the request
             * itself fails (routed through `httpErrorManager`).
             */
            async removeUsersFromRole(
                userIds: string[]
            ): Promise<DotRoleUsersRemovalResult | null> {
                const role = store.selectedRole();
                if (!role || userIds.length === 0) {
                    return null;
                }

                try {
                    const result = await firstValueFrom(
                        rolesService.removeUsers(role.id, userIds).pipe(take(1))
                    );

                    if (result.removedUserIds.length > 0) {
                        const removed = new Set(result.removedUserIds);
                        patchState(store, {
                            members: store.members().filter((m) => !removed.has(m.userId))
                        });
                    }

                    // Refresh from the BE too — inherited-vs-direct labelling
                    // can shift if a user was ONLY direct on this role and now
                    // ends up inherited from an ancestor still granting them.
                    loadMembers({ id: role.id });

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            }
        };
    })
);
