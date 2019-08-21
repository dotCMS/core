import { ComponentFixture } from '@angular/core/testing';
import { DotLanguageSelectorComponent } from './dot-language-selector.component';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotLanguagesServiceMock } from '../../../test/dot-languages-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { mockDotLanguage } from '../../../test/dot-language.mock';
import { Dropdown } from 'primeng/primeng';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-messages-service';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

const messageServiceMock = new MockDotMessageService({
    'editpage.viewas.label.language': 'Language'
});

describe('DotLanguageSelectorComponent', () => {
    let component: DotLanguageSelectorComponent;
    let fixture: ComponentFixture<DotLanguageSelectorComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotLanguageSelectorComponent],
            imports: [BrowserAnimationsModule, DotIconModule],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                },
                {
                    provide: DotLanguagesService,
                    useClass: DotLanguagesServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotLanguageSelectorComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
    });

    it('should have icon', () => {
        fixture.detectChanges();
        const icon = de.query(By.css('dot-icon'));
        expect(icon.attributes.name).toBe('language');
        expect(icon.attributes.big).toBeDefined();
    });

    it('should have label', () => {
        fixture.detectChanges();
        const label = de.query(By.css('label')).nativeElement;
        expect(label.textContent).toBe('Language');
    });

    it('should load languages in the dropdown', () => {
        fixture.detectChanges();
        const decoratedLanguage = {
            ...mockDotLanguage,
            language: `${mockDotLanguage.language} (${mockDotLanguage.countryCode})`
        };
        expect(component.languagesOptions).toEqual([decoratedLanguage]);
    });

    it('should have right attributes on dropdown', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        expect(pDropDown.attributes.dataKey).toBe('id');
        expect(pDropDown.attributes.optionLabel).toBe('language');
        expect(pDropDown.attributes.tiny).toBeDefined();
    });

    it('should emit the selected language', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: mockDotLanguage });

        expect(component.change).toHaveBeenCalledWith(mockDotLanguage);
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotLanguage);
    });

    it('shoudl set fixed width to dropdown', () => {
        fixture.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.style).toEqual({ width: '120px' });
    });
});
