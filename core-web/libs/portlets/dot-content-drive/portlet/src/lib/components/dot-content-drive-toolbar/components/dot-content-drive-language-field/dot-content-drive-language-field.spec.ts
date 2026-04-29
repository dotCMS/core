import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';
import { Popover } from 'primeng/popover';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotChipFilterComponent } from '@dotcms/portlets/content-drive/ui';
import { createFakeLanguage, MockDotMessageService } from '@dotcms/utils-testing';

import { DotContentDriveLanguageFieldComponent } from './dot-content-drive-language-field.component';

import { DotContentDriveStore } from '../../../../store/dot-content-drive.store';

const MOCK_LANGUAGES: DotLanguage[] = [
    createFakeLanguage({
        id: 1,
        languageCode: 'en',
        countryCode: 'US',
        language: 'English',
        country: 'United States',
        isoCode: 'en-US'
    }),
    createFakeLanguage({
        id: 2,
        languageCode: 'es',
        countryCode: 'ES',
        language: 'Spanish',
        country: 'Spain',
        isoCode: 'es-ES'
    }),
    createFakeLanguage({
        id: 3,
        languageCode: 'fr',
        countryCode: 'FR',
        language: 'French',
        country: 'France',
        isoCode: 'fr-FR'
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
                    'content-drive.language-selector.placeholder': 'Language',
                    'content-drive.chip-filter.overflow-label': '{0} and {1} more'
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

    afterEach(() => jest.clearAllMocks());

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

        component.$selectedLanguages.set([1, 2]);
        component.onChange();

        expect(store.patchFilters).toHaveBeenCalledWith({
            languageId: ['1', '2']
        });
    });

    it('should remove filter when selectedLanguages is empty', () => {
        store.getFilterValue.mockReturnValue(['1']);
        spectator.detectChanges();

        component.$selectedLanguages.set([]);
        component.onChange();

        expect(store.removeFilter).toHaveBeenCalledWith('languageId');
    });

    describe('Chip', () => {
        it('should render the chip with the placeholder as title', () => {
            spectator.detectChanges();

            const chip = spectator.query(byTestId('language-chip'));
            expect(chip).toBeTruthy();
            expect(chip?.querySelector('[data-testid="chip-title"]')?.textContent?.trim()).toBe(
                'Language'
            );
        });

        it('should expose selected language names with iso codes for the chip', () => {
            store.getFilterValue.mockReturnValue(['1', '2']);
            spectator.detectChanges();

            expect(component['$selectedLanguageNames']()).toEqual([
                'English (en-US)',
                'Spanish (es-ES)'
            ]);
        });

        it('should toggle popover when the chip is clicked', () => {
            spectator.detectChanges();

            const popoverDe = spectator.fixture.debugElement.query(By.directive(Popover));
            const popover = popoverDe.componentInstance as Popover;
            const toggleSpy = jest.spyOn(popover, 'toggle');

            const chipDe = spectator.fixture.debugElement.query(
                By.directive(DotChipFilterComponent)
            );
            spectator.triggerEventHandler(chipDe, 'clicked', new MouseEvent('click'));

            expect(toggleSpy).toHaveBeenCalled();
        });

        it('should clear selection and remove filter when the chip emits removed', () => {
            store.getFilterValue.mockReturnValue(['1']);
            spectator.detectChanges();

            const chipDe = spectator.fixture.debugElement.query(
                By.directive(DotChipFilterComponent)
            );
            spectator.triggerEventHandler(chipDe, 'removed', undefined);

            expect(component.$selectedLanguages()).toEqual([]);
            expect(store.removeFilter).toHaveBeenCalledWith('languageId');
        });
    });

    describe('Listbox', () => {
        it('should have correct properties configured', () => {
            spectator.detectChanges();

            // Listbox is inside a closed popover, open it via the chip
            const chipHost = spectator.query(byTestId('language-chip'));
            spectator.click(chipHost as Element);
            spectator.detectChanges();

            const listboxDe = spectator.fixture.debugElement.query(By.directive(Listbox));
            const listbox = listboxDe.componentInstance as Listbox;

            expect(listbox.scrollHeight).toBe('25rem');
            expect(listbox.multiple).toBe(true);
            expect(listbox.checkbox).toBe(true);
        });
    });
});
