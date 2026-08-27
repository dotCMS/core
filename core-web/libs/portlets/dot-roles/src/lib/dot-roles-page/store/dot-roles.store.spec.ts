import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { NEVER, of, throwError } from 'rxjs';

import {
    DotHttpErrorManagerService,
    DotRolesService,
    DotRoleUserResult
} from '@dotcms/data-access';

import { DotRolesStore } from './dot-roles.store';

import { DotRoleDetail, DotRoleFormValue, DotRoleNode } from '../../models/dot-roles.models';

const SELECTED_ROLE = { id: 'r-eco' };

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

const MOCK_USER_FILTER_RESULTS: DotRoleUserResult[] = [
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
    let service: jest.Mocked<DotRolesService>;

    const createService = createServiceFactory({
        service: DotRolesStore,
        providers: [
            mockProvider(DotRolesService, {
                getRoots: jest.fn().mockReturnValue(of(MOCK_NESTED_ROLES)),
                getById: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                getUsers: jest.fn().mockReturnValue(of(MOCK_USER_FILTER_RESULTS)),
                create: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                update: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                delete: jest
                    .fn()
                    .mockReturnValue(of({ deleted: true, roleId: 'r-eco', usersAffected: 0 })),
                grantUser: jest.fn().mockReturnValue(
                    of({
                        granted: true,
                        roleId: 'r-eco',
                        user: { userId: 'u-1' }
                    })
                ),
                removeUsers: jest
                    .fn()
                    .mockReturnValue(of({ removedUserIds: ['u-1'], skipped: [] })),
                searchTree: jest.fn().mockReturnValue(of([])),
                getAllToolGroups: jest.fn().mockReturnValue(of([])),
                getToolGroups: jest.fn().mockReturnValue(of([])),
                saveToolGroups: jest.fn().mockReturnValue(of({}))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotRolesService) as jest.Mocked<DotRolesService>;
        jest.clearAllMocks();
        service.getRoots.mockReturnValue(of(MOCK_NESTED_ROLES));
        service.getById.mockReturnValue(of(MOCK_ROLE_DETAIL));
        service.getUsers.mockReturnValue(of(MOCK_USER_FILTER_RESULTS));
        service.create.mockReturnValue(of(MOCK_ROLE_DETAIL));
        service.update.mockReturnValue(of(MOCK_ROLE_DETAIL));
        service.delete.mockReturnValue(of({ deleted: true, roleId: 'r-eco', usersAffected: 0 }));
        service.grantUser.mockReturnValue(
            of({ granted: true, roleId: 'r-eco', user: { userId: 'u-1' } })
        );
        service.removeUsers.mockReturnValue(of({ removedUserIds: ['u-1'], skipped: [] }));
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

            expect(service.getRoots).toHaveBeenCalledWith(true);
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
            service.getRoots.mockReturnValueOnce(throwError(() => new Error('boom')));

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
            service.getById.mockReturnValueOnce(
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
            expect(service.getById).toHaveBeenCalledWith('r-eco', true);
            expect(store.selectedRole()).toEqual(MOCK_ROLE_DETAIL);
        });

        it('should clear selection without loading when passed null', () => {
            store.selectRole('r-eco');
            jest.clearAllMocks();

            store.selectRole(null);

            expect(store.selectedRoleId()).toBeNull();
            expect(service.getById).not.toHaveBeenCalled();
        });

        it('should clear members when switching roles', () => {
            store.loadMembers(SELECTED_ROLE);

            store.selectRole('r-snow');

            expect(store.members()).toEqual([]);
        });
    });

    describe('loadMembers', () => {
        it('should load members by role id, with no roleKey branching (#37070)', () => {
            store.loadMembers(SELECTED_ROLE);

            expect(service.getUsers).toHaveBeenCalledWith('r-eco');
            expect(store.members()).toHaveLength(2);
            expect(store.membersStatus()).toBe('LOADED');
        });

        it('should carry the email through, so no member row renders blank', () => {
            store.loadMembers(SELECTED_ROLE);

            expect(store.members().every((m) => m.emailAddress !== '')).toBe(true);
        });

        it('should walk the ancestor chain, tag each user with grantedFrom, and prefer the closest ancestor on duplicates', () => {
            store.loadRootRoles();
            // r-eco lives under r-categories in MOCK_NESTED_ROLES, so the
            // chain expected here is [r-eco, r-categories].
            service.getUsers.mockImplementation((roleId: string) => {
                if (roleId === 'r-eco') {
                    return of([
                        { userId: 'u-1', firstName: 'Alan', lastName: 'Cruz', emailAddress: 'a@x' }
                    ]);
                }
                if (roleId === 'r-categories') {
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
            store.loadMembers({ id: 'r-eco' });

            expect(service.getUsers).toHaveBeenCalledWith('r-eco');
            expect(service.getUsers).toHaveBeenCalledWith('r-categories');

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
            service.getUsers.mockReturnValueOnce(throwError(() => new Error('boom')));

            store.loadMembers(SELECTED_ROLE);

            // Partial failure of one ancestor call must not nuke the whole
            // tab: the store surfaces the error via the http manager but
            // still resolves to `loaded` with whatever succeeded.
            expect(errorManager.handle).toHaveBeenCalled();
            expect(store.membersStatus()).toBe('LOADED');
        });
    });

    describe('edit gates match the backend contract', () => {
        const select = (role: Partial<DotRoleDetail>) => {
            service.getById.mockReturnValue(
                of({ id: 'r-x', name: 'Role X', ...role } as DotRoleDetail)
            );
            store.selectRole('r-x');
        };

        it('allows user + tool edits on a system role — CMS Administrator is one', () => {
            // RoleHelper gates grants on `editUsers` only, and layouts on
            // nothing at all. Blocking system roles here made the Beta stricter
            // than both the backend and the legacy portlet.
            select({ system: true, locked: true, editUsers: true, editLayouts: true });

            expect(store.canEditRoleUsers()).toBe(true);
            expect(store.canEditRoleLayouts()).toBe(true);
        });

        it('still blocks updating or deleting a system role', () => {
            select({ system: true, editUsers: true, editLayouts: true });

            expect(store.canModifyRole()).toBe(false);
        });

        it('honours the per-domain flags when they are false', () => {
            select({ editUsers: false, editLayouts: false });

            expect(store.canEditRoleUsers()).toBe(false);
            expect(store.canEditRoleLayouts()).toBe(false);
        });

        it('treats an absent flag as permissive', () => {
            select({});

            expect(store.canEditRoleUsers()).toBe(true);
            expect(store.canEditRoleLayouts()).toBe(true);
        });
    });

    describe('saveToolGroups', () => {
        const TOOL_GROUPS = [
            { id: 'tg-1', name: 'Site' },
            { id: 'tg-2', name: 'Content' }
        ];

        beforeEach(() => {
            store.loadRootRoles();
            service.getToolGroups.mockReturnValue(of([TOOL_GROUPS[0]]));
            service.getAllToolGroups.mockReturnValue(of(TOOL_GROUPS));
            store.selectRole('r-eco');
        });

        it('never flips the table to LOADING while saving — that is the flicker', async () => {
            const seen: string[] = [];
            const stop = setInterval(() => seen.push(store.toolGroupsStatus()), 0);

            await store.saveToolGroups(['tg-1', 'tg-2']);
            clearInterval(stop);

            expect(store.toolGroupsStatus()).toBe('LOADED');
            expect(seen).not.toContain('LOADING');
        });

        it('paints the toggle optimistically, before the request resolves', () => {
            // Never-resolving POST: whatever the grid shows now is the
            // optimistic patch, not a server round-trip.
            service.saveToolGroups.mockReturnValue(NEVER);

            store.saveToolGroups(['tg-1', 'tg-2']);

            const granted = store.toolGroups().filter((group) => group.granted);
            expect(granted.map((group) => group.id)).toEqual(['tg-1', 'tg-2']);
            expect(store.toolGroupsSaving()).toBe(true);
        });

        it('rolls the optimistic patch back when the save fails', async () => {
            const before = store.toolGroups();
            service.saveToolGroups.mockReturnValue(throwError(() => new Error('boom')));

            const ok = await store.saveToolGroups(['tg-1', 'tg-2']);

            expect(ok).toBe(false);
            expect(store.toolGroups()).toEqual(before);
            expect(store.toolGroupsSaving()).toBe(false);
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
            const searchSpy = jest.spyOn(service, 'searchTree');
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
            (service.searchTree as jest.Mock).mockReturnValueOnce(of(matchedTree));

            store.setFilter('eco');

            expect(service.searchTree).toHaveBeenCalledWith('eco');
            expect(store.isSearching()).toBe(true);
            expect(store.filteredRoles()).toEqual(matchedTree);
            expect(store.searchStatus()).toBe('LOADED');
        });

        it('should return an empty result when the search returns nothing', () => {
            (service.searchTree as jest.Mock).mockReturnValueOnce(of([]));

            store.setFilter('nomatch');

            expect(store.filteredRoles()).toEqual([]);
            expect(store.isSearching()).toBe(true);
        });

        it('should reset to the full tree when the filter is cleared', () => {
            (service.searchTree as jest.Mock).mockReturnValueOnce(of([{ id: 'x', name: 'x' }]));
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
            store.loadMembers(SELECTED_ROLE);
        });

        it('should compute total memberCount', () => {
            expect(store.memberCount()).toBe(2);
        });
    });

    describe('selectedRoleIsParent and isSystemRole', () => {
        it('should mark the selected role as parent when the detail has roleChildren', () => {
            service.getById.mockReturnValueOnce(
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
            service.getById.mockReturnValueOnce(of({ ...MOCK_ROLE_DETAIL, system: true }));

            store.selectRole('r-system');

            expect(store.isSystemRole()).toBe(true);
        });
    });

    describe('canGrantUsers', () => {
        it('should reflect the editUsers flag from the selected role', () => {
            service.getById.mockReturnValueOnce(of({ ...MOCK_ROLE_DETAIL, editUsers: false }));

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
            service.create.mockReturnValue(
                of({ id: 'r-new-root', name: 'New Root', roleKey: 'new-root' })
            );

            const created = await store.createRole(FORM);

            expect(service.create).toHaveBeenCalledWith(FORM);
            // Local splice — no full reload.
            expect(service.getRoots).not.toHaveBeenCalled();
            expect(store.roles().map((n) => n.id)).toContain('r-new-root');
            expect(store.selectedRoleId()).toBe('r-new-root');
            expect(created?.id).toBe('r-new-root');
        });

        it('should splice into the parent roleChildren when the parent is loaded', async () => {
            store.loadRootRoles();
            jest.clearAllMocks();
            service.create.mockReturnValue(
                of({
                    id: 'r-eco-child',
                    name: 'Eco Child',
                    parent: 'r-eco',
                    roleKey: 'eco-child'
                })
            );

            await store.createRole({ ...FORM, parentRoleId: 'r-eco' });

            // Full reload not triggered. The POST response is hydrated so
            // the store also skips the follow-up `getById(created.id)`
            // that used to fire — see the "seed selectedRole directly"
            // comment in `create`.
            expect(service.getRoots).not.toHaveBeenCalled();
            expect(service.getById).not.toHaveBeenCalled();
            const categories = store.roles().find((n) => n.id === 'r-categories');
            const eco = categories?.roleChildren?.find((n) => n.id === 'r-eco');
            expect(eco?.roleChildren?.map((n) => n.id)).toContain('r-eco-child');
        });

        it('should refresh just the parent subtree when the parent is not loaded', async () => {
            store.loadRootRoles();
            jest.clearAllMocks();
            service.create.mockReturnValue(
                of({ id: 'r-deep', name: 'Deep', parent: 'r-unloaded', roleKey: 'deep' })
            );
            service.getById.mockReturnValueOnce(
                of({
                    id: 'r-unloaded',
                    name: 'Unloaded Parent',
                    roleChildren: [{ id: 'r-deep', name: 'Deep', roleChildren: [] }]
                })
            );

            await store.createRole({ ...FORM, parentRoleId: 'r-unloaded' });

            expect(service.getRoots).not.toHaveBeenCalled();
            // One call for the parent subtree refresh + one for the selected role detail.
            expect(service.getById).toHaveBeenCalledWith('r-unloaded', true);
        });

        it('should delegate to httpErrorManager and return null on failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.create.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.createRole(FORM);

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });
    });

    describe('updateRole', () => {
        it('should delegate to httpErrorManager and return null on failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.update.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.updateRole('r-eco', {
                roleName: 'Eco',
                roleKey: 'eco',
                parentRoleId: null,
                canEditUsers: true,
                canEditPermissions: true,
                canEditLayouts: true,
                description: ''
            });

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });
    });

    describe('deleteRole', () => {
        beforeEach(() => {
            store.loadRootRoles();
        });

        it('should delegate to httpErrorManager and return null on HTTP failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.delete.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.deleteRole('r-eco');

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });

        it('should leave the tree untouched when the response reports deleted:false', async () => {
            service.delete.mockReturnValueOnce(
                of({ deleted: false, roleId: 'r-eco', usersAffected: 0 })
            );

            const result = await store.deleteRole('r-eco');

            const categories = store.roles().find((n) => n.id === 'r-categories');
            expect(categories?.roleChildren?.map((c) => c.id)).toContain('r-eco');
            expect(result?.deleted).toBe(false);
        });
    });

    describe('grantUserToRole', () => {
        beforeEach(() => {
            store.loadRootRoles();
            store.selectRole('r-eco');
        });

        it('should delegate to httpErrorManager and return null on failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.grantUser.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.grantUserToRole('u-1');

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });

        it('should return null without hitting the endpoint when no role is selected', async () => {
            store.selectRole(null);

            const result = await store.grantUserToRole('u-1');

            expect(service.grantUser).not.toHaveBeenCalled();
            expect(result).toBeNull();
        });
    });

    describe('removeUsersFromRole', () => {
        beforeEach(() => {
            store.loadRootRoles();
            store.selectRole('r-eco');
        });

        it('should delegate to httpErrorManager and return null on failure', async () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.removeUsers.mockReturnValueOnce(throwError(() => new Error('boom')));

            const result = await store.removeUsersFromRole(['u-1']);

            expect(errorManager.handle).toHaveBeenCalled();
            expect(result).toBeNull();
        });

        it('should short-circuit and return null when userIds is empty', async () => {
            const result = await store.removeUsersFromRole([]);

            expect(service.removeUsers).not.toHaveBeenCalled();
            expect(result).toBeNull();
        });
    });
});
