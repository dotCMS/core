import { ComponentFixture } from '@angular/core/testing';
import { DotLanguageSelectorComponent } from './dot-language-selector.component';
import { DotLanguagesService } from '../../../api/services/dot-languages/dot-languages.service';
import { DotLanguagesServiceMock } from '../../../test/dot-languages-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { mockDotLanguage } from '../../../test/dot-language.mock';
import { Observable } from 'rxjs/Observable';
import { StringPixels } from '../../../api/util/string-pixels-util';
import { DotLanguage } from '../../../shared/models/dot-language/dot-language.model';

describe('DotLanguageSelectorComponent', () => {
    let component: DotLanguageSelectorComponent;
    let fixture: ComponentFixture<DotLanguageSelectorComponent>;
    let de: DebugElement;

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotLanguageSelectorComponent],
            imports: [BrowserAnimationsModule],
            providers: [
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

    it('should load languages in the dropdown', () => {
        fixture.detectChanges();
        expect(component.languagesOptions).toEqual([mockDotLanguage]);
    });

    it('should emit the selected language', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: mockDotLanguage });

        expect(component.change).toHaveBeenCalledWith(mockDotLanguage);
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotLanguage);
    });

    it('should set max text width to dropdpown', () => {
        fixture.detectChanges();
        const optionValues = component.languagesOptions.map((languageOption: DotLanguage) => languageOption.language);
        const textSize = StringPixels.getDropdownWidth(optionValues);
        expect(component.dropdownWidth).toEqual(textSize);
    });
});
