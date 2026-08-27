import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { EMPTY } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';
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
                roles: jest.fn().mockReturnValue([]),
                roleTree: jest.fn().mockReturnValue([]),
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
            }),
            mockProvider(DotAlertConfirmService, { alert: jest.fn() })
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

    describe('leaf detection via childCount (#37071)', () => {
        // `$treeNodes` is protected; reach it by index so the assertions
        // target the mapping logic rather than PrimeNG's rendered DOM.
        const treeNodes = () =>
            (
                spectator.component as unknown as { $treeNodes: () => { leaf: boolean }[] }
            ).$treeNodes();

        it('marks a node with childCount 0 as a leaf before any expansion', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.filteredRoles as jest.Mock).mockReturnValue([
                { id: 'r-leaf', name: 'Leaf Role', childCount: 0, roleChildren: [] }
            ]);
            spectator.detectChanges();

            expect(treeNodes()[0].leaf).toBe(true);
        });

        it('keeps the chevron when childCount is positive but children are not hydrated', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.filteredRoles as jest.Mock).mockReturnValue([
                { id: 'r-parent', name: 'Parent Role', childCount: 3, roleChildren: [] }
            ]);
            spectator.detectChanges();

            expect(treeNodes()[0].leaf).toBe(false);
        });

        it('falls back to the fetched-set heuristic when childCount is absent (legacy search nodes)', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.filteredRoles as jest.Mock).mockReturnValue([
                { id: 'r-legacy', name: 'Legacy Node', roleChildren: [] }
            ]);
            spectator.detectChanges();

            // Never expanded → not in the fetched set → stays expandable.
            expect(treeNodes()[0].leaf).toBe(false);
        });

        it('treats user-roles as leaves regardless of childCount', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.filteredRoles as jest.Mock).mockReturnValue([
                { id: 'r-user', name: 'Some User', user: true, childCount: 5 }
            ]);
            spectator.detectChanges();

            expect(treeNodes()[0].leaf).toBe(true);
        });
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
