import { ComponentFixture } from '@angular/core/testing';
import { DotLanguageSelectorComponent } from './dot-language-selector.component';
import { DotLanguagesService } from '@services/dot-languages/dot-languages.service';
import { DotLanguagesServiceMock } from '../../../test/dot-languages-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement, Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { mockDotLanguage } from '../../../test/dot-language.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { of } from 'rxjs';
import { DotLanguage } from '@shared/models/dot-language/dot-language.model';

const messageServiceMock = new MockDotMessageService({
    'editpage.viewas.label.language': 'Language'
});

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-language-selector [value]="value"></dot-language-selector>
    `
})
class TestHostComponent {
    value: DotLanguage = mockDotLanguage;
    contentInode = '123';
}

describe('DotLanguageSelectorComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let deHost: DebugElement;
    let dotLanguagesService: DotLanguagesService;
    let component: DotLanguageSelectorComponent;
    let de: DebugElement;

    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent, DotLanguageSelectorComponent],
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

        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        de = deHost.query(By.css('dot-language-selector'));
        component = de.componentInstance;

        dotLanguagesService = testbed.get(DotLanguagesService);
    });

    it('should have icon', () => {
        fixtureHost.detectChanges();
        const icon = de.query(By.css('dot-icon'));
        expect(icon.attributes.name).toBe('language');
        expect(icon.attributes.big).toBeDefined();
    });

    it('should have label', () => {
        fixtureHost.detectChanges();
        const label = de.query(By.css('label')).nativeElement;
        expect(label.textContent).toBe('Language');
    });

    it('should load languages in the dropdown', () => {
        fixtureHost.detectChanges();
        const decoratedLanguage = {
            ...mockDotLanguage,
            language: `${mockDotLanguage.language} (${mockDotLanguage.countryCode})`
        };
        expect(component.options).toEqual([decoratedLanguage]);
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

    describe('disabled', () => {
        it('should set disable when no lang options present', () => {
            spyOn(dotLanguagesService, 'get').and.returnValue(of([]));

            fixtureHost.detectChanges();

            expect(dotLanguagesService.get).toHaveBeenCalledTimes(1);
            const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
            expect(pDropDown.componentInstance.disabled).toBe(true);
        });

        it('should add class to the host when disabled', () => {
            spyOn(dotLanguagesService, 'get').and.returnValue(of([]));
            fixtureHost.detectChanges();
            expect(de.nativeElement.classList.contains('disabled')).toBe(true);
        });
    });
});
