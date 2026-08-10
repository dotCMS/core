import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DotPermissionsIframeComponent } from './dot-permissions-iframe.component';

describe('DotPermissionsIframeComponent', () => {
    let spectator: Spectator<DotPermissionsIframeComponent>;

    const createComponent = createComponentFactory({
        component: DotPermissionsIframeComponent
    });

    describe('with valid url', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    url: '/html/portlet/ext/categories/permissions.jsp?categoryInode=inode-123'
                }
            });
        });

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
            spectator = createComponent({
                props: {
                    url: '/some/path',
                    minHeight: '34rem'
                }
            });
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.style.minHeight).toBe('34rem');
        });

        it('should have title "Permissions"', () => {
            const iframe = spectator.query(byTestId('permissions-iframe')) as HTMLIFrameElement;
            expect(iframe.getAttribute('title')).toBe('Permissions');
        });
    });

    describe('without valid url', () => {
        it('should render permissions-empty when url is empty', () => {
            spectator = createComponent({ props: { url: '' } });

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url is an absolute external URL', () => {
            spectator = createComponent({
                props: { url: 'https://evil.example.com/steal-cookies' }
            });

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url is a protocol-relative URL', () => {
            spectator = createComponent({ props: { url: '//evil.example.com' } });

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });

        it('should render permissions-empty when url uses javascript: scheme', () => {
            spectator = createComponent({ props: { url: 'javascript:alert(1)' } });

            expect(spectator.query(byTestId('permissions-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('permissions-iframe'))).toBeFalsy();
        });
    });
});
