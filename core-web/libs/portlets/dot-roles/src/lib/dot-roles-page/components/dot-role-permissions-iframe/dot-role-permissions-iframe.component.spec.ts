import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRolePermissionsIframeComponent } from './dot-role-permissions-iframe.component';

import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.permissions.select-role': 'Select a role',
    'roles.tab.permissions': 'Permissions'
};

describe('DotRolePermissionsIframeComponent', () => {
    let spectator: Spectator<DotRolePermissionsIframeComponent>;

    const createComponent = createComponentFactory({
        component: DotRolePermissionsIframeComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(DotRolesStore, {
                selectedRoleId: jest.fn().mockReturnValue(null)
            })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should render the empty state when no role is selected', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('permissions-iframe'))).toBeNull();
    });

    it('should render an iframe pointing at the wrapper JSP when a role is selected', () => {
        const store = spectator.inject(DotRolesStore, true);
        (store.selectedRoleId as jest.Mock).mockReturnValue('a1b2c3d4-e5f6-7788-99aa-bbccddeeff00');
        spectator.detectChanges();

        const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
        expect(iframe).toBeTruthy();
        expect(iframe.getAttribute('src')).toContain(
            '/html/portlet/ext/roleadmin/view_role_permissions_wrapper.jsp?roleId=a1b2c3d4-e5f6-7788-99aa-bbccddeeff00'
        );
    });
});
