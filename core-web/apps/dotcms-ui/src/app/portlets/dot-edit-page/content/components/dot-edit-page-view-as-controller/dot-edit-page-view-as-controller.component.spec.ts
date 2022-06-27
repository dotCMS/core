import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotPersonasService } from '@services/dot-personas/dot-personas.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DotDevicesServiceMock } from '../../../../../test/dot-device-service.mock';
import { DotLanguagesServiceMock } from '../../../../../test/dot-languages-service.mock';
import { DotPersonasServiceMock } from '../../../../../test/dot-personas-service.mock';
import { mockDotDevices } from '../../../../../test/dot-device.mock';
import { DotPersona } from '@models/dot-persona/dot-persona.model';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { DotLanguage } from '@models/dot-language/dot-language.model';
import { mockDotEditPageViewAs } from '../../../../../test/dot-edit-page-view-as.mock';
import { mockDotPersona } from '../../../../../test/dot-persona.mock';
import { mockUser, LoginServiceMock } from '../../../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../../../test/dot-page-render.mock';
import { DotDeviceSelectorComponent } from '@components/dot-device-selector/dot-device-selector.component';
import { DotPersonaSelectorComponent } from '@components/dot-persona-selector/dot-persona-selector.component';
import { DotLanguageSelectorComponent } from '@components/dot-language-selector/dot-language-selector.component';
import { LoginService } from '@dotcms/dotcms-js';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { of } from 'rxjs';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';
import { DotPageRender } from '@models/dot-page/dot-rendered-page.model';
import { DotPageStateService } from '../../services/dot-page-state/dot-page-state.service';
import { DotPageStateServiceMock } from '@tests/dot-page-state.service.mock';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { DotPersonalizeServiceMock } from '@tests/dot-personalize-service.mock';
import { TooltipModule } from 'primeng/tooltip';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@Component({
    selector: 'dot-test-host',
    template: `<dot-edit-page-view-as-controller
        [pageState]="pageState"
    ></dot-edit-page-view-as-controller>`
})
class DotTestHostComponent {
    @Input()
    pageState: DotPageRenderState;
}

@Component({
    selector: 'dot-persona-selector',
    template: ''
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
    selector: 'dot-device-selector',
    template: ''
})
class MockDotDeviceSelectorComponent {
    @Input()
    value: DotDevice;
    @Output()
    selected = new EventEmitter<DotDevice>();
}

@Component({
    selector: 'dot-language-selector',
    template: ''
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

describe('DotEditPageViewAsControllerComponent', () => {
    let componentHost: DotTestHostComponent;
    let fixtureHost: ComponentFixture<DotTestHostComponent>;

    let component: DotEditPageViewAsControllerComponent;
    let de: DebugElement;
    let languageSelector: DotLanguageSelectorComponent;
    let deviceSelector: DotDeviceSelectorComponent;
    let personaSelector: DotPersonaSelectorComponent;
    let dotLicenseService: DotLicenseService;

    beforeEach(
        waitForAsync(() => {
            DOTTestBed.configureTestingModule({
                declarations: [
                    DotTestHostComponent,
                    DotEditPageViewAsControllerComponent,
                    MockDotPersonaSelectorComponent,
                    MockDotDeviceSelectorComponent,
                    MockDotLanguageSelectorComponent
                ],
                imports: [BrowserAnimationsModule, TooltipModule, DotPipesModule],
                providers: [
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
                    }
                ]
            });
        })
    );

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(DotTestHostComponent);
        componentHost = fixtureHost.componentInstance;
        de = fixtureHost.debugElement.query(By.css('dot-edit-page-view-as-controller'));
        component = de.componentInstance;
        dotLicenseService = de.injector.get(DotLicenseService);
    });

    describe('community license', () => {
        beforeEach(() => {
            spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(false));
            // spyOn(component.changeViewAs, 'emit');

            componentHost.pageState = new DotPageRenderState(mockUser(), mockDotRenderedPage());

            fixtureHost.detectChanges();
        });

        it('should have only language', () => {
            expect(de.query(By.css('dot-language-selector'))).not.toBeNull();
            expect(de.query(By.css('dot-device-selector'))).toBeFalsy();
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
            deviceSelector = de.query(By.css('dot-device-selector')).componentInstance;
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
            // expect(component.changeViewAs.emit).toHaveBeenCalledWith({
            //     language: mockDotLanguage,
            //     persona: mockDotPersona,
            //     mode: 'PREVIEW'
            // });
        });

        it('should have Device selector with tooltip', () => {
            const deviceSelectorDe = de.query(By.css('dot-device-selector'));
            expect(deviceSelector).not.toBeNull();
            expect(deviceSelectorDe.attributes.appendTo).toBe('body');
            expect(deviceSelectorDe.attributes['ng-reflect-text']).toBe('Default Device');
            expect(deviceSelectorDe.attributes['ng-reflect-tooltip-position']).toBe('bottom');
        });

        it('should emit changes in Device', () => {
            fixtureHost.detectChanges();
            deviceSelector.selected.emit(mockDotDevices[0]);

            expect(component.changeDeviceHandler).toHaveBeenCalledWith(mockDotDevices[0]);
            // expect(component.changeViewAs.emit).toHaveBeenCalledWith({
            //     language: mockDotLanguage,
            //     device: mockDotDevices[0],
            //     mode: 'PREVIEW'
            // });
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
            // expect(component.changeViewAs.emit).toHaveBeenCalledWith({
            //     language: testlanguage,
            //     mode: 'PREVIEW'
            // });
        });

        it('should propagate the values to the selector components on init', () => {
            componentHost.pageState = new DotPageRenderState(
                mockUser(),
                new DotPageRender({
                    ...mockDotRenderedPage(),
                    viewAs: mockDotEditPageViewAs
                })
            );
            fixtureHost.detectChanges();

            // expect(languageSelector.value).toEqual(mockDotPersona);
            expect(deviceSelector.value).toEqual(mockDotEditPageViewAs.device);

            // expect(personaSelector.value).toEqual(mockDotEditPageViewAs.persona);
            // expect(personaSelector.pageId).toEqual(mockDotRenderedPage.page.identifier);
        });
    });
});
