import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@openng/spectator/jest';
import { of } from 'rxjs';

import { By } from '@angular/platform-browser';

import { Listbox } from 'primeng/listbox';
import { Popover } from 'primeng/popover';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { createFakeLanguage, MockDotMessageService } from '@dotcms/utils-testing';

import { DotLanguageFilterComponent } from './dot-language-filter.component';

import { DotChipFilterComponent } from '../dot-chip-filter/dot-chip-filter.component';

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

describe('DotLanguageFilterComponent', () => {
    let spectator: Spectator<DotLanguageFilterComponent>;
    let component: DotLanguageFilterComponent;
    let languagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotLanguageFilterComponent,
        providers: [
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(MOCK_LANGUAGES))
            }),
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'content-drive.language-selector.placeholder': 'Language',
                    'content-drive.chip-filter.overflow-label': '{0} and {1} more',
                    search: 'Search'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        languagesService = spectator.inject(DotLanguagesService);
    });

    afterEach(() => jest.clearAllMocks());

    it('should fetch languages and populate state', () => {
        spectator.detectChanges();

        expect(languagesService.get).toHaveBeenCalled();
        expect(component.$state().languages).toEqual(MOCK_LANGUAGES);
    });

    it('should not inject any portlet store', () => {
        // The component is instantiated with only DotLanguagesService + DotMessageService provided,
        // so construction succeeding is the assertion.
        expect(() => spectator.detectChanges()).not.toThrow();
    });

    describe('selectedLanguageIds input', () => {
        it('should seed the working selection from the host', () => {
            spectator.setInput('selectedLanguageIds', [1, 2]);
            spectator.detectChanges();

            expect(component.$selectedLanguages()).toEqual([1, 2]);
        });

        it('should re-seed when the host pushes a different selection', () => {
            spectator.setInput('selectedLanguageIds', [1, 2]);
            spectator.detectChanges();

            spectator.setInput('selectedLanguageIds', []);
            spectator.detectChanges();

            expect(component.$selectedLanguages()).toEqual([]);
        });
    });

    describe('selectionChange output', () => {
        it('should emit the selected ids on change', () => {
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('selectionChange').subscribe(handler);

            component.$selectedLanguages.set([1, 2]);
            component.onChange();

            expect(handler).toHaveBeenCalledWith([1, 2]);
        });

        it('should emit an empty array when the selection is cleared', () => {
            spectator.setInput('selectedLanguageIds', [1]);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('selectionChange').subscribe(handler);

            component.$selectedLanguages.set([]);
            component.onChange();

            expect(handler).toHaveBeenCalledWith([]);
        });
    });

    describe('Chip', () => {
        it('should render the chip with the default title key translated', () => {
            spectator.detectChanges();

            const chip = spectator.query(byTestId('language-chip'));
            expect(chip).toBeTruthy();
            expect(chip?.querySelector('[data-testid="chip-title"]')?.textContent?.trim()).toBe(
                'Language'
            );
        });

        it('should expose selected language names with iso codes for the chip', () => {
            spectator.setInput('selectedLanguageIds', [1, 2]);
            spectator.detectChanges();

            expect(component['$selectedLanguageNames']()).toEqual([
                'English (en-US)',
                'Spanish (es-ES)'
            ]);
        });

        describe('removable', () => {
            // These live here, in the SHARED component's spec, on purpose. Content Drive needs the
            // chip's remove control suppressed while the only selection is the environment default,
            // because clearing it re-seeds the same value. A guard in the portlet's spec would not
            // protect that: whoever refactors this component runs `nx test ui`, not the portlet suite,
            // so the capability has to be pinned where the change happens. The portlet spec separately
            // pins that it passes the input.
            it('should offer the remove control by default', () => {
                spectator.setInput('selectedLanguageIds', [1, 2]);
                spectator.detectChanges();

                const chip = spectator.query(byTestId('language-chip'));

                expect(chip?.querySelector('[data-testid="chip-remove"]')).toBeTruthy();
            });

            it('should suppress the remove control when the host says it is not removable', () => {
                spectator.setInput('selectedLanguageIds', [1]);
                spectator.setInput('removable', false);
                spectator.detectChanges();

                const chip = spectator.query(byTestId('language-chip'));

                expect(chip?.querySelector('[data-testid="chip-remove"]')).toBeFalsy();
            });

            it('should keep showing the selection while it is not removable', () => {
                // Suppressing the X must not hide what is selected — the chip still has to read as
                // "English (en-US)", it just cannot be cleared from here.
                spectator.setInput('selectedLanguageIds', [1]);
                spectator.setInput('removable', false);
                spectator.detectChanges();

                const chip = spectator.query(byTestId('language-chip'));

                expect(chip?.querySelector('[data-testid="chip-values"]')?.textContent).toContain(
                    'English (en-US)'
                );
            });

            it('should pass the input straight through to the chip', () => {
                spectator.setInput('removable', false);
                spectator.detectChanges();

                const chipDe = spectator.fixture.debugElement.query(
                    By.directive(DotChipFilterComponent)
                );

                expect((chipDe.componentInstance as DotChipFilterComponent).removable()).toBe(
                    false
                );
            });
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

        it('should clear the selection and emit when the chip emits removed', () => {
            spectator.setInput('selectedLanguageIds', [1]);
            spectator.detectChanges();

            const handler = jest.fn();
            spectator.output('selectionChange').subscribe(handler);

            const chipDe = spectator.fixture.debugElement.query(
                By.directive(DotChipFilterComponent)
            );
            spectator.triggerEventHandler(chipDe, 'removed', undefined);

            expect(component.$selectedLanguages()).toEqual([]);
            expect(handler).toHaveBeenCalledWith([]);
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
