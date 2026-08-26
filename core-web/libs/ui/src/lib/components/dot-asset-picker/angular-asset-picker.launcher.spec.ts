import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotSite } from '@dotcms/dotcms-models';
import { createFakeSite } from '@dotcms/utils-testing';

import { AngularAssetPickerLauncher } from './angular-asset-picker.launcher';
import { DotAssetPickerComponent } from './dot-asset-picker.component';

const SITE: DotSite = createFakeSite({ identifier: 'site-1', hostname: 'dotcms.com' });

describe('AngularAssetPickerLauncher', () => {
    let spectator: SpectatorService<AngularAssetPickerLauncher>;
    let launcher: AngularAssetPickerLauncher;
    let dialogService: DialogService;
    let ref: DynamicDialogRef;

    const createService = createServiceFactory({
        service: AngularAssetPickerLauncher,
        providers: [mockProvider(DialogService)]
    });

    beforeEach(() => {
        spectator = createService();
        launcher = spectator.service;
        // Deliberately resolved from the injector and then *passed in*: the launcher must not reach
        // for a `DialogService` of its own — see the token's docs.
        dialogService = spectator.inject(DialogService);
        ref = { onClose: jest.fn(), close: jest.fn() } as unknown as DynamicDialogRef;
        (dialogService.open as jest.Mock).mockReturnValue(ref);
    });

    it('should open the new AssetPicker through the dialog service it is handed', () => {
        launcher.open(dialogService, { mode: 'image', site: SITE });

        expect(dialogService.open).toHaveBeenCalledTimes(1);
        expect((dialogService.open as jest.Mock).mock.calls[0][0]).toBe(DotAssetPickerComponent);
    });

    it('should return the dialog ref so the caller keeps owning teardown', () => {
        expect(launcher.open(dialogService, { mode: 'file', site: SITE })).toBe(ref);
    });

    it('should translate the entry options into picker filters', () => {
        launcher.open(dialogService, {
            mode: 'video',
            site: SITE,
            title: 'pick a video',
            languageId: '2'
        });

        const config = (dialogService.open as jest.Mock).mock.calls[0][1];

        expect(config.data).toEqual(
            expect.objectContaining({
                site: SITE,
                mimeTypes: ['video/*'],
                title: 'pick a video',
                languageId: '2'
            })
        );
    });

    it('should apply the picker dialog contract', () => {
        launcher.open(dialogService, { mode: 'image', site: SITE });

        const config = (dialogService.open as jest.Mock).mock.calls[0][1];

        // The picker draws its own header; the flags belong to it, not to the caller.
        expect(config.showHeader).toBe(false);
        expect(config.modal).toBe(true);
    });

    it('should let the caller lift the dialog above a fullscreen shell', () => {
        launcher.open(dialogService, { mode: 'image', site: SITE }, { baseZIndex: 10050 });

        expect((dialogService.open as jest.Mock).mock.calls[0][1].baseZIndex).toBe(10050);
    });
});
