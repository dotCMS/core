import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import {
    DotContentThumbnailComponent,
    CONTENT_THUMBNAIL_TYPE
} from './dot-content-thumbnail.component';

describe('DotContentThumbnailComponent', () => {
    let spectator: Spectator<DotContentThumbnailComponent>;
    const createComponent = createComponentFactory(DotContentThumbnailComponent);

    describe('thumbnail', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    inode: '123-456',
                    name: 'name',
                    contentType: 'video/mp4',
                    iconSize: '74',
                    titleImage: ''
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const videoElement = spectator.query(byTestId('thumbail-video'));

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.video);
            expect(spectator.component.src).toBe('/dA/123-456');
            expect(videoElement).toBeTruthy();
        });
    });

    describe('image', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    inode: '123-456',
                    name: 'name',
                    contentType: 'image/png',
                    iconSize: '74',
                    titleImage: ''
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const imageElement = spectator.query(byTestId('thumbail-image'));

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.image);
            expect(spectator.component.src).toBe('/dA/123-456/500w/50q');
            expect(imageElement).toBeTruthy();
        });
    });

    describe('image', () => {
        beforeEach(async () => {
            spectator = createComponent({
                props: {
                    inode: '123-456',
                    name: 'name',
                    contentType: 'unknown',
                    iconSize: '74',
                    titleImage: ''
                }
            });
            spectator.detectChanges();
            await spectator.fixture.whenStable();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should set thumbnailType to video when contentType is video/*', () => {
            const iconElement = spectator.query(byTestId('thumbail-icon'));

            expect(spectator.component.thumbnailType).toBe(CONTENT_THUMBNAIL_TYPE.icon);
            expect(spectator.component.src).not.toBeDefined();
            expect(iconElement).toBeTruthy();
        });
    });
});
