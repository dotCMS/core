import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { ComponentFixture } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { DOTTestBed } from './test/dot-test-bed';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import { DotcmsConfigService } from 'dotcms-js';
import { of } from 'rxjs';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

describe('AppComponent', () => {
    let fixture: ComponentFixture<AppComponent>;
    let de: DebugElement;
    let dotCmsConfigService: DotcmsConfigService;
    let dotUiColorsService: DotUiColorsService;
    let dotMessageService: DotMessageService;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [AppComponent],
            imports: [RouterTestingModule],
            providers: []
        });

        fixture = DOTTestBed.createComponent(AppComponent);
        de = fixture.debugElement;
        dotCmsConfigService = de.injector.get(DotcmsConfigService);
        dotUiColorsService = de.injector.get(DotUiColorsService);
        dotMessageService = de.injector.get(DotMessageService);

        spyOn<any>(dotCmsConfigService, 'getConfig').and.returnValue(
            of({
                colors: {
                    primary: '#123',
                    secondary: '#456',
                    background: '#789'
                }
            })
        );

        spyOn(dotUiColorsService, 'setColors');

        spyOn(dotMessageService, 'init');

        fixture.detectChanges();
    });

    it('should init message service', () => {
        expect(dotMessageService.init).toHaveBeenCalledWith(false);
    });

    it('should have router-outlet', () => {
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
    });

    it('should set ui colors', () => {
        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(jasmine.any(HTMLElement), {
            primary: '#123',
            secondary: '#456',
            background: '#789'
        });
    });
});
