import { ComponentFixture } from '@angular/core/testing';
import { DotEditContentViewAsToolbarComponent } from './dot-edit-content-view-as-toolbar.component';
import { DotDevicesService } from '../../../../../api/services/dot-devices/dot-devices.service';
import { DotLanguagesService } from '../../../../../api/services/dot-languages/dot-languages.service';
import { DotPersonasService } from '../../../../../api/services/dot-personas/dot-personas.service';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { Component, DebugElement, EventEmitter, Input, Output } from '@angular/core';
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
    let component: DotEditContentViewAsToolbarComponent;
    let fixture: ComponentFixture<DotEditContentViewAsToolbarComponent>;
    let de: DebugElement;
    let languageSelector: DebugElement;
    let deviceSelector: DebugElement;
    let personaSelector: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [
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
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotEditContentViewAsToolbarComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.value = { language: mockDotLanguage };
        languageSelector = de.query(By.css('dot-language-selector'));
        deviceSelector = de.query(By.css('dot-device-selector'));
        personaSelector = de.query(By.css('dot-persona-selector'));
    });

    it('should have Persona selector', () => {
        expect(personaSelector).not.toBeNull();
    });

    it('should emit changes in Personas', () => {
        spyOn(component, 'changePersonaHandler').and.callThrough();
        spyOn(component.changeViewAs, 'emit');
        personaSelector.componentInstance.selected.emit(mockDotPersona);
        fixture.detectChanges();

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
        spyOn(component, 'changeDeviceHandler').and.callThrough();
        spyOn(component.changeViewAs, 'emit');
        deviceSelector.componentInstance.selected.emit(mockDotDevice);
        fixture.detectChanges();

        expect(component.changeDeviceHandler).toHaveBeenCalledWith(mockDotDevice);
        expect(component.changeViewAs.emit).toHaveBeenCalledWith({ language: mockDotLanguage, device: mockDotDevice });
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
        spyOn(component, 'changeLanguageHandler').and.callThrough();
        spyOn(component.changeViewAs, 'emit');
        languageSelector.componentInstance.selected.emit(testlanguage);
        fixture.detectChanges();

        expect(component.changeLanguageHandler).toHaveBeenCalledWith(testlanguage);
        expect(component.changeViewAs.emit).toHaveBeenCalledWith({ language: testlanguage });
    });

    it('should propagate the values to the selector components on init', () => {
        component.value = mockDotEditPageViewAs;
        fixture.detectChanges();

        expect(languageSelector.componentInstance.value).toEqual(mockDotEditPageViewAs.language);
        expect(deviceSelector.componentInstance.value).toEqual(mockDotEditPageViewAs.device);
        expect(personaSelector.componentInstance.value).toEqual(mockDotEditPageViewAs.persona);
    });
});
