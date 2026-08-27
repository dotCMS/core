import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersPermissionsTabComponent } from './dot-users-permissions-tab.component';

const WRAPPER_JSP = '/html/portlet/ext/useradmin/view_users_permissions_wrapper.jsp';

const MESSAGES = {
    'users.dialog.permissions.create-mode':
        'Save this user first to configure per-resource permissions.',
    'users.dialog.permissions.iframe.title': 'User permissions',
    'users.dialog.permissions.unavailable': 'Permissions view unavailable.'
};

describe('DotUsersPermissionsTabComponent', () => {
    let spectator: Spectator<DotUsersPermissionsTabComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersPermissionsTabComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    const getIframe = () => spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;

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

        it('should render the permissions iframe', () => {
            expect(getIframe()).toBeTruthy();
        });

        it('should target the JSP wrapper with the userId query param', () => {
            const url = spectator.component.$permissionsUrl();
            expect(url).toContain(WRAPPER_JSP);
            expect(url).toContain('userId=user-42');
        });

        it('should include popup=true — legacy top_inc.jsp renders blank without it', () => {
            expect(spectator.component.$permissionsUrl()).toContain('popup=true');
        });

        it('should set the iframe src to the built url', () => {
            const iframe = getIframe();
            expect(iframe.src).toContain(WRAPPER_JSP);
            expect(iframe.src).toContain('userId=user-42');
        });

        it('should render the translated iframe title', () => {
            expect(getIframe().getAttribute('title')).toBe('User permissions');
        });
    });

    describe('url safety', () => {
        it('should URL-encode userId values with reserved chars', () => {
            spectator = createComponent({ props: { userId: 'dotcms.org.42/admin' } });
            const url = spectator.component.$permissionsUrl();
            // URLSearchParams encodes `/` as %2F and `.` stays literal.
            expect(url).toContain('userId=dotcms.org.42%2Fadmin');
        });

        it('should keep the url same-origin when userId looks like a foreign host', () => {
            spectator = createComponent({ props: { userId: '//evil.example.com' } });
            const url = spectator.component.$permissionsUrl();

            expect(url.startsWith(`${WRAPPER_JSP}?`)).toBe(true);
            expect(getIframe().src.startsWith(window.location.origin)).toBe(true);
        });

        it('should keep the url same-origin when userId carries a javascript: scheme', () => {
            spectator = createComponent({ props: { userId: 'javascript:alert(1)' } });

            expect(spectator.component.$permissionsUrl().startsWith(`${WRAPPER_JSP}?`)).toBe(true);
            expect(getIframe().src.startsWith(window.location.origin)).toBe(true);
        });
    });

    describe('load lifecycle', () => {
        beforeEach(() => {
            spectator = createComponent({ props: { userId: 'user-42' } });
        });

        it('should show the skeleton and hide the iframe before the load event', () => {
            expect(spectator.query(byTestId('permissions-loading'))).toBeTruthy();
            expect(getIframe().classList).toContain('opacity-0');
        });

        it('should show the unavailable message when the JSP renders a blank body', () => {
            spectator.dispatchFakeEvent(getIframe(), 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-unavailable'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeFalsy();
        });

        it('should reveal the iframe when the JSP renders content', () => {
            const iframe = getIframe();
            // jsdom never fetches the JSP, so its `contentDocument` is
            // always a blank document — stub it to stand in for a
            // rendered response.
            Object.defineProperty(iframe, 'contentDocument', {
                value: { body: { innerHTML: '<div>permissions</div>' } },
                configurable: true
            });

            spectator.dispatchFakeEvent(iframe, 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-unavailable'))).toBeFalsy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeFalsy();
            expect(iframe.classList).not.toContain('opacity-0');
        });
    });
});
