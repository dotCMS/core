import { ImageLoaderConfig, IMAGE_LOADER } from '@angular/common';

import { provideDotCMSImageLoader } from './image_loader';

describe('Image Loader Utils', () => {
    describe('provideDotCMSImageLoader', () => {
        const validPath = 'https://demo.dotcms.com';

        it('should throw error when invalid path is provided', () => {
            const invalidPath = 'invalid-url';
            expect(() => provideDotCMSImageLoader(invalidPath)).toThrow(
                `Image loader has detected an invalid path (\`${invalidPath}\`). ` +
                    `To fix this, supply either the full URL to the dotCMS site, or leave it empty to use the current site.`
            );
        });

        it('should return array of providers when valid path is provided', () => {
            const providers = provideDotCMSImageLoader(validPath);
            const provider = providers[0] as { provide: typeof IMAGE_LOADER; useValue: unknown };

            expect(Array.isArray(providers)).toBe(true);
            expect(providers.length).toBe(1);
            expect(provider.provide).toBe(IMAGE_LOADER);
            expect(provider).toHaveProperty('useValue');
        });

        it('should return array of providers when no path is provided', () => {
            const providers = provideDotCMSImageLoader();
            const provider = providers[0] as { provide: typeof IMAGE_LOADER; useValue: unknown };

            expect(Array.isArray(providers)).toBe(true);
            expect(providers.length).toBe(1);
            expect(provider.provide).toBe(IMAGE_LOADER);
            expect(provider).toHaveProperty('useValue');
        });

        describe('Image URL Generation with path', () => {
            let imageLoader: (config: ImageLoaderConfig) => string;

            beforeEach(() => {
                const [provider] = provideDotCMSImageLoader(validPath);
                imageLoader = (provider as { useValue: (config: ImageLoaderConfig) => string })
                    .useValue;
            });

            it('should generate correct URL for internal images', () => {
                const config: ImageLoaderConfig = {
                    src: 'image.jpg',
                    width: 100,
                    loaderParams: { languageId: '2' }
                };

                const expectedUrl = 'https://demo.dotcms.com/dA/image.jpg/100?language_id=2';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should preserve /dA/ in source path if already present', () => {
                const config: ImageLoaderConfig = {
                    src: '/dA/existing/image.jpg',
                    width: 200,
                    loaderParams: { languageId: '1' }
                };

                const expectedUrl =
                    'https://demo.dotcms.com/dA/existing/image.jpg/200?language_id=1';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should use default language ID when not provided', () => {
                const config: ImageLoaderConfig = {
                    src: 'image.jpg',
                    width: 300
                };

                const expectedUrl = 'https://demo.dotcms.com/dA/image.jpg/300?language_id=1';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should return original source when isOutsideSRC is true', () => {
                const config: ImageLoaderConfig = {
                    src: 'https://external-domain.com/image.jpg',
                    width: 400,
                    loaderParams: { isOutsideSRC: true }
                };

                expect(imageLoader(config)).toBe(config.src);
            });

            it('should handle complex URLs correctly', () => {
                const config: ImageLoaderConfig = {
                    src: 'folder/subfolder/image-with-dash.jpg',
                    width: 500,
                    loaderParams: { languageId: '3' }
                };

                const expectedUrl =
                    'https://demo.dotcms.com/dA/folder/subfolder/image-with-dash.jpg/500?language_id=3';
                expect(imageLoader(config)).toBe(expectedUrl);
            });
        });

        describe('Image URL Generation without path', () => {
            let imageLoader: (config: ImageLoaderConfig) => string;

            beforeEach(() => {
                const [provider] = provideDotCMSImageLoader();
                imageLoader = (provider as { useValue: (config: ImageLoaderConfig) => string })
                    .useValue;
            });

            it('should generate correct URL for internal images without host', () => {
                const config: ImageLoaderConfig = {
                    src: 'image.jpg',
                    width: 100,
                    loaderParams: { languageId: '2' }
                };

                const expectedUrl = '/dA/image.jpg/100?language_id=2';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should preserve /dA/ in source path if already present', () => {
                const config: ImageLoaderConfig = {
                    src: '/dA/existing/image.jpg',
                    width: 200,
                    loaderParams: { languageId: '1' }
                };

                const expectedUrl = '/dA/existing/image.jpg/200?language_id=1';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should use default language ID when not provided', () => {
                const config: ImageLoaderConfig = {
                    src: 'image.jpg',
                    width: 300
                };

                const expectedUrl = '/dA/image.jpg/300?language_id=1';
                expect(imageLoader(config)).toBe(expectedUrl);
            });

            it('should return original source when isOutsideSRC is true', () => {
                const config: ImageLoaderConfig = {
                    src: 'https://external-domain.com/image.jpg',
                    width: 400,
                    loaderParams: { isOutsideSRC: true }
                };

                expect(imageLoader(config)).toBe(config.src);
            });
        });
    });
});
