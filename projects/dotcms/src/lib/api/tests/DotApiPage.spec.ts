import { DotCMSHttpClient } from '../../utils/DotCMSHttpClient';
import { DotApiLanguage } from '../DotApiLanguage';
import { DotApiPage } from '../DotApiPage';

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

        spyOn(appLanguage, 'getId').and.returnValue(new Promise(resolve => resolve('1')));
        const data = {
            entity: 'test'
        };

        spyOn(httpClient, 'request').and.returnValue(
            new Promise(resolve =>
                resolve({
                    status: 200,
                    json: () => data
                })
            )
        );
    });

    it('should make request with the correct params when come a language code', () => {
        dotApiPage.get({ url: '/test', language: 'en' }).then(() => {
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/json/test',
                language: '1'
            });
        });
    });

    it('should make request with the correct params when come the language id', () => {
        dotApiPage.get({ url: '/test', language: '2' }).then(() => {
            expect(httpClient.request).toHaveBeenCalledWith({
                url: '/api/v1/page/json/test',
                language: '2'
            });
        });
    });
});
