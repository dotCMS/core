import { SpectatorPipe, createPipeFactory } from '@ngneat/spectator/jest';

import { DotLanguage } from '@dotcms/dotcms-models';

import { LanguagePipe } from './language.pipe';

describe('LanguagePipe', () => {
    let spectator: SpectatorPipe<LanguagePipe>;
    const createPipe = createPipeFactory({
        pipe: LanguagePipe,
        template: '{{ prop | language }}'
    });

    it('should create', () => {
        spectator = createPipe();
        expect(spectator.element).toBeTruthy();
    });

    it('should return empty string when language is null', () => {
        spectator = createPipe({
            hostProps: {
                prop: null
            }
        });
        expect(spectator.element.textContent).toBe('');
    });

    it('should return empty string when language is undefined', () => {
        spectator = createPipe({
            hostProps: {
                prop: undefined
            }
        });
        expect(spectator.element.textContent).toBe('');
    });

    it('should format language with languageCode', () => {
        const language: DotLanguage = {
            language: 'English',
            languageCode: 'en',
            id: 1,
            countryCode: 'US'
        };
        spectator = createPipe({
            hostProps: {
                prop: language
            }
        });
        expect(spectator.element.textContent).toBe('English (en)');
    });

    it('should format language with isoCode when languageCode is empty', () => {
        const language: DotLanguage = {
            language: 'Spanish',
            languageCode: '',
            isoCode: 'es',
            id: 2,
            countryCode: 'ES'
        };
        spectator = createPipe({
            hostProps: {
                prop: language
            }
        });
        expect(spectator.element.textContent).toBe('Spanish (es)');
    });

    it('should format language without code when neither languageCode nor isoCode has value', () => {
        const language: DotLanguage = {
            language: 'French',
            languageCode: '',
            id: 3,
            countryCode: 'FR'
        };
        spectator = createPipe({
            hostProps: {
                prop: language
            }
        });
        expect(spectator.element.textContent).toBe('French');
    });
});
