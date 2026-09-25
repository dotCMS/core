import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { EMPTY } from 'rxjs';
import { Mock, vi } from 'vitest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Menu } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesDetailHeaderComponent } from './dot-roles-detail-header.component';

import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.action.edit': 'Edit',
    'roles.action.delete': 'Delete',
    'roles.menu.header': 'Role',
    'roles.header.actions': 'Role actions',
    'roles.header.users': 'users',
    'roles.header.tools-granted': 'tools granted',
    'roles.chip.system': 'System',
    'roles.chip.locked': 'Locked',
    'roles.edit.title': 'Edit Role'
};

const ROLE = { id: 'r-eco', name: 'Eco Role', children: [] };

describe('DotRolesDetailHeaderComponent', () => {
    let spectator: Spectator<DotRolesDetailHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesDetailHeaderComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(DotRolesStore, {
                selectedRole: vi.fn().mockReturnValue(null),
                selectedRoleStatus: vi.fn().mockReturnValue('INIT'),
                memberCount: vi.fn().mockReturnValue(0),
                toolGroupCount: vi.fn().mockReturnValue(0),
                isSystemRole: vi.fn().mockReturnValue(false),
                canModifyRole: vi.fn().mockReturnValue(true),
                deleteRole: vi.fn().mockResolvedValue({ deleted: true })
            }),
            mockProvider(DialogService, { open: vi.fn() }),
            mockProvider(ConfirmationService, {
                confirm: vi.fn().mockImplementation((cfg) => cfg.accept?.()),
                // p-confirmDialog subscribes to these on init
                requireConfirmation$: EMPTY,
                accept: EMPTY
            })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    /** The items nested under the menu's "Role" group heading. */
    const groupItems = (): MenuItem[] => {
        const model = spectator.query(Menu)?.model ?? [];

        return model[0]?.items ?? [];
    };

    const selectRole = (role: object | null = ROLE): void => {
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRole as Mock).mockReturnValue(role);
    };

    beforeEach(() => {
        spectator = createComponent();
        const store = spectator.inject(DotRolesStore, true);
        (store.canModifyRole as Mock).mockReturnValue(true);
        vi.clearAllMocks();
    });

    it('should render nothing when no role is selected', () => {
        spectator.detectChanges();

        expect(spectator.query('h1')).toBeNull();
        expect(spectator.query(byTestId('role-actions-btn'))).toBeNull();
    });

    it('should render the role name and the actions button, not an Edit Role button', () => {
        const store = spectator.inject(DotRolesStore, true);
        selectRole();
        (store.memberCount as Mock).mockReturnValue(2);
        spectator.detectChanges();

        expect(spectator.query('h1')?.textContent).toContain('Eco Role');
        expect(spectator.query(byTestId('edit-role-btn'))).toBeNull();
        expect(spectator.query(byTestId('role-actions-btn'))).toBeTruthy();
    });

    it('should give the icon-only actions button an accessible name', () => {
        selectRole();
        spectator.detectChanges();

        // On the native button, which is what assistive tech reads — the `p-button` host would
        // carry an `[attr.aria-label]` and still leave the real control unnamed.
        const button = spectator.query(byTestId('role-actions-btn'))?.querySelector('button');
        expect(button?.getAttribute('aria-label')).toBe('Role actions');
    });

    it('should render System and Locked chips when the role is system + locked', () => {
        selectRole({ id: 'r-cms', name: 'CMS Admin', system: true, locked: true });
        spectator.detectChanges();

        expect(spectator.query(byTestId('chip-system'))).toBeTruthy();
        expect(spectator.query(byTestId('chip-locked'))).toBeTruthy();
    });

    it('should group Edit, a separator and Delete under a "Role" heading', () => {
        selectRole();
        spectator.detectChanges();

        const model = spectator.query(Menu)?.model ?? [];
        expect(model).toHaveLength(1);
        expect(model[0].label).toBe('Role');
        expect(groupItems().map((item) => item.label ?? 'separator')).toEqual([
            'Edit',
            'separator',
            'Delete'
        ]);
        expect(groupItems()[1].separator).toBe(true);
    });

    it('should not style Delete as destructive', () => {
        selectRole();
        spectator.detectChanges();

        const deleteItem = groupItems()[2];
        expect(deleteItem.styleClass).toBeUndefined();
        expect(deleteItem.labelClass).toBeUndefined();
    });

    it('should open the Edit dialog from the Edit item', () => {
        const dialogService = spectator.inject(DialogService, true);
        selectRole();
        spectator.detectChanges();

        groupItems()[0].command?.({});

        expect(dialogService.open).toHaveBeenCalledWith(
            expect.anything(),
            expect.objectContaining({
                width: '700px',
                closable: true,
                closeOnEscape: true,
                data: { role: expect.objectContaining({ id: 'r-eco' }) }
            })
        );
    });

    it('should confirm, then delete the selected role, from the Delete item', () => {
        const store = spectator.inject(DotRolesStore, true);
        const confirmation = spectator.inject(ConfirmationService, true);
        selectRole();
        spectator.detectChanges();

        groupItems()[2].command?.({});

        // The mocked confirm accepts straight away.
        expect(confirmation.confirm).toHaveBeenCalledWith(
            expect.objectContaining({ acceptLabel: 'Delete', defaultFocus: 'reject' })
        );
        expect(store.deleteRole).toHaveBeenCalledWith('r-eco');
    });

    it('should not delete when the admin cancels the confirmation', () => {
        const store = spectator.inject(DotRolesStore, true);
        const confirmation = spectator.inject(ConfirmationService, true);
        (confirmation.confirm as Mock).mockImplementationOnce((cfg) => cfg.reject?.());
        selectRole();
        spectator.detectChanges();

        groupItems()[2].command?.({});

        expect(confirmation.confirm).toHaveBeenCalled();
        expect(store.deleteRole).not.toHaveBeenCalled();
    });

    it('should disable Edit and Delete for a role that cannot be modified', () => {
        const store = spectator.inject(DotRolesStore, true);
        selectRole({ id: 'r-cms', name: 'CMS Admin', system: true });
        (store.canModifyRole as Mock).mockReturnValue(false);
        spectator.detectChanges();

        expect(groupItems()[0].disabled).toBe(true);
        expect(groupItems()[2].disabled).toBe(true);
    });
});
