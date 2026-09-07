import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesAddComponent } from './dot-roles-add.component';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import { DotRoleNode, ROOT_PARENT_OPTION_KEY } from '../models/dot-roles.models';

const MESSAGES = {
    'roles.add.title': 'Add Role',
    'roles.action.save': 'Save',
    'roles.action.cancel': 'Cancel',
    'roles.form.name': 'Role',
    'roles.form.key': 'Key',
    'roles.form.parent': 'Parent',
    'roles.form.parent.root': 'None (Top Level)',
    'roles.form.description': 'Description',
    'roles.form.can-grant': 'Can Grant',
    'roles.form.users': 'Users',
    'roles.form.permissions': 'Permissions',
    'roles.form.tools': 'Tools'
};

describe('DotRolesAddComponent', () => {
    let spectator: Spectator<DotRolesAddComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesAddComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        providers: [
            mockProvider(DotRolesStore, {
                roleTree: jest.fn().mockReturnValue([
                    { id: 'r-a', name: 'Root A', childCount: 2, roleChildren: [] },
                    { id: 'r-b', name: 'Root B', childCount: 0, roleChildren: [] }
                ]),
                loadRoleChildren: jest.fn(),
                searchRoleTree: jest.fn().mockResolvedValue([]),
                createRole: jest.fn().mockResolvedValue({ id: 'r-new', name: 'New' })
            }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            { provide: DynamicDialogConfig, useValue: { data: {} } },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('defaults the picker to "None (Top Level)" when opened from New', () => {
        const spectator = createComponent();
        spectator.detectChanges();

        expect(spectator.component['form'].controls.parent.value?.key).toBe(ROOT_PARENT_OPTION_KEY);
    });

    describe('parent picker', () => {
        beforeEach(() => {
            // `mockProvider` builds its jest.fn()s once at factory scope, so
            // BOTH call history and implementation survive between tests.
            // `mockClear` only resets the former — re-seed the default too, or
            // one test's `mockResolvedValue` silently becomes every later
            // test's behaviour.
            const store = spectator.inject(DotRolesStore, true);
            (store.searchRoleTree as jest.Mock).mockReset().mockResolvedValue([]);
            (store.loadRoleChildren as jest.Mock).mockReset();
        });

        it('marks a node with children as expandable even before they are fetched', () => {
            const spectator = createComponent();
            spectator.detectChanges();

            // Index 0 is the pinned "None (Top Level)" entry, not a role.
            const [, withChildren, withoutChildren] = spectator.component['$parentTree']();
            // `leaf: false` is what gives PrimeNG a toggler to click.
            expect(withChildren.leaf).toBe(false);
            expect(withoutChildren.leaf).toBe(true);
        });

        it('hydrates a branch on expand — the backend only sends two levels', () => {
            const spectator = createComponent();
            spectator.detectChanges();
            const store = spectator.inject(DotRolesStore, true);

            spectator.component['onNodeExpand']({ node: { key: 'r-a' } });

            expect(store.loadRoleChildren).toHaveBeenCalledWith('r-a');
        });

        it('keeps a branch open after its children load', () => {
            // PrimeNG records expansion by mutating node.expanded; our options
            // come from a computed, so without tracking the keys the branch
            // snaps shut the moment loadRoleChildren patches the store.
            const spectator = createComponent();
            spectator.detectChanges();

            spectator.component['onNodeExpand']({ node: { key: 'r-a' } });
            spectator.detectChanges();

            const expanded = spectator.component['$parentTree']().find((n) => n.key === 'r-a');
            expect(expanded?.expanded).toBe(true);
        });

        it('forgets the branch once collapsed', () => {
            const spectator = createComponent();
            spectator.detectChanges();

            spectator.component['onNodeExpand']({ node: { key: 'r-a' } });
            spectator.component['onNodeCollapse']({ node: { key: 'r-a' } });
            spectator.detectChanges();

            const node = spectator.component['$parentTree']().find((n) => n.key === 'r-a');
            expect(node?.expanded).toBe(false);
        });

        it('runs a deep search past 3 characters and shows the matches', fakeAsync(() => {
            const spectator = createComponent();
            spectator.detectChanges();
            const store = spectator.inject(DotRolesStore, true);
            (store.searchRoleTree as jest.Mock).mockResolvedValueOnce([
                { id: 'r-found', name: 'Found', roleChildren: [] }
            ]);

            spectator.component['onFilter']({ filter: 'fou' });
            tick(300);
            flushMicrotasks();
            spectator.detectChanges();

            expect(store.searchRoleTree).toHaveBeenCalledWith('fou');
            // The root entry is a choice, not a search hit, so it survives.
            expect(spectator.component['$parentTree']().map((n) => n.key)).toEqual([
                ROOT_PARENT_OPTION_KEY,
                'r-found'
            ]);
        }));

        it('does not search under 3 characters and falls back to the cached tree', fakeAsync(() => {
            const spectator = createComponent();
            spectator.detectChanges();
            const store = spectator.inject(DotRolesStore, true);

            spectator.component['onFilter']({ filter: 'fo' });
            tick(300);
            flushMicrotasks();

            expect(store.searchRoleTree).not.toHaveBeenCalled();
            expect(spectator.component['$parentTree']().map((n) => n.key)).toEqual([
                ROOT_PARENT_OPTION_KEY,
                'r-a',
                'r-b'
            ]);
        }));

        it('clears the busy flag once the search resolves', fakeAsync(() => {
            const spectator = createComponent();
            spectator.detectChanges();

            spectator.component['onFilter']({ filter: 'fou' });
            tick(300);
            flushMicrotasks();

            expect(spectator.component['$searching']()).toBe(false);
        }));

        it('clears the busy flag when a search is superseded by a shorter query', fakeAsync(() => {
            // The picker binds `[loading]` to this flag. The superseded run
            // returns through the token guard, so nothing else clears it —
            // leaving it set spins the picker for the rest of the dialog.
            const spectator = createComponent();
            spectator.detectChanges();
            const store = spectator.inject(DotRolesStore, true);

            let resolveSearch: (value: DotRoleNode[]) => void = () => {
                /* replaced below */
            };
            (store.searchRoleTree as jest.Mock).mockReturnValueOnce(
                new Promise<DotRoleNode[]>((resolve) => {
                    resolveSearch = resolve;
                })
            );

            spectator.component['onFilter']({ filter: 'fou' });
            tick(300);
            expect(spectator.component['$searching']()).toBe(true);

            // Admin backspaces below the 3-char gate while the request is out.
            spectator.component['onFilter']({ filter: 'fo' });
            tick(300);
            resolveSearch([]);
            flushMicrotasks();

            expect(spectator.component['$searching']()).toBe(false);
        }));

        it('keeps the busy flag set while a newer search is still running', fakeAsync(() => {
            const spectator = createComponent();
            spectator.detectChanges();
            const store = spectator.inject(DotRolesStore, true);

            let resolveFirst: (value: DotRoleNode[]) => void = () => {
                /* replaced below */
            };
            (store.searchRoleTree as jest.Mock)
                .mockReturnValueOnce(
                    new Promise<DotRoleNode[]>((resolve) => {
                        resolveFirst = resolve;
                    })
                )
                .mockReturnValueOnce(new Promise<DotRoleNode[]>(() => undefined));

            spectator.component['onFilter']({ filter: 'fou' });
            tick(300);
            spectator.component['onFilter']({ filter: 'four' });
            tick(300);

            // The superseded run must not drop the spinner out from under the
            // search that replaced it.
            resolveFirst([]);
            flushMicrotasks();

            expect(spectator.component['$searching']()).toBe(true);
        }));
    });

    it('should render the required inputs and an enabled Save button', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('input-role-name'))).toBeTruthy();
        expect(spectator.query(byTestId('input-description'))).toBeTruthy();

        // Save is never disabled by validation — an empty form is reported in
        // the footer on submit, not by a dead button.
        const saveBtn = spectator.query(byTestId('btn-save')) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(false);
    });

    it('should report the missing required field in the footer instead of creating', () => {
        const store = spectator.inject(DotRolesStore);
        // The `mockProvider` factory reuses the same `jest.fn()` across tests
        // in this suite, so a bare `not.toHaveBeenCalled()` would inherit
        // earlier calls.
        (store.createRole as jest.Mock).mockClear();
        spectator.detectChanges();

        spectator.click(byTestId('btn-save'));
        spectator.detectChanges();

        expect(store.createRole).not.toHaveBeenCalled();
        expect(spectator.query(byTestId('add-error'))).toBeTruthy();
        expect(spectator.component['form'].get('roleName')?.touched).toBe(true);
    });

    it('should clear the validation error once the form is completed and submitted', () => {
        const store = spectator.inject(DotRolesStore);
        spectator.detectChanges();

        spectator.click(byTestId('btn-save'));
        spectator.detectChanges();
        expect(spectator.query(byTestId('add-error'))).toBeTruthy();

        spectator.typeInElement('New Role', byTestId('input-role-name'));
        spectator.detectChanges();
        spectator.click(byTestId('btn-save'));
        spectator.detectChanges();

        expect(store.createRole).toHaveBeenCalled();
        expect(spectator.query(byTestId('add-error'))).toBeFalsy();
    });

    it('should close the dialog when Cancel is clicked', () => {
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        spectator.click(byTestId('btn-cancel'));

        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should set the inline error and keep the dialog open when createRole returns null', async () => {
        const store = spectator.inject(DotRolesStore);
        const dialogRef = spectator.inject(DynamicDialogRef);
        // The `mockProvider` factory reuses the same `jest.fn()` across
        // tests in this suite — clear before asserting so we only see
        // calls from this test.
        (dialogRef.close as jest.Mock).mockClear();
        (store.createRole as jest.Mock).mockResolvedValueOnce(null);

        spectator.detectChanges();
        spectator.typeInElement('New Role', byTestId('input-role-name'));
        spectator.detectChanges();

        spectator.click(byTestId('btn-save'));
        await Promise.resolve();
        spectator.detectChanges();

        expect(store.createRole).toHaveBeenCalled();
        expect(dialogRef.close).not.toHaveBeenCalled();
        expect(spectator.query(byTestId('add-error'))).toBeTruthy();
    });
});

describe('DotRolesAddComponent (opened from inline +)', () => {
    const createComponent = createComponentFactory({
        component: DotRolesAddComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        providers: [
            mockProvider(DotRolesStore, {
                roleTree: jest
                    .fn()
                    .mockReturnValue([
                        { id: 'r-categories', name: 'Categories', roleChildren: [] }
                    ]),
                createRole: jest.fn().mockResolvedValue({ id: 'r-new', name: 'New' })
            }),
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            {
                provide: DynamicDialogConfig,
                useValue: { data: { parentRoleId: 'r-categories' } }
            },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    it('should prefill the parent from the dialog data', () => {
        const spectator = createComponent();
        spectator.detectChanges();

        // The picker binds to a TreeNode, not a bare id — the `+` on a tree
        // row hands over an id, which the component resolves to its node.
        const parent = spectator.component['form'].controls.parent.value;
        expect(parent?.key).toBe('r-categories');
        expect(parent?.label).toBe('Categories');
    });

    it('sends the picked node id as parentRoleId on save', async () => {
        const spectator = createComponent();
        spectator.detectChanges();

        const store = spectator.inject(DotRolesStore, true);
        spectator.component['form'].controls.roleName.setValue('New Role');
        await spectator.component['onSave']();

        expect(store.createRole).toHaveBeenCalledWith(
            expect.objectContaining({ roleName: 'New Role', parentRoleId: 'r-categories' })
        );
    });

    it('sends a null parentRoleId when the picker is cleared — a root role', async () => {
        const spectator = createComponent();
        spectator.detectChanges();

        const store = spectator.inject(DotRolesStore, true);
        spectator.component['form'].controls.roleName.setValue('New Root');
        spectator.component['form'].controls.parent.setValue(null);
        await spectator.component['onSave']();

        expect(store.createRole).toHaveBeenCalledWith(
            expect.objectContaining({ parentRoleId: null })
        );
    });

    it('keeps the prefilled parent rather than defaulting to root', () => {
        const spectator = createComponent();
        spectator.detectChanges();

        // The root default must never clobber the parent the row's `+` asked
        // for — that would silently create a root role instead of a child.
        expect(spectator.component['form'].controls.parent.value?.key).toBe('r-categories');
    });

    it('pins "None (Top Level)" as the first option', () => {
        const spectator = createComponent();
        spectator.detectChanges();

        const [first] = spectator.component['$parentTree']();
        expect(first.key).toBe(ROOT_PARENT_OPTION_KEY);
        expect(first.label).toBe('None (Top Level)');
        // A choice, not a branch — it must not render a toggler.
        expect(first.leaf).toBe(true);
    });

    it('maps the "None (Top Level)" selection to a null parentRoleId, not the sentinel', async () => {
        const spectator = createComponent();
        spectator.detectChanges();

        const store = spectator.inject(DotRolesStore, true);
        spectator.component['form'].controls.roleName.setValue('New Root');
        spectator.component['form'].controls.parent.setValue(
            spectator.component['$parentTree']()[0]
        );
        await spectator.component['onSave']();

        // Sending the sentinel through would be read as a real id and answered
        // with a 404 "parent role not found".
        expect(store.createRole).toHaveBeenCalledWith(
            expect.objectContaining({ parentRoleId: null })
        );
    });
});
