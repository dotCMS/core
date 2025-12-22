import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { EditEmaLanguageSelectorComponent } from './edit-ema-language-selector.component';

// Mock window.matchMedia for PrimeNG components
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: jest.fn(),
        removeListener: jest.fn(),
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
        dispatchEvent: jest.fn()
    }))
});

describe('EditEmaLanguageSelectorComponent', () => {
    let spectator: Spectator<EditEmaLanguageSelectorComponent>;
    let component: EditEmaLanguageSelectorComponent;

    const mockLanguage = {
        id: 1,
        language: 'English',
        countryCode: 'US',
        languageCode: 'EN',
        country: 'United States'
    };

    const createComponent = createComponentFactory({
        component: EditEmaLanguageSelectorComponent,
        imports: [HttpClientTestingModule, NoopAnimationsModule],
        providers: [{ provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                language: mockLanguage
            }
        });

        component = spectator.component;
        spectator.detectChanges();
    });

    describe('DOM', () => {
        it('should render a button with the language that comes from the page', () => {
            expect(spectator.query(byTestId('language-button'))?.textContent).toBe('English - US');
        });

        it('should render a overlay panel', () => {
            expect(spectator.query(byTestId('language-op'))).not.toBeNull();
        });

        it('should render a listbox of languages', () => {
            const button = spectator.query(byTestId('language-button'));

            spectator.click(button);
            spectator.detectChanges();

            // Popover content may render in the document body
            const list =
                spectator.query(byTestId('language-listbox')) ||
                document.querySelector('[data-testId="language-listbox"]');

            expect(list).not.toBeNull();
        });
    });

    describe('Model synchronization', () => {
        it('should sync model signal when language input changes', () => {
            const newLanguage = {
                id: 2,
                language: 'Italian',
                countryCode: 'IT',
                languageCode: 'IT',
                country: 'Italy'
            };

            spectator.setInput('language', newLanguage);
            spectator.detectChanges();

            const model = component.selectedLanguageModel();
            expect(model).not.toBeNull();
            expect(model?.id).toBe(2);
            expect(model?.label).toBe('Italian - IT');
        });

        it('should not update model if language id has not changed', () => {
            const initialModel = component.selectedLanguageModel();
            expect(initialModel).not.toBeNull();

            // Update language with same id but different properties
            spectator.setInput('language', {
                ...mockLanguage,
                language: 'English Updated'
            });
            spectator.detectChanges();

            const updatedModel = component.selectedLanguageModel();
            expect(updatedModel?.id).toBe(initialModel?.id);
        });

        it('should compute selectedLanguage with correct label format', () => {
            const selected = component.selectedLanguage();
            expect(selected).not.toBeNull();
            expect(selected?.id).toBe(1);
            expect(selected?.label).toBe('English - US');
        });

        it('should compute selectedLanguage label without country code when countryCode is empty', () => {
            spectator.setInput('language', {
                id: 3,
                language: 'Spanish',
                countryCode: '',
                languageCode: 'ES',
                country: 'Spain'
            });
            spectator.detectChanges();

            const selected = component.selectedLanguage();
            expect(selected?.label).toBe('Spanish');
        });
    });

    describe('events', () => {
        it('should emit selected event when language changes', () => {
            const languageChangeSpy = jest.spyOn(component.selected, 'emit');

            component.onChange({
                originalEvent: new Event('change'),
                value: {
                    id: 2,
                    languageCode: 'IT',
                    countryCode: '',
                    language: 'Italian',
                    country: 'Italy',
                    label: 'Italian'
                }
            });

            expect(languageChangeSpy).toHaveBeenCalledWith(2);
        });
    });

    describe('resetModel', () => {
        it('should reset the model to the provided language', () => {
            const newLanguage = {
                id: 3,
                language: 'French',
                countryCode: 'FR',
                languageCode: 'FR',
                country: 'France'
            };

            // Test that resetModel sets the model correctly
            component.resetModel(newLanguage);
            const immediateModel = component.selectedLanguageModel();
            expect(immediateModel).not.toBeNull();
            expect(immediateModel?.id).toBe(3);
            expect(immediateModel?.label).toBe('French - FR');
        });

        it('should create label correctly when resetting model', () => {
            const languageWithoutCountry = {
                id: 4,
                language: 'German',
                countryCode: '',
                languageCode: 'DE',
                country: 'Germany'
            };

            component.resetModel(languageWithoutCountry);
            const immediateModel = component.selectedLanguageModel();
            expect(immediateModel?.label).toBe('German');
        });

        it('should reset model to current input language (use case: dialog rejection)', () => {
            // This is the actual use case: resetting to the current input language
            // when a confirmation dialog is rejected
            component.resetModel(mockLanguage);
            spectator.detectChanges();

            const model = component.selectedLanguageModel();
            expect(model).not.toBeNull();
            expect(model?.id).toBe(mockLanguage.id);
            expect(model?.label).toBe('English - US');
        });
    });
});
