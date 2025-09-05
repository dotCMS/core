import { describe, it, expect } from '@jest/globals';

import { decodeFilters, encodeFilters, decodeByFilterKey } from './functions';

import { DotContentDriveFilters } from '../shared/models';

describe('Utility Functions', () => {
    describe('decodeFilters', () => {
        it('should return an empty object when input is empty string', () => {
            const result = decodeFilters('');
            expect(result).toEqual({});
        });

        it('should return an empty object when input is undefined', () => {
            const result = decodeFilters(undefined as unknown as string);
            expect(result).toEqual({});
        });

        it('should decode a single filter correctly', () => {
            const result = decodeFilters('contentType:Blog');
            expect(result).toEqual({ contentType: ['Blog'] });
        });

        it('should decode multiple filters correctly', () => {
            const result = decodeFilters('contentType:Blog;status:published');
            expect(result).toEqual({ contentType: ['Blog'], status: 'published' });
        });

        it('should handle filters with spaces correctly', () => {
            const result = decodeFilters('contentType:Blog; status:published');
            expect(result).toEqual({ contentType: ['Blog'], status: 'published' });
        });

        it('should handle filters with spaces in the value correctly', () => {
            const result = decodeFilters('title: Some Random Title;status:published');
            expect(result).toEqual({ title: 'Some Random Title', status: 'published' });
        });

        it('should ignore empty filter parts - edge case', () => {
            const result = decodeFilters('contentType:Blog;;status:published;');
            expect(result).toEqual({ contentType: ['Blog'], status: 'published' });
        });

        it('should overwrite duplicated keys with the last value - edge case', () => {
            const result = decodeFilters('contentType:Blog;contentType:News');
            expect(result).toEqual({ contentType: ['News'] });
        });

        it('should handle datetime values with multiple colons - edge case', () => {
            const result = decodeFilters('modDate:2023-10-15T14:30:45;status:published');
            expect(result).toEqual({ modDate: '2023-10-15T14:30:45', status: 'published' });
        });

        it('should handle values with multiple colons and multiple semicolons - edge case', () => {
            const result = decodeFilters(
                'someContentType.url:http://some.url;modDate:2023-10-15T14:30:45'
            );
            expect(result).toEqual({
                'someContentType.url': 'http://some.url',
                modDate: '2023-10-15T14:30:45'
            });
        });

        it('should handle filters without colons - edge case', () => {
            const result = decodeFilters('contentType:Blog;status');
            expect(result).toEqual({ contentType: ['Blog'] });
        });

        it('should handle multiselector correctly', () => {
            const result = decodeFilters('contentType:Blog,News;status:published');
            expect(result).toEqual({ contentType: ['Blog', 'News'], status: 'published' });
        });

        it('should handle multiselector with spaces correctly', () => {
            const result = decodeFilters('contentType:Blog, News;status:published');
            expect(result).toEqual({ contentType: ['Blog', 'News'], status: 'published' });
        });

        it('should handle multiselector with a wrong value', () => {
            const result = decodeFilters('contentType:Blog,;status:published,draft');
            expect(result).toEqual({
                contentType: ['Blog'],
                status: ['published', 'draft']
            });
        });
    });

    describe('encodeFilters', () => {
        it('should return an empty string when filters is an empty object', () => {
            const result = encodeFilters({});
            expect(result).toBe('');
        });

        it('should return an empty string when filters is undefined', () => {
            const result = encodeFilters(undefined as unknown as DotContentDriveFilters);
            expect(result).toBe('');
        });

        it('should encode a single filter correctly', () => {
            const result = encodeFilters({ contentType: ['Blog'] });
            expect(result).toBe('contentType:Blog');
        });

        it('should encode multiple filters correctly', () => {
            const result = encodeFilters({ contentType: ['Blog'], status: 'published' });
            const parts = result.split(';');
            expect(parts.length).toBe(2);
            expect(parts).toEqual(expect.arrayContaining(['contentType:Blog', 'status:published']));
        });

        it('should ignore filters with empty string values', () => {
            const result = encodeFilters({ contentType: ['Blog'], status: '' });
            expect(result).toBe('contentType:Blog');
        });

        it('should handle filters with spaces in the value correctly', () => {
            const result = encodeFilters({ title: 'Some Random Title', status: 'published' });
            expect(result).toBe('title:Some Random Title;status:published');
        });

        it('should encode multiselector values correctly', () => {
            const result = encodeFilters({ contentType: ['Blog', 'News'] });
            expect(result).toBe('contentType:Blog,News');
        });

        it('should encode multiple multiselect filters correctly', () => {
            const result = encodeFilters({
                contentType: ['Blog', 'News'],
                status: ['published', 'draft']
            });
            const parts = result.split(';');
            expect(parts.length).toBe(2);
            expect(parts).toEqual(
                expect.arrayContaining(['contentType:Blog,News', 'status:published,draft'])
            );
        });

        it('should encode values containing colons correctly', () => {
            const result = encodeFilters({
                'someContentType.url': 'http://some.url',
                modDate: '2023-10-15T14:30:45'
            });
            const parts = result.split(';');
            expect(parts.length).toBe(2);
            expect(parts).toEqual(
                expect.arrayContaining([
                    'someContentType.url:http://some.url',
                    'modDate:2023-10-15T14:30:45'
                ])
            );
        });
    });

    describe('decodeByFilterKey', () => {
        it('should decode baseType values as an array', () => {
            const result = decodeByFilterKey.baseType('type1,type2,type3');
            expect(result).toEqual(['type1', 'type2', 'type3']);
        });

        it('should decode baseType values with spaces correctly', () => {
            const result = decodeByFilterKey.baseType('type1, type2 , type3');
            expect(result).toEqual(['type1', 'type2', 'type3']);
        });

        it('should filter out empty baseType values', () => {
            const result = decodeByFilterKey.baseType('type1,,type3,');
            expect(result).toEqual(['type1', 'type3']);
        });

        it('should decode contentType values as an array', () => {
            const result = decodeByFilterKey.contentType('Blog,News,Article');
            expect(result).toEqual(['Blog', 'News', 'Article']);
        });

        it('should decode contentType values with spaces correctly', () => {
            const result = decodeByFilterKey.contentType('Blog, News , Article');
            expect(result).toEqual(['Blog', 'News', 'Article']);
        });

        it('should filter out empty contentType values', () => {
            const result = decodeByFilterKey.contentType('Blog,,Article,');
            expect(result).toEqual(['Blog', 'Article']);
        });

        it('should return title value as-is', () => {
            const result = decodeByFilterKey.title('some title term');
            expect(result).toBe('some title term');
        });

        it('should handle single values for baseType and contentType', () => {
            const baseTypeResult = decodeByFilterKey.baseType('singleType');
            const contentTypeResult = decodeByFilterKey.contentType('Blog');

            expect(baseTypeResult).toEqual(['singleType']);
            expect(contentTypeResult).toEqual(['Blog']);
        });

        it('should handle empty values for baseType and contentType', () => {
            const baseTypeResult = decodeByFilterKey.baseType(undefined as unknown as string);
            const contentTypeResult = decodeByFilterKey.contentType(undefined as unknown as string);

            expect(baseTypeResult).toEqual([]);
            expect(contentTypeResult).toEqual([]);
        });

        it('should handle undefined values for title', () => {
            const titleResult = decodeByFilterKey.title(undefined as unknown as string);

            expect(titleResult).toEqual('');
        });
    });

    describe('encode and decode together', () => {
        it('should preserve the filters when encoding and then decoding', () => {
            const original: DotContentDriveFilters = {
                contentType: ['Blog', 'News'],
                status: 'published',
                'someContentType.url': 'http://some.url'
            };

            const encoded = encodeFilters(original);
            const decoded = decodeFilters(encoded);

            expect(decoded).toEqual(original);
        });
    });
});
