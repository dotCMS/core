import { updatePrimaryPalette } from '@primeuix/themes';

import { TestBed } from '@angular/core/testing';

import { DEFAULT_COLORS, DotUiColorsService } from './dot-ui-colors.service';

// Mock PrimeNG updatePrimaryPalette
jest.mock('@primeuix/themes', () => ({
    updatePrimaryPalette: jest.fn()
}));

describe('DotUiColorsService', () => {
    let service: DotUiColorsService;
    let mockElement: HTMLElement;
    let setPropertySpy: jest.Mock;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotUiColorsService]
        });

        service = TestBed.inject(DotUiColorsService);

        setPropertySpy = jest.fn();
        mockElement = {
            style: {
                setProperty: setPropertySpy
            }
        } as unknown as HTMLElement;

        jest.clearAllMocks();
    });

    describe('setColors', () => {
        it('should set all colors and update PrimeNG theme', () => {
            const colors = {
                primary: '#78E4FF',
                secondary: '#98FF78',
                background: '#CB8978'
            };

            service.setColors(mockElement, colors);

            // Verify PrimeNG theme was updated
            expect(updatePrimaryPalette).toHaveBeenCalledTimes(1);
            expect(updatePrimaryPalette).toHaveBeenCalledWith(
                expect.objectContaining({
                    '50': expect.any(String),
                    '500': colors.primary,
                    '950': expect.any(String)
                })
            );

            // Verify CSS variables were set
            expect(setPropertySpy).toHaveBeenCalled();
            expect(setPropertySpy).toHaveBeenCalledWith('--color-primary-h', expect.any(String));
            expect(setPropertySpy).toHaveBeenCalledWith('--color-primary-s', expect.any(String));
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-primary-100',
                expect.any(String)
            );
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-primary-500',
                expect.any(String)
            );
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-primary-900',
                expect.any(String)
            );
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-primary-op-10',
                expect.stringContaining('hsla')
            );
            expect(setPropertySpy).toHaveBeenCalledWith('--color-secondary-h', expect.any(String));
            expect(setPropertySpy).toHaveBeenCalledWith('--color-secondary-s', expect.any(String));
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-secondary-100',
                expect.any(String)
            );
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-secondary-op-10',
                expect.stringContaining('hsla')
            );
            expect(setPropertySpy).toHaveBeenCalledWith('--color-background', colors.background);
        });

        it('should use default colors when invalid colors are provided', () => {
            service.setColors(mockElement, {
                primary: 'invalid',
                secondary: 'invalid',
                background: 'invalid'
            });

            // Legacy CSS variables should not be set for invalid colors
            expect(setPropertySpy).not.toHaveBeenCalled();

            // PrimeNG should use default colors when invalid colors are provided
            expect(updatePrimaryPalette).toHaveBeenCalledTimes(1);
            expect(updatePrimaryPalette).toHaveBeenCalledWith(
                expect.objectContaining({
                    '500': DEFAULT_COLORS.primary
                })
            );
        });

        it('should use current colors when colors parameter is not provided', () => {
            const initialColors = {
                primary: '#426BF0',
                secondary: '#7042F0',
                background: '#FFFFFF'
            };

            // Set initial colors
            service.setColors(mockElement, initialColors);
            setPropertySpy.mockClear();
            jest.clearAllMocks();

            // Call without colors parameter
            service.setColors(mockElement);

            // Should use the previously set colors
            expect(setPropertySpy).toHaveBeenCalled();
            expect(updatePrimaryPalette).toHaveBeenCalledWith(
                expect.objectContaining({
                    '500': initialColors.primary
                })
            );
        });

        it('should generate all required CSS variables for primary color', () => {
            service.setColors(mockElement, {
                primary: '#426BF0',
                secondary: '#7042F0',
                background: '#FFFFFF'
            });

            // Verify all shades are generated (100-900)
            const shades = ['100', '200', '300', '400', '500', '600', '700', '800', '900'];
            shades.forEach((shade) => {
                expect(setPropertySpy).toHaveBeenCalledWith(
                    `--color-palette-primary-${shade}`,
                    expect.any(String)
                );
            });

            // Verify all opacities are generated (10-90)
            const opacities = ['10', '20', '30', '40', '50', '60', '70', '80', '90'];
            opacities.forEach((opacity) => {
                expect(setPropertySpy).toHaveBeenCalledWith(
                    `--color-palette-primary-op-${opacity}`,
                    expect.stringContaining('hsla')
                );
            });
        });

        it('should generate all required CSS variables for secondary color', () => {
            service.setColors(mockElement, {
                primary: '#426BF0',
                secondary: '#7042F0',
                background: '#FFFFFF'
            });

            // Verify secondary shades are generated
            const shades = ['100', '200', '300', '400', '500', '600', '700', '800', '900'];
            shades.forEach((shade) => {
                expect(setPropertySpy).toHaveBeenCalledWith(
                    `--color-palette-secondary-${shade}`,
                    expect.any(String)
                );
            });

            // Verify secondary opacities use secondary color variables (not primary)
            expect(setPropertySpy).toHaveBeenCalledWith(
                '--color-palette-secondary-op-10',
                expect.stringContaining('var(--color-secondary-h)')
            );
        });
    });

    describe('getColors', () => {
        it('should return default colors initially', () => {
            const colors = service.getColors();

            expect(colors).toEqual(DEFAULT_COLORS);
        });

        it('should return updated colors after setColors', () => {
            const newColors = {
                primary: '#FF0000',
                secondary: '#00FF00',
                background: '#0000FF'
            };

            service.setColors(mockElement, newColors);

            const currentColors = service.getColors();
            expect(currentColors).toEqual(newColors);
        });
    });

    describe('PrimeNG integration', () => {
        it('should call updatePrimaryPalette with generated palette', () => {
            const colors = {
                primary: '#426BF0',
                secondary: '#7042F0',
                background: '#FFFFFF'
            };

            service.setColors(mockElement, colors);

            expect(updatePrimaryPalette).toHaveBeenCalledTimes(1);
            const palette = (updatePrimaryPalette as jest.Mock).mock.calls[0][0];

            // Verify palette structure
            expect(palette).toHaveProperty('50');
            expect(palette).toHaveProperty('500', colors.primary);
            expect(palette).toHaveProperty('950');
            expect(Object.keys(palette)).toHaveLength(11); // 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950
        });

        it('should handle PrimeNG updatePrimaryPalette errors gracefully', () => {
            (updatePrimaryPalette as jest.Mock).mockImplementation(() => {
                throw new Error('PrimeNG not initialized');
            });

            const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();

            expect(() => {
                service.setColors(mockElement, {
                    primary: '#426BF0',
                    secondary: '#7042F0',
                    background: '#FFFFFF'
                });
            }).not.toThrow();

            expect(consoleSpy).toHaveBeenCalledWith(
                'Failed to update PrimeNG colors:',
                expect.any(Error)
            );

            consoleSpy.mockRestore();
        });
    });

    describe('static getDefaultPrimeNGPalette', () => {
        it('should generate default palette from DEFAULT_COLORS.primary', () => {
            const palette = DotUiColorsService.getDefaultPrimeNGPalette();

            expect(palette).toHaveProperty('50');
            expect(palette).toHaveProperty('500', DEFAULT_COLORS.primary);
            expect(palette).toHaveProperty('950');
            expect(Object.keys(palette)).toHaveLength(11);
        });

        it('should generate consistent palette structure', () => {
            const palette = DotUiColorsService.getDefaultPrimeNGPalette();

            // Verify all expected shades exist
            const expectedShades = [
                '50',
                '100',
                '200',
                '300',
                '400',
                '500',
                '600',
                '700',
                '800',
                '900',
                '950'
            ];
            expectedShades.forEach((shade) => {
                expect(palette).toHaveProperty(shade);
                expect(typeof palette[shade]).toBe('string');
                expect(palette[shade]).toMatch(/^#[0-9A-Fa-f]{6}$/); // Valid hex color
            });
        });
    });
});
