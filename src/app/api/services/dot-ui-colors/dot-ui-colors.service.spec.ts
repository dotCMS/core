import { DotUiColorsService } from './dot-ui-colors.service';
import { TestBed } from '@angular/core/testing';

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
        spyOn(document, 'querySelector').and.returnValue({
            style: {
                setProperty: setPropertySpy
            }
        });

        spyOn(window, 'getComputedStyle').and.returnValue({
            getPropertyValue: (cssVar: string) => {
                const map = {
                    '--color-main': '#C336E5',
                    '--color-sec': '#54428E',
                    '--color-background': '#3A3847'
                };
                return map[cssVar];
            }
        });
    });

    it('should set all colors', () => {
        service.setColors(document.querySelector('html'), {
            primary: '#78E4FF',
            secondary: '#98FF78',
            background: '#CB8978'
        });

        const html = <HTMLElement>document.querySelector('');

        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main', '#78E4FF');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main_mod', '#B5F0FF');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-main_rgb', '120, 228, 255');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-sec', '#98FF78');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-sec_rgb', '152, 255, 120');
        expect(html.style.setProperty).toHaveBeenCalledWith('--color-background', '#CB8978');
        expect(html.style.setProperty).toHaveBeenCalledTimes(6);
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
