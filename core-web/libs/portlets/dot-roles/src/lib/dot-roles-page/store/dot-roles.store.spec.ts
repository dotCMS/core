import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotRolesStore } from './dot-roles.store';

import {
    DotRoleDetail,
    DotRoleFormValue,
    DotRoleMember,
    DotRoleNode
} from '../../models/dot-roles.models';
import { DotRolesPortletService } from '../../services/dot-roles-portlet.service';

const MOCK_ROOT_ROLES: DotRoleNode[] = [
    {
        id: 'r-categories',
        name: 'Categories',
        children: [
            { id: 'r-eco', name: 'Eco Role', parent: 'r-categories', userCount: 2 },
            { id: 'r-snow', name: 'Snow Role', parent: 'r-categories', userCount: 0 }
        ]
    },
    { id: 'r-system', name: 'System', system: true }
];

const MOCK_ROLE_DETAIL: DotRoleDetail = {
    id: 'r-eco',
    name: 'Eco Role',
    parent: 'r-categories',
    description: 'Eco team',
    editUsers: true
};

const MOCK_MEMBERS: DotRoleMember[] = [
    {
        userId: 'u-1',
        firstName: 'Alan',
        lastName: 'Cruz',
        emailAddress: 'alan.cruz@dotcms.com',
        grantedFromRoleId: 'r-eco',
        grantedFromRoleName: 'Eco Role'
    },
    {
        userId: 'u-2',
        firstName: 'Elena',
        lastName: 'Petrov',
        emailAddress: 'elena.p@dotcms.com',
        grantedFromRoleId: 'r-categories',
        grantedFromRoleName: 'Categories'
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
                loadRootRoles: jest.fn().mockReturnValue(of(MOCK_ROOT_ROLES)),
                loadRoleById: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL)),
                loadRoleMembers: jest.fn().mockReturnValue(of(MOCK_MEMBERS)),
                createRole: jest.fn().mockReturnValue(of(MOCK_ROLE_DETAIL))
            }),
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotRolesPortletService) as jest.Mocked<DotRolesPortletService>;
        jest.clearAllMocks();
        service.loadRootRoles.mockReturnValue(of(MOCK_ROOT_ROLES));
        service.loadRoleById.mockReturnValue(of(MOCK_ROLE_DETAIL));
        service.loadRoleMembers.mockReturnValue(of(MOCK_MEMBERS));
        service.createRole.mockReturnValue(of(MOCK_ROLE_DETAIL));
    });

    describe('initial state', () => {
        it('should start with empty rootRoles + init status', () => {
            expect(store.rootRoles()).toEqual([]);
            expect(store.status()).toBe('init');
            expect(store.selectedRoleId()).toBeNull();
            expect(store.activeTab()).toBe('users');
            expect(store.filter()).toBe('');
        });
    });

    describe('loadRootRoles', () => {
        it('should populate rootRoles and set status to loaded', () => {
            store.loadRootRoles();

            expect(service.loadRootRoles).toHaveBeenCalledWith(true);
            expect(store.rootRoles()).toEqual(MOCK_ROOT_ROLES);
            expect(store.status()).toBe('loaded');
        });

        it('should set status to error and delegate to httpErrorManager on failure', () => {
            const errorManager = spectator.inject(DotHttpErrorManagerService);
            service.loadRootRoles.mockReturnValueOnce(throwError(() => new Error('boom')));

            store.loadRootRoles();

            expect(store.status()).toBe('error');
            expect(errorManager.handle).toHaveBeenCalled();
        });
    });

    describe('selectRole', () => {
        it('should set selectedRoleId and trigger detail + members load', () => {
            store.selectRole('r-eco');

            expect(store.selectedRoleId()).toBe('r-eco');
            expect(service.loadRoleById).toHaveBeenCalledWith('r-eco', true);
            expect(service.loadRoleMembers).toHaveBeenCalledWith('r-eco');
            expect(store.selectedRole()).toEqual(MOCK_ROLE_DETAIL);
            expect(store.members()).toEqual(MOCK_MEMBERS);
        });

        it('should clear selection without loading when passed null', () => {
            store.selectRole('r-eco');
            jest.clearAllMocks();

            store.selectRole(null);

            expect(store.selectedRoleId()).toBeNull();
            expect(service.loadRoleById).not.toHaveBeenCalled();
            expect(service.loadRoleMembers).not.toHaveBeenCalled();
        });

        it('should clear selectedMembers when switching roles', () => {
            store.selectRole('r-eco');
            store.setSelectedMembers([MOCK_MEMBERS[0]]);
            expect(store.selectedMembers()).toHaveLength(1);

            store.selectRole('r-snow');

            expect(store.selectedMembers()).toEqual([]);
        });
    });

    describe('setFilter and filteredRoles', () => {
        beforeEach(() => {
            store.loadRootRoles();
        });

        it('should return the full tree when the filter is empty', () => {
            expect(store.filteredRoles()).toEqual(MOCK_ROOT_ROLES);
        });

        it('should keep parent nodes whose children match the filter', () => {
            store.setFilter('eco');

            const filtered = store.filteredRoles();
            expect(filtered).toHaveLength(1);
            expect(filtered[0].id).toBe('r-categories');
            expect(filtered[0].children).toHaveLength(1);
            expect(filtered[0].children?.[0].id).toBe('r-eco');
        });

        it('should keep leaf nodes matching the filter directly', () => {
            store.setFilter('system');

            const filtered = store.filteredRoles();
            expect(filtered).toHaveLength(1);
            expect(filtered[0].id).toBe('r-system');
        });

        it('should return no nodes when nothing matches', () => {
            store.setFilter('nomatch');

            expect(store.filteredRoles()).toEqual([]);
        });
    });

    describe('computed member counts', () => {
        beforeEach(() => {
            store.selectRole('r-eco');
        });

        it('should compute total memberCount', () => {
            expect(store.memberCount()).toBe(2);
        });

        it('should compute directMemberCount from grantedFromRoleId matching selected role', () => {
            // 1 direct (u-1 on r-eco), 1 inherited (u-2 from r-categories)
            expect(store.directMemberCount()).toBe(1);
        });
    });

    describe('isSystemRole', () => {
        it('should be true when the selected role is a system role', () => {
            service.loadRoleById.mockReturnValueOnce(of({ ...MOCK_ROLE_DETAIL, system: true }));

            store.selectRole('r-system');

            expect(store.isSystemRole()).toBe(true);
        });

        it('should be false when the selected role is not a system role', () => {
            store.selectRole('r-eco');

            expect(store.isSystemRole()).toBe(false);
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

        it('should POST, reload tree, select the new role, and return the created detail', async () => {
            const created = await store.createRole(FORM);

            expect(service.createRole).toHaveBeenCalledWith(FORM);
            expect(service.loadRootRoles).toHaveBeenCalled();
            expect(store.selectedRoleId()).toBe(MOCK_ROLE_DETAIL.id);
            expect(created).toEqual(MOCK_ROLE_DETAIL);
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
