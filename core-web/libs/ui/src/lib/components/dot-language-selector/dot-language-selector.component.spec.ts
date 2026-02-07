import { createComponentFactory, mockProvider, Spectator, SpyObject } from '@ngneat/spectator/jest';
import { of, throwError } from 'rxjs';

import { Select } from 'primeng/select';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { mockMatchMedia } from '@dotcms/utils-testing';

import { DotLanguageSelectorComponent } from './dot-language-selector.component';

type DotLanguageWithLabel = DotLanguage & { label: string };

const MOCK_LANGUAGES: DotLanguage[] = [
    {
        id: 1,
        language: 'Spanish',
        countryCode: 'ES',
        languageCode: 'es',
        isoCode: 'es-es',
        defaultLanguage: false
    } as DotLanguage,
    {
        id: 2,
        language: 'English',
        countryCode: 'US',
        languageCode: 'en',
        isoCode: 'en-us',
        defaultLanguage: true
    } as DotLanguage,
    {
        id: 3,
        language: 'French',
        countryCode: '',
        languageCode: 'fr',
        isoCode: 'fr',
        defaultLanguage: false
    } as DotLanguage
];

describe('DotLanguageSelectorComponent', () => {
    let spectator: Spectator<DotLanguageSelectorComponent>;
    let languagesService: SpyObject<DotLanguagesService>;

    const createComponent = createComponentFactory({
        component: DotLanguageSelectorComponent,
        providers: [mockProvider(DotLanguagesService)]
    });

    beforeEach(() => {
        mockMatchMedia();

        spectator = createComponent({ detectChanges: false });
        languagesService = spectator.inject(DotLanguagesService, true);
        languagesService.get.mockReturnValue(of(MOCK_LANGUAGES));
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    describe('initialization', () => {
        it('should load languages and select the default language when value is empty', async () => {
            const cvaOnChange = jest.fn();
            spectator.component.registerOnChange(cvaOnChange);

            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            expect(languagesService.get).toHaveBeenCalledTimes(1);
            expect(cvaOnChange).toHaveBeenCalledWith(2);

            const selectEl = spectator.query('[data-testId="language-selector"]') as HTMLElement;
            expect(selectEl).toBeTruthy();
            expect(selectEl.textContent).toContain('English - US');
        });

        it('should stop loading when the service fails', () => {
            languagesService.get.mockReturnValue(throwError(() => new Error('boom')));

            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.loading).toBe(false);
        });
    });

    describe('template / overlay', () => {
        it('should render options sorted by label when opening the overlay', async () => {
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            spectator.click('[data-testId="language-selector"]');
            spectator.detectChanges();

            const optionEls = Array.from(document.querySelectorAll('li[role="option"]'));
            const optionTexts = optionEls
                .map((el) => el.textContent?.trim())
                .filter((t): t is string => !!t);

            // Expected order: English - US, French, Spanish - ES
            expect(optionTexts).toEqual(
                expect.arrayContaining(['English - US', 'French', 'Spanish - ES'])
            );
            expect(optionTexts[0]).toBe('English - US');
        });
    });

    describe('interactions', () => {
        it('should emit onChange (id) and call ControlValueAccessor touched when a language is selected', () => {
            const onChangeSpy = jest.fn();
            const onLanguageChangeSpy = jest.fn();
            const onTouchedSpy = jest.fn();
            const cvaOnChange = jest.fn();

            spectator.component.onChange.subscribe(onChangeSpy);
            spectator.component.onLanguageChange.subscribe(onLanguageChangeSpy);
            spectator.component.registerOnTouched(onTouchedSpy);
            spectator.component.registerOnChange(cvaOnChange);

            spectator.detectChanges();

            // Use the actual option instance from the component state to mirror PrimeNG behavior
            const spanish = spectator.component.$state.languages().find((l) => l.id === 1) as
                | DotLanguageWithLabel
                | undefined;
            expect(spanish).toBeTruthy();

            spectator.triggerEventHandler(Select, 'onChange', { value: spanish });
            spectator.detectChanges();

            expect(onTouchedSpy).toHaveBeenCalledTimes(1);
            expect(onChangeSpy).toHaveBeenCalledWith(1);
            expect(onLanguageChangeSpy).toHaveBeenCalledWith(spanish);
            expect(cvaOnChange).toHaveBeenCalledWith(1);

            // Verify via DOM: open overlay and assert the selected option is highlighted
            spectator.click('[data-testId="language-selector"]');
            spectator.detectChanges();

            const selectedOption = (document.querySelector(
                'li[role="option"][aria-selected="true"]'
            ) ?? document.querySelector('li.p-highlight')) as HTMLElement | null;

            expect(selectedOption?.textContent?.trim()).toBe('Spanish - ES');
        });

        it('should emit onShow and onHide when the dropdown is shown/hidden', () => {
            const showSpy = jest.fn();
            const hideSpy = jest.fn();

            spectator.component.onShow.subscribe(showSpy);
            spectator.component.onHide.subscribe(hideSpy);

            spectator.detectChanges();

            // PrimeNG Select types onShow/onHide as AnimationEvent
            const animationEvent = new AnimationEvent('animationstart');
            spectator.triggerEventHandler(Select, 'onShow', animationEvent);
            spectator.triggerEventHandler(Select, 'onHide', animationEvent);

            expect(showSpy).toHaveBeenCalledTimes(1);
            expect(hideSpy).toHaveBeenCalledTimes(1);
        });

        it('should focus the PrimeNG select input when the host receives focus', async () => {
            spectator.detectChanges();
            await spectator.fixture.whenStable();
            spectator.detectChanges();

            type SelectWithFocus = Select & {
                focusInputViewChild?: { nativeElement?: { focus?: () => void } };
            };

            const selectInstance = spectator.component.select() as unknown as SelectWithFocus;
            expect(selectInstance).toBeTruthy();

            // PrimeNG internal input ref may vary by version; stub it to validate our host focus behavior.
            const focusSpy = jest.fn();
            selectInstance.focusInputViewChild = { nativeElement: { focus: focusSpy } };

            spectator.element.dispatchEvent(new FocusEvent('focus'));

            expect(focusSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('disabled state', () => {
        it('should disable the underlying select when disabled input is true', () => {
            spectator.setInput('disabled', true);
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.disabled()).toBe(true);
        });

        it('should disable the underlying select when disabled via ControlValueAccessor', () => {
            spectator.detectChanges();

            const select = spectator.query(Select);
            expect(select.disabled()).toBe(false);

            spectator.component.setDisabledState(true);
            spectator.detectChanges();

            expect(select.disabled()).toBe(true);
        });
    });
});
