import { describe, expect, it } from '@jest/globals';

import { DEFAULT_VARIANT_ID, DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import {
    buildContentletsQuery,
    buildContentletsResponse,
    buildESContentParams,
    filterAndBuildFavoriteResponse,
    getPaletteState,
    getSortActiveClass
} from './index';

import { DotPaletteListStatus } from '../models';

describe('Dot UVE Palette Utils', () => {
    describe('getSortActiveClass', () => {
        it('should return "active-menu-item" when both orderby and direction match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'name' as const, direction: 'ASC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe('active-menu-item');
        });

        it('should return empty string when orderby does not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'usage' as const, direction: 'ASC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe('');
        });

        it('should return empty string when direction does not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'name' as const, direction: 'DESC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe('');
        });

        it('should return empty string when both orderby and direction do not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'usage' as const, direction: 'DESC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe('');
        });
    });

    describe('getPaletteState', () => {
        it('should return LOADED when content types array has elements', () => {
            const contentTypes = [
                { id: '1', name: 'Blog' },
                { id: '2', name: 'News' }
            ] as DotCMSContentType[];

            const result = getPaletteState(contentTypes);

            expect(result).toBe(DotPaletteListStatus.LOADED);
        });

        it('should return LOADED when contentlets array has elements', () => {
            const contentlets = [
                { identifier: '1', title: 'Article 1' },
                { identifier: '2', title: 'Article 2' }
            ] as DotCMSContentlet[];

            const result = getPaletteState(contentlets);

            expect(result).toBe(DotPaletteListStatus.LOADED);
        });

        it('should return EMPTY when content types array is empty', () => {
            const contentTypes: DotCMSContentType[] = [];

            const result = getPaletteState(contentTypes);

            expect(result).toBe(DotPaletteListStatus.EMPTY);
        });

        it('should return EMPTY when contentlets array is empty', () => {
            const contentlets: DotCMSContentlet[] = [];

            const result = getPaletteState(contentlets);

            expect(result).toBe(DotPaletteListStatus.EMPTY);
        });
    });

    describe('filterAndBuildFavoriteResponse', () => {
        const mockContentTypes: DotCMSContentType[] = [
            { id: '1', name: 'Blog', variable: 'blog' } as DotCMSContentType,
            { id: '2', name: 'News', variable: 'news' } as DotCMSContentType,
            { id: '3', name: 'Article', variable: 'article' } as DotCMSContentType,
            { id: '4', name: 'Product', variable: 'product' } as DotCMSContentType,
            { id: '5', name: 'Banner', variable: 'banner' } as DotCMSContentType
        ];

        it('should return all content types sorted alphabetically when no filter is provided', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes
            });

            expect(result.contenttypes).toHaveLength(5);
            expect(result.contenttypes[0].name).toBe('Article');
            expect(result.contenttypes[1].name).toBe('Banner');
            expect(result.contenttypes[2].name).toBe('Blog');
            expect(result.contenttypes[3].name).toBe('News');
            expect(result.contenttypes[4].name).toBe('Product');
        });

        it('should filter content types by name (case-insensitive)', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes,
                filter: 'blog'
            });

            expect(result.contenttypes).toHaveLength(1);
            expect(result.contenttypes[0].name).toBe('Blog');
        });

        it('should filter content types by partial name match', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes,
                filter: 'an'
            });

            expect(result.contenttypes).toHaveLength(1);
            expect(result.contenttypes[0].name).toBe('Banner');
        });

        it('should return pagination metadata for first page', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes,
                page: 1
            });

            expect(result.pagination).toEqual({
                currentPage: 1,
                perPage: 30,
                totalEntries: 5
            });
        });

        it('should paginate results correctly', () => {
            const manyContentTypes = Array.from({ length: 50 }, (_, i) => ({
                id: String(i),
                name: `Content ${i}`,
                variable: `content${i}`
            })) as DotCMSContentType[];

            const page1 = filterAndBuildFavoriteResponse({
                contentTypes: manyContentTypes,
                page: 1
            });

            expect(page1.contenttypes).toHaveLength(30);
            expect(page1.pagination.totalEntries).toBe(50);
            expect(page1.pagination.currentPage).toBe(1);

            const page2 = filterAndBuildFavoriteResponse({
                contentTypes: manyContentTypes,
                page: 2
            });

            expect(page2.contenttypes).toHaveLength(20);
            expect(page2.pagination.currentPage).toBe(2);
        });

        it('should handle empty filter string as no filter', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes,
                filter: ''
            });

            expect(result.contenttypes).toHaveLength(5);
        });

        it('should return empty array when filter matches no content types', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes,
                filter: 'nonexistent'
            });

            expect(result.contenttypes).toHaveLength(0);
            expect(result.pagination.totalEntries).toBe(0);
        });

        it('should use default page 1 when page is not provided', () => {
            const result = filterAndBuildFavoriteResponse({
                contentTypes: mockContentTypes
            });

            expect(result.pagination.currentPage).toBe(1);
        });
    });

    describe('buildContentletsResponse', () => {
        it('should transform ES response into normalized format', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: [
                        { identifier: '1', title: 'Article 1' },
                        { identifier: '2', title: 'Article 2' }
                    ] as DotCMSContentlet[]
                },
                resultsSize: 100
            };

            const result = buildContentletsResponse(esResponse, 0);

            expect(result.contentlets).toHaveLength(2);
            expect(result.contentlets).toEqual(esResponse.jsonObjectView.contentlets);
            expect(result.pagination.totalEntries).toBe(100);
        });

        it('should calculate correct page number for offset 0', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: [{ identifier: '1', title: 'Article 1' }] as DotCMSContentlet[]
                },
                resultsSize: 50
            };

            const result = buildContentletsResponse(esResponse, 0);

            expect(result.pagination.currentPage).toBe(1);
        });

        it('should calculate correct page number for offset 30', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: [{ identifier: '1', title: 'Article 1' }] as DotCMSContentlet[]
                },
                resultsSize: 50
            };

            const result = buildContentletsResponse(esResponse, 30);

            expect(result.pagination.currentPage).toBe(2);
        });

        it('should calculate correct page number for offset 60', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: [{ identifier: '1', title: 'Article 1' }] as DotCMSContentlet[]
                },
                resultsSize: 100
            };

            const result = buildContentletsResponse(esResponse, 60);

            expect(result.pagination.currentPage).toBe(3);
        });

        it('should set perPage based on actual contentlets length', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: Array.from({ length: 25 }, (_, i) => ({
                        identifier: String(i),
                        title: `Article ${i}`
                    })) as DotCMSContentlet[]
                },
                resultsSize: 25
            };

            const result = buildContentletsResponse(esResponse, 0);

            expect(result.pagination.perPage).toBe(25);
        });

        it('should handle empty contentlets array', () => {
            const esResponse = {
                contentTook: 10,
                queryTook: 5,
                jsonObjectView: {
                    contentlets: []
                },
                resultsSize: 0
            };

            const result = buildContentletsResponse(esResponse, 0);

            expect(result.contentlets).toHaveLength(0);
            expect(result.pagination.perPage).toBe(0);
            expect(result.pagination.totalEntries).toBe(0);
        });
    });

    describe('buildContentletsQuery', () => {
        it('should build query with DEFAULT variant when no variantId is provided', () => {
            const result = buildContentletsQuery('Blog', '');

            expect(result).toBe(`+contentType:Blog +deleted:false +variant:${DEFAULT_VARIANT_ID}`);
        });

        it('should build query with DEFAULT OR custom variant when variantId is provided', () => {
            const result = buildContentletsQuery('Blog', 'christmas-variant');

            expect(result).toBe(
                `+contentType:Blog +deleted:false +variant:(${DEFAULT_VARIANT_ID} OR christmas-variant)`
            );
        });

        it('should exclude deleted content', () => {
            const result = buildContentletsQuery('News', 'summer-variant');

            expect(result).toContain('+deleted:false');
        });

        it('should handle content type names with special characters', () => {
            const result = buildContentletsQuery('Special-Content_Type', 'test-variant');

            expect(result).toContain('+contentType:Special-Content_Type');
        });

        it('should handle DEFAULT_VARIANT_ID constant', () => {
            const result = buildContentletsQuery('Product', DEFAULT_VARIANT_ID);

            expect(result).toBe(
                `+contentType:Product +deleted:false +variant:(${DEFAULT_VARIANT_ID} OR ${DEFAULT_VARIANT_ID})`
            );
        });
    });

    describe('buildESContentParams', () => {
        it('should build ES content params with all fields', () => {
            const searchParams = {
                selectedContentType: 'Blog',
                variantId: 'christmas-variant',
                language: 1,
                page: 1,
                filter: 'news'
            };

            const result = buildESContentParams(searchParams);

            expect(result).toEqual({
                query: `+contentType:Blog +deleted:false +variant:(${DEFAULT_VARIANT_ID} OR christmas-variant)`,
                offset: '0',
                itemsPerPage: 30,
                lang: '1',
                filter: 'news'
            });
        });

        it('should calculate offset correctly for page 1', () => {
            const searchParams = {
                selectedContentType: 'Blog',
                variantId: '',
                language: 1,
                page: 1,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.offset).toBe('0');
        });

        it('should calculate offset correctly for page 2', () => {
            const searchParams = {
                selectedContentType: 'Blog',
                variantId: '',
                language: 1,
                page: 2,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.offset).toBe('30');
        });

        it('should calculate offset correctly for page 3', () => {
            const searchParams = {
                selectedContentType: 'Blog',
                variantId: '',
                language: 1,
                page: 3,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.offset).toBe('60');
        });

        it('should convert language number to string', () => {
            const searchParams = {
                selectedContentType: 'News',
                variantId: '',
                language: 2,
                page: 1,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.lang).toBe('2');
        });

        it('should handle empty filter string', () => {
            const searchParams = {
                selectedContentType: 'Product',
                variantId: '',
                language: 1,
                page: 1,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.filter).toBe('');
        });

        it('should set itemsPerPage to DEFAULT_PER_PAGE (30)', () => {
            const searchParams = {
                selectedContentType: 'Banner',
                variantId: '',
                language: 1,
                page: 1,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.itemsPerPage).toBe(30);
        });

        it('should handle complex filter strings', () => {
            const searchParams = {
                selectedContentType: 'Article',
                variantId: 'test',
                language: 1,
                page: 1,
                filter: 'test search with spaces'
            };

            const result = buildESContentParams(searchParams);

            expect(result.filter).toBe('test search with spaces');
        });

        it('should build query without variant when variantId is empty string', () => {
            const searchParams = {
                selectedContentType: 'Widget',
                variantId: '',
                language: 1,
                page: 1,
                filter: ''
            };

            const result = buildESContentParams(searchParams);

            expect(result.query).toBe(
                `+contentType:Widget +deleted:false +variant:${DEFAULT_VARIANT_ID}`
            );
        });
    });
});
