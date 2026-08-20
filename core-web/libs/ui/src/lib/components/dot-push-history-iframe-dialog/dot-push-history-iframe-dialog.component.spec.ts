import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import {
    DotPushHistoryIframeDialogComponent,
    DotPushHistoryIframeDialogData
} from './dot-push-history-iframe-dialog.component';

const FOLDER_PUSH_HISTORY_URL =
    '/html/portlet/ext/folders/push_history.jsp?folderIdentifier=folder-123&popup=true';

const MESSAGES: Record<string, string> = {
    publisher_push_history: 'Push History',
    'dot.push-history.iframe.dialog.no-asset':
        'No asset selected. Push history requires a valid asset.'
};

describe('DotPushHistoryIframeDialogComponent', () => {
    let spectator: Spectator<DotPushHistoryIframeDialogComponent>;

    const configRef: { data: DotPushHistoryIframeDialogData | null | undefined } = {
        data: { url: FOLDER_PUSH_HISTORY_URL }
    };

    const createComponent = createComponentFactory({
        component: DotPushHistoryIframeDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: configRef
            },
            mockProvider(DotMessageService, {
                get: jest.fn((key: string) => MESSAGES[key] ?? key)
            })
        ]
    });

    beforeEach(() => {
        configRef.data = { url: FOLDER_PUSH_HISTORY_URL };
        spectator = createComponent();
    });

    describe('with valid url', () => {
        it('should render push-history-iframe', () => {
            expect(spectator.query(byTestId('push-history-iframe'))).toBeTruthy();
        });

        it('should NOT render push-history-empty', () => {
            expect(spectator.query(byTestId('push-history-empty'))).toBeFalsy();
        });

        it('should set iframe src to the provided url', () => {
            const iframe = spectator.query(byTestId('push-history-iframe')) as HTMLIFrameElement;

            expect(iframe.src).toContain('push_history.jsp');
            expect(iframe.src).toContain('folderIdentifier=folder-123');
            expect(iframe.src).toContain('popup=true');
        });

        it('should use default minHeight of 60vh', () => {
            const iframe = spectator.query(byTestId('push-history-iframe')) as HTMLIFrameElement;

            expect(iframe.style.minHeight).toBe('60vh');
        });

        it('should use custom minHeight when provided', () => {
            configRef.data = { url: FOLDER_PUSH_HISTORY_URL, minHeight: '80vh' };
            spectator = createComponent();
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('push-history-iframe')) as HTMLIFrameElement;

            expect(iframe.style.minHeight).toBe('80vh');
        });

        it('should title the iframe from the push history i18n key', () => {
            const iframe = spectator.query(byTestId('push-history-iframe')) as HTMLIFrameElement;

            expect(iframe.getAttribute('title')).toBe(MESSAGES.publisher_push_history);
        });
    });

    describe('without valid url', () => {
        it('should render push-history-empty when data is undefined', () => {
            configRef.data = undefined;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render push-history-empty when data is null', () => {
            configRef.data = null;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render push-history-empty when url is empty string', () => {
            configRef.data = { url: '' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render push-history-empty when url is an absolute external URL', () => {
            configRef.data = { url: 'https://evil.example.com/steal-cookies' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render push-history-empty when url is a protocol-relative URL', () => {
            configRef.data = { url: '//evil.example.com' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render push-history-empty when url uses javascript: scheme', () => {
            configRef.data = { url: 'javascript:alert(1)' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('push-history-iframe'))).toBeFalsy();
        });

        it('should render the push history empty message', () => {
            configRef.data = undefined;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-history-empty'))?.textContent?.trim()).toBe(
                MESSAGES['dot.push-history.iframe.dialog.no-asset']
            );
        });
    });
});
