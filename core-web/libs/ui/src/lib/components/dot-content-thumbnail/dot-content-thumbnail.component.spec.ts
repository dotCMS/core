import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import {
    DotContentThumbnailComponent,
    CONTENT_THUMBNAIL_TYPE
} from './dot-content-thumbnail.component';

const mockDotThumbnailOptions = {
    tempUrl: '',
    inode: '123-456',
    name: 'name',
    contentType: 'video/mp4',
    iconSize: '74',
    titleImage: ''
};

describe('DotContentThumbnailComponent', () => {
    let spectator: Spectator<DotContentThumbnailComponent>;
    const createComponent = createComponentFactory(DotContentThumbnailComponent);

    describe('video', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    dotThumbanilOptions: mockDotThumbnailOptions
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const videoElement = spectator.query(byTestId('thumbail-video'));
            const sourceElement = videoElement.querySelector('source');

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.video);
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
                    dotThumbanilOptions: {
                        ...mockDotThumbnailOptions,
                        name: 'image.png',
                        contentType: 'image/png'
                    }
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const imageElement = spectator.query(byTestId('thumbail-image'));

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.image);
            expect(spectator.component.src).toBe('/dA/123-456/500w/50q');
            expect(imageElement.getAttribute('src')).toBe('/dA/123-456/500w/50q');
            expect(imageElement.getAttribute('title')).toBe('image.png');
            expect(imageElement.getAttribute('alt')).toBe('image.png');
            expect(imageElement).toBeTruthy();
        });
    });

    describe('icon', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    dotThumbanilOptions: {
                        ...mockDotThumbnailOptions,
                        name: 'name',
                        contentType: 'unknown'
                    }
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const iconElement = spectator.query(byTestId('thumbail-icon'));

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.icon);
            expect(spectator.component.src).not.toBeDefined();
            expect(iconElement.getAttribute('class')).toBe('pi pi-file');
            expect(iconElement).toBeTruthy();
        });
    });
});
