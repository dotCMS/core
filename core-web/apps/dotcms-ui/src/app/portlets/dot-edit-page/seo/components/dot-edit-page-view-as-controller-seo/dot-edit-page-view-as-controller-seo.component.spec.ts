import { of } from 'rxjs';

import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { TooltipModule } from 'primeng/tooltip';

import {
    DotDevicesService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageDisplayService,
    DotMessageService,
    DotPageStateService,
    DotPersonalizeService,
    DotPersonasService,
    DotSessionStorageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotDevice, DotLanguage, DotPageRenderState, DotPersona } from '@dotcms/dotcms-models';
import { DotSafeHtmlPipe } from '@dotcms/ui';
import {
    DotDevicesServiceMock,
    DotLanguagesServiceMock,
    DotMessageDisplayServiceMock,
    DotPageStateServiceMock,
    DotPersonalizeServiceMock,
    DotPersonasServiceMock,
    LoginServiceMock,
    MockDotMessageService,
    mockDotPersona,
    mockDotRenderedPage,
    mockUser
} from '@dotcms/utils-testing';

import { DotEditPageViewAsControllerSeoComponent } from './dot-edit-page-view-as-controller-seo.component';

import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { DotPersonaSelectorComponent } from '../../../../../view/components/dot-persona-selector/dot-persona-selector.component';

@Component({
    selector: 'dot-test-host',
    template: `
        <dot-edit-page-view-as-controller-seo
            [pageState]="pageState"></dot-edit-page-view-as-controller-seo>
    `,
    standalone: false
})
class DotTestHostComponent {
    @Input()
    pageState: DotPageRenderState;
}

@Component({
    selector: 'dot-persona-selector',
    template: '',
    standalone: false
})
class MockDotPersonaSelectorComponent {
    @Input()
    pageId: string;
    @Input()
    value: DotPersona;
    @Input() disabled: boolean;
    @Input()
    pageState: DotPageRenderState;

    @Output()
    selected = new EventEmitter<DotPersona>();
}

@Component({
    selector: 'dot-device-selector-seo',
    template: '',
    standalone: false
})
class MockDotDeviceSelectorComponent {
    @Input()
    value: DotDevice;
    @Output()
    selected = new EventEmitter<DotDevice>();
}

@Component({
    selector: 'dot-language-selector',
    template: '',
    standalone: false
})
class MockDotLanguageSelectorComponent {
    @Input()
    value: DotLanguage;
    @Input()
    contentInode: string;

    @Output()
    selected = new EventEmitter<DotLanguage>();
}

const messageServiceMock = new MockDotMessageService({
    'editpage.viewas.previewing': 'Previewing',
    'editpage.viewas.default.device': 'Default Device'
});

describe('DotEditPageViewAsControllerSeoComponent', () => {
    let componentHost: DotTestHostComponent;
    let fixtureHost: ComponentFixture<DotTestHostComponent>;

    let component: DotEditPageViewAsControllerSeoComponent;
    let de: DebugElement;
    let languageSelector: DotLanguageSelectorComponent;
    let personaSelector: DotPersonaSelectorComponent;
    let dotLicenseService: DotLicenseService;

    beforeEach(waitForAsync(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                MockDotPersonaSelectorComponent,
                MockDotDeviceSelectorComponent,
                MockDotLanguageSelectorComponent,
                DotTestHostComponent
            ],
            imports: [
                DotEditPageViewAsControllerSeoComponent,
                BrowserAnimationsModule,
                TooltipModule,
                DotSafeHtmlPipe
            ],
            providers: [
                DotSessionStorageService,
                DotLicenseService,
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
                {
                    provide: DotPersonasService,
                    useClass: DotPersonasServiceMock
                },
                {
                    provide: DotLanguagesService,
                    useClass: DotLanguagesServiceMock
                },
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotPageStateService,
                    useClass: DotPageStateServiceMock
                },
                {
                    provide: DotPersonalizeService,
                    useClass: DotPersonalizeServiceMock
                },
                {
                    provide: DotMessageDisplayService,
                    useClass: DotMessageDisplayServiceMock
                }
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(DotTestHostComponent);
        componentHost = fixtureHost.componentInstance;
        de = fixtureHost.debugElement.query(By.css('dot-edit-page-view-as-controller-seo'));
        component = de.componentInstance;
        dotLicenseService = de.injector.get(DotLicenseService);
    });

    describe('community license', () => {
        beforeEach(() => {
            spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));

            componentHost.pageState = new DotPageRenderState(mockUser(), mockDotRenderedPage());

            fixtureHost.detectChanges();
        });

        it('should have only language', () => {
            expect(de.query(By.css('dot-language-selector'))).not.toBeNull();
            expect(de.query(By.css('dot-persona-selector'))).toBeFalsy();
            expect(de.query(By.css('p-checkbox'))).toBeFalsy();
        });
    });

    describe('enterprise license', () => {
        beforeEach(() => {
            spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
            spyOn(component, 'changePersonaHandler').and.callThrough();
            spyOn(component, 'changeDeviceHandler').and.callThrough();
            spyOn(component, 'changeLanguageHandler').and.callThrough();

            componentHost.pageState = new DotPageRenderState(mockUser(), mockDotRenderedPage());

            fixtureHost.detectChanges();

            languageSelector = de.query(By.css('dot-language-selector')).componentInstance;
            personaSelector = de.query(By.css('dot-persona-selector')).componentInstance;
        });

        it('should have persona selector', () => {
            expect(personaSelector).not.toBeNull();
        });

        xit('should persona selector be enabled', () => {
            expect(personaSelector.disabled).toBe(false);
        });

        it('should persona selector be disabled after haveContent is set to false', () => {
            const dotPageStateService: DotPageStateService = de.injector.get(DotPageStateService);
            dotPageStateService.haveContent$.next(false);

            fixtureHost.detectChanges();
            expect(personaSelector.disabled).toBe(true);
        });

        it('should persona selector be enabled after haveContent is set to true', () => {
            const dotPageStateService: DotPageStateService = de.injector.get(DotPageStateService);
            dotPageStateService.haveContent$.next(true);

            fixtureHost.detectChanges();
            expect(personaSelector.disabled).toBe(false);
        });

        it('should emit changes in personas', () => {
            personaSelector.selected.emit(mockDotPersona);

            expect(component.changePersonaHandler).toHaveBeenCalledWith(mockDotPersona);
        });

        it('should have Language selector', () => {
            const languageSelectorDe = de.query(By.css('dot-language-selector'));
            expect(languageSelector).not.toBeNull();
            expect(languageSelectorDe.attributes.appendTo).toBe('body');
            expect(languageSelectorDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
        });

        it('should emit changes in Language', () => {
            const testlanguage: DotLanguage = {
                id: 2,
                languageCode: 'es',
                countryCode: 'es',
                language: 'test',
                country: 'test'
            };
            fixtureHost.detectChanges();
            languageSelector.selected.emit(testlanguage);

            expect(component.changeLanguageHandler).toHaveBeenCalledWith(testlanguage);
        });
    });
});
