/* eslint-disable @typescript-eslint/no-explicit-any */

import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AppComponent } from './app.component';
import { DotUiColorsService } from '@services/dot-ui-colors/dot-ui-colors.service';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    LoggerService,
    StringUtils
} from '@dotcms/dotcms-js';
import { of } from 'rxjs';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotNavLogoService } from '@services/dot-nav-logo/dot-nav-logo.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('AppComponent', () => {
    let fixture: ComponentFixture<AppComponent>;
    let de: DebugElement;
    let dotCmsConfigService: DotcmsConfigService;
    let dotUiColorsService: DotUiColorsService;
    let dotMessageService: DotMessageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [AppComponent],
            imports: [RouterTestingModule, HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotUiColorsService,
                DotNavLogoService,
                DotcmsConfigService,
                LoggerService,
                StringUtils
            ]
        });

        fixture = TestBed.createComponent(AppComponent);
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
                },
                releaseInfo: {
                    buildDate: 'Jan 1, 2022'
                }
            })
        );
        spyOn(dotUiColorsService, 'setColors');
        spyOn(dotMessageService, 'init');
    });

    it('should init message service', () => {
        fixture.detectChanges();
        expect(dotMessageService.init).toHaveBeenCalledWith({ buildDate: 'Jan 1, 2022' });
    });

    it('should have router-outlet', () => {
        fixture.detectChanges();
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
    });

    it('should set ui colors', () => {
        fixture.detectChanges();
        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(jasmine.any(HTMLElement), {
            primary: '#123',
            secondary: '#456',
            background: '#789'
        });
    });
});
