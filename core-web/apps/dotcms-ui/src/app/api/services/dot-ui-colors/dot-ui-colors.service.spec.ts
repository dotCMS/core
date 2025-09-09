import { TestBed } from '@angular/core/testing';

import { DEFAULT_COLORS, DotUiColorsService } from './dot-ui-colors.service';

describe('DotUiColorsService', () => {
    let service: DotUiColorsService;
    let setPropertySpy;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotUiColorsService]
        });

        service = TestBed.inject(DotUiColorsService);

        setPropertySpy = jest.fn();
        jest.spyOn(document as Document, 'querySelector').mockReturnValue({
            style: {
                setProperty: setPropertySpy
            }
        } as HTMLElement);
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

    it('should not set invalid colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: 'sdfadfg',
            secondary: 'dfgsdfg',
            background: 'dsfgsdfg'
        });

        const html = <HTMLElement>document.querySelector('');

        expect(html.style.setProperty).not.toHaveBeenCalled();
    });

    it('should set manual picked colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: DEFAULT_COLORS.primary,
            secondary: DEFAULT_COLORS.secondary,
            background: '#CB8978'
        });

        const html = <HTMLElement>document.querySelector('');

        [
            { key: '--color-primary-h', value: '226deg' },
            { key: '--color-primary-s', value: '85%' },
            {
                key: '--color-palette-primary-100',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 98%)'
            },
            {
                key: '--color-palette-primary-200',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 96%)'
            },
            {
                key: '--color-palette-primary-300',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 90%)'
            },
            {
                key: '--color-palette-primary-400',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 78%)'
            },
            {
                key: '--color-palette-primary-500',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 60%)'
            },
            {
                key: '--color-palette-primary-600',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 48%)'
            },
            {
                key: '--color-palette-primary-700',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 36%)'
            },
            {
                key: '--color-palette-primary-800',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 27%)'
            },
            {
                key: '--color-palette-primary-900',
                value: 'hsl(var(--color-primary-h) var(--color-primary-s) 21%)'
            },
            { key: '--color-secondary-h', value: '256deg' },
            { key: '--color-secondary-s', value: '85%' },
            {
                key: '--color-palette-secondary-100',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 98%)'
            },
            {
                key: '--color-palette-secondary-200',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 94%)'
            },
            {
                key: '--color-palette-secondary-300',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 84%)'
            },
            {
                key: '--color-palette-secondary-400',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 71%)'
            },
            {
                key: '--color-palette-secondary-500',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 60%)'
            },
            {
                key: '--color-palette-secondary-600',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 51%)'
            },
            {
                key: '--color-palette-secondary-700',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 42%)'
            },
            {
                key: '--color-palette-secondary-800',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 30%)'
            },
            {
                key: '--color-palette-secondary-900',
                value: 'hsl(var(--color-secondary-h) var(--color-secondary-s) 22%)'
            }
        ].forEach(({ key, value }) => {
            expect(html.style.setProperty).toHaveBeenCalledWith(key, value);
        });
    });
});
