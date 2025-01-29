import { SpyObject, mockProvider } from '@ngneat/spectator/jest';
import { Observable, of, throwError } from 'rxjs';

import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { delay } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';
import { mockLocales } from '@dotcms/utils-testing';

import { LanguageFieldStore } from './language-field.store';

describe('LanguageFieldStore', () => {
    let store: InstanceType<typeof LanguageFieldStore>;
    let languageService: SpyObject<DotLanguagesService>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                LanguageFieldStore,
                mockProvider(DotLanguagesService, {
                    get: jest.fn().mockReturnValue(of(mockLocales))
                })
            ]
        });

        store = TestBed.inject(LanguageFieldStore);
        languageService = TestBed.inject(DotLanguagesService) as SpyObject<DotLanguagesService>;
    });

    it('should be created', () => {
        expect(store).toBeTruthy();
    });

    describe('Initial State', () => {
        it('should have correct initial state', () => {
            expect(store.languages()).toEqual([]);
            expect(store.selectedLanguageId()).toBeNull();
            expect(store.error()).toBeNull();
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.hasLanguages()).toBeFalsy();
            expect(store.isLoading()).toBeFalsy();
            expect(store.selectedLanguage()).toBeUndefined();
        });
    });

    describe('State Management', () => {
        it('should load languages successfully', fakeAsync(() => {
            store.loadLanguages();
            tick();

            expect(store.status()).toBe(ComponentStatus.LOADED);
            expect(store.languages()).toEqual(mockLocales);
            expect(store.error()).toBeNull();
            expect(store.hasLanguages()).toBeTruthy();
        }));

        it('should handle error when loading languages', fakeAsync(() => {
            const errorMessage = 'dot.file.relationship.dialog.search.language.failed';
            languageService.get.mockReturnValue(throwError(() => new Error(errorMessage)));

            store.loadLanguages();
            tick();

            expect(store.status()).toBe(ComponentStatus.ERROR);
            expect(store.error()).toBe(errorMessage);
            expect(store.languages()).toEqual([]);
            expect(store.hasLanguages()).toBeFalsy();
        }));

        it('should set loading state while fetching languages', fakeAsync(() => {
            const mockObservable = of(mockLocales).pipe(delay(100)) as Observable<DotLanguage[]>;
            languageService.get.mockReturnValue(mockObservable);

            store.loadLanguages();
            expect(store.isLoading()).toBeTruthy();
            expect(store.status()).toBe(ComponentStatus.LOADING);

            tick(100);
            expect(store.isLoading()).toBeFalsy();
            expect(store.status()).toBe(ComponentStatus.LOADED);
        }));
    });

    describe('Language Selection', () => {
        beforeEach(fakeAsync(() => {
            store.loadLanguages();
            tick();
        }));

        it('should select a language by id', () => {
            store.setSelectedLanguage(1);
            expect(store.selectedLanguageId()).toBe(1);
            expect(store.selectedLanguage()).toEqual(mockLocales[0]);
        });

        it('should handle selecting non-existent language id', () => {
            store.setSelectedLanguage(999);
            expect(store.selectedLanguageId()).toBe(999);
            expect(store.selectedLanguage()).toBeUndefined();
        });

        it('should handle clearing language selection', () => {
            store.setSelectedLanguage(1);
            store.setSelectedLanguage(null);
            expect(store.selectedLanguageId()).toBeNull();
            expect(store.selectedLanguage()).toBeUndefined();
        });
    });

    describe('Error Handling', () => {
        it('should set error message', () => {
            const errorMessage = 'Test error';
            store.setError(errorMessage);
            expect(store.error()).toBe(errorMessage);
            expect(store.status()).toBe(ComponentStatus.ERROR);
        });

        it('should clear error message', () => {
            store.setError('Test error');
            store.setError(null);
            expect(store.error()).toBeNull();
        });
    });

    describe('Reset Functionality', () => {
        it('should reset store to initial state', fakeAsync(() => {
            // Setup some state
            store.loadLanguages();
            tick();
            store.setSelectedLanguage(1);

            // Reset
            store.reset();

            // Verify initial state
            expect(store.languages()).toEqual([]);
            expect(store.selectedLanguageId()).toBeNull();
            expect(store.error()).toBeNull();
            expect(store.status()).toBe(ComponentStatus.INIT);
            expect(store.hasLanguages()).toBeFalsy();
            expect(store.isLoading()).toBeFalsy();
            expect(store.selectedLanguage()).toBeUndefined();
        }));
    });
});
