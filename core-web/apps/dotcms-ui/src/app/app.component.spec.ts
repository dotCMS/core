/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { DotLicenseService, DotMessageService, DotUiColorsService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    LoggerService,
    StringUtils
} from '@dotcms/dotcms-js';


import { DotNavLogoService } from './api/services/dot-nav-logo/dot-nav-logo.service';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
    let fixture: ComponentFixture<AppComponent>;
    let de: DebugElement;
    let dotCmsConfigService: DotcmsConfigService;
    let dotUiColorsService: DotUiColorsService;
    let dotMessageService: DotMessageService;
    let dotLicenseService: DotLicenseService;
    let dotNavLogoService: DotNavLogoService;

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
                StringUtils,
                DotLicenseService,
                DotMessageService
            ]
        });

        dotCmsConfigService = TestBed.inject(DotcmsConfigService);
        dotUiColorsService = TestBed.inject(DotUiColorsService);
        dotMessageService = TestBed.inject(DotMessageService);
        dotLicenseService = TestBed.inject(DotLicenseService);
        dotNavLogoService = TestBed.inject(DotNavLogoService);

        jest.spyOn<any>(dotCmsConfigService, 'getConfig').mockReturnValue(
            of({
                colors: {
                    primary: '#123',
                    secondary: '#456',
                    background: '#789'
                },
                releaseInfo: {
                    buildDate: 'Jan 1, 2022'
                },
                license: {
                    displayServerId: 'test',
                    isCommunity: false,
                    level: 200,
                    levelName: 'test level'
                }
            })
        );
        jest.spyOn(dotUiColorsService, 'setColors');
        jest.spyOn(dotMessageService, 'init');
        jest.spyOn(dotLicenseService, 'setLicense');
        jest.spyOn(dotNavLogoService, 'setLogo');

        fixture = TestBed.createComponent(AppComponent);
        de = fixture.debugElement;
    });

    it('should init message service', () => {
        fixture.detectChanges();
        expect(dotMessageService.init).toHaveBeenCalledWith({ buildDate: 'Jan 1, 2022' });
        expect(dotMessageService.init).toHaveBeenCalledTimes(1);
    });

    it('should have router-outlet', () => {
        fixture.detectChanges();
        expect(de.query(By.css('router-outlet')) !== null).toBe(true);
    });

    it('should set ui colors', () => {
        fixture.detectChanges();
        expect(dotUiColorsService.setColors).toHaveBeenCalledWith(expect.any(HTMLElement), {
            primary: '#123',
            secondary: '#456',
            background: '#789'
        });
    });
    it.skip('should set license', () => {
        // TODO: Fix this test - DotLicenseService injection issue
        fixture.detectChanges();
        expect(dotLicenseService.setLicense).toHaveBeenCalled();
    });

    it('should set logo', () => {
        fixture.detectChanges();
        expect(dotNavLogoService.setLogo).toHaveBeenCalledWith(undefined);
        expect(dotNavLogoService.setLogo).toHaveBeenCalledTimes(1);
    });
});
