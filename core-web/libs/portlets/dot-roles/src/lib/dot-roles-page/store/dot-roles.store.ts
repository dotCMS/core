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
    /** Full tree as loaded from `/v1/roles` (root roles + their first-level children). */
    rootRoles: DotRoleNode[];
    /** Client-side filter typed into the `Filter roles` input. */
    filter: string;
    /** Currently selected role id (drives the right-hand detail area). */
    selectedRoleId: string | null;
    /** Detail of the currently selected role (name/desc/can-grant flags/parent). */
    selectedRole: DotRoleDetail | null;
    /** Active tab on the right-hand detail area. */
    activeTab: DotRoleTab;
    /** Members of the selected role, annotated with inheritance metadata. */
    members: DotRoleMember[];
    /** Users selected for bulk-remove on the members table. */
    selectedMembers: DotRoleMember[];
    status: DotRolesStatus;
    membersStatus: DotRolesStatus;
    error: string | null;
}

const initialState: DotRolesState = {
    rootRoles: [],
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

    withComputed(({ rootRoles, filter, selectedRoleId, selectedRole, members }) => ({
        /**
         * Client-side filter applied to the loaded tree. The Dojo portlet
         * hits `/api/role/loadbyname` on filter; the Angular portlet
         * loads the tree once and filters in memory to avoid extra HTTP
         * per keystroke.
         */
        filteredRoles: computed(() => {
            const q = filter().trim().toLowerCase();
            if (!q) {
                return rootRoles();
            }

            return filterTree(rootRoles(), q);
        }),

        /** Total users granted this role — direct + inherited. */
        memberCount: computed(() => members().length),

        /** How many members were granted directly on the selected role. */
        directMemberCount: computed(
            () => members().filter((m) => m.grantedFromRoleId === selectedRoleId()).length
        ),

        /** True when the selected role is a system role (locked / immutable). */
        isSystemRole: computed(() => selectedRole()?.system ?? false)
    })),

    withMethods((store) => {
        const service = inject(DotRolesPortletService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        const loadRootRoles = rxMethod<void>(
            pipe(
                tap(() => patchState(store, { status: 'loading', error: null })),
                switchMap(() =>
                    service.loadRootRoles(true).pipe(
                        tap((rootRoles) => {
                            patchState(store, { rootRoles, status: 'loaded' });
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

        const loadRoleMembers = rxMethod<string>(
            pipe(
                tap(() => patchState(store, { membersStatus: 'loading' })),
                switchMap((roleId) =>
                    service.loadRoleMembers(roleId).pipe(
                        tap((members) => {
                            patchState(store, {
                                members,
                                membersStatus: 'loaded'
                            });
                        }),
                        catchError((error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { membersStatus: 'error' });

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
            loadRoleMembers,
            loadRoleDetail,

            /** Update the free-text filter applied to the tree. */
            setFilter(filter: string): void {
                patchState(store, { filter });
            },

            /** Select a role and load its detail + members. */
            selectRole(roleId: string | null): void {
                patchState(store, {
                    selectedRoleId: roleId,
                    selectedMembers: []
                });

                if (roleId) {
                    loadRoleDetail(roleId);
                    loadRoleMembers(roleId);
                }
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
             */
            async createRole(form: DotRoleFormValue): Promise<DotRoleDetail | null> {
                try {
                    const created = await new Promise<DotRoleDetail>((resolve, reject) => {
                        service.createRole(form).subscribe({
                            next: (role) => resolve(role),
                            error: (err) => reject(err)
                        });
                    });
                    // Reload the tree so the new node appears; select it.
                    loadRootRoles();
                    patchState(store, { selectedRoleId: created.id });

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
            // Components must render the corresponding UI in a disabled /
            // "coming soon" state until those endpoints land.
        };
    })
);

function filterTree(nodes: DotRoleNode[], query: string): DotRoleNode[] {
    return nodes.reduce<DotRoleNode[]>((acc, node) => {
        const matches = node.name.toLowerCase().includes(query);
        const filteredChildren = node.children ? filterTree(node.children, query) : [];

        if (matches || filteredChildren.length > 0) {
            acc.push({
                ...node,
                children: filteredChildren.length > 0 ? filteredChildren : node.children
            });
        }

        return acc;
    }, []);
}
