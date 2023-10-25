import { SpectatorService, createServiceFactory } from '@ngneat/spectator';

import { DotEditBinaryFieldImageService } from './dot-edit-binary-field-image.service';

describe('DotEditBinaryFieldImageService', () => {
    let spectator: SpectatorService<DotEditBinaryFieldImageService>;
    const createService = createServiceFactory({
        service: DotEditBinaryFieldImageService
    });

    beforeEach(() => {
        spectator = createService();
    });

    it('should emit edited image', () => {
        const tempFile = { id: '123', url: 'http://example.com/image.jpg' };
        const variable = 'test';
        const inode = '456';
        const tempId = '789';

        const spyDispatchEvent = spyOn(document, 'dispatchEvent');
        const spyAddEventListener = spyOn(document, 'addEventListener');
        const spyRemoveEventListener = spyOn(document, 'removeEventListener');

        spectator.service.openImageEditor({ inode, tempId, variable });
        document.dispatchEvent(
            new CustomEvent(`binaryField-tempfile-${variable}`, { detail: { tempFile } })
        );

        expect(spyDispatchEvent).toHaveBeenCalledWith(
            new CustomEvent(`binaryField-open-image-editor-${variable}`, {
                detail: { inode, tempId, variable }
            })
        );
        expect(spyAddEventListener).toHaveBeenCalledWith(
            `binaryField-tempfile-${variable}`,
            jasmine.any(Function)
        );
        expect(spyRemoveEventListener).toHaveBeenCalledWith(
            `binaryField-tempfile-${variable}`,
            jasmine.any(Function)
        );
        expect(spectator.service.editedImage().getValue()).toEqual(tempFile);
    });
});
