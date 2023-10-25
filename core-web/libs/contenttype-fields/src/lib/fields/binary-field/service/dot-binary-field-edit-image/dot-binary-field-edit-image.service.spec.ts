import { expect } from '@jest/globals';
import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { DotBinaryFieldEditImageService } from './dot-binary-field-edit-image.service';

describe('DotBinaryFieldEditImageService', () => {
    let spectator: SpectatorService<DotBinaryFieldEditImageService>;

    const createService = createServiceFactory({
        service: DotBinaryFieldEditImageService
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should listen to edited image', () => {
        const spyDispatchEvent = jest.spyOn(document, 'dispatchEvent');
        const spyAddEventListener = jest.spyOn(document, 'addEventListener');
        const detail = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${detail.variable}`;
        const openImageCustomEvent = new CustomEvent(
            `binaryField-open-image-editor-${detail.variable}`,
            {
                detail
            }
        );

        spectator.service.openImageEditor(detail);
        expect(spyDispatchEvent).toHaveBeenCalledWith(openImageCustomEvent);
        expect(spyAddEventListener).toHaveBeenCalledWith(tempEventName, expect.any(Function));
    });

    it('should emit edited image and remove listener', () => {
        const tempFile = { id: '123', url: 'http://example.com/image.jpg' };
        const spy = jest.spyOn(spectator.service.editedImage(), 'next');
        const spyRemoveEventListener = jest.spyOn(document, 'removeEventListener');
        const data = {
            variable: 'test',
            inode: '456',
            tempId: '789'
        };

        const tempEventName = `binaryField-tempfile-${data.variable}`;

        spectator.service.openImageEditor(data);
        document.dispatchEvent(
            new CustomEvent(`binaryField-tempfile-${data.variable}`, { detail: { tempFile } })
        );

        expect(spy).toHaveBeenCalledWith(tempFile);
        expect(spyRemoveEventListener).toHaveBeenCalledWith(tempEventName, expect.any(Function));
    });
});
