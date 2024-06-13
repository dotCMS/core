import { getPageRequestParams } from './common-utils';

describe('Common Utils', () => {
    it('should return the correct Request Params', () => {
        const pageRequestParams = getPageRequestParams({
            path: 'test',
            params: {
                'com.dotmarketing.persona.id': '123',
                language_id: '1',
                mode: 'LIVE',
                variantName: 'default'
            }
        });

        expect(pageRequestParams).toEqual({
            path: 'test',
            mode: 'LIVE',
            language_id: '1',
            variantName: 'default',
            'com.dotmarketing.persona.id': '123'
        });
    });

    it('should return the correct Request Params with empty params', () => {
        const pageRequestParams = getPageRequestParams({
            path: 'test',
            params: {
                'com.dotmarketing.persona.id': '',
                language_id: '',
                mode: '',
                variantName: ''
            }
        });

        expect(pageRequestParams).toEqual({
            path: 'test',
            mode: '',
            language_id: '',
            variantName: '',
            'com.dotmarketing.persona.id': ''
        });
    });
});
