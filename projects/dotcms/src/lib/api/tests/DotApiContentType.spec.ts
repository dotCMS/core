import { DotCMSError } from './../../models';
import { DotCMSHttpClient } from './../../utils/DotCMSHttpClient';
import { DotApiContentType } from '../DotApiContentType';

describe('DotApiContentType', () => {
    let httpClient: DotCMSHttpClient;
    let dotApiContentType;

    const expectedMsg = {
        entity: {
            name: 'content',
            fields: [
                {
                    name: 'field1',
                    value: 'value1'
                },
                {
                    name: 'field2',
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

    it('should request a content type', () => {
        spyOn(httpClient, 'request').and.returnValue(
            new Promise((resolve) =>
                resolve({
                    status: 200,
                    json: () => expectedMsg
                })
            )
        );
        dotApiContentType.get('123').then((data) => {
            expect(data).toEqual(expectedMsg.entity);
        });
        expect(httpClient.request).toHaveBeenCalledWith({ url: '/api/v1/contenttype/id/123' });
    });

    it('should request a content type\'s fields', () => {
        spyOn(httpClient, 'request').and.returnValue(
            new Promise((resolve) =>
                resolve({
                    status: 200,
                    json: () => expectedMsg
                })
            )
        );
        dotApiContentType.getFields('123').then((data) => {
            expect(data).toEqual(expectedMsg.entity.fields);
        });
        expect(httpClient.request).toHaveBeenCalledWith({ url: '/api/v1/contenttype/id/123' });
    });

    it('should throw error', () => {
        spyOn(httpClient, 'request').and.returnValue(
            new Promise((resolve) =>
                resolve({
                    status: 500,
                    text: () => 'Error'
                })
            )
        );

        dotApiContentType.get('123').catch((err: DotCMSError) => {
            expect(err).toEqual({
                status: 500,
                message: 'Error'
            });
        });
    });

});
