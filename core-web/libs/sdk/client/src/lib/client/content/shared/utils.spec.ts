/// <reference types="jest" />

import { sanitizeQueryForContentType, shouldAddSiteIdConstraint } from './utils';

describe('Utils', () => {
    describe('sanitizeQueryForContentType', () => {
        const contentType = 'Blog';

        it('should prefix custom fields with content type', () => {
            const query = '+title:hello +author:john';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('+Blog.title:hello +Blog.author:john');
        });

        it('should not prefix main fields with content type', () => {
            const query = '+contentType:Blog +languageId:1 +live:true +variant:DEFAULT';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('+contentType:Blog +languageId:1 +live:true +variant:DEFAULT');
        });

        it('should handle mixed custom and main fields', () => {
            const query = '+contentType:Blog +title:hello +languageId:1 +author:john +live:true';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe(
                '+contentType:Blog +Blog.title:hello +languageId:1 +Blog.author:john +live:true'
            );
        });

        it('should handle empty string', () => {
            const query = '';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('');
        });

        it('should handle query without positive fields', () => {
            const query = '-title:unwanted -author:blocked';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('-title:unwanted -author:blocked');
        });

        it('should handle complex field names with underscores and numbers', () => {
            const query = '+field_name:value +field123:test +custom_field_2:data';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe(
                '+Blog.field_name:value +Blog.field123:test +Blog.custom_field_2:data'
            );
        });

        it('should handle fields with quoted values', () => {
            const query = '+title:"Hello World" +description:"This is a test"';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('+Blog.title:"Hello World" +Blog.description:"This is a test"');
        });

        it('should handle fields with special characters in values', () => {
            const query = '+title:hello-world +email:user@example.com';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('+Blog.title:hello-world +Blog.email:user@example.com');
        });

        it('should handle content type with special characters', () => {
            const specialContentType = 'Content_Type-123';
            const query = '+title:hello +author:john';
            const result = sanitizeQueryForContentType(query, specialContentType);

            expect(result).toBe('+Content_Type-123.title:hello +Content_Type-123.author:john');
        });

        it('should handle multiple spaces and formatting', () => {
            const query = '  +title:hello   +author:john  ';
            const result = sanitizeQueryForContentType(query, contentType);

            expect(result).toBe('  +Blog.title:hello   +Blog.author:john  ');
        });

        it('should handle fields that are already prefixed', () => {
            const query = '+Blog.title:hello +author:john';
            const result = sanitizeQueryForContentType(query, contentType);

            // The regex will still match dotted fields and prefix them again
            // This is the current behavior of the function
            expect(result).toBe('+Blog.Blog.title:hello +Blog.author:john');
        });
    });

    describe('shouldAddSiteIdConstraint', () => {
        describe('when siteId is not provided', () => {
            it('should return false for null siteId', () => {
                const query = '+contentType:Blog +languageId:1';
                const result = shouldAddSiteIdConstraint(query, null);

                expect(result).toBe(false);
            });

            it('should return false for undefined siteId', () => {
                const query = '+contentType:Blog +languageId:1';
                const result = shouldAddSiteIdConstraint(query, undefined);

                expect(result).toBe(false);
            });

            it('should return false for empty string siteId', () => {
                const query = '+contentType:Blog +languageId:1';
                const result = shouldAddSiteIdConstraint(query, '');

                expect(result).toBe(false);
            });

            it('should return false for zero siteId', () => {
                const query = '+contentType:Blog +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 0);

                expect(result).toBe(false);
            });
        });

        describe('when query already has positive site constraint', () => {
            it('should return false when +conhost exists with any value', () => {
                const query = '+contentType:Blog +conhost:differentSite +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(false);
            });

            it('should return false when +conhost exists with same siteId', () => {
                const query = '+contentType:Blog +conhost:mySite +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(false);
            });

            it('should return false when +conhost appears anywhere in query', () => {
                const query = '+languageId:1 +conhost:site123 +contentType:Blog';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(false);
            });
        });

        describe('when query explicitly excludes the specific site', () => {
            it('should return false when -conhost:siteId matches exactly', () => {
                const query = '+contentType:Blog -conhost:mySite +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(false);
            });

            it('should return true when -conhost excludes different site', () => {
                const query = '+contentType:Blog -conhost:otherSite +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should handle numeric siteId exclusion', () => {
                const query = '+contentType:Blog -conhost:123 +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 123);

                expect(result).toBe(false);
            });

            it('should handle string siteId exclusion', () => {
                const query = '+contentType:Blog -conhost:123 +languageId:1';
                const result = shouldAddSiteIdConstraint(query, '123');

                expect(result).toBe(false);
            });
        });

        describe('when site constraint should be added', () => {
            it('should return true for basic query without site constraints', () => {
                const query = '+contentType:Blog +languageId:1 +live:true';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should return true for empty query', () => {
                const query = '';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should return true with numeric siteId', () => {
                const query = '+contentType:Blog +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 123);

                expect(result).toBe(true);
            });

            it('should return true when query has other negative constraints', () => {
                const query = '+contentType:Blog -archived:true +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should return true when query excludes different site but not current one', () => {
                const query = '+contentType:Blog -conhost:otherSite1 -conhost:otherSite2';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });
        });

        describe('edge cases', () => {
            it('should handle query with conhost as part of other field names', () => {
                const query = '+myconhost:value +conhostfield:data';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                // The current implementation uses includes('+conhost') which will match '+myconhost'
                // This is the actual behavior of the function
                expect(result).toBe(false);
            });

            it('should be case sensitive for conhost', () => {
                const query = '+CONHOST:site +Conhost:site';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should handle complex queries with multiple conditions', () => {
                const query =
                    '+contentType:Blog +title:"Hello World" +author:john -archived:true +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });

            it('should handle queries with nested conditions', () => {
                const query = '+(contentType:Blog OR contentType:News) +languageId:1';
                const result = shouldAddSiteIdConstraint(query, 'mySite');

                expect(result).toBe(true);
            });
        });
    });
});
