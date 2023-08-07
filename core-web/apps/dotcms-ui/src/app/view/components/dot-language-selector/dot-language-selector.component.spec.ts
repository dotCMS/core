import { of } from 'rxjs';

import { Component, DebugElement } from '@angular/core';
import { ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import {
    DotLanguagesServiceMock,
    mockDotLanguage,
    MockDotMessageService,
    mockLanguageArray
} from '@dotcms/utils-testing';

import { DotLanguageSelectorComponent } from './dot-language-selector.component';

const messageServiceMock = new MockDotMessageService({
    'editpage.viewas.label.language': 'Language'
});

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-language-selector [value]="value"></dot-language-selector> `
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
    });

    it('should load languages in the dropdown', () => {
        fixtureHost.detectChanges();

        const decoratedLanguages = mockLanguageArray.map((lang) => {
            const countryCode = lang.countryCode.length ? ` (${lang.countryCode})` : '';

            return { ...lang, language: `${lang.language}${countryCode}` };
        });

        expect(component.options).toEqual(decoratedLanguages);
    });

    it('should have right attributes on dropdown', () => {
        fixtureHost.detectChanges();
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
        expect(pDropDown.attributes.dataKey).toBe('id');
        expect(pDropDown.attributes.optionLabel).toBe('language');
        expect(pDropDown.classes['p-dropdown-sm']).toEqual(true);
    });

    it('should emit the selected language', () => {
        fixtureHost.detectChanges();
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: mockDotLanguage });

        expect(component.change).toHaveBeenCalledWith(mockDotLanguage);
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotLanguage);
    });

    describe('disabled', () => {
        it('should set disable when no lang options present', async () => {
            spyOn(dotLanguagesService, 'get').and.returnValue(of([]));

            fixtureHost.detectChanges();
            await fixtureHost.whenStable();

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
