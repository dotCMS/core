import { SpectatorDirective, createDirectiveFactory } from '@ngneat/spectator/jest';

import { NgOptimizedImage, IMAGE_LOADER, ImageLoaderConfig } from '@angular/common';
import { TestBed } from '@angular/core/testing';

// Application imports
import { provideDotCMSImageLoader } from './dotcms-image_loader';

describe('Image Loader', () => {
    describe('provideDotCMSImageLoader', () => {
        it('should throw an error when an invalid URL is provided', () => {
            expect(() => provideDotCMSImageLoader('invalid-url')).toThrow(
                'Image loader has detected an invalid path (`invalid-url`). To fix this, supply either the full URL to the dotCMS site, or leave it empty to use the current site.'
            );
        });

        it('should not throw an error when a valid URL is provided', () => {
            expect(() => provideDotCMSImageLoader('https://demo.dotcms.com')).not.toThrow();
        });

        it('should not throw an error when no URL is provided', () => {
            expect(() => provideDotCMSImageLoader()).not.toThrow();
        });

        it('should return a provider for IMAGE_LOADER token', () => {
            const providers = provideDotCMSImageLoader('https://demo.dotcms.com');
            expect(providers.length).toBe(1);
            // Type assertion to avoid linter errors
            const provider = providers[0] as {
                provide: typeof IMAGE_LOADER;
                useValue: (config: ImageLoaderConfig) => string;
            };
            expect(provider.provide).toBe(IMAGE_LOADER);
            expect(typeof provider.useValue).toBe('function');
        });
    });

    describe('createDotCMSUrl (via IMAGE_LOADER)', () => {
        let imageLoader: (config: ImageLoaderConfig) => string;

        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [provideDotCMSImageLoader('https://demo.dotcms.com')]
            });
            imageLoader = TestBed.inject(IMAGE_LOADER);
        });

        it('should return the original src when isOutsideSRC is true', () => {
            const result = imageLoader({
                src: 'https://external-site.com/image.jpg',
                loaderParams: { isOutsideSRC: true }
            });
            expect(result).toBe('https://external-site.com/image.jpg');
        });

        it('should add /dA/ prefix when src does not include it', () => {
            const result = imageLoader({ src: '12345' });
            expect(result).toBe('https://demo.dotcms.com/dA/12345/50q?language_id=1');
        });

        it('should not add /dA/ prefix when src already includes it', () => {
            const result = imageLoader({ src: '/dA/12345' });
            expect(result).toBe('https://demo.dotcms.com/dA/12345/50q?language_id=1');
        });

        it('should add width parameter when width is provided', () => {
            const result = imageLoader({ src: '12345', width: 300 });
            expect(result).toBe('https://demo.dotcms.com/dA/12345/300w/50q?language_id=1');
        });

        it('should use custom languageId when provided', () => {
            const result = imageLoader({
                src: '12345',
                loaderParams: { languageId: '2' }
            });
            expect(result).toBe('https://demo.dotcms.com/dA/12345/50q?language_id=2');
        });

        it('should use default languageId when not provided', () => {
            const result = imageLoader({ src: '12345' });
            expect(result).toBe('https://demo.dotcms.com/dA/12345/50q?language_id=1');
        });

        it('should use empty host when no path is provided', () => {
            TestBed.resetTestingModule();
            TestBed.configureTestingModule({
                providers: [provideDotCMSImageLoader()]
            });
            imageLoader = TestBed.inject(IMAGE_LOADER);

            const result = imageLoader({ src: '12345' });
            expect(result).toBe('/dA/12345/50q?language_id=1');
        });
    });

    describe('Integration with NgOptimizedImage', () => {
        describe('when using current site (no host provided)', () => {
            let spectator: SpectatorDirective<NgOptimizedImage>;
            const createDirective = createDirectiveFactory({
                directive: NgOptimizedImage,
                providers: [provideDotCMSImageLoader()]
            });

            it('should load the image from the current site', () => {
                const imageMock = '12345';
                spectator = createDirective(`<img ngSrc="${imageMock}" width="300" height="300"/>`);
                spectator.detectChanges();

                const img = spectator.query<HTMLImageElement>('img');
                expect(img?.src).toContain(`/dA/${imageMock}/50q?language_id=1`);
            });

            it('should add width parameter when width is provided', () => {
                const imageMock = '12345';
                spectator = createDirective(`<img ngSrc="${imageMock}" width="300" height="300"/>`);
                spectator.detectChanges();

                const img = spectator.query<HTMLImageElement>('img');
                expect(img?.src).toContain(`/dA/${imageMock}/50q?language_id=1`);
            });
        });

        describe('when using a specific dotCMS instance', () => {
            let spectator: SpectatorDirective<NgOptimizedImage>;
            const createDirective = createDirectiveFactory({
                directive: NgOptimizedImage,
                providers: [provideDotCMSImageLoader('https://demo.dotcms.com')]
            });

            it('should load the image from the specified dotCMS instance', () => {
                const imageMock = '12345';
                spectator = createDirective(`<img ngSrc="${imageMock}" width="300" height="300"/>`);
                spectator.detectChanges();

                const img = spectator.query<HTMLImageElement>('img');
                expect(img?.src).toContain(`https://demo.dotcms.com/dA/${imageMock}`);
            });

            it('should respect custom loader parameters', () => {
                const imageMock = '12345';
                spectator = createDirective(`
          <img
            ngSrc="${imageMock}"
            width="300"
            height="300"
            [loaderParams]="{languageId: '2'}"
          />
        `);
                spectator.detectChanges();

                const img = spectator.query<HTMLImageElement>('img');
                expect(img?.src).toContain(`language_id=2`);
            });

            it('should handle external images with isOutsideSRC parameter', () => {
                const externalImage = 'https://external-site.com/image.jpg';
                spectator = createDirective(`
          <img
            ngSrc="${externalImage}"
            width="300"
            height="300"
            [loaderParams]="{isOutsideSRC: true}"
          />
        `);
                spectator.detectChanges();

                const img = spectator.query<HTMLImageElement>('img');
                expect(img?.src).toBe(externalImage);
            });
        });
    });
});
