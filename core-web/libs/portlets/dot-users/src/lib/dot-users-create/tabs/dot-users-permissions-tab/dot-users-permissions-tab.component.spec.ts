import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersPermissionsTabComponent } from './dot-users-permissions-tab.component';

const MESSAGES = {
    'users.dialog.permissions.create-mode':
        'Save this user first to configure per-resource permissions.',
    'users.dialog.permissions.iframe.title': 'User permissions',
    'dot.permissions.iframe.dialog.no-asset': 'No asset selected.',
    'dot.permissions.iframe.dialog.empty-body': 'Permissions view unavailable.'
};

describe('DotUsersPermissionsTabComponent', () => {
    let spectator: Spectator<DotUsersPermissionsTabComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersPermissionsTabComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    describe('create mode (no userId)', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { userId: null } });
        });

        it('should render the create-mode empty state', () => {
            expect(spectator.query(byTestId('users-permissions-tab-empty'))).toBeTruthy();
        });

        it('should NOT render the iframe', () => {
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should resolve $permissionsUrl to an empty string', () => {
            expect(spectator.component.$permissionsUrl()).toBe('');
        });
    });

    describe('edit mode (with userId)', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { userId: 'user-42' } });
        });

        it('should NOT render the create-mode empty state', () => {
            expect(spectator.query(byTestId('users-permissions-tab-empty'))).toBeFalsy();
        });

        it('should render the shared permissions iframe', () => {
            expect(spectator.query(byTestId('permissions-iframe'))).toBeTruthy();
        });

        it('should target the JSP wrapper with the userId query param', () => {
            const url = spectator.component.$permissionsUrl();
            expect(url).toContain('/html/portlet/ext/useradmin/permissions.jsp');
            expect(url).toContain('userId=user-42');
        });

        it('should include popup=true — legacy top_inc.jsp renders blank without it', () => {
            expect(spectator.component.$permissionsUrl()).toContain('popup=true');
        });

        it('should URL-encode userId values with reserved chars', () => {
            spectator = createComponent({ props: { userId: 'dotcms.org.42/admin' } });
            const url = spectator.component.$permissionsUrl();
            // URLSearchParams encodes `/` as %2F and `.` stays literal.
            expect(url).toContain('userId=dotcms.org.42%2Fadmin');
        });

        it('should pass the translated title through to the iframe', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.getAttribute('title')).toBe('User permissions');
        });
    });
});
