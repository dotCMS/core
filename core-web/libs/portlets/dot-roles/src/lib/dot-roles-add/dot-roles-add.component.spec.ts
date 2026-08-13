import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesAddComponent } from './dot-roles-add.component';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';

const MESSAGES = {
    'roles.add.title': 'Add Role',
    'roles.action.save': 'Save',
    'roles.action.cancel': 'Cancel',
    'roles.form.name': 'Role',
    'roles.form.key': 'Key',
    'roles.form.parent': 'Parent',
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
                rootRoles: jest.fn().mockReturnValue([]),
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

    it('should render the required inputs and disabled Save button', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('input-role-name'))).toBeTruthy();
        expect(spectator.query(byTestId('input-description'))).toBeTruthy();

        const saveBtn = spectator.query(byTestId('btn-save')) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(true);
    });

    it('should enable Save once the required roleName is filled', () => {
        spectator.detectChanges();

        spectator.typeInElement('New Role', byTestId('input-role-name'));
        spectator.detectChanges();

        const saveBtn = spectator.query(byTestId('btn-save')) as HTMLButtonElement;
        expect(saveBtn.disabled).toBe(false);
    });

    it('should close the dialog when Cancel is clicked', () => {
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        spectator.click(byTestId('btn-cancel'));

        expect(dialogRef.close).toHaveBeenCalled();
    });
});

describe('DotRolesAddComponent (opened from inline +)', () => {
    const createComponent = createComponentFactory({
        component: DotRolesAddComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        providers: [
            mockProvider(DotRolesStore, {
                rootRoles: jest.fn().mockReturnValue([]),
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

        expect(spectator.component['form'].controls.parentRoleId.value).toBe('r-categories');
    });
});
