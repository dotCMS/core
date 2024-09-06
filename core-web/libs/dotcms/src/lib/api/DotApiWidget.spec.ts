import { DotApiWidget } from './DotApiWidget';

import { DotCMSError } from '../models';
import { DotCMSHttpClient } from '../utils/DotCMSHttpClient';

describe('DotApiWidget', () => {
    let httpClient: DotCMSHttpClient;
    let dotApiWidget;

    beforeEach(() => {
        httpClient = new DotCMSHttpClient({
            token: '',
            host: 'http://localhost'
        });
        dotApiWidget = new DotApiWidget(httpClient);
    });

    it('should request a widget html', () => {
        spyOn(httpClient, 'request').and.returnValue(
            new Promise((resolve) =>
                resolve({
                    status: 200,
                    text: () => '<h1>Hello Widget</h1>'
                })
            )
        );

        dotApiWidget.getHtml('123').then((html) => {
            expect(html).toEqual('<h1>Hello Widget</h1>');
        });
        expect(httpClient.request).toHaveBeenCalledWith({ url: '/api/widget/id/123' });
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

        dotApiWidget.getHtml('123').catch((err: DotCMSError) => {
            expect(err).toEqual({
                statusCode: 500,
                message: 'Error'
            });
        });
    });
});
