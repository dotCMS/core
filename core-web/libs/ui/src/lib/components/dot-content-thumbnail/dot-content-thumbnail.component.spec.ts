import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import {
    DotContentThumbnailComponent,
    CONTENT_THUMBNAIL_TYPE
} from './dot-content-thumbnail.component';

const inputs = {
    url: '',
    inode: '123-456',
    name: 'name',
    contentType: 'video/mp4',
    iconSize: '1rem',
    titleImage: ''
};

describe('DotContentThumbnailComponent', () => {
    let spectator: Spectator<DotContentThumbnailComponent>;
    const createComponent = createComponentFactory(DotContentThumbnailComponent);

    describe('video', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    ...inputs
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
                    ...inputs,
                    name: 'image.png',
                    contentType: 'image/png'
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

    describe('titleImage', () => {
        beforeEach(async () => {
            spectator = createComponent({
                detectChanges: false,
                props: {
                    ...inputs,
                    name: 'image.png',
                    contentType: 'unknown',
                    titleImage: 'image.png'
                }
            });
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to image when contentType has titleImage', () => {
            spectator.detectChanges();
            const imageElement = spectator.query(byTestId('thumbail-image'));

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.image);
            expect(spectator.component.src).toBe('/dA/123-456/500w/50q/image.png');
            expect(imageElement.getAttribute('src')).toBe('/dA/123-456/500w/50q/image.png');
            expect(imageElement.getAttribute('title')).toBe('image.png');
            expect(imageElement.getAttribute('alt')).toBe('image.png');
            expect(imageElement).toBeTruthy();
        });

        it('should not set thumbnailType to image when titleImage is "TITLE_IMAGE_NOT_FOUND"', () => {
            spectator.setInput('titleImage', 'TITLE_IMAGE_NOT_FOUND');
            spectator.detectChanges();

            const imageElement = spectator.query(byTestId('thumbail-image'));

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.icon);
            expect(spectator.component.src).toBeUndefined();
            expect(imageElement).toBeFalsy();
        });
    });

    describe('icon', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    ...inputs,
                    name: 'name',
                    contentType: 'unknown'
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const iconElement = spectator.query(byTestId('thumbail-icon'));

            expect(spectator.component.type).toBe(CONTENT_THUMBNAIL_TYPE.icon);
            expect(spectator.component.src).not.toBeDefined();
            expect(iconElement.getAttribute('class')).toBe('pi pi-file');
            expect(iconElement).toBeTruthy();
        });
    });
});
