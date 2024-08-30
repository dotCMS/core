import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { Dropdown } from 'primeng/dropdown';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { mockLanguageArray } from '@dotcms/utils-testing';

import { DotLanguageSelectorComponent } from './dot-language-selector.component';

const PAGE_ID = '0000-1111-2222-3333';

describe('DotLanguageSelectorComponent', () => {
    let spectator: Spectator<DotLanguageSelectorComponent>;
    let dotLanguagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotLanguageSelectorComponent,
        providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()],
        componentProviders: [mockProvider(DotLanguagesService)]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        dotLanguagesService = spectator.inject(DotLanguagesService, true);
        dotLanguagesService.getLanguagesUsedPage.and.returnValue(of(mockLanguageArray));

        spectator.setInput('pageId', PAGE_ID);
        spectator.detectChanges();
    });

    it('should exist a dropdown', () => {
        expect(spectator.query(Dropdown)).toExist();
    });

    it('should load languages in the dropdown with every change of value input', () => {
        const DOT_LANG: DotLanguage = mockLanguageArray[0];
        spectator.setInput('value', DOT_LANG);
        spectator.detectComponentChanges();
        expect(dotLanguagesService.getLanguagesUsedPage).toHaveBeenCalledWith(PAGE_ID);
        expect(spectator.component.languagesList().length).toBe(mockLanguageArray.length);

        const pDropdown: Dropdown = spectator.query(Dropdown);
        expect(pDropdown.options).toEqual(mockLanguageArray);
    });

    it('should have right attributes on dropdown', () => {
        const valueKey = 'id';
        const labelKey = 'language';
        const pDropdown: Dropdown = spectator.query(Dropdown);

        expect(pDropdown.dataKey).toBe(valueKey);
        expect(pDropdown.optionLabel).toBe(labelKey);

        expect(spectator.query(byTestId('language-selector'))).toHaveClass('p-dropdown-sm');
    });
});
