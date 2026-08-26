import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import {
    DotJspIframeDialogComponent,
    DotJspIframeDialogData
} from './dot-jsp-iframe-dialog.component';

describe('DotJspIframeDialogComponent', () => {
    let spectator: Spectator<DotJspIframeDialogComponent>;

    const PUSH_HISTORY: DotJspIframeDialogData = {
        url: '/html/portlet/ext/folders/push_history.jsp?folderIdentifier=inode-123&popup=true',
        titleKey: 'publisher_push_history',
        emptyKey: 'dot.push-history.iframe.dialog.no-asset',
        testIdPrefix: 'push-history'
    };

    const PERMISSIONS: DotJspIframeDialogData = {
        url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=inode-123',
        titleKey: 'Permissions',
        emptyKey: 'dot.permissions.iframe.dialog.no-asset',
        testIdPrefix: 'permissions'
    };

    const configRef: { data: DotJspIframeDialogData | null | undefined } = { data: PUSH_HISTORY };

    const createComponent = createComponentFactory({
        component: DotJspIframeDialogComponent,
        providers: [{ provide: DynamicDialogConfig, useValue: configRef }]
    });

    const build = (data: typeof configRef.data) => {
        configRef.data = data;
        spectator = createComponent();
        spectator.detectChanges();
    };

    // Parameterised over both call sites rather than duplicated per caller: one component now serves
    // them, so a case that only held for one of them would be the seam where they diverge again.
    describe.each([
        ['push history', PUSH_HISTORY],
        ['permissions', PERMISSIONS]
    ])('for %s', (_name, data) => {
        beforeEach(() => build(data));

        it('should render the iframe under the caller test id', () => {
            expect(spectator.query(byTestId(`${data.testIdPrefix}-iframe`))).toBeTruthy();
        });

        it('should not render the empty state', () => {
            expect(spectator.query(byTestId(`${data.testIdPrefix}-empty`))).toBeFalsy();
        });

        it('should point the iframe at the given url', () => {
            const iframe = spectator.query(
                byTestId(`${data.testIdPrefix}-iframe`)
            ) as HTMLIFrameElement;

            expect(iframe.src).toContain(data.url.split('?')[0]);
            expect(iframe.src).toContain('inode-123');
        });

        it('should title the iframe from the caller key', () => {
            const iframe = spectator.query(
                byTestId(`${data.testIdPrefix}-iframe`)
            ) as HTMLIFrameElement;

            expect(iframe.getAttribute('title')).toBe(data.titleKey);
        });

        it('should default the min height to 60vh', () => {
            const iframe = spectator.query(
                byTestId(`${data.testIdPrefix}-iframe`)
            ) as HTMLIFrameElement;

            expect(iframe.style.minHeight).toBe('60vh');
        });
    });

    it('should use a custom min height when given one', () => {
        build({ ...PUSH_HISTORY, minHeight: '80vh' });

        const iframe = spectator.query(byTestId('push-history-iframe')) as HTMLIFrameElement;

        expect(iframe.style.minHeight).toBe('80vh');
    });

    it('should keep each caller test ids distinct', () => {
        build(PUSH_HISTORY);
        expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();

        build(PERMISSIONS);
        expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
    });

    // A caller that forgets the prefix must not silently claim a test id another caller selects on.
    it('should fall back to a neutral prefix rather than a bare one', () => {
        build({ ...PUSH_HISTORY, testIdPrefix: undefined as unknown as string });

        expect(spectator.query(byTestId('jsp-iframe'))).toBeTruthy();
    });

    describe('rejected urls', () => {
        // Each of these resolves off-origin or is not a URL the dialog may load. They render the
        // empty state, never an iframe.
        const REJECTED: [string, unknown][] = [
            ['data is undefined', undefined],
            ['data is null', null],
            ['url is empty', { ...PUSH_HISTORY, url: '' }],
            [
                'url is absolute and external',
                { ...PUSH_HISTORY, url: 'https://evil.example.com/x' }
            ],
            ['url is protocol-relative', { ...PUSH_HISTORY, url: '//evil.example.com' }],
            ['url uses the javascript: scheme', { ...PUSH_HISTORY, url: 'javascript:alert(1)' }],
            // Browsers normalise the backslash into the authority position, so this resolves
            // cross-origin despite starting with a single slash.
            [
                'url escapes the origin with a backslash',
                { ...PUSH_HISTORY, url: '/\\evil.example.com' }
            ]
        ];

        it.each(REJECTED)('should render the empty state when %s', (_case, data) => {
            build(data as DotJspIframeDialogData | null | undefined);

            // Prefix falls back when `data` is absent entirely, so both ids are checked.
            const iframe =
                spectator.query(byTestId('push-history-iframe')) ??
                spectator.query(byTestId('jsp-iframe'));
            const empty =
                spectator.query(byTestId('push-history-empty')) ??
                spectator.query(byTestId('jsp-empty'));

            expect(iframe).toBeFalsy();
            expect(empty).toBeTruthy();
        });
    });
});
