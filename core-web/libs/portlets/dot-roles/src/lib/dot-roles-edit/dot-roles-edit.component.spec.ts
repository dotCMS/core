import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesEditComponent } from './dot-roles-edit.component';

const MESSAGES = {
    'roles.edit.title': 'Edit Role',
    'roles.edit.blocked': 'blocked',
    'roles.delete.blocked': 'delete blocked',
    'roles.action.save': 'Save',
    'roles.action.cancel': 'Cancel',
    'roles.action.delete': 'Delete Role',
    'roles.form.name': 'Role',
    'roles.form.key': 'Key',
    'roles.form.parent': 'Parent',
    'roles.form.parent.root': 'None (top level)',
    'roles.form.description': 'Description'
};

describe('DotRolesEditComponent', () => {
    let spectator: Spectator<DotRolesEditComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesEditComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        providers: [
            mockProvider(DynamicDialogRef, { close: jest.fn() }),
            {
                provide: DynamicDialogConfig,
                useValue: {
                    data: {
                        role: {
                            id: 'r-eco',
                            name: 'Eco Role',
                            roleKey: 'eco',
                            description: 'The eco team'
                        }
                    }
                }
            },
            { provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the selected role details', () => {
        spectator.detectChanges();

        const body = spectator.query('dl')?.textContent ?? '';
        expect(body).toContain('Eco Role');
        expect(body).toContain('eco');
        expect(body).toContain('The eco team');
    });

    it('should show the blocked notice', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('edit-blocked-notice'))).toBeTruthy();
    });

    it('should render Save and Delete buttons as disabled placeholders', () => {
        spectator.detectChanges();

        const saveBtn = spectator.query(byTestId('btn-save')) as HTMLButtonElement;
        const deleteBtn = spectator.query(byTestId('btn-delete')) as HTMLButtonElement;

        expect(saveBtn.disabled).toBe(true);
        expect(deleteBtn.disabled).toBe(true);
    });

    it('should close the dialog when Cancel is clicked', () => {
        const dialogRef = spectator.inject(DynamicDialogRef);
        spectator.detectChanges();

        spectator.click(byTestId('btn-cancel'));

        expect(dialogRef.close).toHaveBeenCalled();
    });
});
