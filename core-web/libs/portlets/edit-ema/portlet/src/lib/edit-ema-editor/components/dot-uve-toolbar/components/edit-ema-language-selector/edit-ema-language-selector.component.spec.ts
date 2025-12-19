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

describe('DotEmaLanguageSelectorComponent', () => {
    let spectator: Spectator<EditEmaLanguageSelectorComponent>;
    let component: EditEmaLanguageSelectorComponent;

    const createComponent = createComponentFactory({
        component: EditEmaLanguageSelectorComponent,
        imports: [HttpClientTestingModule, NoopAnimationsModule],
        providers: [{ provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                language: {
                    id: 1,
                    language: 'English',
                    countryCode: 'US',
                    languageCode: 'EN',
                    country: 'United States'
                }
            }
        });

        component = spectator.component;
    });

    describe('DOM', () => {
        it('should render a button with the language that comes from the page', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('language-button')).textContent).toBe('English - US');
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

    describe('events', () => {
        it('should trigger the languageChange emitter when the language changes', () => {
            const languageChangeSpy = jest.spyOn(component.selected, 'emit');

            // Call onChange directly to test the event emission
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
});
