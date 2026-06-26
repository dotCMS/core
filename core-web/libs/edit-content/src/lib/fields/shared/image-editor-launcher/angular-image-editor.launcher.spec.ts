import { expect } from '@jest/globals';
import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { BehaviorSubject, Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import { DotPropertiesService } from '@dotcms/data-access';
import { DotCMSTempFile, FeaturedFlags } from '@dotcms/dotcms-models';
import { DotImageEditorComponent, ImageEditorOpenParams } from '@dotcms/image-editor';

import { AngularImageEditorLauncher } from './angular-image-editor.launcher';

describe('AngularImageEditorLauncher', () => {
    let spectator: SpectatorService<AngularImageEditorLauncher>;
    let onClose: Subject<DotCMSTempFile | undefined>;
    // Drives the new-image-editor flag; `next()` flows through `toSignal` so the same
    // service instance reflects on/off without re-creating it.
    const featureFlag$ = new BehaviorSubject<boolean>(true);
    const getFeatureFlag = jest.fn(() => featureFlag$);

    const params: ImageEditorOpenParams = {
        inode: 'inode-1',
        variable: 'binaryField',
        fieldName: 'binary'
    };

    const createService = createServiceFactory({
        service: AngularImageEditorLauncher,
        providers: [mockProvider(DotPropertiesService, { getFeatureFlag })],
        mocks: [DialogService]
    });

    beforeEach(() => {
        // Default the flag ON so the open() tests below run the Angular path; the
        // gating itself is covered by the dedicated tests.
        featureFlag$.next(true);
        onClose = new Subject<DotCMSTempFile | undefined>();
        spectator = createService();
        spectator.inject(DialogService).open.mockReturnValue({ onClose });
    });

    it('should be available when FEATURE_FLAG_NEW_IMAGE_EDITOR is on', () => {
        expect(spectator.service.isAvailable()).toBe(true);
        expect(getFeatureFlag).toHaveBeenCalledWith(FeaturedFlags.FEATURE_FLAG_NEW_IMAGE_EDITOR);
    });

    it('should NOT be available when the feature flag is off', () => {
        featureFlag$.next(false);

        expect(spectator.service.isAvailable()).toBe(false);
    });

    it('should open the DotImageEditorComponent with a headerless, closable dialog that owns Esc', () => {
        spectator.service.open(params).subscribe();

        expect(spectator.inject(DialogService).open).toHaveBeenCalledWith(
            DotImageEditorComponent,
            expect.objectContaining({
                data: params,
                modal: true,
                // The editor renders its own header; PrimeNG's chrome header is hidden.
                showHeader: false,
                closable: true,
                // Esc is handled inside the editor (through its unsaved-changes guard),
                // so PrimeNG's direct close-on-escape is disabled.
                closeOnEscape: false
            })
        );
    });

    it('should resolve the temp file emitted on close', () => {
        const tempFile = { id: 'temp-123' } as DotCMSTempFile;
        let result: DotCMSTempFile | null | undefined;

        spectator.service.open(params).subscribe((value) => (result = value));
        onClose.next(tempFile);

        expect(result).toEqual(tempFile);
    });

    it('should resolve null when the dialog closes without a value', () => {
        let result: DotCMSTempFile | null | undefined;

        spectator.service.open(params).subscribe((value) => (result = value));
        onClose.next(undefined);

        expect(result).toBeNull();
    });
});
