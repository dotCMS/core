import { describe, expect, it } from '@jest/globals';

import { MenuItemCommandEvent } from 'primeng/api';

import {
    DEFAULT_VARIANT_ID,
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType
} from '@dotcms/dotcms-models';

import {
    buildContentletsQuery,
    buildESContentParams,
    buildPaletteContent,
    buildPaletteFavorite,
    buildPaletteMenuItems,
    filterFormValues,
    getPaletteState,
    getSortActiveClass
} from './index';

import { DotPaletteListStatus } from '../models';

describe('Dot UVE Palette Utils', () => {
    describe('getSortActiveClass', () => {
        it('should return true when both orderby and direction match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'name' as const, direction: 'ASC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe(true);
        });

        it('should return false when orderby does not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'usage' as const, direction: 'ASC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe(false);
        });

        it('should return false when direction does not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'name' as const, direction: 'DESC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe(false);
        });

        it('should return false when both orderby and direction do not match', () => {
            const itemSort = { orderby: 'name' as const, direction: 'ASC' as const };
            const currentSort = { orderby: 'usage' as const, direction: 'DESC' as const };

            const result = getSortActiveClass(itemSort, currentSort);

            expect(result).toBe(false);
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

    describe('buildPaletteMenuItems', () => {
        it('should return menu structure with sort and view sections', () => {
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);

            expect(result).toHaveLength(2);
            expect(result[0].label).toBe('uve.palette.menu.sort.title');
            expect(result[1].label).toBe('uve.palette.menu.view.title');
        });

        it('should include three sort options with correct labels', () => {
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            const sortItems = result[0].items;

            expect(sortItems).toHaveLength(3);
            expect(sortItems[0].label).toBe('uve.palette.menu.sort.option.popular');
            expect(sortItems[1].label).toBe('uve.palette.menu.sort.option.a-to-z');
            expect(sortItems[2].label).toBe('uve.palette.menu.sort.option.z-to-a');
        });

        it('should include two view options with correct labels', () => {
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            const viewItems = result[1].items;

            expect(viewItems).toHaveLength(2);
            expect(viewItems[0].label).toBe('uve.palette.menu.view.option.grid');
            expect(viewItems[1].label).toBe('uve.palette.menu.view.option.list');
        });

        it('should mark the current sort option as active', () => {
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            const sortItems = result[0].items;

            expect(sortItems[0].isActive).toBe(false); // popular (usage ASC) not active
            expect(sortItems[1].isActive).toBe(true); // name ASC is active
            expect(sortItems[2].isActive).toBe(false); // name DESC not active
        });

        it('should mark the current view mode as active', () => {
            const mockCallbacks = {
                viewMode: 'list' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            const viewItems = result[1].items;

            expect(viewItems[0].isActive).toBe(false); // grid not active
            expect(viewItems[1].isActive).toBe(true); // list is active
        });

        it('should call onSortSelect when sort command is executed', () => {
            const onSortSelect = jest.fn();
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect,
                onViewSelect: jest.fn()
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            const sortItems = result[0].items;

            sortItems[0].command({} as unknown as MenuItemCommandEvent);

            expect(onSortSelect).toHaveBeenCalledWith({ orderby: 'usage', direction: 'ASC' });
        });

        it('should call onViewSelect when view command is executed', () => {
            const onViewSelect = jest.fn();
            const mockCallbacks = {
                viewMode: 'grid' as const,
                currentSort: { orderby: 'name' as const, direction: 'ASC' as const },
                onSortSelect: jest.fn(),
                onViewSelect
            };

            const result = buildPaletteMenuItems(mockCallbacks);
            if (!result[1].items) {
                throw new Error('View items should be defined');
            }
            const viewItems = result[1].items;

            viewItems[1].command({} as unknown as MenuItemCommandEvent);

            expect(onViewSelect).toHaveBeenCalledWith('list');
        });
    });

    describe('buildPaletteFavorite', () => {
        const mockContentTypes: DotCMSContentType[] = [
            { id: '1', name: 'Blog', variable: 'blog' } as DotCMSContentType,
            { id: '2', name: 'News', variable: 'news' } as DotCMSContentType,
            { id: '3', name: 'Article', variable: 'article' } as DotCMSContentType,
            { id: '4', name: 'Product', variable: 'product' } as DotCMSContentType,
            { id: '5', name: 'Banner', variable: 'banner' } as DotCMSContentType
        ];

        it('should return all content types sorted alphabetically when no filter is provided', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes
            });

            expect(result.contenttypes).toHaveLength(5);
            expect(result.contenttypes[0].name).toBe('Article');
            expect(result.contenttypes[1].name).toBe('Banner');
            expect(result.contenttypes[2].name).toBe('Blog');
            expect(result.contenttypes[3].name).toBe('News');
            expect(result.contenttypes[4].name).toBe('Product');
            expect(result.status).toBe(DotPaletteListStatus.LOADED);
        });

        it('should filter content types by name (case-insensitive)', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                filter: 'blog'
            });

            expect(result.contenttypes).toHaveLength(1);
            expect(result.contenttypes[0].name).toBe('Blog');
        });

        it('should filter content types by partial name match', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                filter: 'an'
            });

            expect(result.contenttypes).toHaveLength(1);
            expect(result.contenttypes[0].name).toBe('Banner');
        });

        it('should return pagination metadata for first page', () => {
            const result = buildPaletteFavorite({
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

            const page1 = buildPaletteFavorite({
                contentTypes: manyContentTypes,
                page: 1
            });

            expect(page1.contenttypes).toHaveLength(30);
            expect(page1.pagination.totalEntries).toBe(50);
            expect(page1.pagination.currentPage).toBe(1);

            const page2 = buildPaletteFavorite({
                contentTypes: manyContentTypes,
                page: 2
            });

            expect(page2.contenttypes).toHaveLength(20);
            expect(page2.pagination.currentPage).toBe(2);
        });

        it('should handle empty filter string as no filter', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                filter: ''
            });

            expect(result.contenttypes).toHaveLength(5);
        });

        it('should return empty array when filter matches no content types', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                filter: 'nonexistent'
            });

            expect(result.contenttypes).toHaveLength(0);
            expect(result.pagination.totalEntries).toBe(0);
            expect(result.status).toBe(DotPaletteListStatus.EMPTY);
        });

        it('should mark all favorites as disabled when allowedContentTypes is undefined', () => {
            const result = buildPaletteFavorite({ contentTypes: mockContentTypes });

            expect(result.contenttypes).toHaveLength(5);
            expect(result.contenttypes.every((ct) => ct.disabled === true)).toBe(true);
        });

        it('should mark all favorites as disabled when allowedContentTypes is empty', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                allowedContentTypes: {}
            });

            expect(result.contenttypes).toHaveLength(5);
            expect(result.contenttypes.every((ct) => ct.disabled === true)).toBe(true);
        });

        it('should mark only allowed variables as enabled when allowedContentTypes is provided', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes,
                allowedContentTypes: { banner: true, blog: true }
            });

            const byVariable = Object.fromEntries(
                result.contenttypes.map((ct) => [ct.variable, ct])
            );

            expect(byVariable.blog.disabled).toBeUndefined();
            expect(byVariable.banner.disabled).toBeUndefined();
            expect(byVariable.article.disabled).toBe(true);
            expect(byVariable.news.disabled).toBe(true);
            expect(byVariable.product.disabled).toBe(true);
        });

        it('should treat WIDGET baseType as allowed when allowedContentTypes is non-empty', () => {
            const contentTypes = [
                { id: '1', name: 'Alpha', variable: 'alpha' } as DotCMSContentType,
                {
                    id: '2',
                    name: 'WidgetZ',
                    variable: 'widgetZ',
                    baseType: DotCMSBaseTypesContentTypes.WIDGET
                } as DotCMSContentType,
                { id: '3', name: 'Beta', variable: 'beta' } as DotCMSContentType
            ];

            const result = buildPaletteFavorite({
                contentTypes,
                allowedContentTypes: { alpha: true } // non-empty map is required
            });

            const byVariable = Object.fromEntries(
                result.contenttypes.map((ct) => [ct.variable, ct])
            );
            expect(byVariable.alpha.disabled).toBeUndefined();
            expect(byVariable.widgetZ.disabled).toBeUndefined(); // allowed because widget
            expect(byVariable.beta.disabled).toBe(true);
        });

        it('should keep alphabetical order even when some favorites are disabled', () => {
            const contentTypes = [
                { id: '1', name: 'B', variable: 'b' } as DotCMSContentType,
                { id: '2', name: 'A', variable: 'a' } as DotCMSContentType,
                { id: '3', name: 'C', variable: 'c' } as DotCMSContentType
            ];

            const result = buildPaletteFavorite({
                contentTypes,
                allowedContentTypes: { b: true } // A and C will be disabled
            });

            expect(result.contenttypes.map((ct) => ct.name)).toEqual(['A', 'B', 'C']);
        });

        it('should not normalize variable matching (case-sensitive)', () => {
            const contentTypes = [
                { id: '1', name: 'Banner', variable: 'Banner' } as DotCMSContentType
            ];

            const result = buildPaletteFavorite({
                contentTypes,
                allowedContentTypes: { banner: true } // different casing
            });

            expect(result.contenttypes[0].disabled).toBe(true);
        });

        it('should use default page 1 when page is not provided', () => {
            const result = buildPaletteFavorite({
                contentTypes: mockContentTypes
            });

            expect(result.pagination.currentPage).toBe(1);
        });
    });

    describe('buildPaletteContent', () => {
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

            const result = buildPaletteContent(esResponse, 0);

            expect(result.contentlets).toHaveLength(2);
            expect(result.contentlets).toEqual(esResponse.jsonObjectView.contentlets);
            expect(result.pagination.totalEntries).toBe(100);
            expect(result.status).toBe(DotPaletteListStatus.LOADED);
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

            const result = buildPaletteContent(esResponse, 0);

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

            const result = buildPaletteContent(esResponse, 30);

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

            const result = buildPaletteContent(esResponse, 60);

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

            const result = buildPaletteContent(esResponse, 0);

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

            const result = buildPaletteContent(esResponse, 0);

            expect(result.contentlets).toHaveLength(0);
            expect(result.pagination.perPage).toBe(0);
            expect(result.pagination.totalEntries).toBe(0);
            expect(result.status).toBe(DotPaletteListStatus.EMPTY);
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

    describe('filterNullAndUndefined', () => {
        it('should filter out null values', () => {
            const input = {
                name: 'John',
                age: null,
                active: true
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John',
                active: true
            });
            expect(result).not.toHaveProperty('age');
        });

        it('should filter out undefined values', () => {
            const input = {
                name: 'John',
                age: undefined,
                active: true
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John',
                active: true
            });
            expect(result).not.toHaveProperty('age');
        });

        it('should keep false values as they are valid', () => {
            const input = {
                name: 'John',
                active: false,
                verified: false
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John',
                active: false,
                verified: false
            });
        });

        it('should keep zero values as they are valid', () => {
            const input = {
                count: 0,
                score: 0
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                count: 0,
                score: 0
            });
        });

        it('should keep empty strings as they are valid', () => {
            const input = {
                name: '',
                description: ''
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: '',
                description: ''
            });
        });

        it('should recursively filter nested objects', () => {
            const input = {
                name: 'John',
                tags: {
                    tag1: true,
                    tag2: null,
                    tag3: false
                }
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John',
                tags: {
                    tag1: true,
                    tag3: false
                }
            });
            expect(result.tags).not.toHaveProperty('tag2');
        });

        it('should filter out nested objects that become empty after filtering', () => {
            const input = {
                name: 'John',
                tags: {
                    tag1: null,
                    tag2: undefined
                }
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John'
            });
            expect(result).not.toHaveProperty('tags');
        });

        it('should keep arrays as they are', () => {
            const input = {
                items: [1, 2, 3],
                tags: []
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                items: [1, 2, 3],
                tags: []
            });
        });

        it('should handle complex nested structures', () => {
            const input = {
                name: 'John',
                age: null,
                settings: {
                    theme: 'dark',
                    notifications: {
                        email: true,
                        sms: null,
                        push: false
                    },
                    preferences: null
                },
                tags: {
                    tag1: null,
                    tag2: undefined
                }
            };

            const result = filterFormValues(input);

            expect(result).toEqual({
                name: 'John',
                settings: {
                    theme: 'dark',
                    notifications: {
                        email: true,
                        push: false
                    }
                }
            });
            expect(result).not.toHaveProperty('age');
            expect(result.settings).not.toHaveProperty('preferences');
            expect(result.settings.notifications).not.toHaveProperty('sms');
            expect(result).not.toHaveProperty('tags');
        });

        it('should return empty object when all values are null or undefined', () => {
            const input = {
                name: null,
                age: undefined,
                active: null
            };

            const result = filterFormValues(input);

            expect(result).toEqual({});
        });

        it('should handle empty object', () => {
            const input = {};

            const result = filterFormValues(input);

            expect(result).toEqual({});
        });
    });
});
