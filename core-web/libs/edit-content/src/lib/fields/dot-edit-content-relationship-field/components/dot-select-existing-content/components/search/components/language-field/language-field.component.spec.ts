import { createComponentFactory, Spectator } from '@ngneat/spectator';
import { mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, mockLocales } from '@dotcms/utils-testing';

import { LanguageFieldComponent } from './language-field.component';
import { LanguageFieldStore } from './language-field.store';

describe('LanguageFieldComponent', () => {
    let spectator: Spectator<LanguageFieldComponent>;
    let component: LanguageFieldComponent;
    let store: InstanceType<typeof LanguageFieldStore>;

    const messageServiceMock = new MockDotMessageService({
        'dot.file.relationship.dialog.search.language.failed': 'Failed to load languages'
    });

    const createComponent = createComponentFactory({
        component: LanguageFieldComponent,
        componentProviders: [LanguageFieldStore],
        providers: [
            mockProvider(DotLanguagesService, {
                get: jest.fn().mockReturnValue(of(mockLocales))
            }),
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });
        component = spectator.component;
        store = spectator.inject(LanguageFieldStore, true);
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('Initialization', () => {
        it('should load languages on init', () => {
            const spyLoadLanguages = jest.spyOn(store, 'loadLanguages');
            spectator.detectChanges();
            expect(spyLoadLanguages).toHaveBeenCalled();
        });

        it('should initialize with null value', () => {
            expect(component.languageControl.value).toBeNull();
        });
    });

    describe('ControlValueAccessor Implementation', () => {
        it('should write value and update control', () => {
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();

            const languageId = 1;
            component.writeValue(languageId);

            expect(component.languageControl.value).toEqual(mockLocales[0]);
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(languageId);
        });

        it('should handle null value in writeValue', () => {
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();

            component.writeValue(null);

            expect(component.languageControl.value).toBeNull();
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(null);
        });

        it('should handle non-existent language id in writeValue', () => {
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();
            component.writeValue(999);

            expect(component.languageControl.value).toBeNull();
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(null);
        });

        it('should register onChange callback', () => {
            spectator.detectChanges();
            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);

            component.handleLanguageChange();
            component.languageControl.setValue(mockLocales[0]);
            component.handleLanguageChange();

            expect(onChangeSpy).toHaveBeenCalledWith(mockLocales[0].id);
        });

        it('should register onTouched callback', () => {
            spectator.detectChanges();
            const onTouchedSpy = jest.fn();
            component.registerOnTouched(onTouchedSpy);

            component.handleLanguageChange();

            expect(onTouchedSpy).toHaveBeenCalled();
        });
    });

    describe('Language Selection', () => {
        it('should emit selected language on change', () => {
            const languageChangeSpy = jest.spyOn(component.languageChange, 'emit');
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();

            component.languageControl.setValue(mockLocales[0]);
            component.handleLanguageChange();

            expect(languageChangeSpy).toHaveBeenCalledWith(mockLocales[0]);
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(mockLocales[0].id);
        });

        it('should handle null selection', () => {
            const languageChangeSpy = jest.spyOn(component.languageChange, 'emit');
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();

            component.languageControl.setValue(null);
            component.handleLanguageChange();

            expect(languageChangeSpy).not.toHaveBeenCalled();
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(null);
        });
    });

    describe('Disabled State', () => {
        it('should disable the control', () => {
            spectator.detectChanges();
            component.setDisabledState(true);
            expect(component.languageControl.disabled).toBe(true);
        });

        it('should enable the control', () => {
            spectator.detectChanges();
            component.setDisabledState(true);
            component.setDisabledState(false);
            expect(component.languageControl.disabled).toBe(false);
        });

        it('should not emit changes when disabled', () => {
            spectator.detectChanges();

            const onChangeSpy = jest.fn();
            component.registerOnChange(onChangeSpy);
            component.setDisabledState(true);

            component.languageControl.setValue(mockLocales[0]);
            component.handleLanguageChange();

            expect(onChangeSpy).not.toHaveBeenCalled();
        });
    });

    describe('Edge Cases', () => {
        it('should handle rapid language changes', () => {
            const languageChangeSpy = jest.spyOn(component.languageChange, 'emit');
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');

            spectator.detectChanges();

            mockLocales.forEach((language) => {
                component.languageControl.setValue(language);
                component.handleLanguageChange();
            });

            expect(languageChangeSpy).toHaveBeenCalledTimes(mockLocales.length);
            expect(spySetSelectedLanguage).toHaveBeenCalledTimes(mockLocales.length);
        });

        it('should handle undefined language value', () => {
            const spySetSelectedLanguage = jest.spyOn(store, 'setSelectedLanguage');
            spectator.detectChanges();
            component.writeValue(null);

            expect(component.languageControl.value).toBeNull();
            expect(spySetSelectedLanguage).toHaveBeenCalledWith(null);
        });

        it('should maintain selected value after disable/enable cycle', () => {
            spectator.detectChanges();

            component.writeValue(mockLocales[0].id);
            component.setDisabledState(true);
            component.setDisabledState(false);

            expect(component.languageControl.value).toEqual(mockLocales[0]);
        });
    });
});
