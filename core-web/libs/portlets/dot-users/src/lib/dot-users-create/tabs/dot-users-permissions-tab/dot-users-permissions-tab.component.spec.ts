// Mocked so the same-origin guard's rejection branch is reachable: the
// component builds its URL from a hard-coded prefix, so no `userId` can
// make the real predicate return false. Must precede the import.
jest.mock('@dotcms/utils', () => ({
    ...jest.requireActual('@dotcms/utils'),
    isSameOriginRelativeUrl: jest.fn()
}));

import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotMessageService } from '@dotcms/data-access';
import { isSameOriginRelativeUrl } from '@dotcms/utils';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUsersPermissionsTabComponent } from './dot-users-permissions-tab.component';

const WRAPPER_JSP = '/html/portlet/ext/useradmin/view_users_permissions_wrapper.jsp';

const mockIsSameOriginRelativeUrl = isSameOriginRelativeUrl as unknown as jest.Mock;

const MESSAGES = {
    'users.dialog.permissions.create-mode':
        'Save this user first to configure per-resource permissions.',
    'users.dialog.permissions.iframe.title': 'User permissions',
    'users.dialog.permissions.unavailable': 'Permissions view unavailable.',
    'users.dialog.permissions.timeout': 'Taking longer than expected.',
    'users.dialog.permissions.retry': 'Try again'
};

describe('DotUsersPermissionsTabComponent', () => {
    let spectator: Spectator<DotUsersPermissionsTabComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersPermissionsTabComponent,
        imports: [NoopAnimationsModule],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    const getIframe = () => spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;

    /**
     * jsdom never fetches the JSP, so the real `contentDocument` is
     * always a blank document. Stand in for what the wrapper JSP
     * actually returns: `top_inc.jsp` / `messages_inc.jsp` leave
     * `<script>` elements in the body on every path, and only the
     * granted path appends the ready marker.
     */
    const stubJspResponse = (iframe: HTMLIFrameElement, { marker }: { marker: boolean }) => {
        Object.defineProperty(iframe, 'contentDocument', {
            value: {
                body: { innerHTML: '<script src="/html/js/messages.js"></script>' },
                getElementById: (id: string) =>
                    marker && id === 'dot-permissions-ready' ? document.createElement('div') : null
            },
            configurable: true
        });
    };

    beforeEach(() => {
        // Default to the real predicate's answer for every valid URL the
        // component can build; individual tests override.
        mockIsSameOriginRelativeUrl.mockImplementation((url: string) => Boolean(url));
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

    describe('url construction', () => {
        it('should URL-encode userId values with reserved chars', () => {
            spectator = createComponent({ props: { userId: 'dotcms.org.42/admin' } });
            const url = spectator.component.$permissionsUrl();
            // URLSearchParams encodes `/` as %2F and `.` stays literal.
            expect(url).toContain('userId=dotcms.org.42%2Fadmin');
        });

        it('should confine a userId that looks like a foreign host to the query string', () => {
            spectator = createComponent({ props: { userId: '//evil.example.com' } });
            const url = spectator.component.$permissionsUrl();

            expect(url.startsWith(`${WRAPPER_JSP}?`)).toBe(true);
            expect(getIframe().src.startsWith(window.location.origin)).toBe(true);
        });

        it('should confine a javascript: scheme userId to the query string', () => {
            spectator = createComponent({ props: { userId: 'javascript:alert(1)' } });

            expect(spectator.component.$permissionsUrl().startsWith(`${WRAPPER_JSP}?`)).toBe(true);
            expect(getIframe().src.startsWith(window.location.origin)).toBe(true);
        });
    });

    describe('same-origin guard', () => {
        it('should pass the built url to the guard before trusting it', () => {
            spectator = createComponent({ props: { userId: 'user-42' } });

            expect(mockIsSameOriginRelativeUrl).toHaveBeenCalledWith(
                spectator.component.$permissionsUrl()
            );
        });

        it('should render no iframe when the guard rejects the url', () => {
            mockIsSameOriginRelativeUrl.mockReturnValue(false);
            spectator = createComponent({ props: { userId: 'user-42' } });

            // The URL still builds — it is the guard, not the builder,
            // that refuses to hand it to the sanitizer.
            expect(spectator.component.$permissionsUrl()).not.toBe('');
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
            expect(spectator.query(byTestId('users-permissions-tab-empty'))).toBeTruthy();
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

        it('should show the unavailable message when the ready marker is absent', () => {
            const iframe = getIframe();
            stubJspResponse(iframe, { marker: false });

            spectator.dispatchFakeEvent(iframe, 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-unavailable'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeFalsy();
            expect(iframe.classList).toContain('opacity-0');
        });

        it('should stay unavailable even though the failure path still writes scripts to the body', () => {
            const iframe = getIframe();
            stubJspResponse(iframe, { marker: false });

            spectator.dispatchFakeEvent(iframe, 'load');

            // Guards the regression: an emptiness check would read this
            // body as content and wrongly reveal a blank pane.
            expect(iframe.contentDocument?.body.innerHTML).not.toBe('');
            expect(spectator.query(byTestId('permissions-unavailable'))).toBeTruthy();
        });

        it('should reveal the iframe when the ready marker is present', () => {
            const iframe = getIframe();
            stubJspResponse(iframe, { marker: true });

            spectator.dispatchFakeEvent(iframe, 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-unavailable'))).toBeFalsy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeFalsy();
            expect(iframe.classList).not.toContain('opacity-0');
        });

        it('should treat an unreadable contentDocument as loaded rather than hiding a rendered child', () => {
            const iframe = getIframe();
            Object.defineProperty(iframe, 'contentDocument', {
                get() {
                    throw new DOMException('blocked');
                },
                configurable: true
            });

            spectator.dispatchFakeEvent(iframe, 'load');
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-unavailable'))).toBeFalsy();
            expect(iframe.classList).not.toContain('opacity-0');
        });
    });

    describe('load timeout', () => {
        beforeEach(() => {
            jest.useFakeTimers();
            spectator = createComponent({ props: { userId: 'user-42' } });
        });

        afterEach(() => {
            jest.useRealTimers();
        });

        it('should stay on the skeleton until the timeout elapses', () => {
            jest.advanceTimersByTime(19_000);
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-loading'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-timeout'))).toBeFalsy();
        });

        it('should surface the timeout message when load never fires', () => {
            jest.advanceTimersByTime(20_000);
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-timeout'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeFalsy();
        });

        it('should NOT time out a navigation that already loaded', () => {
            const iframe = getIframe();
            stubJspResponse(iframe, { marker: true });
            spectator.dispatchFakeEvent(iframe, 'load');

            jest.advanceTimersByTime(60_000);
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-timeout'))).toBeFalsy();
            expect(iframe.classList).not.toContain('opacity-0');
        });

        it('should re-navigate and re-arm the watchdog on retry', () => {
            jest.advanceTimersByTime(20_000);
            spectator.detectChanges();

            const before = spectator.component.$permissionsUrl();
            spectator.click(
                spectator
                    .query(byTestId('permissions-retry-btn'))!
                    .querySelector('button') as HTMLButtonElement
            );
            spectator.detectChanges();

            // New URL -> new navigation -> back to the skeleton.
            expect(spectator.component.$permissionsUrl()).not.toBe(before);
            expect(spectator.component.$permissionsUrl()).toContain('_retry=1');
            expect(spectator.query(byTestId('permissions-timeout'))).toBeFalsy();
            expect(spectator.query(byTestId('permissions-loading'))).toBeTruthy();

            // And the fresh navigation can time out again.
            jest.advanceTimersByTime(20_000);
            spectator.detectChanges();
            expect(spectator.query(byTestId('permissions-timeout'))).toBeTruthy();
        });
    });
});
