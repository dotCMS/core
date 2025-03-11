import { normalizeUrl } from './utils';

describe('Utils', () => {
    describe('normalizeUrl', () => {
        it('should remove consecutive slashes in the path portion', () => {
            expect(normalizeUrl('http://dotcms-instance.com/api/v1/page/json//')).toEqual(
                'http://dotcms-instance.com/api/v1/page/json/'
            );
            expect(normalizeUrl('http://dotcms-instance.com/api/v1///page/json/')).toEqual(
                'http://dotcms-instance.com/api/v1/page/json/'
            );
        });

        it('should preserve the protocol slashes', () => {
            expect(normalizeUrl('http://dotcms-instance.com')).toEqual(
                'http://dotcms-instance.com/'
            );
            expect(normalizeUrl('https://demo.dotcms.com')).toEqual('https://demo.dotcms.com/');
        });

        it('should maintain a trailing slash if present in the original URL', () => {
            expect(normalizeUrl('http://dotcms-instance.com/api/v1/page/json/')).toEqual(
                'http://dotcms-instance.com/api/v1/page/json/'
            );
            expect(normalizeUrl('http://dotcms-instance.com/api/v1/page/json')).toEqual(
                'http://dotcms-instance.com/api/v1/page/json'
            );
        });

        it('should not modify URLs without consecutive slashes', () => {
            expect(normalizeUrl('http://dotcms-instance.com/api/v1/page/json/index')).toEqual(
                'http://dotcms-instance.com/api/v1/page/json/index'
            );
        });

        it('should handle URLs with query parameters', () => {
            expect(normalizeUrl('http://dotcms-instance.com/api/v1//page?param=value')).toEqual(
                'http://dotcms-instance.com/api/v1/page?param=value'
            );
            expect(normalizeUrl('http://dotcms-instance.com/api/v1//page/?param=value')).toEqual(
                'http://dotcms-instance.com/api/v1/page/?param=value'
            );
        });

        it('should handle URLs with hash fragments', () => {
            expect(normalizeUrl('http://dotcms-instance.com/api/v1//page#section')).toEqual(
                'http://dotcms-instance.com/api/v1/page#section'
            );
            expect(normalizeUrl('http://dotcms-instance.com/api/v1//page/#section')).toEqual(
                'http://dotcms-instance.com/api/v1/page/#section'
            );
        });

        it('should return empty string for empty input', () => {
            expect(normalizeUrl('')).toEqual('');
        });

        it('should handle URLs with authentication information', () => {
            expect(normalizeUrl('http://user:pass@dotcms-instance.com/api//v1/page')).toEqual(
                'http://user:pass@dotcms-instance.com/api/v1/page'
            );
        });
    });
});
