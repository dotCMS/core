import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator';
import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { DotLanguagesService } from '@dotcms/data-access';
import { DotLanguagesServiceMock } from '@dotcms/utils-testing';

import { EmaLanguageSelectorComponent } from './edit-ema-language-selector.component';

import { EditEmaStore } from '../../feature/store/dot-ema.store';
import { DotPageApiService } from '../../services/dot-page-api.service';

describe('DotEmaLanguageSelectorComponent', () => {
    let spectator: Spectator<EmaLanguageSelectorComponent>;
    let store: EditEmaStore;
    let component: EmaLanguageSelectorComponent;

    const createComponent = createComponentFactory({
        component: EmaLanguageSelectorComponent,
        imports: [HttpClientTestingModule],
        providers: [
            EditEmaStore,
            {
                provide: DotPageApiService,
                useValue: {
                    get() {
                        return of({
                            page: {
                                title: 'hello world'
                            },
                            viewAs: {
                                language: {
                                    id: 1,
                                    language: 'English',
                                    countryCode: 'US',
                                    languageCode: 'EN',
                                    country: 'United States'
                                }
                            }
                        });
                    },
                    save() {
                        return of({});
                    }
                }
            },
            { provide: DotLanguagesService, useValue: new DotLanguagesServiceMock() }
        ]
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

        store = spectator.inject(EditEmaStore);
        component = spectator.component;

        store.load({ language_id: '1', url: 'page-one' });
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

    describe('store changes', () => {
        it('should trigger the store when the language changes', () => {
            const button = spectator.query(byTestId('language-button'));
            const languageChangeSpy = jest.spyOn(component.languageSelected, 'emit');

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
