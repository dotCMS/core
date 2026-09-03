import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { EMPTY, of } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA, signal } from '@angular/core';

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
                deleteRole: jest.fn().mockResolvedValue(null),
                loadRoleChildren: jest.fn()
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

    it('should not inherit the shared tree folder icons (#37362)', () => {
        // This portlet renders a *roles* hierarchy through the shared DotFolderTreeComponent and
        // draws its own Material Symbols icons in the projected label template. The shared
        // folder-icon input is opt-in precisely so a folder glyph never lands next to them here.
        const store = spectator.inject(DotRolesStore, true);
        (store.filteredRoles as jest.Mock).mockReturnValue([
            { id: 'r-eco', name: 'Eco Role', children: [] }
        ]);
        spectator.detectChanges();

        expect(spectator.query(byTestId('tree-node-folder-icon'))).toBeNull();

        // The row still draws its own Material Symbols icon. Which glyph it picks is leaf
        // detection, covered by the `childCount` tests below — what matters here is that the icon
        // is this portlet's and no PrimeIcons folder joined it.
        const ownIcon = spectator.query(byTestId('node-icon-r-eco'));
        expect(ownIcon?.classList.contains('material-symbols-outlined')).toBe(true);
        expect(ownIcon?.querySelector('.pi')).toBeNull();
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

    describe('revealing the selected role after a move', () => {
        const treeWithMoveTarget = (parentOfSelected: string) => [
            {
                id: 'r-target',
                name: 'Target',
                childCount: 1,
                roleChildren:
                    parentOfSelected === 'r-target'
                        ? [{ id: 'r-moved', name: 'Moved', parent: 'r-target', childCount: 0 }]
                        : []
            },
            {
                id: 'r-origin',
                name: 'Origin',
                childCount: 1,
                roleChildren:
                    parentOfSelected === 'r-origin'
                        ? [{ id: 'r-moved', name: 'Moved', parent: 'r-origin', childCount: 0 }]
                        : []
            }
        ];

        const expandedOf = (key: string) =>
            (
                spectator.component as unknown as {
                    $treeNodes: () => { key: string; expanded: boolean }[];
                }
            )
                .$treeNodes()
                .find((n) => n.key === key)?.expanded;

        /**
         * The store is a plain jest mock, so its getters are not signals and
         * nothing downstream would re-evaluate. Back the tree with a real
         * signal so the computed + effect see the move.
         */
        const seedReactiveTree = (parentOfSelected: string) => {
            const store = spectator.inject(DotRolesStore, true);
            const tree = signal(treeWithMoveTarget(parentOfSelected));
            (store.selectedRoleId as jest.Mock).mockReturnValue('r-moved');
            (store.roleTree as jest.Mock).mockImplementation(() => tree());
            (store.filteredRoles as jest.Mock).mockImplementation(() => tree());
            spectator.detectChanges();

            return tree;
        };

        it('opens the new parent so the moved role stays visible', () => {
            const tree = seedReactiveTree('r-origin');
            expect(expandedOf('r-target')).toBe(false);

            // Reparent: the role keeps the selection but lands under a branch
            // the admin never opened, so without this it vanishes on save.
            tree.set(treeWithMoveTarget('r-target'));
            spectator.detectChanges();

            expect(expandedOf('r-target')).toBe(true);
        });

        it('leaves a branch the admin collapsed alone when the tree is patched', () => {
            // `roles` is rewritten on every member load (the user-count badge),
            // and re-expanding on that would fight the admin's own collapses.
            const tree = seedReactiveTree('r-origin');

            (
                spectator.component as unknown as {
                    onNodeCollapse: (e: { node: { data: { id: string } } }) => void;
                }
            ).onNodeCollapse({ node: { data: { id: 'r-origin' } } });
            spectator.detectChanges();
            expect(expandedOf('r-origin')).toBe(false);

            // Same positions, new object identities — a badge refresh.
            tree.set(treeWithMoveTarget('r-origin'));
            spectator.detectChanges();

            expect(expandedOf('r-origin')).toBe(false);
        });
    });

    describe('lazy-load gate on expand', () => {
        beforeEach(() => {
            (spectator.inject(DotRolesStore, true).loadRoleChildren as jest.Mock).mockClear();
        });

        const expand = (data: Record<string, unknown>) =>
            (
                spectator.component as unknown as {
                    onNodeExpand: (e: { node: { data: Record<string, unknown> } }) => void;
                }
            ).onNodeExpand({ node: { data } });

        it('fetches on first expand and not on a re-open', () => {
            const store = spectator.inject(DotRolesStore, true);

            expand({ id: 'r-a', name: 'A', childCount: 2, roleChildren: [] });
            // The load populated the branch, so re-opening it must not hit the
            // backend again.
            expand({
                id: 'r-a',
                name: 'A',
                childCount: 2,
                roleChildren: [{ id: 'r-b' }, { id: 'r-c' }]
            });

            expect(store.loadRoleChildren).toHaveBeenCalledTimes(1);
        });

        it('re-fetches a branch that lost the children it had already loaded', () => {
            // A reparent replaces the moved role with a response that hydrates
            // two levels, so its grandchildren leave state. The fetched marker
            // is add-only, so without this the branch stays expanded and empty
            // until a full reload.
            const store = spectator.inject(DotRolesStore, true);

            expand({ id: 'r-a', name: 'A', childCount: 1, roleChildren: [{ id: 'r-b' }] });
            expect(store.loadRoleChildren).not.toHaveBeenCalled();

            expand({ id: 'r-a', name: 'A', childCount: 1, roleChildren: [] });

            expect(store.loadRoleChildren).toHaveBeenCalledWith('r-a');
        });

        it('does not re-fetch a confirmed leaf', () => {
            // `childCount: 0` means the emptiness is the truth, not a loss.
            const store = spectator.inject(DotRolesStore, true);
            const node = { id: 'r-a', name: 'A', childCount: 0, roleChildren: [] };

            expand(node);
            expand(node);

            expect(store.loadRoleChildren).toHaveBeenCalledTimes(1);
        });
    });

    describe('revealing a newly created role', () => {
        beforeEach(() => {
            // `mockProvider` shares its jest.fn()s across tests, so call
            // history survives unless it is cleared here.
            (spectator.inject(DotRolesStore, true).loadRoleChildren as jest.Mock).mockClear();
        });

        const openWithResult = (created: unknown) => {
            const dialogService = spectator.inject(DialogService, true);
            (dialogService.open as jest.Mock).mockReturnValue({ onClose: of(created) });
            spectator.detectChanges();
            spectator.click(byTestId('new-role-btn'));
        };

        it('expands the parent branch so the new child is actually visible', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.roleTree as jest.Mock).mockReturnValue([
                { id: 'r-parent', name: 'Parent', childCount: 1, roleChildren: [] }
            ]);
            (store.filteredRoles as jest.Mock).mockReturnValue([
                { id: 'r-parent', name: 'Parent', childCount: 1, roleChildren: [] }
            ]);

            openWithResult({ id: 'r-child', name: 'Child', parent: 'r-parent' });
            spectator.detectChanges();

            const tree = (
                spectator.component as unknown as {
                    $treeNodes: () => { key: string; expanded: boolean }[];
                }
            ).$treeNodes();
            expect(tree.find((n) => n.key === 'r-parent')?.expanded).toBe(true);
        });

        it('fetches the parent children so the new role does not appear alone', () => {
            const store = spectator.inject(DotRolesStore, true);
            (store.roleTree as jest.Mock).mockReturnValue([
                { id: 'r-parent', name: 'Parent', childCount: 1, roleChildren: [] }
            ]);

            openWithResult({ id: 'r-child', name: 'Child', parent: 'r-parent' });

            // Without this the branch holds only the spliced-in role, which
            // reads as though its siblings were deleted.
            expect(store.loadRoleChildren).toHaveBeenCalledWith('r-parent');
        });

        it('does nothing for a root role — it is visible already', () => {
            const store = spectator.inject(DotRolesStore, true);

            openWithResult({ id: 'r-root', name: 'Root', parent: 'r-root' });

            expect(store.loadRoleChildren).not.toHaveBeenCalled();
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
