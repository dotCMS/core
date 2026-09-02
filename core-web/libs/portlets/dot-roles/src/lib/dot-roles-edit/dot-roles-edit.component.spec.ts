import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { EMPTY } from 'rxjs';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesEditComponent } from './dot-roles-edit.component';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import { DotRoleNode } from '../models/dot-roles.models';

const MESSAGES = {
    'roles.edit.title': 'Edit Role',
    'roles.edit.readonly': 'read only',
    'roles.edit.error': 'update failed',
    'roles.delete.blocked': 'delete blocked',
    'roles.action.save': 'Save',
    'roles.action.cancel': 'Cancel',
    'roles.action.delete': 'Delete Role',
    'roles.form.name': 'Role',
    'roles.form.key': 'Key',
    'roles.form.parent': 'Parent',
    'roles.form.parent.root': 'None (top level)',
    'roles.form.description': 'Description',
    'roles.form.can-grant': 'Can grant',
    'roles.form.users': 'Users',
    'roles.form.permissions': 'Permissions',
    'roles.form.tools': 'Tools'
};

const BASE_ROLE = {
    id: 'r-eco',
    name: 'Eco Role',
    roleKey: 'eco',
    description: 'The eco team',
    parent: 'r-eco',
    editUsers: true,
    editPermissions: true,
    editLayouts: true
};

const dialogConfig = { data: { role: BASE_ROLE } };

describe('DotRolesEditComponent', () => {
    let spectator: Spectator<DotRolesEditComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesEditComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(ConfirmationService, {
                confirm: jest.fn().mockImplementation((cfg) => cfg.accept?.()),
                // p-confirmDialog subscribes to these on init
                requireConfirmation$: EMPTY,
                accept: EMPTY,
                reject: EMPTY
            })
        ],
        providers: [
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            mockProvider(DotRolesStore, {
                roleTree: jest.fn().mockReturnValue([]),
                searchRoleTree: jest.fn().mockResolvedValue([]),
                updateRole: jest.fn().mockResolvedValue(BASE_ROLE),
                deleteRole: jest
                    .fn()
                    .mockResolvedValue({ deleted: true, roleId: 'r-eco', usersAffected: 2 })
            }),
            { provide: DynamicDialogConfig, useValue: dialogConfig },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    beforeEach(() => {
        dialogConfig.data = { role: BASE_ROLE };
        spectator = createComponent();
        const store = spectator.inject(DotRolesStore, true);
        (store.updateRole as jest.Mock).mockClear();
        (store.updateRole as jest.Mock).mockResolvedValue(BASE_ROLE);
        (store.deleteRole as jest.Mock).mockClear();
        (store.deleteRole as jest.Mock).mockResolvedValue({
            deleted: true,
            roleId: 'r-eco',
            usersAffected: 2
        });
        // `mockProvider` builds its jest.fn()s once at factory scope, so an
        // implementation set by one test would otherwise become every later
        // test's behaviour.
        (store.searchRoleTree as jest.Mock).mockReset().mockResolvedValue([]);
    });

    describe('parent picker search', () => {
        it('clears the busy flag when a search is superseded by a shorter query', fakeAsync(() => {
            // The picker binds `[loading]` to this flag. The superseded run
            // returns through the token guard, so nothing else clears it —
            // leaving it set spins the picker for the rest of the dialog.
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

            spectator.component['onFilter']({ filter: 'fo' });
            tick(300);
            resolveSearch([]);
            flushMicrotasks();

            expect(spectator.component['$searching']()).toBe(false);
        }));

        it('keeps the busy flag set while a newer search is still running', fakeAsync(() => {
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

            resolveFirst([]);
            flushMicrotasks();

            expect(spectator.component['$searching']()).toBe(true);
        }));
    });

    it('prefills the form with the role fields', () => {
        spectator.detectChanges();

        const nameInput = spectator.query(byTestId('input-role-name')) as HTMLInputElement;
        const keyInput = spectator.query(byTestId('input-role-key')) as HTMLInputElement;
        expect(nameInput.value).toBe('Eco Role');
        expect(keyInput.value).toBe('eco');
    });

    it('calls store.updateRole and closes on submit', async () => {
        const store = spectator.inject(DotRolesStore, true);
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        // The submit button carries `data-testid` on the native `<button>`
        // (via `pButton` — not `p-button`), so a direct click hits the DOM
        // event the template binds to.
        spectator.click(byTestId('btn-save'));
        await Promise.resolve();

        expect(store.updateRole).toHaveBeenCalledWith(
            'r-eco',
            expect.objectContaining({ roleName: 'Eco Role' })
        );
        expect(dialogRef.close).toHaveBeenCalledWith(BASE_ROLE);
    });

    it('closes on Cancel without calling updateRole', () => {
        const store = spectator.inject(DotRolesStore, true);
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        spectator.click(byTestId('btn-cancel'));

        expect(store.updateRole).not.toHaveBeenCalled();
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('opens the confirm and calls store.deleteRole when Delete is clicked', async () => {
        const store = spectator.inject(DotRolesStore, true);
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        spectator.click(byTestId('btn-delete'));
        await Promise.resolve();

        expect(store.deleteRole).toHaveBeenCalledWith('r-eco');
        expect(dialogRef.close).toHaveBeenCalledWith(
            expect.objectContaining({ deleted: true, usersAffected: 2 })
        );
    });

    it('disables Save + shows readonly notice for system roles', () => {
        dialogConfig.data = { role: { ...BASE_ROLE, system: true } as typeof BASE_ROLE };
        spectator = createComponent();
        spectator.detectChanges();

        expect(spectator.query(byTestId('edit-readonly-notice'))).toBeTruthy();
        const saveBtn = spectator.query(byTestId('btn-save')) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(true);
    });
});
