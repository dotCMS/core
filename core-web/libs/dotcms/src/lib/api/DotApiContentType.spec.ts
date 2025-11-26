import { DotApiContentType } from './DotApiContentType';

import { DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

describe('DotApiContentType', () => {
    let httpClient: DotCMSHttpClient;
    let dotApiContentType;

    const expectedMsg = {
        entity: {
            name: 'content',
            layout: [
                {
                    name: 'row1',
                    value: 'value1'
                },
                {
                    name: 'row1',
                    value: 'value2'
                }
            ]
        }
    };

    beforeEach(() => {
        httpClient = new DotCMSHttpClient({
            token: '',
            host: 'http://localhost'
        });
        dotApiContentType = new DotApiContentType(httpClient);
    });

    describe('Requests', () => {
        beforeEach(() => {
            spyOn(httpClient, 'request').and.returnValue(
                new Promise((resolve) =>
                    resolve({
                        status: 200,
                        json: () => expectedMsg
                    })
                )
            );
        });

        it('should request a content type', () => {
            dotApiContentType.get('123').then((data) => {
                expect(data).toEqual(expectedMsg.entity);
            });
            expect(httpClient.request).toHaveBeenCalledWith({ url: '/api/v1/contenttype/id/123' });
        });

        it("should request a content type's fields", () => {
            dotApiContentType.getLayout('123').then((data) => {
                expect(data).toEqual(expectedMsg.entity.layout);
            });
            expect(httpClient.request).toHaveBeenCalledWith({ url: '/api/v1/contenttype/id/123' });
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

        it('should throw error Get()', () => {
            dotApiContentType.get('123').catch((err: DotCMSError) => {
                expect(err).toEqual({
                    statusCode: 500,
                    message: 'Error'
                });
            });
        });

        it('should throw error getLayout()', () => {
            dotApiContentType.getLayout('123').catch((err: DotCMSError) => {
                expect(err).toEqual({
                    statusCode: 500,
                    message: 'Error'
                });
            });
        });
    });
});
