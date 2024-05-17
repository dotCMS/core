import { SpectatorService, createServiceFactory } from '@ngneat/spectator/jest';

import { DotLanguage } from '@dotcms/dotcms-models';

import { DotLocalesListStore } from './dot-locales-list.store';

describe('DotLocalesListStore', () => {
    let spectator: SpectatorService<DotLocalesListStore>;

    const storeService = createServiceFactory({ service: DotLocalesListStore, providers: [] });

    beforeEach(() => (spectator = storeService()));

    it('should set locales correctly', () => {
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

        spectator.service.setLocales(languages);

        spectator.service.vm$.subscribe((viewModel) => {
            expect(viewModel.locales.length).toBe(2);
            expect(viewModel.locales[0].locale).toBe('English (en-US)');
            expect(viewModel.locales[0].language).toBe('English - en');
            expect(viewModel.locales[0].country).toBe('United States - US');
            expect(viewModel.locales[0].defaultLanguage).toBe(true);
            expect(viewModel.locales[1].locale).toBe('Spanish (es-ES)');
            expect(viewModel.locales[1].language).toBe('Spanish - es');
            expect(viewModel.locales[1].country).toBe('Spain - ES');
            expect(viewModel.locales[1].defaultLanguage).toBe(false);
        });
    });

    it('should handle empty locales array', () => {
        spectator.service.setLocales([]);

        spectator.service.vm$.subscribe((viewModel) => {
            expect(viewModel.locales.length).toBe(0);
        });
    });
});
