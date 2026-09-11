import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { Mock, vi } from 'vitest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import {
    DotAlertConfirmService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
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
        roles: vi.fn().mockReturnValue([]),
        roleTree: vi.fn().mockReturnValue([]),
        filter: vi.fn().mockReturnValue(''),
        filteredRoles: vi.fn().mockReturnValue([]),
        isSearching: vi.fn().mockReturnValue(false),
        searchStatus: vi.fn().mockReturnValue('INIT'),
        selectedRoleId: vi.fn().mockReturnValue(null),
        selectedRole: vi.fn().mockReturnValue(null),
        selectedRoleStatus: vi.fn().mockReturnValue('INIT'),
        activeTab: vi.fn().mockReturnValue('users'),
        status: vi.fn().mockReturnValue('LOADED'),
        membersStatus: vi.fn().mockReturnValue('LOADED'),
        members: vi.fn().mockReturnValue([]),
        toolGroups: vi.fn().mockReturnValue([]),
        toolGroupsStatus: vi.fn().mockReturnValue('LOADED'),
        toolGroupsSaving: vi.fn().mockReturnValue(false),
        toolGroupCount: vi.fn().mockReturnValue(0),
        canEditRoleLayouts: vi.fn().mockReturnValue(true),
        memberCount: vi.fn().mockReturnValue(0),
        isSystemRole: vi.fn().mockReturnValue(false),
        canModifyRole: vi.fn().mockReturnValue(true),
        fetchRoleDetail: vi.fn(),
        canGrantUsers: vi.fn().mockReturnValue(true),
        setFilter: vi.fn(),
        selectRole: vi.fn(),
        setActiveTab: vi.fn(),
        loadRootRoles: vi.fn(),
        loadMembers: vi.fn(),
        loadRoleChildren: vi.fn(),
        createRole: vi.fn(),
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
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotAlertConfirmService, { alert: vi.fn() })
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
        (store.selectedRoleId as Mock).mockReturnValue('r-eco');
        (store.selectedRole as Mock).mockReturnValue({ id: 'r-eco', name: 'Eco Role' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('empty-selection'))).toBeNull();
        expect(spectator.query(byTestId('role-detail-tabs'))).toBeTruthy();
    });
});
