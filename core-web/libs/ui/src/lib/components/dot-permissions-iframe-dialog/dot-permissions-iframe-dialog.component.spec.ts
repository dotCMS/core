import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import {
    DotPermissionsIframeDialogComponent,
    DotPermissionsIframeDialogData
} from './dot-permissions-iframe-dialog.component';

describe('DotPermissionsIframeDialogComponent', () => {
    let spectator: Spectator<DotPermissionsIframeDialogComponent>;

    const configRef: { data: DotPermissionsIframeDialogData | null | undefined } = {
        data: { url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=inode-123' }
    };

    const createComponent = createComponentFactory({
        component: DotPermissionsIframeDialogComponent,
        providers: [
            {
                provide: DynamicDialogConfig,
                useValue: configRef
            }
        ]
    });

    beforeEach(() => {
        configRef.data = {
            url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=inode-123'
        };
        spectator = createComponent();
    });

    describe('with valid url', () => {
        it('should render permissions-iframe', () => {
            expect(spectator.query(byTestId('permissions-iframe'))).toBeTruthy();
        });

        it('should NOT render permissions-empty', () => {
            expect(spectator.query(byTestId('permissions-empty'))).toBeFalsy();
        });

        it('should set iframe src to the provided url', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.src).toContain('permissions.jsp');
            expect(iframe.src).toContain('categoryInode=inode-123');
        });

        it('should use default minHeight of 60vh', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.style.minHeight).toBe('60vh');
        });

        it('should use custom minHeight when provided', () => {
            configRef.data = {
                url: '/some/path',
                minHeight: '80vh'
            };
            spectator = createComponent();
            spectator.detectChanges();

            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.style.minHeight).toBe('80vh');
        });

        it('should have title "Permissions"', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.getAttribute('title')).toBe('Permissions');
        });
    });

    describe('without valid url', () => {
        it('should render permissions-empty when data is undefined', () => {
            configRef.data = undefined;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when data is null', () => {
            configRef.data = null;
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url is empty string', () => {
            configRef.data = { url: '' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url is an absolute external URL', () => {
            configRef.data = { url: 'https://evil.example.com/steal-cookies' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url is a protocol-relative URL', () => {
            configRef.data = { url: '//evil.example.com' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url uses javascript: scheme', () => {
            configRef.data = { url: 'javascript:alert(1)' };
            spectator = createComponent();
            spectator.detectChanges();

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });
    });
});
