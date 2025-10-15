import { DotCMSBaseTypesContentTypes, DotPageToolUrlParams } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { getImageAssetUrl, ellipsizeText, getRunnableLink, hasValidValue } from './dot-utils';

describe('Dot Utils', () => {
    describe('getImageAssetUrl', () => {
        it('should return fileAssetVersion when baseType is FILEASSET and fileAssetVersion is defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                fileAssetVersion: 'fileAssetVersion',
                fileAsset: 'fileAsset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('fileAssetVersion');
        });

        it('should return fileAsset when baseType is FILEASSET and fileAssetVersion is not defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                fileAsset: 'fileAsset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('fileAsset');
        });

        it('should return assetVersion when baseType is DOTASSET and assetVersion is defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                assetVersion: 'assetVersion',
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('assetVersion');
        });

        it('should return asset when baseType is DOTASSET and assetVersion is not defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('asset');
        });

        it('should return asset when baseType is not FILEASSET or DOTASSET', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: 'OTHER',
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('asset');
        });

        it('should return empty string when asset is not defined and baseType is not FILEASSET or DOTASSET', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: 'OTHER'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('');
        });
    });
});

describe('Ellipsize Text Utility', () => {
    it('should return empty string when text is undefined', () => {
        expect(ellipsizeText(undefined, 10)).toEqual('');
    });

    it('should return empty string when text is empty', () => {
        expect(ellipsizeText('', 10)).toEqual('');
    });

    it('should return empty string when limit is 0', () => {
        expect(ellipsizeText('Any text', 0)).toEqual('');
    });

    it('should return empty string when limit is negative', () => {
        expect(ellipsizeText('Any text', -5)).toEqual('');
    });

    it('should return the original text when it is shorter than the limit', () => {
        const text = 'Short text';
        expect(ellipsizeText(text, 20)).toEqual(text);
    });

    it('should truncate the text with ellipsis when it exceeds the limit', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a longer...';
        expect(ellipsizeText(text, 18)).toEqual(truncated);
    });

    it('should handle no spaces correctly and truncate at the limit', () => {
        const text = 'Thisisaverylongwordthatneedstruncation';
        const truncated = 'Thisisaverylongwo...';
        expect(ellipsizeText(text, 18)).toEqual(truncated);
    });
    it('should return empty string when a non-string value is passed', () => {
        expect(ellipsizeText(12345 as any, 10)).toEqual('');
        expect(ellipsizeText({} as any, 10)).toEqual('');
        expect(ellipsizeText([] as any, 10)).toEqual('');
        expect(ellipsizeText(null, 10)).toEqual('');
    });
    it('should return empty string when limit is not a number', () => {
        const text = 'Any text';
        expect(ellipsizeText(text, NaN)).toEqual('');
        expect(ellipsizeText(text, 'abc' as any)).toEqual('');
        expect(ellipsizeText(text, {} as any)).toEqual('');
        expect(ellipsizeText(text, [] as any)).toEqual('');
        expect(ellipsizeText(text, null)).toEqual('');
    });

    it('should truncate text at word boundary if there is a space before the limit', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a...';
        expect(ellipsizeText(text, 15)).toEqual(truncated);
    });
});

describe('Dot Utils', () => {
    describe('getRunnableLink', () => {
        it('should replace {requestHostName} with the actual requestHostName in WAVE URL', () => {
            const url =
                'https://wave.webaim.org/report#/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: 'https://my-site.com',
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://wave.webaim.org/report#/https://my-site.com?language_id=1'
            );
        });

        it('should replace {currentUrl} and {requestHostName} in WAVE URL and append query parameters', () => {
            const url =
                'https://wave.webaim.org/report#/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com',
                siteId: '50a79decd9e21702cb2f52fc4935a52b',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://wave.webaim.org/report#/https://my-site.com/current-page?host_id=50a79decd9e21702cb2f52fc4935a52b&language_id=1'
            );
        });

        it('should replace {requestHostName} and append query parameters in Mozilla Observatory URL', () => {
            const url = 'https://developer.mozilla.org/en-US/observatory/analyze?host={domainName}';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: 'http://my-site.com:80',
                siteId: '50a79decd9e21702cb2f52fc4935a52b',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://developer.mozilla.org/en-US/observatory/analyze?host=my-site.com'
            );
        });

        it('should replace {requestHostName}, {currentUrl} and append query parameters in Security Headers URL', () => {
            const url =
                'https://securityheaders.com/?q={requestHostName}{currentUrl}{urlSearchParams}&followRedirects=on';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com',
                siteId: '50a79decd9e21702cb2f52fc4935a52b',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://securityheaders.com/?q=https://my-site.com/current-page?host_id=50a79decd9e21702cb2f52fc4935a52b&language_id=1&followRedirects=on'
            );
        });

        it('should handle empty URL correctly', () => {
            const url = '';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: '',
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual('');
        });
        it('should replace {requestHostName} with the actual requestHostName', () => {
            const url = 'https://example.com/{requestHostName}/page';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: 'https://my-site.com',
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/https://my-site.com/page'
            );
        });

        it('should replace {currentUrl} with the actual currentUrl', () => {
            const url = 'https://example.com{currentUrl}/page';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: '',
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual('https://example.com/current-page/page');
        });

        it('should append query parameters when siteId and languageId are provided', () => {
            const url = 'https://example.com/page{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: '',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/page?host_id=123&language_id=456'
            );
        });

        it('should replace all placeholders and append query parameters correctly', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/https://my-site.com/current-page?host_id=123&language_id=456'
            );
        });

        it('should handle empty URL correctly', () => {
            const url = '';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: '',
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual('');
        });

        it('should handle null currentUrl and requestHostName', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: null, // Handle null by substituting with empty string
                requestHostName: null, // Handle null by substituting with empty string
                siteId: '',
                languageId: 1
            };

            expect(getRunnableLink(url, params)).toEqual('https://example.com/?language_id=1');
        });

        it('should handle null siteId and languageId', () => {
            const url = 'https://example.com/page{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '',
                requestHostName: '',
                siteId: null, // Handle null by not appending the query parameter
                languageId: null // Handle null by not appending the query parameter
            };

            expect(getRunnableLink(url, params)).toEqual('https://example.com/page');
        });

        it('should handle all parameters as null or empty', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: null, // Handle null by substituting with empty string
                requestHostName: null, // Handle null by substituting with empty string
                siteId: null, // Handle null by not appending the query parameter
                languageId: null // Handle null by not appending the query parameter
            };

            expect(getRunnableLink(url, params)).toEqual('https://example.com/');
        });

        it('should and respect https protocol and port in requestHostName', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com:4200',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/https://my-site.com:4200/current-page?host_id=123&language_id=456'
            );
        });

        it('should and respect http protocol and port in requestHostName', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'http://my-site.com:4200',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/http://my-site.com:4200/current-page?host_id=123&language_id=456'
            );
        });

        it('should remove port and protocol in domainName', () => {
            const url = 'https://example.com/{domainName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com:4200',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/my-site.com/current-page?host_id=123&language_id=456'
            );
        });

        it('should remove port in requestHostName given https protocol and 443 port ', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'https://my-site.com:443',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/https://my-site.com/current-page?host_id=123&language_id=456'
            );
        });

        it('should remove port in requestHostName given http protocol and 80 port ', () => {
            const url = 'https://example.com/{requestHostName}{currentUrl}{urlSearchParams}';
            const params: DotPageToolUrlParams = {
                currentUrl: '/current-page',
                requestHostName: 'http://my-site.com:80',
                siteId: '123',
                languageId: 456
            };

            expect(getRunnableLink(url, params)).toEqual(
                'https://example.com/http://my-site.com/current-page?host_id=123&language_id=456'
            );
        });
    });

    describe('hasValidValue', () => {
        it('should return true when value is not empty string, null, or undefined', () => {
            expect(hasValidValue('test')).toEqual(true);
        });

        it('should return false when value is empty string, null, or undefined', () => {
            expect(hasValidValue('')).toEqual(false);
            expect(hasValidValue('   ')).toEqual(false);
            expect(hasValidValue(null)).toEqual(false);
            expect(hasValidValue(undefined)).toEqual(false);
        });
    });
});
