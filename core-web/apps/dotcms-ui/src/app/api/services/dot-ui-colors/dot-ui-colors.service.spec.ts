import { TestBed } from '@angular/core/testing';

import { DotUiColorsService } from './dot-ui-colors.service';

describe('DotUiColorsService', () => {
    let service: DotUiColorsService;
    let injector;
    let setPropertySpy;

    beforeEach(() => {
        injector = TestBed.configureTestingModule({
            providers: [DotUiColorsService]
        });

        service = injector.get(DotUiColorsService);

        setPropertySpy = jasmine.createSpy('setProperty');
        spyOn(document as Document, 'querySelector').and.returnValue({
            style: {
                setProperty: setPropertySpy
            }
        } as HTMLElement);

        spyOn(window as Window, 'getComputedStyle').and.returnValue({
            getPropertyValue: (cssVar: string) => {
                const map = {
                    '--color-main': '#C336E5',
                    '--color-sec': '#54428E',
                    '--color-background': '#3A3847'
                };

                return map[cssVar];
            }
        } as CSSStyleDeclaration);
    });

    it('should set all colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: '#78E4FF',
            secondary: '#98FF78',
            background: '#CB8978'
        });

        const html = <HTMLElement>document.querySelector('');

        [
            { key: '--color-primary-h', value: '192deg' },
            { key: '--color-primary-s', value: '100%' },
            { key: '--color-palette-primary-100', value: 'hsl(194deg, 100%, 97%)' },
            { key: '--color-palette-primary-200', value: 'hsl(192deg, 100%, 92%)' },
            { key: '--color-palette-primary-300', value: 'hsl(192deg, 100%, 87%)' },
            { key: '--color-palette-primary-400', value: 'hsl(192deg, 100%, 82%)' },
            { key: '--color-palette-primary-500', value: 'hsl(192deg, 100%, 74%)' },
            { key: '--color-palette-primary-600', value: 'hsl(192deg, 51%, 59%)' },
            { key: '--color-palette-primary-700', value: 'hsl(192deg, 36%, 44%)' },
            { key: '--color-palette-primary-800', value: 'hsl(193deg, 36%, 22%)' },
            { key: '--color-palette-primary-900', value: 'hsl(193deg, 37%, 7%)' },
            {
                key: '--color-palette-primary-op-10',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.1)'
            },
            {
                key: '--color-palette-primary-op-20',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.2)'
            },
            {
                key: '--color-palette-primary-op-30',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.3)'
            },
            {
                key: '--color-palette-primary-op-40',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.4)'
            },
            {
                key: '--color-palette-primary-op-50',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.5)'
            },
            {
                key: '--color-palette-primary-op-60',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.6)'
            },
            {
                key: '--color-palette-primary-op-70',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.7)'
            },
            {
                key: '--color-palette-primary-op-80',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.8)'
            },
            {
                key: '--color-palette-primary-op-90',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.9)'
            },
            { key: '--color-secondary-h', value: '106deg' },
            { key: '--color-secondary-s', value: '100%' },
            { key: '--color-palette-secondary-100', value: 'hsl(106deg, 100%, 97%)' },
            { key: '--color-palette-secondary-200', value: 'hsl(107deg, 100%, 92%)' },
            { key: '--color-palette-secondary-300', value: 'hsl(106deg, 100%, 87%)' },
            { key: '--color-palette-secondary-400', value: 'hsl(106deg, 100%, 82%)' },
            { key: '--color-palette-secondary-500', value: 'hsl(106deg, 100%, 74%)' },
            { key: '--color-palette-secondary-600', value: 'hsl(106deg, 51%, 59%)' },
            { key: '--color-palette-secondary-700', value: 'hsl(106deg, 36%, 44%)' },
            { key: '--color-palette-secondary-800', value: 'hsl(105deg, 36%, 22%)' },
            { key: '--color-palette-secondary-900', value: 'hsl(107deg, 37%, 7%)' },
            {
                key: '--color-palette-secondary-op-10',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.1)'
            },
            {
                key: '--color-palette-secondary-op-20',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.2)'
            },
            {
                key: '--color-palette-secondary-op-30',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.3)'
            },
            {
                key: '--color-palette-secondary-op-40',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.4)'
            },
            {
                key: '--color-palette-secondary-op-50',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.5)'
            },
            {
                key: '--color-palette-secondary-op-60',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.6)'
            },
            {
                key: '--color-palette-secondary-op-70',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.7)'
            },
            {
                key: '--color-palette-secondary-op-80',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.8)'
            },
            {
                key: '--color-palette-secondary-op-90',
                value: 'hsla(var(--color-primary-h), var(--color-primary-s), 100%, 0.9)'
            },
            { key: '--color-background', value: '#CB8978' }
        ].forEach(({ key, value }) => {
            expect(html.style.setProperty).toHaveBeenCalledWith(key, value);
        });
    });

    fit('should not set invalid colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: 'sdfadfg',
            secondary: 'dfgsdfg',
            background: 'dsfgsdfg'
        });

        const html = <HTMLElement>document.querySelector('');

        expect(html.style.setProperty).not.toHaveBeenCalled();
    });

    it('should only primary colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: '#78E4FF',
            secondary: '#54428E',
            background: '#3A3847'
        });

        const html = <HTMLElement>document.querySelector('');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main', '#78E4FF');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main_mod', '#B5F0FF');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main_rgb', '120, 228, 255');
        expect(html.style.setProperty).toHaveBeenCalledTimes(3);
    });

    it('should only secondary colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: '#C336E5',
            secondary: '#98FF78',
            background: '#3A3847'
        });

        const html = <HTMLElement>document.querySelector('');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-sec', '#98FF78');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-sec_rgb', '152, 255, 120');
        expect(html.style.setProperty).toHaveBeenCalledTimes(2);
    });

    it('should only background colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: '#C336E5',
            secondary: '#54428E',
            background: '#CB8978'
        });

        const html = <HTMLElement>document.querySelector('');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-background', '#CB8978');
        expect(html.style.setProperty).toHaveBeenCalledTimes(1);
    });
});
