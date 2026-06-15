import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService, SpyObject } from '@ngneat/spectator/jest';

import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import { DotLegacyImageEditorDialogComponent } from './dot-legacy-image-editor-dialog.component';
import { DotLegacyImageEditorLauncherService } from './dot-legacy-image-editor-launcher.service';

describe('DotLegacyImageEditorLauncherService', () => {
    let spectator: SpectatorService<DotLegacyImageEditorLauncherService>;
    let dialogService: SpyObject<DialogService>;
    let dialogRef: DynamicDialogRef;

    const createService = createServiceFactory({
        service: DotLegacyImageEditorLauncherService,
        mocks: [DialogService]
    });

    const variable = 'binaryField';
    const openEventName = `binaryField-open-image-editor-${variable}`;
    const tempEventName = `binaryField-tempfile-${variable}`;
    const closeEventName = `binaryField-close-image-editor-${variable}`;

    beforeEach(() => {
        dialogRef = { close: jest.fn() } as unknown as DynamicDialogRef;
        spectator = createService();
        dialogService = spectator.inject(DialogService);
        dialogService.open.mockReturnValue(dialogRef);
    });

    afterEach(() => {
        spectator.service.stopListening();
    });

    it('should open dialog when open-image-editor event is dispatched', () => {
        spectator.service.listen(variable);

        document.dispatchEvent(
            new CustomEvent(openEventName, {
                detail: {
                    inode: 'inode-1',
                    tempId: 'temp-1',
                    variable
                }
            })
        );

        expect(dialogService.open).toHaveBeenCalledWith(
            DotLegacyImageEditorDialogComponent,
            expect.objectContaining({
                appendTo: 'body',
                modal: true,
                data: {
                    inode: 'inode-1',
                    tempId: 'temp-1',
                    variable
                }
            })
        );
    });

    it('should re-dispatch tempfile event when postMessage tempfile is received', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');
        const tempFile = { id: 'temp-123' } as DotCMSTempFile;

        spectator.service.listen(variable);
        document.dispatchEvent(
            new CustomEvent(openEventName, {
                detail: { inode: 'inode-1', tempId: 'temp-1', variable }
            })
        );

        window.dispatchEvent(
            new MessageEvent('message', {
                origin: window.location.origin,
                data: {
                    source: 'dot-image-editor',
                    type: 'tempfile',
                    tempFile
                }
            })
        );

        expect(dispatchSpy).toHaveBeenCalledWith(
            expect.objectContaining({
                type: tempEventName,
                detail: { tempFile }
            })
        );
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should re-dispatch close event when postMessage close is received', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        spectator.service.listen(variable);
        document.dispatchEvent(
            new CustomEvent(openEventName, {
                detail: { inode: 'inode-1', tempId: 'temp-1', variable }
            })
        );

        window.dispatchEvent(
            new MessageEvent('message', {
                origin: window.location.origin,
                data: {
                    source: 'dot-image-editor',
                    type: 'close'
                }
            })
        );

        expect(dispatchSpy).toHaveBeenCalledWith(
            expect.objectContaining({
                type: closeEventName
            })
        );
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should ignore postMessage from a different origin', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        spectator.service.listen(variable);

        window.dispatchEvent(
            new MessageEvent('message', {
                origin: 'https://evil.example',
                data: {
                    source: 'dot-image-editor',
                    type: 'tempfile',
                    tempFile: { id: 'temp-123' }
                }
            })
        );

        const tempfileDispatches = dispatchSpy.mock.calls.filter(
            ([event]) => (event as Event).type === tempEventName
        );

        expect(tempfileDispatches).toHaveLength(0);
    });

    it('should not open dialog after stopListening', () => {
        spectator.service.listen(variable);
        spectator.service.stopListening();

        document.dispatchEvent(
            new CustomEvent(openEventName, {
                detail: { inode: 'inode-1', tempId: 'temp-1', variable }
            })
        );

        expect(dialogService.open).not.toHaveBeenCalled();
    });
});
