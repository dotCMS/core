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
        jest.restoreAllMocks();
    });

    const openEditorDialog = (): void => {
        document.dispatchEvent(
            new CustomEvent(openEventName, {
                detail: { inode: 'inode-1', tempId: 'temp-1', variable }
            })
        );
    };

    const dispatchPostMessage = (data: Record<string, unknown>): void => {
        window.dispatchEvent(
            new MessageEvent('message', {
                origin: window.location.origin,
                data
            })
        );
    };

    const getTempfileDispatches = (dispatchSpy: jest.SpyInstance): CustomEvent[] =>
        dispatchSpy.mock.calls
            .map(([event]) => event)
            .filter(
                (event): event is CustomEvent =>
                    event instanceof CustomEvent && event.type === tempEventName
            );

    it('should open dialog when open-image-editor event is dispatched', () => {
        spectator.service.listen(variable);
        openEditorDialog();

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
        openEditorDialog();
        dispatchSpy.mockClear();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            tempFile,
            variable
        });

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(1);
        expect(getTempfileDispatches(dispatchSpy)[0].detail).toEqual({ tempFile });
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should re-dispatch close event when postMessage close is received', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        spectator.service.listen(variable);
        openEditorDialog();
        dispatchSpy.mockClear();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'close',
            variable
        });

        expect(dispatchSpy).toHaveBeenCalledWith(
            expect.objectContaining({
                type: closeEventName
            })
        );
        expect(dialogRef.close).toHaveBeenCalled();
    });

    it('should ignore postMessage from a different origin', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');
        const invalidOrigin =
            window.location.origin === 'https://evil.example'
                ? 'http://evil.example'
                : 'https://evil.example';

        spectator.service.listen(variable);
        openEditorDialog();
        dispatchSpy.mockClear();

        window.dispatchEvent(
            new MessageEvent('message', {
                origin: invalidOrigin,
                data: {
                    source: 'dot-image-editor',
                    type: 'tempfile',
                    tempFile: { id: 'temp-123' },
                    variable
                }
            })
        );

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(0);
    });

    it('should ignore postMessage when dialog is not open', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        spectator.service.listen(variable);
        dispatchSpy.mockClear();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            tempFile: { id: 'temp-123' },
            variable
        });

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(0);
    });

    it('should ignore postMessage when variable does not match', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        spectator.service.listen(variable);
        openEditorDialog();
        dispatchSpy.mockClear();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            tempFile: { id: 'temp-123' },
            variable: 'otherField'
        });

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(0);
    });

    it('should ignore postMessage after the editor dialog closes', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');
        const tempFile = { id: 'temp-123' } as DotCMSTempFile;

        spectator.service.listen(variable);
        openEditorDialog();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            tempFile,
            variable
        });

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(1);
        expect(dialogRef.close).toHaveBeenCalledTimes(1);

        dispatchSpy.mockClear();
        (dialogRef.close as jest.Mock).mockClear();

        dispatchPostMessage({
            source: 'dot-image-editor',
            type: 'tempfile',
            tempFile,
            variable
        });

        expect(getTempfileDispatches(dispatchSpy)).toHaveLength(0);
        expect(dialogRef.close).not.toHaveBeenCalled();
    });

    it('should not open dialog after stopListening', () => {
        spectator.service.listen(variable);
        spectator.service.stopListening();
        openEditorDialog();

        expect(dialogService.open).not.toHaveBeenCalled();
    });
});
