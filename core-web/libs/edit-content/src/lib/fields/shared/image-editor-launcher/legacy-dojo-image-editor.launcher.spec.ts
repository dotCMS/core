import { expect } from '@jest/globals';
import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { Subscription } from 'rxjs';

import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { ImageEditorOpenParams } from '@dotcms/image-editor';

import { LegacyDojoImageEditorLauncher } from './legacy-dojo-image-editor.launcher';

describe('LegacyDojoImageEditorLauncher', () => {
    let spectator: SpectatorService<LegacyDojoImageEditorLauncher>;
    let subscription: Subscription | undefined;

    const params: ImageEditorOpenParams = {
        inode: 'inode-1',
        tempId: 'temp-1',
        variable: 'binaryField',
        fieldName: 'binary'
    };

    const openEventName = `binaryField-open-image-editor-${params.variable}`;
    const tempEventName = `binaryField-tempfile-${params.variable}`;
    const closeEventName = `binaryField-close-image-editor-${params.variable}`;

    const createService = createServiceFactory(LegacyDojoImageEditorLauncher);

    beforeEach(() => {
        spectator = createService();
    });

    afterEach(() => {
        subscription?.unsubscribe();
    });

    it('should report itself as available', () => {
        expect(spectator.service.isAvailable()).toBe(true);
    });

    it('should dispatch the open event with the asset detail', () => {
        const dispatchSpy = jest.spyOn(document, 'dispatchEvent');

        subscription = spectator.service.open(params).subscribe();

        expect(dispatchSpy).toHaveBeenCalledWith(expect.objectContaining({ type: openEventName }));
        const openEvent = dispatchSpy.mock.calls
            .map(([event]) => event)
            .find(
                (event): event is CustomEvent =>
                    event instanceof CustomEvent && event.type === openEventName
            );
        expect(openEvent?.detail).toEqual({
            inode: 'inode-1',
            tempId: 'temp-1',
            variable: 'binaryField'
        });
    });

    it('should resolve the temp file when the tempfile event fires', () => {
        const tempFile = { id: 'temp-123' } as DotCMSTempFile;
        let result: DotCMSTempFile | null | undefined;

        subscription = spectator.service.open(params).subscribe((value) => (result = value));

        document.dispatchEvent(new CustomEvent(tempEventName, { detail: { tempFile } }));

        expect(result).toEqual(tempFile);
    });

    it('should resolve null when the close event fires', () => {
        let emitted = false;
        let result: DotCMSTempFile | null | undefined;

        subscription = spectator.service.open(params).subscribe((value) => {
            emitted = true;
            result = value;
        });

        document.dispatchEvent(new CustomEvent(closeEventName));

        expect(emitted).toBe(true);
        expect(result).toBeNull();
    });

    it('should stop listening after the first emission', () => {
        const tempFile = { id: 'temp-123' } as DotCMSTempFile;
        const emissions: (DotCMSTempFile | null)[] = [];

        subscription = spectator.service.open(params).subscribe((value) => emissions.push(value));

        document.dispatchEvent(new CustomEvent(tempEventName, { detail: { tempFile } }));
        document.dispatchEvent(new CustomEvent(tempEventName, { detail: { tempFile } }));
        document.dispatchEvent(new CustomEvent(closeEventName));

        expect(emissions).toEqual([tempFile]);
    });
});
