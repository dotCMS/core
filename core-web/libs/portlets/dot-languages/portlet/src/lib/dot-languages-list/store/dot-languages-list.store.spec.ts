import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';

import { DotLanguage } from '@dotcms/dotcms-models';

import { DotLanguagesListStore } from './dot-languages-list.store';

describe('DotLanguagesListStore', () => {
    let spectator: SpectatorService<DotLanguagesListStore>;

    const storeService = createServiceFactory({ service: DotLanguagesListStore, providers: [] });

    beforeEach(() => (spectator = storeService()));

    it('should create an instance', () => {
        expect(spectator.service).toBeTruthy();
    });

    it('should set languages correctly', () => {
        const languages: DotLanguage[] = [
            {
                id: 1,
                languageCode: 'en',
                countryCode: 'US',
                language: 'English',
                country: 'United States',
                isoCode: 'en-US',
                defaultLanguage: true
            },
            {
                id: 2,
                languageCode: 'es',
                countryCode: 'ES',
                language: 'Spanish',
                country: 'Spain',
                isoCode: 'es-ES',
                defaultLanguage: false
            }
        ];

        spectator.service.setLanguages(languages);

        spectator.service.vm$.subscribe((viewModel) => {
            expect(viewModel.languages.length).toBe(2);
            expect(viewModel.languages[0].locale).toBe('English (en-US)');
            expect(viewModel.languages[0].language).toBe('English - en');
            expect(viewModel.languages[0].country).toBe('United States - US');
            expect(viewModel.languages[0].defaultLanguage).toBe(true);
            expect(viewModel.languages[1].locale).toBe('Spanish (es-ES)');
            expect(viewModel.languages[1].language).toBe('Spanish - es');
            expect(viewModel.languages[1].country).toBe('Spain - ES');
            expect(viewModel.languages[1].defaultLanguage).toBe(false);
        });
    });

    it('should handle empty languages array', () => {
        spectator.service.setLanguages([]);

        spectator.service.vm$.subscribe((viewModel) => {
            expect(viewModel.languages.length).toBe(0);
        });
    });
});
