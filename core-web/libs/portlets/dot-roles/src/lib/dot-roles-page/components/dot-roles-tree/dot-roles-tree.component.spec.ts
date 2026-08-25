import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { EMPTY } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesTreeComponent } from './dot-roles-tree.component';

import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.panel.title': 'ROLES',
    'roles.filter.placeholder': 'Filter roles',
    'roles.action.new': 'New',
    'roles.action.add-child': 'Add child',
    'roles.tree.empty': 'No roles',
    loading: 'Loading',
    'roles.error.load-failed': 'Failed'
};

describe('DotRolesTreeComponent', () => {
    let spectator: Spectator<DotRolesTreeComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesTreeComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(DotRolesStore, {
                rootRoles: jest.fn().mockReturnValue([]),
                filteredRoles: jest.fn().mockReturnValue([]),
                filter: jest.fn().mockReturnValue(''),
                isSearching: jest.fn().mockReturnValue(false),
                selectedRoleId: jest.fn().mockReturnValue(null),
                status: jest.fn().mockReturnValue('LOADED'),
                setFilter: jest.fn(),
                selectRole: jest.fn(),
                deleteRole: jest.fn().mockResolvedValue(null)
            }),
            mockProvider(DialogService, { open: jest.fn() }),
            mockProvider(ConfirmationService, {
                confirm: jest.fn().mockImplementation((cfg) => cfg.accept?.()),
                requireConfirmation$: EMPTY,
                accept: EMPTY,
                reject: EMPTY
            })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the New button and filter input', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('new-role-btn'))).toBeTruthy();
        expect(spectator.query(byTestId('filter-input'))).toBeTruthy();
    });

    it('should render the empty state when no roles match', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('tree-empty'))).toBeTruthy();
    });

    it('should render the tree view when roles are loaded', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.filteredRoles as jest.Mock).mockReturnValue([
            { id: 'r-eco', name: 'Eco Role', children: [] }
        ]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('roles-tree-view'))).toBeTruthy();
        expect(spectator.query(byTestId('tree-empty'))).toBeNull();
    });

    it('should open the Add Role dialog when the New button is clicked', () => {
        const dialogService = spectator.inject(DialogService, true);
        spectator.detectChanges();

        spectator.click(byTestId('new-role-btn'));

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                width: '700px',
                closable: true,
                closeOnEscape: true,
                data: { parentRoleId: null }
            })
        );
    });
});
