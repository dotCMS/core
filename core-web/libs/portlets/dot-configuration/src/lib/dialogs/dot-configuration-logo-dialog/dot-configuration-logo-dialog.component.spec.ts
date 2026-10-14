import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator
} from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { FileUpload } from 'primeng/fileupload';

import {
    DotCompanyConfigurationService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import {
    DotConfigurationLogoDialogComponent,
    LOGO_ACCEPT
} from './dot-configuration-logo-dialog.component';

import { DotConfigurationAssetService } from '../../services/dot-configuration-asset.service';

describe('DotConfigurationLogoDialogComponent', () => {
    let spectator: Spectator<DotConfigurationLogoDialogComponent>;

    const STOCK_LOGO = '/dA/stock-id/asset/dotcms-logo.png';
    const getDotcmsLogo = vi.fn();

    const createComponent = createComponentFactory({
        component: DotConfigurationLogoDialogComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.validation.asset-path':
                        'Use an asset path that starts with /dA/.'
                })
            },
            mockProvider(DynamicDialogRef, { close: vi.fn() }),
            mockProvider(DotConfigurationAssetService, {
                uploadImage: vi.fn().mockReturnValue(of('/dA/new-id/asset/brand.svg'))
            }),
            mockProvider(DotHttpErrorManagerService, { handle: vi.fn() }),
            mockProvider(DotCompanyConfigurationService, { getDotcmsLogo })
        ],
        detectChanges: false
    });

    const open = (current: string, offerDotcmsLogo = false) => {
        spectator = createComponent({
            providers: [
                { provide: DynamicDialogConfig, useValue: { data: { current, offerDotcmsLogo } } }
            ]
        });
        spectator.detectChanges();
    };

    const dotcmsLogoButton = () =>
        spectator
            .query(byTestId('configuration-logo-dotcms-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    const applyButton = () =>
        spectator
            .query(byTestId('configuration-logo-apply-btn'))
            ?.querySelector('button') as HTMLButtonElement;

    const selectFile = (file: File) => {
        spectator.triggerEventHandler(FileUpload, 'onSelect', {
            originalEvent: new Event('change'),
            files: [file],
            currentFiles: [file]
        });
        spectator.detectChanges();
    };

    beforeEach(() => getDotcmsLogo.mockReturnValue(of(STOCK_LOGO)));

    afterEach(() => vi.clearAllMocks());

    describe('Use dotCMS Logo', () => {
        it('is not offered for the navbar logo', () => {
            open('');

            expect(spectator.query(byTestId('configuration-logo-dotcms-btn'))).not.toExist();
            expect(getDotcmsLogo).not.toHaveBeenCalled();
        });

        it('puts the stock logo in the field, applied like any other path', () => {
            open('/dA/logo-id/asset/custom.png', true);

            spectator.click(dotcmsLogoButton());
            spectator.detectChanges();
            spectator.click(applyButton());

            expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(STOCK_LOGO);
        });

        it('is disabled, without an error, when the instance has no stock logo', () => {
            getDotcmsLogo.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
            open('/dA/logo-id/asset/custom.png', true);

            expect(dotcmsLogoButton()).toBeDisabled();
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('reports any other failure and stays disabled', () => {
            const error = new HttpErrorResponse({ status: 500 });
            getDotcmsLogo.mockReturnValue(throwError(() => error));
            open('/dA/logo-id/asset/custom.png', true);

            expect(dotcmsLogoButton()).toBeDisabled();
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        });
    });

    it('lets the picker offer the common web image formats', () => {
        open('/dA/logo-id/asset/logo.svg');

        expect(spectator.query(FileUpload)?.accept).toBe(LOGO_ACCEPT);
    });

    it.each(['.svg', '.png', '.jpg', '.jpeg', '.gif', '.webp'])('accepts %s', (extension) => {
        expect(LOGO_ACCEPT.split(',')).toContain(extension);
    });

    it('does not accept files that are not images', () => {
        expect(LOGO_ACCEPT).not.toMatch(/pdf|zip/);
    });

    it('starts with the current logo selected', async () => {
        open('/dA/logo-id/asset/logo.svg');
        await spectator.fixture.whenStable();

        expect(spectator.query(byTestId('configuration-logo-path'))).toHaveValue(
            '/dA/logo-id/asset/logo.svg'
        );
        expect(spectator.query(byTestId('configuration-logo-selected'))).toContainText('logo.svg');
    });

    it('uploads a file and applies its asset path', () => {
        open('/dA/logo-id/asset/logo.svg');
        const file = new File(['<svg/>'], 'brand.svg', { type: 'image/svg+xml' });

        selectFile(file);
        spectator.click(applyButton());

        expect(spectator.inject(DotConfigurationAssetService).uploadImage).toHaveBeenCalledWith(
            file
        );
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(
            '/dA/new-id/asset/brand.svg'
        );
    });

    it('applies an asset path typed by hand', () => {
        open('/dA/logo-id/asset/logo.svg');

        spectator.typeInElement(
            '/dA/other-id/asset/other.png',
            byTestId('configuration-logo-path')
        );
        spectator.detectChanges();
        spectator.click(applyButton());

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(
            '/dA/other-id/asset/other.png'
        );
    });

    it('accepts an asset whose file name has spaces and parentheses', () => {
        open('/dA/logo-id/asset/logo.svg');

        spectator.typeInElement(
            '/dA/fa15df96/asset/image (8).png',
            byTestId('configuration-logo-path')
        );
        spectator.detectChanges();
        spectator.click(applyButton());

        expect(spectator.query(byTestId('configuration-logo-path-error'))).not.toExist();
        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith(
            '/dA/fa15df96/asset/image (8).png'
        );
    });

    it('rejects a path outside the asset store and blocks Apply', () => {
        open('/dA/logo-id/asset/logo.svg');

        spectator.typeInElement('/application/logo.svg', byTestId('configuration-logo-path'));
        spectator.detectChanges();

        expect(spectator.query(byTestId('configuration-logo-path-error'))).toHaveText(
            'Use an asset path that starts with /dA/.'
        );
        expect(applyButton()).toBeDisabled();
    });

    it('does not allow leaving the logo empty', () => {
        open('');

        expect(applyButton()).toBeDisabled();
    });

    it('opens empty without showing an error', () => {
        open('');

        expect(spectator.query(byTestId('configuration-logo-path-error'))).not.toExist();
        expect(spectator.query(byTestId('configuration-logo-path'))).not.toHaveAttribute(
            'aria-invalid',
            'true'
        );
    });

    it('reports a failed upload and keeps the current logo', () => {
        open('/dA/logo-id/asset/logo.svg');
        const error = new Error('upload failed');
        vi.mocked(spectator.inject(DotConfigurationAssetService).uploadImage).mockReturnValueOnce(
            throwError(() => error)
        );

        selectFile(new File(['x'], 'brand.png'));

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        expect(spectator.query(byTestId('configuration-logo-selected'))).toContainText('logo.svg');
    });

    it('closes without a value on cancel', () => {
        open('/dA/logo-id/asset/logo.svg');

        spectator.click(
            spectator
                .query(byTestId('configuration-logo-cancel-btn'))
                ?.querySelector('button') as HTMLButtonElement
        );

        expect(spectator.inject(DynamicDialogRef).close).toHaveBeenCalledWith();
    });
});
