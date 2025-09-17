import { describe, it, expect } from '@jest/globals';

import { createFakeSite } from '@dotcms/utils-testing';

import {
    decodeFilters,
    encodeFilters,
    decodeByFilterKey,
    buildContentDriveQuery
} from './functions';

import { SYSTEM_HOST } from '../shared/constants';
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

    describe('buildContentDriveQuery', () => {
        // const mockSite: SiteEntity = {
        //     aliases: '',
        //     archived: false,
        //     categoryId: '',
        //     contentTypeId: '',
        //     default: false,
        //     dotAsset: false,
        //     fileAsset: false,
        //     folder: '/',
        //     form: false,
        //     host: 'test-site.com',
        //     hostThumbnail: null,
        //     hostname: 'test-site.com',
        //     htmlpage: false,
        //     identifier: 'test-site-123',
        //     indexPolicyDependencies: '',
        //     inode: 'test-inode',
        //     keyValue: false,
        //     languageId: 1,
        //     languageVariable: false,
        //     live: true,
        //     locked: false,
        //     lowIndexPriority: false,
        //     modDate: 1234567890,
        //     modUser: 'test-user',
        //     name: 'Test Site',
        //     new: false,
        //     owner: 'admin',
        //     parent: false,
        //     permissionId: 'permission-123',
        //     permissionType: 'INDIVIDUAL',
        //     persona: false,
        //     sortOrder: 0,
        //     structureInode: 'structure-123',
        //     systemHost: false,
        //     tagStorage: 'SCHEMA',
        //     title: 'Test Site',
        //     titleImage: null,
        //     type: 'HOST',
        //     vanityUrl: false,
        //     variantId: '',
        //     versionId: 'version-123',
        //     working: true
        // };

        const mockSite = createFakeSite({
            aliases: '',
            archived: false,
            categoryId: '',
            contentTypeId: '',
            default: false,
            dotAsset: false,
            fileAsset: false,
            folder: '/',
            form: false,
            host: 'test-site.com',
            hostThumbnail: null,
            hostname: 'test-site.com',
            htmlpage: false,
            identifier: 'test-site-123',
            indexPolicyDependencies: '',
            inode: 'test-inode',
            keyValue: false,
            languageId: 1,
            languageVariable: false,
            live: true,
            locked: false,
            lowIndexPriority: false,
            modDate: 1234567890,
            modUser: 'test-user',
            name: 'Test Site',
            new: false,
            owner: 'admin',
            parent: false,
            permissionId: 'permission-123',
            permissionType: 'INDIVIDUAL',
            persona: false,
            sortOrder: 0,
            structureInode: 'structure-123',
            systemHost: false,
            tagStorage: 'SCHEMA',
            title: 'Test Site',
            titleImage: null,
            type: 'HOST',
            vanityUrl: false,
            variantId: '',
            versionId: 'version-123',
            working: true
        });

        it('should build basic query with site and default filters', () => {
            const result = buildContentDriveQuery({
                currentSite: mockSite
            });

            // Should include base query, site filter, and default working/variant filters
            expect(result).toContain(
                '+systemType:false -contentType:forms -contentType:Host +deleted:false'
            );
            expect(result).toContain(
                `+(conhost:${mockSite.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
            );
        });

        it('should add path filter when path is provided', () => {
            const testPath = '/test/path';
            const result = buildContentDriveQuery({
                currentSite: mockSite,
                path: testPath
            });

            expect(result).toContain(`parentPath:${testPath}`);
        });

        it('should not add path filter when path is undefined', () => {
            const result = buildContentDriveQuery({
                currentSite: mockSite,
                path: undefined
            });

            expect(result).not.toContain('parentPath:');
        });

        it('should handle single value filters correctly', () => {
            const filters: DotContentDriveFilters = {
                status: 'published',
                language: 'en'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('status:published');
            expect(result).toContain('language:en');
        });

        it('should handle multiselect filters with single value', () => {
            const filters: DotContentDriveFilters = {
                contentType: ['Blog']
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('+contentType:Blog');
        });

        it('should handle multiselect filters with multiple values', () => {
            const filters: DotContentDriveFilters = {
                contentType: ['Blog', 'News', 'Article']
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('+contentType:(Blog OR News OR Article)');
        });

        it('should handle baseType multiselect filters correctly', () => {
            const filters: DotContentDriveFilters = {
                baseType: ['1', '2']
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('+baseType:(1 OR 2)');
        });

        it('should handle title filter with special search logic', () => {
            const filters: DotContentDriveFilters = {
                title: 'test search'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            // Should include catchall search
            expect(result).toContain('+catchall:*test search*');
            // Should include title_dotraw search with boost
            expect(result).toContain('title_dotraw:*test search*^5');
            // Should include exact title search with higher boost
            expect(result).toContain("title:'test search'^15");
            // Should include individual word searches
            expect(result).toContain('title:test^5');
            expect(result).toContain('title:search^5');
        });

        it('should handle title filter with single word', () => {
            const filters: DotContentDriveFilters = {
                title: 'blog'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('+catchall:*blog*');
            expect(result).toContain('title_dotraw:*blog*^5');
            expect(result).toContain("title:'blog'^15");
            expect(result).toContain('title:blog^5');
        });

        it('should filter out empty words in title search', () => {
            const filters: DotContentDriveFilters = {
                title: 'test  search   with   spaces'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            // Should include individual words but not empty strings
            expect(result).toContain('title:test^5');
            expect(result).toContain('title:search^5');
            expect(result).toContain('title:with^5');
            expect(result).toContain('title:spaces^5');
        });

        it('should ignore filters with undefined values', () => {
            const filters: DotContentDriveFilters = {
                contentType: ['Blog'],
                status: undefined,
                language: 'en'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            expect(result).toContain('+contentType:Blog');
            expect(result).toContain('language:en');
            expect(result).not.toContain('status:');
        });

        it('should handle mixed filter types correctly', () => {
            const filters: DotContentDriveFilters = {
                contentType: ['Blog', 'News'],
                baseType: ['1'],
                title: 'search term',
                status: 'published',
                language: 'en'
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                path: '/content',
                filters
            });

            // Should include all filter types
            expect(result).toContain('parentPath:/content');
            expect(result).toContain('+contentType:(Blog OR News)');
            expect(result).toContain('+baseType:1');
            expect(result).toContain('+catchall:*search term*');
            expect(result).toContain('status:published');
            expect(result).toContain('language:en');
        });

        it('should handle empty filters object', () => {
            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters: {}
            });

            // Should only include base query and site filters
            expect(result).toContain(
                '+systemType:false -contentType:forms -contentType:Host +deleted:false'
            );
            expect(result).toContain(
                `+(conhost:${mockSite.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
            );
        });

        it('should handle filters with empty arrays', () => {
            const filters: DotContentDriveFilters = {
                contentType: [],
                baseType: []
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                filters
            });

            // Empty arrays should result in empty OR chains
            expect(result).toContain('+contentType:');
            expect(result).toContain('+baseType:');
        });

        it('should work with SYSTEM_HOST as current site', () => {
            const result = buildContentDriveQuery({
                currentSite: SYSTEM_HOST
            });

            expect(result).toContain(
                `+(conhost:${SYSTEM_HOST.identifier} OR conhost:${SYSTEM_HOST.identifier}) +working:true +variant:default`
            );
        });

        it('should handle complex real-world scenario', () => {
            const filters: DotContentDriveFilters = {
                contentType: ['Blog', 'NewsArticle'],
                baseType: ['1', '2'],
                title: 'dotCMS content management',
                status: 'published',
                languageId: ['1']
            };

            const result = buildContentDriveQuery({
                currentSite: mockSite,
                path: '/site/content/blog',
                filters
            });

            // Verify all components are present
            expect(result).toContain('parentPath:/site/content/blog');
            expect(result).toContain('+contentType:(Blog OR NewsArticle)');
            expect(result).toContain('+baseType:(1 OR 2)');
            expect(result).toContain('+catchall:*dotCMS content management*');
            expect(result).toContain('title:dotCMS^5');
            expect(result).toContain('title:content^5');
            expect(result).toContain('title:management^5');
            expect(result).toContain('status:published');
            expect(result).toContain('languageId:1');
        });
    });
});
