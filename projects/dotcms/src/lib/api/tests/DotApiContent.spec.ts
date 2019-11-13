import { DotCMSError } from './../../models';
import { DotCMSHttpClient } from './../../utils/DotCMSHttpClient';
import { DotApiContent } from '../DotApiContent';

describe('DotApiContent', () => {
    let httpClient: DotCMSHttpClient;
    let dotApiContent;

    const contentParams = {
        stName: 'webContent',
        body: 'Test'
    };

    beforeEach(() => {
        httpClient = new DotCMSHttpClient({
            token: '',
            host: 'http://localhost'
        });
        dotApiContent = new DotApiContent(httpClient);
    });

    describe('Requests', () => {
        const responseExpected = {
            status: 200,
            statusText: ''
        };

        beforeEach(() => {
            spyOn(httpClient, 'request').and.returnValue(
                new Promise((resolve) => resolve(responseExpected))
            );
        });

        it('should save a content type', () => {
            const requestParams = {
                url: '/api/content/save/1',
                method: 'POST',
                body: JSON.stringify(contentParams)
            };
            dotApiContent.save(contentParams).then((data) => {
                expect(data).toEqual(responseExpected);
            });
            expect(httpClient.request).toHaveBeenCalledWith(requestParams);
        });

        it('should publish a content type', () => {
            const requestParams = {
                url: '/api/content/publish/1',
                method: 'POST',
                body: JSON.stringify(contentParams)
            };
            dotApiContent.publish(contentParams).then((data) => {
                expect(data).toEqual(responseExpected);
            });
            expect(httpClient.request).toHaveBeenCalledWith(requestParams);
        });
    });

    describe('Errors', () => {
        beforeEach(() => {
            spyOn(httpClient, 'request').and.returnValue(
                new Promise((resolve) =>
                    resolve({
                        status: 500,
                        text: () => 'Error'
                    })
                )
            );
        });

        it('should throw error Save()', () => {
            dotApiContent.save('123').catch((err: DotCMSError) => {
                expect(err).toEqual({
                    statusCode: 500,
                    message: 'Error'
                });
            });
        });

        it('should throw error Publish()', () => {
            dotApiContent.publish('123').catch((err: DotCMSError) => {
                expect(err).toEqual({
                    statusCode: 500,
                    message: 'Error'
                });
            });
        });
    });
});
