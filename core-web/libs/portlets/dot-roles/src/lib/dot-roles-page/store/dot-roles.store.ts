import { patchState, signalStore, withComputed, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { computed, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import {
    DotRoleDetail,
    DotRoleFormValue,
    DotRoleMember,
    DotRoleNode,
    DotRoleTab,
    DotRolesStatus
} from '../../models/dot-roles.models';
import { DotRolesPortletService } from '../../services/dot-roles-portlet.service';

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
             * Load members for the currently selected role. Called by the
             * Users tab component whenever the selected role changes.
             *
             * Path selection:
             *   - If the role has a `roleKey`, use `/v1/users/filter?roleKey=X`
             *     — fast, returns email and full name in one call.
             *   - Otherwise, fall back to `/rolehierarchyanduserroles` and
             *     parse user-roles from the response. That path is missing
             *     email today because the endpoint returns Role objects, not
             *     User objects (see `DotRolesPortletService.loadRoleMembersById`).
             */
            loadMembers(role: { id: string; roleKey?: string | null }): void {
                patchState(store, { membersStatus: 'loading' });
                const request$ = role.roleKey
                    ? service.loadRoleMembersByKey(role.roleKey)
                    : service.loadRoleMembersById(role.id);
                request$.subscribe({
                    next: (users) => {
                        const members: DotRoleMember[] = users.map((u) => ({
                            userId: u.userId,
                            firstName: u.firstName ?? '',
                            lastName: u.lastName ?? '',
                            emailAddress: u.emailAddress ?? ''
                        }));
                        patchState(store, { members, membersStatus: 'loaded' });
                    },
                    error: (error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { membersStatus: 'error' });
                    }
                });
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
            }

            // NOTE: updateRole / deleteRole / grantUserToRole / removeUsersFromRole /
            // reparentRole are intentionally NOT exposed from the store yet.
            // They depend on backend endpoints that don't exist:
            //   - #36936 (PUT  /v1/roles/{roleId})           → Edit Role, reparent
            //   - #36937 (POST /v1/roles/{roleId}/users/{userId}) → Grant user
            //   - #36938 (DELETE /v1/roles/{roleId}/users)   → Remove members
            //   - #36939 (DELETE /v1/roles/{roleId})         → Delete Role
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
