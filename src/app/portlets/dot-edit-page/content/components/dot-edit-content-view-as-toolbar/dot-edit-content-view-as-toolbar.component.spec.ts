import { ComponentFixture, async } from '@angular/core/testing';
import { DotEditContentViewAsToolbarComponent } from './dot-edit-content-view-as-toolbar.component';
import { DotDevicesService } from '../../../../../api/services/dot-devices/dot-devices.service';
import { DotLanguagesService } from '../../../../../api/services/dot-languages/dot-languages.service';
import { DotPersonasService } from '../../../../../api/services/dot-personas/dot-personas.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { Component, DebugElement, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { DotDevicesServiceMock } from '../../../../../test/dot-device-service.mock';
import { DotLanguagesServiceMock } from '../../../../../test/dot-languages-service.mock';
import { DotPersonasServiceMock } from '../../../../../test/dot-personas-service.mock';
import { mockDotLanguage } from '../../../../../test/dot-language.mock';
import { mockDotDevice } from '../../../../../test/dot-device.mock';
import { DotPersona } from '../../../../../shared/models/dot-persona/dot-persona.model';
import { DotDevice } from '../../../../../shared/models/dot-device/dot-device.model';
import { DotLanguage } from '../../../../../shared/models/dot-language/dot-language.model';
import { mockDotEditPageViewAs } from '../../../../../test/dot-edit-page-view-as.mock';
import { mockDotPersona } from '../../../../../test/dot-persona.mock';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { mockUser, LoginServiceMock } from '../../../../../test/login-service.mock';
import { mockDotRenderedPage } from '../../../../../test/dot-rendered-page.mock';
import { DotDeviceSelectorComponent } from '../../../../../view/components/dot-device-selector/dot-device-selector.component';
import { DotPersonaSelectorComponent } from '../../../../../view/components/dot-persona-selector/dot-persona-selector.component';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { PageMode } from '../../../shared/models/page-mode.enum';
import { LoginService } from 'dotcms-js/dotcms-js';

@Component({
    selector: 'dot-test-host',
    template: `<dot-edit-content-view-as-toolbar [pageState]="pageState"></dot-edit-content-view-as-toolbar>`
})
class DotTestHostComponent implements OnInit {
    @Input() pageState: DotRenderedPageState;

    ngOnInit() {}
}

@Component({
    selector: 'dot-persona-selector',
    template: ''
})
class MockDotPersonaSelectorComponent {
    @Input() value: DotPersona;
    @Output() selected = new EventEmitter<DotPersona>();
}

@Component({
    selector: 'dot-device-selector',
    template: ''
})
class MockDotDeviceSelectorComponent {
    @Input() value: DotDevice;
    @Output() selected = new EventEmitter<DotDevice>();
}

@Component({
    selector: 'dot-language-selector',
    template: ''
})
class MockDotLanguageSelectorComponent {
    @Input() value: DotLanguage;
    @Output() selected = new EventEmitter<DotLanguage>();
}

describe('DotEditContentViewAsToolbarComponent', () => {
    let componentHost: DotTestHostComponent;
    let fixtureHost: ComponentFixture<DotTestHostComponent>;

    let component: DotEditContentViewAsToolbarComponent;
    let de: DebugElement;
    let languageSelector: DotLanguageSelectorComponent;
    let deviceSelector: DotDeviceSelectorComponent;
    let personaSelector: DotPersonaSelectorComponent;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
                DotTestHostComponent,
                DotEditContentViewAsToolbarComponent,
                MockDotPersonaSelectorComponent,
                MockDotDeviceSelectorComponent,
                MockDotLanguageSelectorComponent
            ],
            imports: [BrowserAnimationsModule],
            providers: [
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
                }
            ]
        });
    }));

    beforeEach(() => {
        fixtureHost = DOTTestBed.createComponent(DotTestHostComponent);
        componentHost = fixtureHost.componentInstance;

        de = fixtureHost.debugElement.query(By.css('dot-edit-content-view-as-toolbar'));
        component = de.componentInstance;
        languageSelector = de.query(By.css('dot-language-selector')).componentInstance;
        deviceSelector = de.query(By.css('dot-device-selector')).componentInstance;
        personaSelector = de.query(By.css('dot-persona-selector')).componentInstance;

        spyOn(component, 'changePersonaHandler').and.callThrough();
        spyOn(component, 'changeDeviceHandler').and.callThrough();
        spyOn(component, 'changeLanguageHandler').and.callThrough();
        spyOn(component.changeViewAs, 'emit');

        componentHost.pageState = new DotRenderedPageState(mockUser, JSON.parse(JSON.stringify(mockDotRenderedPage)));
    });

    it('should have Persona selector', () => {
        expect(personaSelector).not.toBeNull();
    });

    it('should emit changes in Personas', () => {
        fixtureHost.detectChanges();
        personaSelector.selected.emit(mockDotPersona);

        expect(component.changePersonaHandler).toHaveBeenCalledWith(mockDotPersona);
        expect(component.changeViewAs.emit).toHaveBeenCalledWith({
            language: mockDotLanguage,
            persona: mockDotPersona
        });
    });

    it('should have Device selector', () => {
        expect(deviceSelector).not.toBeNull();
    });

    it('should emit changes in Device', () => {
        fixtureHost.detectChanges();
        deviceSelector.selected.emit(mockDotDevice);

        expect(component.changeDeviceHandler).toHaveBeenCalledWith(mockDotDevice);
        expect(component.changeViewAs.emit).toHaveBeenCalledWith({
            language: mockDotLanguage,
            device: mockDotDevice
        });
    });

    it('should have Language selector', () => {
        expect(languageSelector).not.toBeNull();
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
        expect(component.changeViewAs.emit).toHaveBeenCalledWith({ language: testlanguage });
    });

    it('should propagate the values to the selector components on init', () => {
        componentHost.pageState = new DotRenderedPageState(mockUser, {
            ...mockDotRenderedPage,
            viewAs: mockDotEditPageViewAs
        });
        fixtureHost.detectChanges();

        expect(languageSelector.value).toEqual(mockDotEditPageViewAs.language);
        expect(deviceSelector.value).toEqual(mockDotEditPageViewAs.device);
        expect(personaSelector.value).toEqual(mockDotEditPageViewAs.persona);
    });

    it('should not have what\'s change checkbox', () => {
        componentHost.pageState = new DotRenderedPageState(mockUser, {
            ...mockDotRenderedPage,
            page: {
                ...mockDotRenderedPage.page,
                lockedBy: '123'
            }
        });
        fixtureHost.detectChanges();
        const whatsChanged: DebugElement = de.query(By.css('p-checkbox'));
        expect(whatsChanged).toBe(null);
    });

    it('should have what\'s change checkbox', () => {
        componentHost.pageState = new DotRenderedPageState(mockUser, JSON.parse(JSON.stringify(mockDotRenderedPage)), PageMode.PREVIEW);
        fixtureHost.detectChanges();
        const whatsChanged: DebugElement = de.query(By.css('p-checkbox'));
        expect(whatsChanged).toBeTruthy();
    });

    describe('what\'s change event', () => {
        let whatsChanged: DebugElement;
        beforeEach(() => {
            spyOn(component.whatschange, 'emit');
            componentHost.pageState = new DotRenderedPageState(mockUser, JSON.parse(JSON.stringify(mockDotRenderedPage)), PageMode.PREVIEW);
            fixtureHost.detectChanges();

            whatsChanged = de.query(By.css('p-checkbox'));
        });

        it('should emit what\'s change in true', () => {
            whatsChanged.triggerEventHandler('onChange', true);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(true);
        });

        it('should emit what\'s change in false', () => {
            whatsChanged.triggerEventHandler('onChange', false);
            expect(component.whatschange.emit).toHaveBeenCalledTimes(1);
            expect(component.whatschange.emit).toHaveBeenCalledWith(false);
        });
    });
});
