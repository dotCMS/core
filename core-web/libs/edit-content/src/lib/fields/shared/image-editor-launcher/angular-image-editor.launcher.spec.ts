import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { Subject } from 'rxjs';

import { DialogService } from 'primeng/dynamicdialog';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotImageEditorComponent, ImageEditorOpenParams } from '@dotcms/image-editor';

import { AngularImageEditorLauncher } from './angular-image-editor.launcher';

describe('AngularImageEditorLauncher', () => {
    let spectator: SpectatorService<AngularImageEditorLauncher>;
    let onClose: Subject<DotCMSTempFile | undefined>;

    const params: ImageEditorOpenParams = {
        inode: 'inode-1',
        variable: 'binaryField',
        fieldName: 'binary'
    };

    const createService = createServiceFactory({
        service: AngularImageEditorLauncher,
        mocks: [DialogService]
    });

    beforeEach(() => {
        onClose = new Subject<DotCMSTempFile | undefined>();
        spectator = createService();
        spectator.inject(DialogService).open.mockReturnValue({ onClose });
    });

    it('should always be available (the new editor is the editor for the new Edit Content)', () => {
        expect(spectator.service.isAvailable()).toBe(true);
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
