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
    });

    it('forwards the DynamicDialogConfig url and default minHeight to the iframe', () => {
        spectator = createComponent();
        const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;

        expect(iframe).toBeTruthy();
        expect(iframe.src).toContain('categoryInode=inode-123');
        expect(iframe.style.minHeight).toBe('60vh');
    });

    it('forwards a custom minHeight from the config', () => {
        configRef.data = { url: '/html/portlet/ext/folders/permissions.jsp', minHeight: '80vh' };
        spectator = createComponent();
        const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;

        expect(iframe.style.minHeight).toBe('80vh');
    });

    it('falls back to the empty state when the config data is missing', () => {
        configRef.data = undefined;
        spectator = createComponent();

        expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
    });

    it('falls back to the empty state when the url is not a safe relative path', () => {
        configRef.data = { url: 'https://evil.example.com' };
        spectator = createComponent();

        expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
    });
});
