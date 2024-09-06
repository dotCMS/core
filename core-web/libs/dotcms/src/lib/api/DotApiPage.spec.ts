import { DotApiLanguage } from './DotApiLanguage';
import { DotApiPage } from './DotApiPage';

import { DotCMSPageFormat } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

describe('DotApiPage', () => {
    let httpClient: DotCMSHttpClient;
    let appLanguage: DotApiLanguage;
    let dotApiPage: DotApiPage;

    beforeEach(() => {
        httpClient = new DotCMSHttpClient({
            token: '',
            host: 'http://localhost'
        });
        appLanguage = new DotApiLanguage(null);
        dotApiPage = new DotApiPage(httpClient, appLanguage);

        spyOn(appLanguage, 'getId').and.returnValue(new Promise((resolve) => resolve('1')));
        const data = {
            entity: 'test'
        };

        spyOn(httpClient, 'request').and.returnValue(
            new Promise((resolve) =>
                resolve({
                    status: 200,
                    json: () => data
                })
            )
        );
    });

    it('should make request with the correct params when come a language code', async () => {
        try {
            await dotApiPage.get({ url: '/test', language: 'en' });
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/json/test',
                language: '1'
            });
        } catch (error) {
            console.log(error);
        }
    });

    it('should make request with the correct params when come the language id', async () => {
        try {
            dotApiPage.get({ url: '/test', language: '2' });
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/json/test',
                language: '2'
            });
        } catch (error) {
            console.log(error);
        }
    });

    it('should make request with the correct format', async () => {
        try {
            await dotApiPage.get({ url: '/test' }, DotCMSPageFormat.JSON);
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/json/test'
            });
        } catch (error) {
            console.log(error);
        }

        try {
            dotApiPage.get({ url: '/test' }, DotCMSPageFormat.Render);
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/render/test'
            });
        } catch (error) {
            console.log(error);
        }
    });
});
