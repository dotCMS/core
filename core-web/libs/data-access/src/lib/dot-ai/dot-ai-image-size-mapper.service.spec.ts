import { TestBed } from '@angular/core/testing';

import { DotAIImageOrientation } from '@dotcms/dotcms-models';

import { DotAIImageSizeMapperService } from './dot-ai-image-size-mapper.service';

describe('DotAIImageSizeMapperService', () => {
    let service: DotAIImageSizeMapperService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(DotAIImageSizeMapperService);
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('getSizeForOrientation', () => {
        describe('dall-e-3 model', () => {
            it('should return correct size for square orientation', () => {
                expect(
                    service.getSizeForOrientation('dall-e-3', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
            });

            it('should return correct size for landscape orientation', () => {
                expect(
                    service.getSizeForOrientation('dall-e-3', DotAIImageOrientation.LANDSCAPE)
                ).toBe('1792x1024');
            });

            it('should return correct size for portrait orientation', () => {
                expect(
                    service.getSizeForOrientation('dall-e-3', DotAIImageOrientation.PORTRAIT)
                ).toBe('1024x1792');
            });

            it('should handle case-insensitive model names', () => {
                expect(
                    service.getSizeForOrientation('DALL-E-3', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
                expect(
                    service.getSizeForOrientation('Dall-E-3', DotAIImageOrientation.LANDSCAPE)
                ).toBe('1792x1024');
            });
        });

        describe('gpt-image-1 model', () => {
            it('should return correct size for square orientation', () => {
                expect(
                    service.getSizeForOrientation('gpt-image-1', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
            });

            it('should return correct size for landscape orientation', () => {
                expect(
                    service.getSizeForOrientation('gpt-image-1', DotAIImageOrientation.LANDSCAPE)
                ).toBe('1536x1024');
            });

            it('should return correct size for portrait orientation', () => {
                expect(
                    service.getSizeForOrientation('gpt-image-1', DotAIImageOrientation.PORTRAIT)
                ).toBe('1024x1536');
            });
        });

        describe('gpt-image-1.5 model', () => {
            it('should return same sizes as gpt-image-1', () => {
                expect(
                    service.getSizeForOrientation('gpt-image-1.5', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
                expect(
                    service.getSizeForOrientation('gpt-image-1.5', DotAIImageOrientation.LANDSCAPE)
                ).toBe('1536x1024');
                expect(
                    service.getSizeForOrientation('gpt-image-1.5', DotAIImageOrientation.PORTRAIT)
                ).toBe('1024x1536');
            });
        });

        describe('gpt-image-1-mini model', () => {
            it('should return same sizes as gpt-image-1', () => {
                expect(
                    service.getSizeForOrientation(
                        'gpt-image-1-mini',
                        DotAIImageOrientation.SQUARE
                    )
                ).toBe('1024x1024');
                expect(
                    service.getSizeForOrientation(
                        'gpt-image-1-mini',
                        DotAIImageOrientation.LANDSCAPE
                    )
                ).toBe('1536x1024');
                expect(
                    service.getSizeForOrientation(
                        'gpt-image-1-mini',
                        DotAIImageOrientation.PORTRAIT
                    )
                ).toBe('1024x1536');
            });
        });

        describe('unknown model', () => {
            it('should fall back to dall-e-3 sizes', () => {
                const consoleSpy = jest.spyOn(console, 'warn');
                expect(
                    service.getSizeForOrientation('unknown-model', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
                expect(consoleSpy).toHaveBeenCalledWith(
                    'Unknown image model: unknown-model, falling back to dall-e-3 sizes'
                );
            });

            it('should handle whitespace in model names', () => {
                expect(
                    service.getSizeForOrientation(' dall-e-3 ', DotAIImageOrientation.SQUARE)
                ).toBe('1024x1024');
            });
        });
    });

    describe('getAvailableOrientations', () => {
        it('should return all three orientations', () => {
            const orientations = service.getAvailableOrientations();
            expect(orientations).toHaveLength(3);
            expect(orientations).toContain(DotAIImageOrientation.SQUARE);
            expect(orientations).toContain(DotAIImageOrientation.LANDSCAPE);
            expect(orientations).toContain(DotAIImageOrientation.PORTRAIT);
        });
    });

    describe('isModelSupported', () => {
        it('should return true for supported models', () => {
            expect(service.isModelSupported('dall-e-3')).toBe(true);
            expect(service.isModelSupported('gpt-image-1')).toBe(true);
            expect(service.isModelSupported('gpt-image-1.5')).toBe(true);
            expect(service.isModelSupported('gpt-image-1-mini')).toBe(true);
        });

        it('should return false for unsupported models', () => {
            expect(service.isModelSupported('unknown-model')).toBe(false);
        });

        it('should handle case-insensitive checks', () => {
            expect(service.isModelSupported('DALL-E-3')).toBe(true);
            expect(service.isModelSupported('Gpt-Image-1')).toBe(true);
        });

        it('should handle whitespace', () => {
            expect(service.isModelSupported(' dall-e-3 ')).toBe(true);
        });
    });
});
