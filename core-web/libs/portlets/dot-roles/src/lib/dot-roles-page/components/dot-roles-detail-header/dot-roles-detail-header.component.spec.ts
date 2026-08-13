import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolesDetailHeaderComponent } from './dot-roles-detail-header.component';

import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.action.edit': 'Edit Role',
    'roles.header.users': 'users',
    'roles.header.tools-granted': 'tools granted',
    'roles.chip.system': 'System',
    'roles.chip.locked': 'Locked'
};

describe('DotRolesDetailHeaderComponent', () => {
    let spectator: Spectator<DotRolesDetailHeaderComponent>;

    const createComponent = createComponentFactory({
        component: DotRolesDetailHeaderComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(DotRolesStore, {
                selectedRole: jest.fn().mockReturnValue(null),
                memberCount: jest.fn().mockReturnValue(0),
                isSystemRole: jest.fn().mockReturnValue(false)
            }),
            mockProvider(DialogService, { open: jest.fn() })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render nothing when no role is selected', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('edit-role-btn'))).toBeNull();
    });

    it('should render the role name and Edit Role button when a role is selected', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRole as jest.Mock).mockReturnValue({
            id: 'r-eco',
            name: 'Eco Role',
            children: []
        });
        (store.memberCount as jest.Mock).mockReturnValue(2);
        spectator.detectChanges();

        expect(spectator.query('h1')?.textContent).toContain('Eco Role');
        expect(spectator.query(byTestId('edit-role-btn'))).toBeTruthy();
    });

    it('should render System and Locked chips when the role is system + locked', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRole as jest.Mock).mockReturnValue({
            id: 'r-cms',
            name: 'CMS Admin',
            system: true,
            locked: true
        });
        spectator.detectChanges();

        expect(spectator.query(byTestId('chip-system'))).toBeTruthy();
        expect(spectator.query(byTestId('chip-locked'))).toBeTruthy();
    });

    it('should open the Edit Role dialog when the button is clicked', () => {
        const dialogService = spectator.inject(DialogService, true);
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRole as jest.Mock).mockReturnValue({ id: 'r-eco', name: 'Eco Role' });
        spectator.detectChanges();

        spectator.click(byTestId('edit-role-btn'));

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
});
