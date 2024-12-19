import { getPageRequestParams, isPreviewMode } from './common-utils';

describe('Common Utils', () => {
    it('should return the correct Request Params', () => {
        const pageRequestParams = getPageRequestParams({
            path: 'test',
            params: {
                personaId: '123',
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
            personaId: '123'
        });
    });

    it('should return the correct Request Params with empty params', () => {
        const pageRequestParams = getPageRequestParams({
            path: 'test',
            params: {
                personaId: '',
                language_id: '',
                mode: '',
                variantName: ''
            }
        });

        expect(pageRequestParams).toEqual({ path: 'test' });
    });

    describe('Is Preview Mode', () => {
        it('should return true when editorMode is preview', () => {
            jest.spyOn(window, 'location', 'get').mockReturnValueOnce({
                search: '?editorMode=preview'
            } as Location);

            expect(isPreviewMode()).toBe(true);
        });

        it('should return false when editorMode is not preview', () => {
            jest.spyOn(window, 'location', 'get').mockReturnValueOnce({
                search: ''
            } as Location);

            expect(isPreviewMode()).toBe(false);
        });
    });
});
