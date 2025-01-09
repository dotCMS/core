import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { EditEmaLanguageSelectorComponent } from './edit-ema-language-selector.component';

describe('DotEmaLanguageSelectorComponent', () => {
    let spectator: Spectator<EditEmaLanguageSelectorComponent>;
    let component: EditEmaLanguageSelectorComponent;

    const createComponent = createComponentFactory({
        component: EditEmaLanguageSelectorComponent,
        imports: [HttpClientTestingModule],
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

            const list = spectator.query(byTestId('language-listbox'));

            expect(list).not.toBeNull();
        });
    });

    describe('events', () => {
        it('should trigger the languageChange emitter when the language changes', () => {
            const button = spectator.query(byTestId('language-button'));
            const languageChangeSpy = jest.spyOn(component.selected, 'emit');

            spectator.click(button);

            const list = spectator.debugElement.query(By.css('[data-testId="language-listbox"]'));

            spectator.triggerEventHandler(list, 'onChange', {
                event: new Event('change'),
                value: {
                    id: 2,
                    languageCode: 'IT',
                    countryCode: '', // It comes like this from the mock and is intended
                    language: 'Italian',
                    country: 'Italy'
                }
            });

            expect(languageChangeSpy).toHaveBeenCalledWith(2);
        });
    });
});
