/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RouterTestingModule } from '@angular/router/testing';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import {
    CoreWebService,
    CoreWebServiceMock,
    DotcmsConfigService,
    LoggerService,
    StringUtils
} from '@dotcms/dotcms-js';

import { DotNavLogoService } from './api/services/dot-nav-logo/dot-nav-logo.service';
import { DotUiColorsService } from './api/services/dot-ui-colors/dot-ui-colors.service';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
    let fixture: ComponentFixture<AppComponent>;
    let de: DebugElement;
    let dotCmsConfigService: DotcmsConfigService;
    let dotUiColorsService: DotUiColorsService;
    let dotMessageService: DotMessageService;
    let dotLicenseService: DotLicenseService;

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
                DotLicenseService
            ]
        });

        fixture = TestBed.createComponent(AppComponent);
        de = fixture.debugElement;

        dotCmsConfigService = TestBed.inject(DotcmsConfigService);
        dotUiColorsService = TestBed.inject(DotUiColorsService);
        dotMessageService = TestBed.inject(DotMessageService);
        dotLicenseService = TestBed.inject(DotLicenseService);

        spyOn<any>(dotCmsConfigService, 'getConfig').and.returnValue(
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
        spyOn(dotUiColorsService, 'setColors');
        spyOn(dotMessageService, 'init');
        spyOn(dotLicenseService, 'setLicense');
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
    it('should set license', () => {
        fixture.detectChanges();
        expect(dotLicenseService.setLicense).toHaveBeenCalledWith({
            displayServerId: 'test',
            isCommunity: false,
            level: 200,
            levelName: 'test level'
        });
    });
});
