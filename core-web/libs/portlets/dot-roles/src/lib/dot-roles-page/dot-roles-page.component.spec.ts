import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotHttpErrorManagerService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesPageComponent } from './dot-roles-page.component';
import { DotRolesStore } from './store/dot-roles.store';

import { DotRolesPortletService } from '../services/dot-roles-portlet.service';

const MESSAGES = {
    'roles.detail.empty': 'Select a role',
    'roles.tab.users': 'Users',
    'roles.tab.permissions': 'Permissions',
    'roles.tab.tools': 'Tools',
    'roles.panel.title': 'ROLES',
    'roles.filter.placeholder': 'Filter roles',
    'roles.action.new': 'New',
    'roles.action.edit': 'Edit Role',
    'roles.tree.empty': 'No roles',
    'roles.permissions.select-role': 'Select a role',
    'roles.tools.select-role': 'Select a role',
    'roles.users.grant': 'Grant to User',
    'roles.users.remove': 'Remove',
    'roles.users.remove.blocked': 'blocked',
    'roles.users.empty.title': 'No users',
    'roles.users.empty.copy': 'Grant a user'
};

/**
 * The page owns child components (tree, detail header, users tab, iframes)
 * that all inject `DotRolesStore`. `componentProviders` swaps the store for a
 * mock so every child sees the same stub without wiring the full DI tree.
 */
function baseStoreMock(overrides: Record<string, unknown> = {}) {
    return mockProvider(DotRolesStore, {
        roles: jest.fn().mockReturnValue([]),
        roleTree: jest.fn().mockReturnValue([]),
        filter: jest.fn().mockReturnValue(''),
        filteredRoles: jest.fn().mockReturnValue([]),
        isSearching: jest.fn().mockReturnValue(false),
        searchStatus: jest.fn().mockReturnValue('init'),
        selectedRoleId: jest.fn().mockReturnValue(null),
        selectedRole: jest.fn().mockReturnValue(null),
        selectedRoleStatus: jest.fn().mockReturnValue('init'),
        selectedRoleIsParent: jest.fn().mockReturnValue(false),
        activeTab: jest.fn().mockReturnValue('users'),
        status: jest.fn().mockReturnValue('loaded'),
        membersStatus: jest.fn().mockReturnValue('loaded'),
        members: jest.fn().mockReturnValue([]),
        memberCount: jest.fn().mockReturnValue(0),
        isSystemRole: jest.fn().mockReturnValue(false),
        canModifyRole: jest.fn().mockReturnValue(true),
        fetchRoleDetail: jest.fn(),
        canGrantUsers: jest.fn().mockReturnValue(true),
        setFilter: jest.fn(),
        selectRole: jest.fn(),
        setActiveTab: jest.fn(),
        loadRootRoles: jest.fn(),
        loadMembers: jest.fn(),
        loadRoleChildren: jest.fn(),
        createRole: jest.fn(),
        ...overrides
    });
}

describe('DotRolesPageComponent', () => {
    let spectator: Spectator<DotRolesPageComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesPageComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [baseStoreMock(), mockProvider(DotRolesPortletService)],
        providers: [
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) },
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should call store.loadRootRoles on init', () => {
        const store = spectator.inject(DotRolesStore, true);
        spectator.detectChanges();

        expect(store.loadRootRoles).toHaveBeenCalled();
    });

    it('should render the roles tree', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('roles-tree'))).toBeTruthy();
    });

    it('should render the empty state when no role is selected', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('empty-selection'))).toBeTruthy();
        expect(spectator.query(byTestId('role-detail-tabs'))).toBeNull();
    });

    it('should render the detail tabs when a role is selected', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRoleId as jest.Mock).mockReturnValue('r-eco');
        (store.selectedRole as jest.Mock).mockReturnValue({ id: 'r-eco', name: 'Eco Role' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('empty-selection'))).toBeNull();
        expect(spectator.query(byTestId('role-detail-tabs'))).toBeTruthy();
    });
});
