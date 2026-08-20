import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, map, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import {
    DotRoleDetail,
    DotRoleFormValue,
    DotRoleMember,
    DotRoleNode,
    DotRoleTab,
    DotRolesStatus
} from '../../models/dot-roles.models';
import {
    DotRoleDeletionResult,
    DotRolesPortletService,
    DotRoleUserGrantResult,
    DotRoleUsersRemovalResult
} from '../../services/dot-roles-portlet.service';

export interface DotRolesState {
    /**
     * Nested role tree as returned by `GET /v1/roles?loadChildrenRoles=true`
     * — root roles with their direct children populated. Grandchildren come
     * back empty from the initial call and are lazy-loaded per node when
     * the user expands them (see `loadRoleChildren`).
     */
    roles: DotRoleNode[];
    /** Client-side filter typed into the `Filter roles` input. */
    filter: string;
    /** Currently selected role id (drives the right-hand detail area). */
    selectedRoleId: string | null;
    /** Detail of the currently selected role (name/desc/can-grant flags/parent). */
    selectedRole: DotRoleDetail | null;
    /** Active tab on the right-hand detail area. */
    activeTab: DotRoleTab;
    /** Members of the selected role. */
    members: DotRoleMember[];
    /** Users selected for bulk-remove on the members table. */
    selectedMembers: DotRoleMember[];
    status: DotRolesStatus;
    membersStatus: DotRolesStatus;
    error: string | null;
}

const initialState: DotRolesState = {
    roles: [],
    filter: '',
    selectedRoleId: null,
    selectedRole: null,
    activeTab: 'users',
    members: [],
    selectedMembers: [],
    status: 'init',
    membersStatus: 'init',
    error: null
};

export const DotRolesStore = signalStore(
    withState<DotRolesState>(initialState),

    withComputed(({ roles, filter, selectedRoleId, selectedRole, members }) => ({
        /** Alias for `roles` — the tree comes nested from the wire response. */
        roleTree: computed(() => roles()),

        /**
         * Nested tree filtered by the free-text `Filter roles` input. A
         * parent whose name doesn't match is still kept if any of its
         * descendants match; a leaf is dropped when it doesn't match.
         */
        filteredRoles: computed(() => {
            const q = filter().trim().toLowerCase();
            if (!q) {
                return roles();
            }

            return filterTree(roles(), q);
        }),

        /** Total users granted this role. */
        memberCount: computed(() => members().length),

        /** True when the selected role is a system role (locked / immutable). */
        isSystemRole: computed(() => selectedRole()?.system ?? false),

        /** True when the selected role can accept user grants. */
        canGrantUsers: computed(() => selectedRole()?.editUsers ?? true),

        /** True when the selected role has children (folder icon in the header). */
        selectedRoleIsParent: computed(() => (selectedRole()?.roleChildren?.length ?? 0) > 0),

        /** Selected role id used by consumers that need to correlate. */
        selectedIdForCorrelation: computed(() => selectedRoleId())
    })),

    withMethods((store) => {
        const service = inject(DotRolesPortletService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        const loadRootRoles = rxMethod<void>(
            pipe(
                tap(() => patchState(store, { status: 'loading', error: null })),
                switchMap(() =>
                    service.loadRootRoles(true).pipe(
                        tap((roles) => {
                            patchState(store, { roles, status: 'loaded' });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'error' });

                            return EMPTY;
                        })
                    )
                )
            )
        );

        const loadRoleDetail = rxMethod<string>(
            pipe(
                switchMap((roleId) =>
                    service.loadRoleById(roleId, true).pipe(
                        tap((selectedRole) => patchState(store, { selectedRole })),
                        catchError((error) => {
                            httpErrorManager.handle(error);

                            return EMPTY;
                        })
                    )
                )
            )
        );

        /**
         * Shared ancestor-walk + fan-out that powers both `loadMembers` and
         * the post-grant / post-remove refresh. Kept as a local closure so
         * cross-method reuse doesn't require `this` binding through NgRx's
         * `withMethods` wrapping.
         */
        const refreshMembersFor = (role: { id: string; roleKey?: string | null }): void => {
            const chain = collectAncestorChain(store.roles(), role);
            if (chain.length === 0) {
                patchState(store, { members: [], membersStatus: 'loaded' });

                return;
            }

            patchState(store, { membersStatus: 'loading' });

            const requests = chain.map((node) => {
                const source$ = node.roleKey
                    ? service.loadRoleMembersByKey(node.roleKey)
                    : service.loadRoleMembersById(node.id);

                return source$.pipe(
                    map((users) =>
                        users.map<DotRoleMember>((u) => ({
                            userId: u.userId,
                            firstName: u.firstName ?? '',
                            lastName: u.lastName ?? '',
                            emailAddress: u.emailAddress ?? '',
                            grantedFromRoleId: node.id,
                            grantedFromRoleName: node.name
                        }))
                    ),
                    catchError((error) => {
                        httpErrorManager.handle(error);

                        return of<DotRoleMember[]>([]);
                    })
                );
            });

            forkJoin(requests).subscribe({
                next: (batches) => {
                    const byUserId = new Map<string, DotRoleMember>();
                    for (const batch of batches) {
                        for (const member of batch) {
                            if (!byUserId.has(member.userId)) {
                                byUserId.set(member.userId, member);
                            }
                        }
                    }
                    patchState(store, {
                        members: Array.from(byUserId.values()),
                        membersStatus: 'loaded'
                    });
                },
                error: (error) => {
                    httpErrorManager.handle(error);
                    patchState(store, { membersStatus: 'error' });
                }
            });
        };

        return {
            loadRootRoles,
            loadRoleDetail,

            /** Update the free-text filter applied to the tree. */
            setFilter(filter: string): void {
                patchState(store, { filter });
            },

            /** Select a role and load its detail + members. */
            selectRole(roleId: string | null): void {
                patchState(store, {
                    selectedRoleId: roleId,
                    selectedMembers: [],
                    members: [],
                    membersStatus: 'init'
                });

                if (roleId) {
                    loadRoleDetail(roleId);
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
             * `parent` id, fires one `/v1/users/filter?roleKey=X` per role
             * in parallel (falling back to the id-based endpoint when a
             * role has no `roleKey`), and merges results. Each user is
             * tagged with the closest ancestor where they were directly
             * granted (selected role first, then parent, grandparent, ...),
             * so the Users tab can render the "Granted From" chip and
             * disable removal on inherited rows.
             *
             * TODO: collapse this whole flow into a single call to
             * `GET /v1/roles/{roleId}/users` once #37070 ships. That
             * endpoint will do the ancestor walk + user enrichment
             * (including email) server-side.
             */
            loadMembers(role: { id: string; roleKey?: string | null }): void {
                refreshMembersFor(role);
            },

            /**
             * Lazy-load a node's children when the user expands it in the
             * roles tree. Fetches `/v1/roles/{roleId}?loadChildrenRoles=true`
             * and splices the returned children into the state tree.
             */
            loadRoleChildren(roleId: string): void {
                service.loadRoleById(roleId, true).subscribe({
                    next: (loaded) => {
                        const next = patchNodeChildren(
                            store.roles(),
                            roleId,
                            loaded.roleChildren ?? []
                        );
                        patchState(store, { roles: next });
                    },
                    error: (error) => httpErrorManager.handle(error)
                });
            },

            setActiveTab(activeTab: DotRoleTab): void {
                patchState(store, { activeTab });
            },

            setSelectedMembers(selectedMembers: DotRoleMember[]): void {
                patchState(store, { selectedMembers });
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
                    const created = await new Promise<DotRoleDetail>((resolve, reject) => {
                        service.createRole(form).subscribe({
                            next: (role) => resolve(role),
                            error: (err) => reject(err)
                        });
                    });

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
                        // Parent isn't in the loaded tree — refresh just that
                        // subtree instead of reloading the whole root list.
                        service.loadRoleById(parentId, true).subscribe({
                            next: (parentDetail) => {
                                patchState(store, {
                                    roles: patchNodeChildren(
                                        store.roles(),
                                        parentId,
                                        parentDetail.roleChildren ?? []
                                    )
                                });
                            },
                            error: (error) => httpErrorManager.handle(error)
                        });
                    }

                    patchState(store, { selectedRoleId: created.id });
                    loadRoleDetail(created.id);

                    return created;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Update a role (PUT /v1/roles/{roleId} — #36936). Returns the
             * hydrated updated role on success, or `null` on failure with the
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
                    const updated = await new Promise<DotRoleDetail>((resolve, reject) => {
                        service.updateRole(roleId, form).subscribe({
                            next: (role) => resolve(role),
                            error: (err) => reject(err)
                        });
                    });

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
             * Delete a role (DELETE /v1/roles/{roleId} — #36939). Returns the
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
                    const result = await new Promise<DotRoleDeletionResult>((resolve, reject) => {
                        service.deleteRole(roleId).subscribe({
                            next: (r) => resolve(r),
                            error: (err) => reject(err)
                        });
                    });

                    patchState(store, { roles: removeNodeFromTree(store.roles(), roleId) });

                    if (store.selectedRoleId() === roleId) {
                        patchState(store, {
                            selectedRoleId: null,
                            selectedRole: null,
                            members: [],
                            selectedMembers: [],
                            membersStatus: 'init'
                        });
                    }

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Grant a user membership in the currently-selected role
             * (POST /v1/roles/{roleId}/users/{userId} — #36937).
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
                    const result = await new Promise<DotRoleUserGrantResult>((resolve, reject) => {
                        service.grantUserToRole(role.id, userId).subscribe({
                            next: (r) => resolve(r),
                            error: (err) => reject(err)
                        });
                    });

                    // Reload members so the new row lands in the table with the
                    // correct grantedFromRoleId/Name (matching the selected role).
                    refreshMembersFor({ id: role.id, roleKey: role.roleKey ?? null });

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            },

            /**
             * Bulk-remove users from the currently-selected role
             * (DELETE /v1/roles/{roleId}/users — #36938). Returns the
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
                    const result = await new Promise<DotRoleUsersRemovalResult>(
                        (resolve, reject) => {
                            service.removeUsersFromRole(role.id, userIds).subscribe({
                                next: (r) => resolve(r),
                                error: (err) => reject(err)
                            });
                        }
                    );

                    if (result.removedUserIds.length > 0) {
                        const removed = new Set(result.removedUserIds);
                        patchState(store, {
                            members: store.members().filter((m) => !removed.has(m.userId)),
                            selectedMembers: store
                                .selectedMembers()
                                .filter((m) => !removed.has(m.userId))
                        });
                    }

                    // Refresh from the BE too — inherited-vs-direct labelling
                    // can shift if a user was ONLY direct on this role and now
                    // ends up inherited from an ancestor still granting them.
                    refreshMembersFor({ id: role.id, roleKey: role.roleKey ?? null });

                    return result;
                } catch (error) {
                    httpErrorManager.handle(error);

                    return null;
                }
            }
        };
    })
);

function filterTree(nodes: DotRoleNode[], query: string): DotRoleNode[] {
    return nodes.reduce<DotRoleNode[]>((acc, node) => {
        const matches = node.name.toLowerCase().includes(query);
        const filteredChildren = node.roleChildren ? filterTree(node.roleChildren, query) : [];

        if (matches || filteredChildren.length > 0) {
            acc.push({
                ...node,
                roleChildren: filteredChildren.length > 0 ? filteredChildren : node.roleChildren
            });
        }

        return acc;
    }, []);
}

/**
 * Immutably splice `newChildren` into the tree under the node with `id`.
 * Returns a new tree; unchanged branches are shared by reference.
 */
function patchNodeChildren(
    nodes: DotRoleNode[],
    id: string,
    newChildren: DotRoleNode[]
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === id) {
            return { ...node, roleChildren: newChildren };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: patchNodeChildren(node.roleChildren, id, newChildren)
            };
        }
        return node;
    });
}

/**
 * Immutably replace the node with `id`, preserving `roleChildren` from the
 * previous version. Used by updateRole when the parent hasn't changed — the
 * server response may omit deeper descendants we already lazy-loaded.
 */
function patchNodeInPlace(
    nodes: DotRoleNode[],
    id: string,
    replacement: DotRoleNode
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === id) {
            return { ...replacement, roleChildren: node.roleChildren ?? [] };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: patchNodeInPlace(node.roleChildren, id, replacement)
            };
        }
        return node;
    });
}

/** Immutably drop the node with `id` from anywhere in the tree. */
function removeNodeFromTree(nodes: DotRoleNode[], id: string): DotRoleNode[] {
    return nodes.reduce<DotRoleNode[]>((acc, node) => {
        if (node.id === id) {
            return acc;
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            acc.push({
                ...node,
                roleChildren: removeNodeFromTree(node.roleChildren, id)
            });
        } else {
            acc.push(node);
        }
        return acc;
    }, []);
}

/**
 * Immutably append a newly-created role to its parent's `roleChildren`.
 * Sharing branches by reference keeps re-render churn minimal.
 */
function appendChildToParent(
    nodes: DotRoleNode[],
    parentId: string,
    child: DotRoleNode
): DotRoleNode[] {
    return nodes.map((node) => {
        if (node.id === parentId) {
            return {
                ...node,
                roleChildren: [...(node.roleChildren ?? []), child]
            };
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            return {
                ...node,
                roleChildren: appendChildToParent(node.roleChildren, parentId, child)
            };
        }
        return node;
    });
}

/**
 * Build the ancestor chain for a role, ordered `[role, parent, grandparent,
 * ..., root]`. Matches the Java `RoleAPI.findRoleHierarchy` semantics (a
 * self-referential `parent === id` marks the root). The chain drives the
 * parallel `/v1/users/filter?roleKey=X` fan-out in `loadMembers`.
 */
function collectAncestorChain(
    tree: DotRoleNode[],
    role: { id: string; roleKey?: string | null }
): DotRoleNode[] {
    const start = findRoleInTree(tree, role.id) ?? {
        id: role.id,
        name: role.id,
        roleKey: role.roleKey ?? undefined
    };
    const chain: DotRoleNode[] = [start];
    let cursor: DotRoleNode | null = start;
    // Guard against pathological data — hierarchies deeper than 20 aren't
    // realistic in practice, and this stops any accidental cycles cold.
    for (let i = 0; i < 20; i++) {
        const parentId = cursor.parent;
        if (!parentId || parentId === cursor.id) {
            break;
        }
        const parentNode = findRoleInTree(tree, parentId);
        if (!parentNode) {
            break;
        }
        chain.push(parentNode);
        cursor = parentNode;
    }

    return chain;
}

/** Walk the tree looking for a node id. Returns the node or `null`. */
function findRoleInTree(nodes: DotRoleNode[], id: string): DotRoleNode | null {
    for (const node of nodes) {
        if (node.id === id) {
            return node;
        }
        if (node.roleChildren && node.roleChildren.length > 0) {
            const found = findRoleInTree(node.roleChildren, id);
            if (found) {
                return found;
            }
        }
    }

    return null;
}
