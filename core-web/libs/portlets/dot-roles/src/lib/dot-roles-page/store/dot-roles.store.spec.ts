import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotRolesStore } from './dot-roles.store';

import { DotRoleDetail, DotRoleFormValue, DotRoleNode } from '../../models/dot-roles.models';
import {
    DotRolesPortletService,
    DotRoleUserFilterResult
} from '../../services/dot-roles-portlet.service';

const ROLE_BY_KEY = { id: 'r-eco', roleKey: 'eco' };
const ROLE_BY_ID = { id: 'r-nokey', roleKey: null };

/**
 * Nested shape returned by `GET /v1/roles?loadChildrenRoles=true` — matches
 * the real `RoleView` wire format (openapi.json misses the `roleChildren`
 * field, but the Java class exposes it).
 */
const MOCK_NESTED_ROLES: DotRoleNode[] = [
    {
        id: 'r-categories',
        name: 'Categories',
        roleKey: 'Categories',
        roleChildren: [
            {
                id: 'r-eco',
                name: 'Eco Role',
                parent: 'r-categories',
                roleKey: 'eco',
                roleChildren: []
            },
            {
                id: 'r-snow',
                name: 'Snow Role',
                parent: 'r-categories',
                roleKey: 'snow',
                roleChildren: []
            }
        ]
    },
    { id: 'r-system', name: 'System', system: true, roleKey: 'system', roleChildren: [] }
];

const MOCK_ROLE_DETAIL: DotRoleDetail = {
    id: 'r-eco',
    name: 'Eco Role',
    parent: 'r-categories',
    roleKey: 'eco',
    description: 'Eco team',
    editUsers: true
};

const MOCK_USER_FILTER_RESULTS: DotRoleUserFilterResult[] = [
    {
        userId: 'u-1',
        firstName: 'Alan',
        lastName: 'Cruz',
        emailAddress: 'alan.cruz@dotcms.com'
    },
    {
        userId: 'u-2',
        firstName: 'Elena',
        lastName: 'Petrov',
        emailAddress: 'elena.p@dotcms.com'
    }
];

describe('DotRolesStore', () => {
    let spectator: SpectatorService<InstanceType<typeof DotRolesStore>>;
    let store: InstanceType<typeof DotRolesStore>;
    let service: jest.Mocked<DotRolesPortletService>;

    const createService = createServiceFactory({
        service: DotRolesStore,
        providers: [
            mockProvider(DotRolesPortletService, {
                loadRootRoles: jest.fn().mockReturnValue(of(MOCK_NESTED_ROLES)),
                loadRoleById: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                loadRoleMembersByKey: jest.fn().mockReturnValue(of(MOCK_USER_FILTER_RESULTS)),
                loadRoleMembersById: jest.fn().mockReturnValue(of(MOCK_USER_FILTER_RESULTS)),
                createRole: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                searchRoles: jest.fn().mockReturnValue(of([]))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotRolesPortletService) as jest.Mocked<DotRolesPortletService>;
        jest.clearAllMocks();
        service.loadRootRoles.mockReturnValue(of(MOCK_NESTED_ROLES));
        service.loadRoleById.mockReturnValue(of(MOCK_ROLE_DETAIL));
        service.loadRoleMembersByKey.mockReturnValue(of(MOCK_USER_FILTER_RESULTS));
        service.loadRoleMembersById.mockReturnValue(of(MOCK_USER_FILTER_RESULTS));
        service.createRole.mockReturnValue(of(MOCK_ROLE_DETAIL));
    });

    describe('initial state', () => {
        it('should start with an empty roles list + init status', () => {
            expect(store.roles()).toEqual([]);
            expect(store.roleTree()).toEqual([]);
            expect(store.status()).toBe('INIT');
            expect(store.selectedRoleId()).toBeNull();
            expect(store.activeTab()).toBe('users');
            expect(store.filter()).toBe('');
        });
    });

    describe('loadRootRoles', () => {
        it('should populate roles and set status to loaded', () => {
            store.loadRootRoles();

            expect(service.loadRootRoles).toHaveBeenCalledWith(true);
            expect(store.roles()).toEqual(MOCK_NESTED_ROLES);
            expect(store.status()).toBe('LOADED');
        });

        it('should expose the nested response verbatim via roleTree', () => {
            store.loadRootRoles();

            const tree = store.roleTree();
            expect(tree.map((n) => n.id)).toEqual(['r-categories', 'r-system']);
            const categories = tree.find((n) => n.id === 'r-categories');
            expect(categories?.roleChildren?.map((c) => c.id)).toEqual(['r-eco', 'r-snow']);
        });

        it('should set status to error and delegate to httpErrorManager on failure', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.loadRootRoles.mockReturnValueOnce(throwError(() => new Error('boom')));

            store.loadRootRoles();

            expect(store.status()).toBe('ERROR');
            expect(errorManager.handle).toHaveBeenCalled();
        });
    });

    describe('loadRoleChildren (lazy-load on expand)', () => {
        beforeEach(() => {
            store.loadRootRoles();
        });

        it('should splice fetched children into the state tree', async () => {
            service.loadRoleById.mockReturnValueOnce(
                of({
                    id: 'r-eco',
                    name: 'Eco Role',
                    parent: 'r-categories',
                    roleChildren: [
                        {
                            id: 'r-eco-child',
                            name: 'Eco Child',
                            parent: 'r-eco',
                            roleChildren: []
                        }
                    ]
                })
            );

            await store.loadRoleChildren('r-eco');

            const categories = store.roles().find((n) => n.id === 'r-categories');
            const eco = categories?.roleChildren?.find((n) => n.id === 'r-eco');
            expect(eco?.roleChildren?.map((n) => n.id)).toEqual(['r-eco-child']);
        });
    });

    describe('selectRole', () => {
        it('should set selectedRoleId and load the role detail', () => {
            store.selectRole('r-eco');

            expect(store.selectedRoleId()).toBe('r-eco');
            expect(service.loadRoleById).toHaveBeenCalledWith('r-eco', true);
            expect(store.selectedRole()).toEqual(MOCK_ROLE_DETAIL);
        });

        it('should clear selection without loading when passed null', () => {
            store.selectRole('r-eco');
            jest.clearAllMocks();

            store.selectRole(null);

            expect(store.selectedRoleId()).toBeNull();
            expect(service.loadRoleById).not.toHaveBeenCalled();
        });

        it('should clear members when switching roles', () => {
            store.loadMembers(ROLE_BY_KEY);

            store.selectRole('r-snow');

            expect(store.members()).toEqual([]);
        });
    });

    describe('loadMembers', () => {
        it('should use the roleKey path when the selected role has a roleKey', () => {
            store.loadMembers(ROLE_BY_KEY);

            expect(service.loadRoleMembersByKey).toHaveBeenCalledWith('eco');
            expect(service.loadRoleMembersById).not.toHaveBeenCalled();
            expect(store.members()).toHaveLength(2);
            expect(store.membersStatus()).toBe('LOADED');
        });

        it('should fall back to the roleId path when the role has no roleKey', () => {
            store.loadMembers(ROLE_BY_ID);

            expect(service.loadRoleMembersById).toHaveBeenCalledWith('r-nokey');
            expect(service.loadRoleMembersByKey).not.toHaveBeenCalled();
            expect(store.members()).toHaveLength(2);
        });

        it('should walk the ancestor chain, tag each user with grantedFrom, and prefer the closest ancestor on duplicates', () => {
            store.loadRootRoles();
            // r-eco lives under r-categories in MOCK_NESTED_ROLES, so the
            // chain expected here is [r-eco, r-categories].
            service.loadRoleMembersByKey.mockImplementation((roleKey: string) => {
                if (roleKey === 'eco') {
                    return of([
                        { userId: 'u-1', firstName: 'Alan', lastName: 'Cruz', emailAddress: 'a@x' }
                    ]);
                }
                if (roleKey === 'Categories') {
                    return of([
                        {
                            userId: 'u-2',
                            firstName: 'Chris',
                            lastName: 'Publisher',
                            emailAddress: 'c@x'
                        },
                        // u-1 is granted at BOTH r-eco and r-categories — the
                        // direct grant on r-eco must win over the inherited one.
                        { userId: 'u-1', firstName: 'Alan', lastName: 'Cruz', emailAddress: 'a@x' }
                    ]);
                }

                return of([]);
            });

            store.selectRole('r-eco');
            store.loadMembers({ id: 'r-eco', roleKey: 'eco' });

            expect(service.loadRoleMembersByKey).toHaveBeenCalledWith('eco');
            expect(service.loadRoleMembersByKey).toHaveBeenCalledWith('Categories');

            const members = store.members();
            expect(members).toHaveLength(2);

            const alan = members.find((m) => m.userId === 'u-1');
            const chris = members.find((m) => m.userId === 'u-2');
            expect(alan?.grantedFromRoleId).toBe('r-eco');
            expect(alan?.grantedFromRoleName).toBe('Eco Role');
            expect(chris?.grantedFromRoleId).toBe('r-categories');
            expect(chris?.grantedFromRoleName).toBe('Categories');
        });

        it('should delegate to httpErrorManager and continue when one ancestor call fails', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.loadRoleMembersByKey.mockReturnValueOnce(throwError(() => new Error('boom')));

            store.loadMembers(ROLE_BY_KEY);

            // Partial failure of one ancestor call must not nuke the whole
            // tab: the store surfaces the error via the http manager but
            // still resolves to `loaded` with whatever succeeded.
            expect(errorManager.handle).toHaveBeenCalled();
            expect(store.membersStatus()).toBe('LOADED');
        });
    });

    describe('setFilter and filteredRoles', () => {
        beforeEach(() => {
            store.loadRootRoles();
        });

        it('should return the full tree when the filter is empty', () => {
            const filtered = store.filteredRoles();
            expect(filtered.map((n) => n.id)).toEqual(['r-categories', 'r-system']);
            expect(store.isSearching()).toBe(false);
        });

        it('should NOT trigger server search for queries under 3 chars', () => {
            const searchSpy = jest.spyOn(service, 'searchRoles');
            store.setFilter('ec');

            expect(searchSpy).not.toHaveBeenCalled();
            expect(store.isSearching()).toBe(false);
            // falls back to the full `roles` cache
            expect(store.filteredRoles().map((n) => n.id)).toEqual(['r-categories', 'r-system']);
        });

        it('should trigger server search and replace filteredRoles for 3+ chars', () => {
            const matchedTree: DotRoleNode[] = [
                {
                    id: 'r-categories',
                    name: 'Categories',
                    roleChildren: [{ id: 'r-eco', name: 'Eco Role', roleChildren: [] }]
                }
            ];
            (service.searchRoles as jest.Mock).mockReturnValueOnce(of(matchedTree));

            store.setFilter('eco');

            expect(service.searchRoles).toHaveBeenCalledWith('eco');
            expect(store.isSearching()).toBe(true);
            expect(store.filteredRoles()).toEqual(matchedTree);
            expect(store.searchStatus()).toBe('LOADED');
        });

        it('should return an empty result when the search returns nothing', () => {
            (service.searchRoles as jest.Mock).mockReturnValueOnce(of([]));

            store.setFilter('nomatch');

            expect(store.filteredRoles()).toEqual([]);
            expect(store.isSearching()).toBe(true);
        });

        it('should reset to the full tree when the filter is cleared', () => {
            (service.searchRoles as jest.Mock).mockReturnValueOnce(of([{ id: 'x', name: 'x' }]));
            store.setFilter('anything');
            expect(store.isSearching()).toBe(true);

            store.setFilter('');

            expect(store.isSearching()).toBe(false);
            expect(store.filteredRoles().map((n) => n.id)).toEqual(['r-categories', 'r-system']);
        });
    });

    describe('member count computeds', () => {
        beforeEach(() => {
            store.selectRole('r-eco');
            store.loadMembers(ROLE_BY_KEY);
        });

        it('should compute total memberCount', () => {
            expect(store.memberCount()).toBe(2);
        });
    });

    describe('selectedRoleIsParent and isSystemRole', () => {
        it('should mark the selected role as parent when the detail has roleChildren', () => {
            service.loadRoleById.mockReturnValueOnce(
                of({
                    ...MOCK_ROLE_DETAIL,
                    roleChildren: [{ id: 'child', name: 'child' }]
                })
            );

            store.selectRole('r-eco');

            expect(store.selectedRoleIsParent()).toBe(true);
        });

        it('should mark the selected role as leaf when the detail has no roleChildren', () => {
            store.selectRole('r-eco');

            expect(store.selectedRoleIsParent()).toBe(false);
        });

        it('isSystemRole should be true only for system roles', () => {
            service.loadRoleById.mockReturnValueOnce(of({ ...MOCK_ROLE_DETAIL, system: true }));

            store.selectRole('r-system');

            expect(store.isSystemRole()).toBe(true);
        });
    });

    describe('canGrantUsers', () => {
        it('should reflect the editUsers flag from the selected role', () => {
            service.loadRoleById.mockReturnValueOnce(of({ ...MOCK_ROLE_DETAIL, editUsers: false }));

            store.selectRole('r-system');

            expect(store.canGrantUsers()).toBe(false);
        });

        it('should default to true when no role is selected', () => {
            expect(store.canGrantUsers()).toBe(true);
        });
    });

    describe('setActiveTab', () => {
        it('should update active tab', () => {
            store.setActiveTab('permissions');
            expect(store.activeTab()).toBe('permissions');

            store.setActiveTab('tools');
            expect(store.activeTab()).toBe('tools');
        });
    });

    describe('createRole', () => {
        const FORM: DotRoleFormValue = {
            roleName: 'New Role',
            roleKey: '',
            parentRoleId: null,
            canEditUsers: true,
            canEditPermissions: true,
            canEditLayouts: true,
            description: ''
        };

        it('should POST, append the created role to the roots when parentRoleId is null', async () => {
            store.loadRootRoles();
            jest.clearAllMocks();
            service.createRole.mockReturnValue(
                of({ id: 'r-new-root', name: 'New Root', roleKey: 'new-root' })
            );

            const created = await store.createRole(FORM);

            expect(service.createRole).toHaveBeenCalledWith(FORM);
            // Local splice — no full reload.
            expect(service.loadRootRoles).not.toHaveBeenCalled();
            expect(store.roles().map((n) => n.id)).toContain('r-new-root');
            expect(store.selectedRoleId()).toBe('r-new-root');
            expect(created?.id).toBe('r-new-root');
        });

        it('should splice into the parent roleChildren when the parent is loaded', async () => {
            store.loadRootRoles();
            jest.clearAllMocks();
            service.createRole.mockReturnValue(
                of({
                    id: 'r-eco-child',
                    name: 'Eco Child',
                    parent: 'r-eco',
                    roleKey: 'eco-child'
                })
            );

            await store.createRole({ ...FORM, parentRoleId: 'r-eco' });

            // Full reload not triggered, targeted fetch not triggered either.
            expect(service.loadRootRoles).not.toHaveBeenCalled();
            expect(service.loadRoleById).toHaveBeenCalledTimes(1);
            const categories = store.roles().find((n) => n.id === 'r-categories');
            const eco = categories?.roleChildren?.find((n) => n.id === 'r-eco');
            expect(eco?.roleChildren?.map((n) => n.id)).toContain('r-eco-child');
        });

        it('should refresh just the parent subtree when the parent is not loaded', async () => {
            store.loadRootRoles();
            jest.clearAllMocks();
            service.createRole.mockReturnValue(
                of({ id: 'r-deep', name: 'Deep', parent: 'r-unloaded', roleKey: 'deep' })
            );
            service.loadRoleById.mockReturnValueOnce(
                of({
                    id: 'r-unloaded',
                    name: 'Unloaded Parent',
                    roleChildren: [{ id: 'r-deep', name: 'Deep', roleChildren: [] }]
                })
            );

            await store.createRole({ ...FORM, parentRoleId: 'r-unloaded' });

            expect(service.loadRootRoles).not.toHaveBeenCalled();
            // One call for the parent subtree refresh + one for the selected role detail.
            expect(service.loadRoleById).toHaveBeenCalledWith('r-unloaded', true);
        });

        it('should delegate to httpErrorManager and return null on failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.createRole.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.createRole(FORM);

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });
    });
});
