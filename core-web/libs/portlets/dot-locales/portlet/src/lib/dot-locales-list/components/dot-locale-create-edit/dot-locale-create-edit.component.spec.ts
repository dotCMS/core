import { Spectator, createComponentFactory } from '@ngneat/spectator';
import { byTestId } from '@ngneat/spectator/jest';

import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, mockLanguagesISO, mockLocales } from '@dotcms/utils-testing';

import { DotLocaleCreateEditComponent } from './dot-locale-create-edit.component';

const messageServiceMock = new MockDotMessageService({
    Cancel: 'Cancel'
});

describe('DotLocaleCreateEditComponent', () => {
    let spectator: Spectator<DotLocaleCreateEditComponent>;
    let ref: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotLocaleCreateEditComponent,
        imports: [ReactiveFormsModule],
        providers: [
            DynamicDialogRef,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    describe('edit form', () => {
        beforeEach(() => {
            const dynamicData = {
                languages: mockLanguagesISO.languages,
                countries: mockLanguagesISO.countries,
                locale: mockLocales[0],
                localeList: mockLocales
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: dynamicData
                        }
                    }
                ]
            });
            ref = spectator.inject(DynamicDialogRef);
            jest.spyOn(ref, 'close');
        });

        it('should load data correctly', () => {
            expect(spectator.component.data).toEqual({
                languages: mockLanguagesISO.languages,
                countries: mockLanguagesISO.countries,
                locale: mockLocales[0],
                localeList: mockLocales
            });
        });

        it('should load the edit form correctly', () => {
            expect((spectator.query(byTestId('langEdit')) as HTMLInputElement).value).toBe(
                'English'
            );
            expect((spectator.query(byTestId('isoCodeEdit')) as HTMLInputElement).value).toBe(
                'en-US'
            );
            expect((spectator.query(byTestId('id')) as HTMLInputElement).value).toBe('1');
        });

        it('should save the form correctly', () => {
            spectator.typeInElement('Spanish', spectator.query(byTestId('langEdit')));
            spectator.click(spectator.query(byTestId('submit-button')));

            expect(ref.close).toHaveBeenCalledWith({
                language: 'Spanish',
                languageCode: 'en',
                country: 'United States',
                countryCode: 'US',
                id: 1
            });
        });
    });

    describe('add form', () => {
        beforeEach(() => {
            const dynamicData = {
                languages: mockLanguagesISO.languages,
                countries: mockLanguagesISO.countries,
                locale: null,
                localeList: mockLocales
            };

            spectator = createComponent({
                providers: [
                    {
                        provide: DynamicDialogConfig,
                        useValue: {
                            data: dynamicData
                        }
                    }
                ]
            });
            ref = spectator.inject(DynamicDialogRef);
            jest.spyOn(ref, 'close');
        });

        it('should load the add Standard Locale form correctly', () => {
            spectator.detectChanges();
            const localeTypeDropdown = spectator.debugElement.query(
                By.css('[data-testId="localeType"]')
            );
            const languageDropdown = spectator.debugElement.query(
                By.css('[data-testId="languageDropdown"]')
            );

            const countryDropdown = spectator.debugElement.query(
                By.css('[data-testId="countryDropdown"]')
            );

            expect(localeTypeDropdown.componentInstance.options).toEqual(
                spectator.component.localeType
            );
            expect(languageDropdown.componentInstance.options).toEqual(mockLanguagesISO.languages);
            expect(countryDropdown.componentInstance.options).toEqual(mockLanguagesISO.countries);
            expect((spectator.query(byTestId('isoCode')) as HTMLInputElement).value).toBe('');
        });

        it('should save the form correctly', () => {
            spectator.component.form
                .get('languageDropdown')
                ?.setValue(mockLanguagesISO.languages[1]);

            spectator.component.form
                .get('countryDropdown')
                ?.setValue(mockLanguagesISO.countries[1]);

            spectator.detectChanges();

            spectator.click(spectator.query(byTestId('submit-button')));

            expect(ref.close).toHaveBeenCalledWith({
                language: 'Spanish',
                languageCode: 'es',
                country: 'Canada',
                countryCode: 'CA',
                id: undefined
            });
        });

        it('should load the add custom Locale form correctly', () => {
            const localeTypeDropdown = spectator.debugElement.query(
                By.css('[data-testId="localeType"]')
            );

            localeTypeDropdown.componentInstance.value = 2;
            spectator.triggerEventHandler(localeTypeSelect, 'onChange', {
                event: new Event('change'),
                value: 2
            });

            expect(spectator.query(byTestId('language'))).not.toBeNull();
            expect(spectator.query(byTestId('languageCode'))).not.toBeNull();
        });

        it('should save the custom Locale form correctly', () => {
            const localeTypeDropdown = spectator.debugElement.query(
                By.css('[data-testId="localeType"]')
            );

            localeTypeDropdown.componentInstance.value = 2;
            spectator.triggerEventHandler(localeTypeSelect, 'onChange', {
                event: new Event('change'),
                value: 2
            });

            spectator.typeInElement('Spanish', spectator.query(byTestId('language')));
            spectator.typeInElement('es', spectator.query(byTestId('languageCode')));

            spectator.click(spectator.query(byTestId('submit-button')));

            expect(ref.close).toHaveBeenCalledWith({
                language: 'Spanish',
                languageCode: 'es',
                country: undefined,
                countryCode: undefined,
                id: undefined
            });
        });

        it('should show the error when the locale already exists', () => {
            const localeTypeDropdown = spectator.debugElement.query(
                By.css('[data-testId="localeType"]')
            );

            localeTypeDropdown.componentInstance.value = 2;
            spectator.triggerEventHandler(localeTypeSelect, 'onChange', {
                event: new Event('change'),
                value: 2
            });

            spectator.typeInElement('English', spectator.query(byTestId('language')));
            spectator.typeInElement('en-US', spectator.query(byTestId('languageCode')));

            spectator.click(spectator.query(byTestId('submit-button')));

            expect(spectator.query(byTestId('error-message'))).not.toBeNull();
        });
    });
});
