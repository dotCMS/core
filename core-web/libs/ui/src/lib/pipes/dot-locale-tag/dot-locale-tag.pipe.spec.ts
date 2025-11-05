import { SpectatorPipe, createPipeFactory } from '@ngneat/spectator/jest';

import { DotLanguage } from '@dotcms/dotcms-models';

import { DotLocaleTagPipe } from './dot-locale-tag.pipe';

describe('DotLocaleTagPipe', () => {
    let spectator: SpectatorPipe<DotLocaleTagPipe>;
    let pipe: DotLocaleTagPipe;

    const mockLanguagesMap = new Map<number, DotLanguage>([
        [
            1,
            {
                id: 1,
                language: 'English',
                languageCode: 'en',
                isoCode: 'en',
                countryCode: 'US',
                country: 'United States'
            }
        ],
        [
            2,
            {
                id: 2,
                language: 'Spanish',
                languageCode: '',
                isoCode: 'es',
                countryCode: 'ES',
                country: 'Spain'
            }
        ],
        [
            3,
            {
                id: 3,
                language: 'French',
                languageCode: '',
                countryCode: 'FR',
                country: 'France'
            }
        ]
    ]);

    const createPipe = createPipeFactory({
        pipe: DotLocaleTagPipe
    });

    beforeEach(() => {
        spectator = createPipe();
        pipe = new DotLocaleTagPipe();
    });

    it('should create', () => {
        expect(spectator.element).toBeTruthy();
    });

    it('should return dash when languageId is null', () => {
        expect(pipe.transform(null, mockLanguagesMap)).toBe('-');
    });

    it('should return dash when languageId is undefined', () => {
        expect(pipe.transform(undefined, mockLanguagesMap)).toBe('-');
    });

    it('should return dash when languageId is 0', () => {
        expect(pipe.transform(0, mockLanguagesMap)).toBe('-');
    });

    it('should return dash when languagesMap is null', () => {
        expect(pipe.transform(1, null)).toBe('-');
    });

    it('should return dash when languagesMap is undefined', () => {
        expect(pipe.transform(1, undefined)).toBe('-');
    });

    it('should return dash when language is not found in map', () => {
        expect(pipe.transform(999, mockLanguagesMap)).toBe('-');
    });

    it('should return isoCode for English language', () => {
        expect(pipe.transform(1, mockLanguagesMap)).toBe('en');
    });

    it('should return isoCode for Spanish language', () => {
        expect(pipe.transform(2, mockLanguagesMap)).toBe('es');
    });

    it('should return dash when language exists but has no isoCode', () => {
        expect(pipe.transform(3, mockLanguagesMap)).toBe('-');
    });
});
