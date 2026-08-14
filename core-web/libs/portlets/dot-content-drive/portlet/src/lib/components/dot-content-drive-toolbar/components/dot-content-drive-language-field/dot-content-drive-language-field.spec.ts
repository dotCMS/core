import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';

import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';
import { Popover } from 'primeng/popover';

import { DotMessageService } from '@dotcms/data-access';
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

    const createComponent = createComponentFactory({
        component: DotContentDriveLanguageFieldComponent,
        providers: [
            mockProvider(DotContentDriveStore, {
                patchFilters: jest.fn(),
                removeFilter: jest.fn(),
                getFilterValue: jest.fn(),
                // The store resolves the languages (it needs them to find the default to seed) and
                // the chip renders from there, so both share one request.
                languages: jest.fn().mockReturnValue(MOCK_LANGUAGES),
                defaultLanguageId: jest.fn().mockReturnValue(1)
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
        store.getFilterValue.mockReturnValue([]);
        // Re-stated per test: `jest.clearAllMocks()` clears calls but keeps return values, so a test
        // that stubs "no default language" would otherwise leak into the ones after it.
        store.languages.mockReturnValue(MOCK_LANGUAGES);
        store.defaultLanguageId.mockReturnValue(1);
    });

    afterEach(() => jest.clearAllMocks());

    it('should render the languages resolved by the store', () => {
        store.getFilterValue.mockReturnValue(['2']);

        spectator.detectChanges();

        expect(component['$selectedLanguageNames']()).toEqual(['Spanish (es-ES)']);
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

    it('should snap back to the default language when the selection is emptied', () => {
        // An empty language filter is not neutral: the backend then omits the language term and
        // returns every language version of a contentlet as its own row.
        store.getFilterValue.mockReturnValue(['2']);
        spectator.detectChanges();

        component.$selectedLanguages.set([]);
        component.onChange();

        expect(store.patchFilters).toHaveBeenCalledWith({ languageId: ['1'] });
        expect(store.removeFilter).not.toHaveBeenCalled();
        expect(component.$selectedLanguages()).toEqual([1]);
    });

    it('should remove the filter when emptied and no default language is known', () => {
        // The languages request failed, so the pre-seeding behaviour has to stand.
        store.defaultLanguageId.mockReturnValue(undefined);
        store.getFilterValue.mockReturnValue(['2']);
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

        it('should reset to the default language when the chip emits removed', () => {
            store.getFilterValue.mockReturnValue(['2', '3']);
            spectator.detectChanges();

            const chipDe = spectator.fixture.debugElement.query(
                By.directive(DotChipFilterComponent)
            );
            spectator.triggerEventHandler(chipDe, 'removed', undefined);

            expect(component.$selectedLanguages()).toEqual([1]);
            expect(store.patchFilters).toHaveBeenCalledWith({ languageId: ['1'] });
        });

        it('should not offer the remove button while only the default language is selected', () => {
            store.getFilterValue.mockReturnValue(['1']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-remove'))).toBeFalsy();
        });

        it('should offer the remove button once a non-default language is selected', () => {
            store.getFilterValue.mockReturnValue(['1', '2']);
            spectator.detectChanges();

            expect(spectator.query(byTestId('chip-remove'))).toBeTruthy();
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
