import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    DotTempFileThumbnailComponent,
    CONTENT_THUMBNAIL_TYPE
} from './dot-temp-file-thumbnail.component';

const METADATA_MOCK = {
    contentType: 'image/jpeg',
    fileSize: 12312,
    length: 12312,
    isImage: true,
    modDate: 12312,
    name: 'image.png',
    sha256: '12345',
    title: 'Asset',
    version: 1,
    height: 100,
    width: 100,
    editableAsText: false
};

const TEMP_FILE_MOCK: DotCMSTempFile = {
    fileName: 'Image.jpg',
    folder: 'folder',
    id: 'tempFileId',
    image: true,
    length: 10000,
    mimeType: 'image/jpeg',
    referenceUrl: '',
    thumbnailUrl: '/dA/123-456/500w/50q/image.png',
    metadata: METADATA_MOCK
};

describe('DotTempFileThumbnailComponent', () => {
    let spectator: Spectator<DotTempFileThumbnailComponent>;
    const createComponent = createComponentFactory(DotTempFileThumbnailComponent);

    describe('video', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    tempFile: {
                        ...TEMP_FILE_MOCK,
                        thumbnailUrl: '',
                        referenceUrl: '/dA/123-456',
                        metadata: {
                            ...METADATA_MOCK,
                            name: 'video.mp4',
                            contentType: 'video/mp4',
                            isImage: false
                        }
                    }
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const videoElement = spectator.query(byTestId('thumbail-video'));
            const sourceElement = videoElement.querySelector('source');

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.video);
            expect(spectator.component.src).toBe('/dA/123-456');
            expect(sourceElement.getAttribute('src')).toBe('/dA/123-456');
            expect(sourceElement).toBeTruthy();
            expect(videoElement).toBeTruthy();
        });
    });

    describe('image', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    tempFile: {
                        ...TEMP_FILE_MOCK,
                        metadata: {
                            ...METADATA_MOCK,
                            name: 'image.png',
                            contentType: 'image/png'
                        }
                    }
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to image when contentType is image/*', () => {
            const imageElement = spectator.query(byTestId('thumbail-image'));

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.image);
            expect(spectator.component.src).toBe('/dA/123-456/500w/50q/image.png');
            expect(imageElement.getAttribute('src')).toBe('/dA/123-456/500w/50q/image.png');
            expect(imageElement.getAttribute('title')).toBe('image.png');
            expect(imageElement.getAttribute('alt')).toBe('image.png');
            expect(imageElement).toBeTruthy();
        });
    });

    describe('icon', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    tempFile: {
                        ...TEMP_FILE_MOCK,
                        thumbnailUrl: '',
                        referenceUrl: '/dA/123-456',
                        metadata: {
                            ...METADATA_MOCK,
                            name: 'file.pdf',
                            contentType: 'unknown',
                            isImage: false
                        }
                    }
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const iconElement = spectator.query(byTestId('thumbail-icon'));

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.icon);
            expect(iconElement.getAttribute('class')).toBe('pi pi-file-pdf');
            expect(iconElement).toBeTruthy();
        });
    });
});
