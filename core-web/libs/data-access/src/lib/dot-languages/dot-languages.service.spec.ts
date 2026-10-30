import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotLanguage } from '@dotcms/dotcms-models';

import {
    DotLanguagesService,
    LANGUAGE_API_URL,
    LANGUAGE_API_URL_WITH_VARS
} from './dot-languages.service';

describe('DotLanguagesService', () => {
    let spectator: SpectatorHttp<DotLanguagesService>;
    const createHttp = createHttpFactory(DotLanguagesService);

    beforeEach(() => (spectator = createHttp()));

    it('should get Languages', () => {
        spectator.service.get().subscribe();
        spectator.expectOne(LANGUAGE_API_URL_WITH_VARS, HttpMethod.GET);
    });

    it('should get Languages by content indode', () => {
        const contentInode = '2';
        spectator.service.get(contentInode).subscribe();
        spectator.expectOne(
            `${LANGUAGE_API_URL_WITH_VARS}&contentInode=${contentInode}`,
            HttpMethod.GET
        );
    });

    it('should get Languages by pageId', () => {
        const pageIdentifier = '0000-1111-2222-3333';
        spectator.service.getLanguagesUsedPage(pageIdentifier).subscribe();
        spectator.expectOne(`/api/v1/page/${pageIdentifier}/languages`, HttpMethod.GET);
    });

    it('should add a new language', () => {
        const language = {
            languageCode: 'fr',
            countryCode: 'FR',
            language: 'French',
            country: 'France'
        };
        spectator.service.add(language).subscribe();
        const req = spectator.expectOne(LANGUAGE_API_URL, HttpMethod.POST);
        expect(req.request.body).toEqual(language);
    });

    it('should get a language by id', () => {
        const id = 1;
        spectator.service.getById(id).subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/id/${id}`, HttpMethod.GET);
    });

    it('should update a language', () => {
        const language = {
            id: '1',
            languageCode: 'fr'
        } as unknown as DotLanguage;
        spectator.service.update(language).subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/${language.id}`, HttpMethod.PUT);
    });

    it('should delete a language by id', () => {
        const id = 1;
        spectator.service.delete(id).subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/${id}`, HttpMethod.DELETE);
    });

    it('should make a language the default language', () => {
        const id = 1;
        spectator.service.makeDefault(id).subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/${id}/_makedefault`, HttpMethod.PUT);
    });

    it('should get languages and countries in ISO format', () => {
        spectator.service.getISO().subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/iso`, HttpMethod.GET);
    });

    it('should get language by ISO code', () => {
        spectator.service.getByISOCode('test').subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/test`, HttpMethod.GET);
    });

    it('should get language variables', () => {
        spectator.service.getLanguageVariables().subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/variables`, HttpMethod.GET);
    });

    it('should get the default language', () => {
        spectator.service.getDefault().subscribe();
        spectator.expectOne(`${LANGUAGE_API_URL}/_getdefault`, HttpMethod.GET);
    });
});
