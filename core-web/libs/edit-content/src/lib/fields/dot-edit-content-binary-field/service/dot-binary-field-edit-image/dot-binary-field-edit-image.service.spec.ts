import { expect } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { skip } from 'rxjs/operators';

import { DotBinaryFieldEditImageService } from './dot-binary-field-edit-image.service';

describe('DotBinaryFieldEditImageService', () => {
    let spectator: SpectatorService<DotBinaryFieldEditImageService>;

    const createService = createServiceFactory({
        service: DotBinaryFieldEditImageService
    });

    let spyDispatchEvent: jest.SpyInstance;
    let spyAddEventListener: jest.SpyInstance;
    let spyRemoveEventListener: jest.SpyInstance;

    beforeEach(() => {
        spyDispatchEvent = jest.spyOn(document, 'dispatchEvent');
        spyAddEventListener = jest.spyOn(document, 'addEventListener');
        spyRemoveEventListener = jest.spyOn(document, 'removeEventListener');
        spectator = createService();
    });

    it('should listen to edited image', () => {
        const detail = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${detail.variable}`;
        const openEditorEventName = `binaryField-open-image-editor-${detail.variable}`;
        const openImageCustomEvent = new CustomEvent(openEditorEventName, { detail });

        spectator.service.openImageEditor(detail);
        expect(spyDispatchEvent).toHaveBeenCalledWith(openImageCustomEvent);
        expect(spyAddEventListener).toHaveBeenCalledWith(tempEventName, expect.any(Function));
    });
    it('should listen to edited image 2', () => {
        const detail = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${detail.variable}`;
        const openEditorEventName = `binaryField-open-image-editor-${detail.variable}`;
        const openImageCustomEvent = new CustomEvent(openEditorEventName, { detail });

        spectator.service.openImageEditor(detail);
        expect(spyDispatchEvent).toHaveBeenCalledWith(openImageCustomEvent);
        expect(spyAddEventListener).toHaveBeenCalledWith(tempEventName, expect.any(Function));
    });

    it('should listen to edited image 3', () => {
        const detail = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${detail.variable}`;
        const openEditorEventName = `binaryField-open-image-editor-${detail.variable}`;
        const openImageCustomEvent = new CustomEvent(openEditorEventName, { detail });

        spectator.service.openImageEditor(detail);
        expect(spyDispatchEvent).toHaveBeenCalledWith(openImageCustomEvent);
        expect(spyAddEventListener).toHaveBeenCalledWith(tempEventName, expect.any(Function));
    });

    it('should emit edited image and remove listener', (done) => {
        const tempFile = { id: '123', url: 'http://example.com/image.jpg' };
        const data = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        spectator.service
            .editedImage()
            .pipe(skip(1))
            .subscribe((file) => {
                expect(file).toEqual(tempFile);
                done();
            });

        const tempEventName = `binaryField-tempfile-${data.variable}`;
        const closeEventName = `binaryField-close-image-editor-${data.variable}`;

        spectator.service.openImageEditor(data);
        document.dispatchEvent(new CustomEvent(tempEventName, { detail: { tempFile } }));

        expect(spyRemoveEventListener.mock.calls[0]).toEqual([tempEventName, expect.any(Function)]);
        expect(spyRemoveEventListener.mock.calls[1]).toEqual([
            closeEventName,
            expect.any(Function)
        ]);
    });

    it('should listen to close image editor and remove listeners', () => {
        const data = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${data.variable}`;
        const closeEventName = `binaryField-close-image-editor-${data.variable}`;

        spectator.service.openImageEditor(data);

        document.dispatchEvent(new CustomEvent(closeEventName, {}));
        expect(spyRemoveEventListener.mock.calls[0]).toEqual([tempEventName, expect.any(Function)]);
        expect(spyRemoveEventListener.mock.calls[1]).toEqual([
            closeEventName,
            expect.any(Function)
        ]);
    });
});
