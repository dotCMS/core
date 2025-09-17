import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { MultiSelect, MultiSelectChangeEvent } from 'primeng/multiselect';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { createFakeLanguage, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveLanguageFieldComponent } from './dot-content-drive-language-field.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const MOCK_LANGUAGES: DotLanguage[] = [
    createFakeLanguage({
        id: 1,
        languageCode: 'en',
        countryCode: 'US',
        language: 'English',
        country: 'United States'
    }),
    createFakeLanguage({
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain'
    }),
    createFakeLanguage({
        id: 3,
        languageCode: 'fr',
        countryCode: 'FR',
        language: 'French',
        country: 'France'
    })
];

describe('DotContentDriveLanguageFieldComponent', () => {
    let spectator: Spectator<DotContentDriveLanguageFieldComponent>;
    let component: DotContentDriveLanguageFieldComponent;
    let store: SpyObject<InstanceType<typeof DotContentDriveStore>>;
    let languagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotContentDriveLanguageFieldComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn()
            }),
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(MOCK_LANGUAGES))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.language-selector.placeholder': 'Language'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        store = spectator.inject(DotContentDriveStore, true);
        languagesService = spectator.inject(DotLanguagesService);
        store.getFilterValue.mockReturnValue([]);
    });

    it('should fetch languages and populate state', () => {
        spectator.detectChanges();

        expect(languagesService.get).toHaveBeenCalled();
        expect(component.$state().languages).toEqual(MOCK_LANGUAGES);
    });

    it('should set selectedLanguages when store has languageId filter', () => {
        store.getFilterValue.mockReturnValue(['1', '2']);

        spectator.detectChanges();

        expect(store.getFilterValue).toHaveBeenCalledWith('languageId');
        expect(component.$selectedLanguages()).toEqual([1, 2]);
    });

    it('should patch filters with string values when selectedLanguages has values', () => {
        spectator.detectChanges();

        const multiSelectDebugElement = spectator.fixture.debugElement.query(
            By.directive(MultiSelect)
        );

        spectator.triggerEventHandler(multiSelectDebugElement, 'ngModelChange', [1, 2]);

        expect(component.$selectedLanguages()).toEqual([1, 2]);

        spectator.triggerEventHandler(MultiSelect, 'onChange', {} as MultiSelectChangeEvent);

        expect(store.patchFilters).toHaveBeenCalledWith({
            languageId: ['1', '2']
        });
    });

    it('should remove filter when selectedLanguages is empty', () => {
        component.$selectedLanguages.set([]);

        const multiSelectDebugElement = spectator.fixture.debugElement.query(
            By.directive(MultiSelect)
        );

        spectator.triggerEventHandler(multiSelectDebugElement, 'ngModelChange', []);

        spectator.triggerEventHandler(MultiSelect, 'onChange', {} as MultiSelectChangeEvent);

        expect(store.removeFilter).toHaveBeenCalledWith('languageId');
    });

    describe('MultiSelect', () => {
        it('should have correct properties configured', () => {
            spectator.detectChanges();

            const multiSelectDebugElement = spectator.fixture.debugElement.query(
                By.directive(MultiSelect)
            );
            const multiSelectComponent = multiSelectDebugElement.componentInstance;

            expect(multiSelectComponent.scrollHeight).toBe('25rem');
            expect(multiSelectComponent.resetFilterOnHide).toBe(true);
            expect(multiSelectComponent.showToggleAll).toBe(true);

            // For placeholder, we need to check the resolved value from the DOM
            const multiSelectElement = spectator.query('p-multiselect');
            expect(multiSelectElement.getAttribute('ng-reflect-placeholder')).toBe('Language');
        });
    });
});
