import { describe, expect, it } from '@jest/globals';
import { of, throwError } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotPagination,
    FolderSearchView,
    isTreeNodeContentData,
    PERMISSIONS_TYPE
} from '@dotcms/dotcms-models';
import {
    createFakeCheckboxField,
    createFakeDateField,
    createFakeFolderSearchView,
    createFakeSelectField,
    createFakeSite,
    createFakeTagField,
    createFakeTextField
} from '@dotcms/utils-testing';

import {
    applyLoadMoreToHierarchy,
    buildLoadMoreNode,
    buildUserSearchablePayload,
    decodeByFilterKey,
    decodeFilters,
    encodeFilters,
    folderSearchViewToDotFolder,
    getFolderHierarchyByPath,
    getFolderNodesByPath,
    getPathLeafName,
    getUserSearchableActive,
    isBinaryCheckboxField,
    isDateFieldFilterType,
    isFolder,
    isMultiValueFieldFilterType,
    parseUserSearchableValue,
    parseWorkflowFilter,
    mergeFolderNodePage,
    parseWorkflowToken,
    resolveHierarchyAncestor,
    serializeUserSearchableValue,
    toLocalIsoString,
    hasNonDefaultFilters,
    withDefaultLanguage,
    withDefaultSharedAssets,
    withFilterDefaults,
    workflowEntryToToken
} from './functions';
import { createTreeNode } from './tree-folder.utils';

import {
    FOLDER_TREE_HIERARCHY_PAGE_SIZE,
    FOLDER_TREE_PAGE_SIZE,
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_ENABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '../shared/constants';
import { DotContentDriveFilters } from '../shared/models';

describe('Utility Functions', () => {
    describe('toLocalIsoString', () => {
        it('formats a Date as a no-offset local wall-clock ISO string (what the user sees)', () => {
            // Built from LOCAL components, so the assertion is timezone-independent.
            const date = new Date(2026, 6, 28, 9, 5, 3); // 2026-07-28 09:05:03 local

            expect(toLocalIsoString(date)).toBe('2026-07-28T09:05:03');
        });

        it('zero-pads and never appends a Z/offset (so the backend keeps the wall-clock)', () => {
            const result = toLocalIsoString(new Date(2026, 0, 1, 0, 0, 0));

            expect(result).toBe('2026-01-01T00:00:00');
            expect(result).not.toContain('Z');
        });

        it('returns an empty string for an Invalid Date (typeable picker can emit one)', () => {
            // Guards the RangeError date-fns `format` would throw on an invalid instant.
            expect(toLocalIsoString(new Date('not-a-date'))).toBe('');
        });
    });

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
            const result = decodeFilters('contentType:Blog;owner:jane');
            expect(result).toEqual({ contentType: ['Blog'], owner: 'jane' });
        });

        it('should handle filters with spaces correctly', () => {
            const result = decodeFilters('contentType:Blog; owner:jane');
            expect(result).toEqual({ contentType: ['Blog'], owner: 'jane' });
        });

        it('should handle filters with spaces in the value correctly', () => {
            const result = decodeFilters('title: Some Random Title;owner:jane');
            expect(result).toEqual({ title: 'Some Random Title', owner: 'jane' });
        });

        it('should ignore empty filter parts - edge case', () => {
            const result = decodeFilters('contentType:Blog;;owner:jane;');
            expect(result).toEqual({ contentType: ['Blog'], owner: 'jane' });
        });

        it('should overwrite duplicated keys with the last value - edge case', () => {
            const result = decodeFilters('contentType:Blog;contentType:News');
            expect(result).toEqual({ contentType: ['News'] });
        });

        it('should handle datetime values with multiple colons - edge case', () => {
            const result = decodeFilters('modDate:2023-10-15T14:30:45;owner:jane');
            expect(result).toEqual({ modDate: '2023-10-15T14:30:45', owner: 'jane' });
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
            const result = decodeFilters('contentType:Blog;owner');
            expect(result).toEqual({ contentType: ['Blog'] });
        });

        it('should handle multiselector correctly', () => {
            const result = decodeFilters('contentType:Blog,News;owner:jane');
            expect(result).toEqual({ contentType: ['Blog', 'News'], owner: 'jane' });
        });

        it('should handle multiselector with spaces correctly', () => {
            const result = decodeFilters('contentType:Blog, News;owner:jane');
            expect(result).toEqual({ contentType: ['Blog', 'News'], owner: 'jane' });
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

        it('should handle empty arrays by encoding them', () => {
            const result = encodeFilters({ contentType: [], baseType: ['1'] });
            // Empty arrays are encoded as "key:" (empty value after colon) since join(',') on empty array returns ''
            expect(result).toBe('contentType:;baseType:1');
        });

        it('should handle filters with null or undefined values', () => {
            // Runtime may still see null/undefined bag values; cast past the index signature.
            const dirtyFilters = {
                contentType: ['Blog'],
                status: undefined,
                title: null
            } as unknown as DotContentDriveFilters;

            const result = encodeFilters(dirtyFilters);
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

        it('should decode workflow tokens, preserving the scheme:step colon', () => {
            // Each token is `schemeId` or `schemeId:stepId`; only commas separate tokens.
            const result = decodeByFilterKey.workflow('schemeA:stepX,schemeB,schemeC:stepY');
            expect(result).toEqual(['schemeA:stepX', 'schemeB', 'schemeC:stepY']);
        });

        it('should decode multiple statuses', () => {
            expect(decodeByFilterKey.status('UNPUBLISHED,LOCKED')).toEqual([
                'UNPUBLISHED',
                'LOCKED'
            ]);
        });

        it('should decode a SINGLE status as an array, not a string', () => {
            // The case an explicit `decodeByFilterKey` entry exists to cover. Without it the key
            // falls through to the comma sniff in `decodeFilterValue`, and a lone value decodes to
            // the string 'ARCHIVED' — whose `.length` is 8, so every `?.status?.length` guard
            // downstream reads as "a status is active" and the filter looks fine right up until
            // someone selects exactly one.
            expect(decodeByFilterKey.status('ARCHIVED')).toEqual(['ARCHIVED']);
        });
    });

    describe('status filter round-trip', () => {
        it('should survive encode → decode unchanged', () => {
            const filters = { status: ['ARCHIVED', 'LOCKED'] };
            expect(decodeFilters(encodeFilters(filters))).toEqual(filters);
        });

        it('should survive the round-trip with a single status', () => {
            const filters = { status: ['LOCKED'] };
            expect(decodeFilters(encodeFilters(filters))).toEqual(filters);
        });
    });

    describe('workflow token (de)serialization', () => {
        describe('parseWorkflowToken', () => {
            it('should parse a scheme-only token', () => {
                expect(parseWorkflowToken('schemeA')).toEqual({ scheme: 'schemeA' });
            });

            it('should parse a scheme:step token', () => {
                expect(parseWorkflowToken('schemeA:stepX')).toEqual({
                    scheme: 'schemeA',
                    step: 'stepX'
                });
            });

            it('should split on the FIRST colon only, preserving colons inside the step id', () => {
                expect(parseWorkflowToken('scheme:step:with:colons')).toEqual({
                    scheme: 'scheme',
                    step: 'step:with:colons'
                });
            });

            it('should treat an empty token as an empty scheme with no step', () => {
                expect(parseWorkflowToken('')).toEqual({ scheme: '' });
            });

            it('should treat a leading separator as an empty scheme with a step', () => {
                expect(parseWorkflowToken(':stepX')).toEqual({ scheme: '', step: 'stepX' });
            });
        });

        describe('workflowEntryToToken', () => {
            it('should serialize a scheme-only entry to the bare scheme id', () => {
                expect(workflowEntryToToken({ scheme: 'schemeA' })).toBe('schemeA');
            });

            it('should serialize a scheme+step entry as scheme:step', () => {
                expect(workflowEntryToToken({ scheme: 'schemeA', step: 'stepX' })).toBe(
                    'schemeA:stepX'
                );
            });

            it('should be the inverse of parseWorkflowToken (round-trip)', () => {
                ['schemeA', 'schemeA:stepX', 'scheme:step:with:colons'].forEach((token) => {
                    expect(workflowEntryToToken(parseWorkflowToken(token))).toBe(token);
                });
            });
        });

        describe('parseWorkflowFilter', () => {
            it('should map a list of tokens to entries', () => {
                expect(parseWorkflowFilter(['schemeA:stepX', 'schemeB'])).toEqual([
                    { scheme: 'schemeA', step: 'stepX' },
                    { scheme: 'schemeB' }
                ]);
            });

            it('should default to an empty array when called with no tokens', () => {
                expect(parseWorkflowFilter()).toEqual([]);
                expect(parseWorkflowFilter([])).toEqual([]);
            });
        });
    });

    describe('encode and decode together', () => {
        it('should preserve the filters when encoding and then decoding', () => {
            const original: DotContentDriveFilters = {
                contentType: ['Blog', 'News'],
                owner: 'jane',
                'someContentType.url': 'http://some.url'
            };

            const encoded = encodeFilters(original);
            const decoded = decodeFilters(encoded);

            expect(decoded).toEqual(original);
        });

        it('should round-trip a workflow filter with pinned-step (scheme:step) tokens', () => {
            const original: DotContentDriveFilters = {
                workflow: ['schemeA:stepX', 'schemeB']
            };

            const encoded = encodeFilters(original);
            const decoded = decodeFilters(encoded);

            expect(encoded).toBe('workflow:schemeA:stepX,schemeB');
            expect(decoded).toEqual(original);
        });
    });

    describe('withDefaultLanguage', () => {
        const DEFAULT_LANGUAGE_ID = 2;

        it('should seed the default language when the key is absent', () => {
            expect(withDefaultLanguage({ title: 'Blog' }, DEFAULT_LANGUAGE_ID)).toEqual({
                title: 'Blog',
                languageId: ['2']
            });
        });

        it('should seed the default language when the key is an empty array', () => {
            expect(withDefaultLanguage({ languageId: [] }, DEFAULT_LANGUAGE_ID)).toEqual({
                languageId: ['2']
            });
        });

        it('should leave an existing selection untouched', () => {
            const filters: DotContentDriveFilters = { languageId: ['1', '3'] };

            expect(withDefaultLanguage(filters, DEFAULT_LANGUAGE_ID)).toEqual({
                languageId: ['1', '3']
            });
        });

        it('should leave the filters untouched when the default is unknown', () => {
            // The languages request has not answered (or failed): the portlet must fall back to
            // exactly its pre-seeding behaviour rather than inventing a language.
            expect(withDefaultLanguage({ title: 'Blog' }, undefined)).toEqual({ title: 'Blog' });
        });

        it('should not mutate the filters it was given', () => {
            const filters: DotContentDriveFilters = { title: 'Blog' };

            withDefaultLanguage(filters, DEFAULT_LANGUAGE_ID);

            expect(filters).toEqual({ title: 'Blog' });
        });
    });

    describe('withDefaultSharedAssets', () => {
        it('should seed the toggle as on when the key is absent', () => {
            expect(withDefaultSharedAssets({ title: 'Blog' })).toEqual({
                title: 'Blog',
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
            });
        });

        it('should leave an explicit opt-out untouched', () => {
            expect(
                withDefaultSharedAssets({
                    [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
                })
            ).toEqual({ [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE });
        });

        it('should not mutate the filters it was given', () => {
            const filters: DotContentDriveFilters = { title: 'Blog' };

            withDefaultSharedAssets(filters);

            expect(filters).toEqual({ title: 'Blog' });
        });
    });

    describe('withFilterDefaults', () => {
        it('should apply every default in one pass', () => {
            expect(withFilterDefaults({}, 2)).toEqual({
                languageId: ['2'],
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
            });
        });

        it('should still seed the toggle when the default language is unknown', () => {
            // The languages request has not answered yet; that must not hold back an unrelated
            // default.
            expect(withFilterDefaults({}, undefined)).toEqual({
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
            });
        });

        it('should preserve values the caller already set', () => {
            expect(
                withFilterDefaults(
                    {
                        languageId: ['3'],
                        [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
                    },
                    2
                )
            ).toEqual({
                languageId: ['3'],
                [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
            });
        });
    });

    describe('hasNonDefaultFilters', () => {
        const DEFAULT_LANGUAGE_ID = 1;

        it('should report nothing to clear when only the seeded defaults are set', () => {
            // Both defaults are always present, so counting keys would report a filtered drive to
            // every user who has filtered nothing.
            expect(
                hasNonDefaultFilters(
                    {
                        languageId: ['1'],
                        [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
                    },
                    DEFAULT_LANGUAGE_ID
                )
            ).toBe(false);
        });

        it('should report nothing to clear for an empty filter set', () => {
            expect(hasNonDefaultFilters({}, DEFAULT_LANGUAGE_ID)).toBe(false);
        });

        it('should report a change once shared assets are turned off', () => {
            expect(
                hasNonDefaultFilters(
                    {
                        languageId: ['1'],
                        [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_DISABLED_VALUE
                    },
                    DEFAULT_LANGUAGE_ID
                )
            ).toBe(true);
        });

        it('should report a change once a non-default language is picked', () => {
            expect(
                hasNonDefaultFilters(
                    {
                        languageId: ['2'],
                        [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE
                    },
                    DEFAULT_LANGUAGE_ID
                )
            ).toBe(true);
        });

        it('should report a change once more than one language is picked', () => {
            expect(hasNonDefaultFilters({ languageId: ['1', '2'] }, DEFAULT_LANGUAGE_ID)).toBe(
                true
            );
        });

        it('should treat a language selection as a change while the default is unknown', () => {
            expect(hasNonDefaultFilters({ languageId: ['1'] }, undefined)).toBe(true);
        });

        it.each([
            ['title', { title: 'Blog' }],
            ['contentType', { contentType: ['Blog'] }],
            ['baseType', { baseType: ['1'] }],
            ['workflow', { workflow: ['scheme-1'] }],
            ['a field filter', { 'us.body': 'hello' }]
        ])('should report a change for %s', (_label: string, filters: DotContentDriveFilters) => {
            expect(
                hasNonDefaultFilters(
                    {
                        languageId: ['1'],
                        [SHARED_ASSETS_FILTER_KEY]: SHARED_ASSETS_ENABLED_VALUE,
                        ...filters
                    },
                    DEFAULT_LANGUAGE_ID
                )
            ).toBe(true);
        });
    });

    describe('getFolderHierarchyByPath', () => {
        let mockDotFolderService: jest.Mocked<DotFolderService>;
        const SITE_ID = 'site-123';
        const HOSTNAME = 'test.com';
        const SITE = createFakeSite({ identifier: SITE_ID, hostname: HOSTNAME });

        const searchResult = (folders: FolderSearchView[]) =>
            of({ folders, pagination: {} as DotPagination });

        beforeEach(() => {
            mockDotFolderService = {
                searchFolders: jest.fn().mockReturnValue(searchResult([]))
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should search the root and every parent path with the hierarchy page size', (done) => {
            const folderPath = '/main/sub-folder/inner-folder';

            // Every level returns the ancestor the next one descends into, so the hierarchy
            // resolves in one request per level with no follow-up lookups.
            const childOf: Record<string, string> = {
                '/': 'main',
                '/main/': 'sub-folder',
                '/main/sub-folder/': 'inner-folder'
            };
            mockDotFolderService.searchFolders.mockImplementation(({ path }) =>
                searchResult(
                    childOf[path] ? [createFakeFolderSearchView({ path, name: childOf[path] })] : []
                )
            );

            getFolderHierarchyByPath(folderPath, SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(4);

                    const expectedPaths = [
                        '/',
                        '/main/',
                        '/main/sub-folder/',
                        '/main/sub-folder/inner-folder/'
                    ];
                    expectedPaths.forEach((path) => {
                        expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                            expect.objectContaining({
                                siteId: SITE_ID,
                                path,
                                recursive: false,
                                page: 1,
                                per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE
                            })
                        );
                    });
                    done();
                },
                error: done
            });
        });

        it('should adapt search results into DotFolder full paths with the site hostname', (done) => {
            const view = createFakeFolderSearchView({
                id: 'm',
                inode: 'im',
                name: 'main',
                path: '/',
                addChildrenAllowed: true,
                hasChildren: true
            });

            mockDotFolderService.searchFolders.mockReturnValueOnce(searchResult([view]));

            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].folders[0]).toEqual({
                        id: 'm',
                        inode: 'im',
                        hostName: HOSTNAME,
                        path: '/main/',
                        addChildrenAllowed: true,
                        hasChildren: true,
                        name: 'main',
                        title: view.title,
                        sortOrder: view.sortOrder,
                        filesMasks: view.filesMasks,
                        defaultFileType: view.defaultFileType,
                        showOnMenu: view.showOnMenu,
                        defaultBaseType: view.defaultBaseType,
                        // The hierarchy load cannot opt into permissions, so the endpoint's `null`
                        // is carried through as "unresolved" rather than "no grants".
                        permissions: undefined
                    });
                    done();
                },
                error: done
            });
        });

        it('should request permissions so first-paint nodes can gate their context menu', (done) => {
            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ includePermissions: true })
                    );
                    done();
                },
                error: done
            });
        });

        it('should query only the site root for the root path', (done) => {
            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ path: '/' })
                    );
                    expect(levels).toHaveLength(1);
                    done();
                },
                error: done
            });
        });

        it('should query only the site root for an empty path', (done) => {
            getFolderHierarchyByPath('', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ path: '/' })
                    );
                    done();
                },
                error: done
            });
        });

        it('should request the large hierarchy page size (not the interactive 40)', (done) => {
            const many = Array.from({ length: 45 }, (_, i) =>
                createFakeFolderSearchView({ id: `f${i}`, name: `folder-${i}`, path: '/' })
            );
            mockDotFolderService.searchFolders.mockReturnValue(searchResult(many));

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].folders).toHaveLength(45);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE })
                    );
                    expect(FOLDER_TREE_HIERARCHY_PAGE_SIZE).toBeGreaterThan(FOLDER_TREE_PAGE_SIZE);
                    done();
                },
                error: done
            });
        });

        it('should include folders past interactive page position 40 for deep-link restore', (done) => {
            // Simulates a level where the deep-linked name sorts after the first 40 siblings.
            const siblings = Array.from({ length: 45 }, (_, i) =>
                createFakeFolderSearchView({
                    id: `f${i}`,
                    name: `qa36151-child-${i}`,
                    path: '/qa36151-many-parent/'
                })
            );
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: siblings,
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                        totalEntries: siblings.length
                    }
                })
            );

            getFolderHierarchyByPath(
                '/qa36151-many-parent/qa36151-child-9/',
                SITE,
                mockDotFolderService
            ).subscribe({
                next: (levels) => {
                    // Hierarchy returns every sibling in one large page so a late-sorted
                    // name (string-sort: child-9 is past position 40) is still present.
                    const parentLevel = levels.find(
                        (level) => level.path === '/qa36151-many-parent/'
                    );
                    expect(parentLevel).toBeDefined();
                    expect(parentLevel!.folders.length).toBeGreaterThan(FOLDER_TREE_PAGE_SIZE);
                    expect(
                        parentLevel!.folders.some(
                            (folder) => folder.path === '/qa36151-many-parent/qa36151-child-9/'
                        )
                    ).toBe(true);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({
                            path: '/qa36151-many-parent/',
                            per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE
                        })
                    );
                    done();
                },
                error: done
            });
        });

        it('should expose totalEntries so callers can append load-more', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [createFakeFolderSearchView({ path: '/' })],
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                        totalEntries: FOLDER_TREE_HIERARCHY_PAGE_SIZE + 10
                    }
                })
            );

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0].totalEntries).toBe(FOLDER_TREE_HIERARCHY_PAGE_SIZE + 10);
                    expect(levels[0].path).toBe('/');
                    done();
                },
                error: done
            });
        });

        it('should propagate service errors', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: () => done(new Error('Should have thrown an error')),
                error: (error) => {
                    expect(error.message).toBe('Service error');
                    done();
                }
            });
        });

        describe('deep-link ancestor pinning', () => {
            const page = (names: string[], parentPath: string, total: number) =>
                of({
                    folders: names.map((name) =>
                        createFakeFolderSearchView({ id: `id-${name}`, name, path: parentPath })
                    ),
                    pagination: { totalEntries: total } as DotPagination
                });

            it('should pin an ancestor that sorts past the first page to the top of its level', (done) => {
                mockDotFolderService.searchFolders.mockImplementation(({ path, name }) =>
                    path === '/'
                        ? name
                            ? page(['zzz'], '/', 1)
                            : page(['a-one', 'a-two'], '/', 253)
                        : page([], path, 0)
                );

                getFolderHierarchyByPath('/zzz/', SITE, mockDotFolderService).subscribe({
                    next: (levels) => {
                        // Top of the level, not appended after its siblings.
                        expect(levels[0].folders.map(({ path }) => path)).toEqual([
                            '/zzz/',
                            '/a-one/',
                            '/a-two/'
                        ]);
                        done();
                    },
                    error: done
                });
            });

            it('should pin a nested ancestor into its own level, leaving the root level alone', (done) => {
                mockDotFolderService.searchFolders.mockImplementation(({ path, name }) => {
                    if (path === '/') {
                        return page(['parent'], '/', 1);
                    }

                    if (path === '/parent/') {
                        return name
                            ? page(['zzz'], '/parent/', 1)
                            : page(['a-one'], '/parent/', 253);
                    }

                    return page([], path, 0);
                });

                getFolderHierarchyByPath('/parent/zzz/', SITE, mockDotFolderService).subscribe({
                    next: (levels) => {
                        expect(levels[0].folders.map(({ path }) => path)).toEqual(['/parent/']);
                        expect(levels[1].folders.map(({ path }) => path)).toEqual([
                            '/parent/zzz/',
                            '/parent/a-one/'
                        ]);
                        done();
                    },
                    error: done
                });
            });

            it('should not look the ancestor up when it is already on the first page', (done) => {
                mockDotFolderService.searchFolders.mockImplementation(({ path }) =>
                    path === '/' ? page(['zzz'], '/', 1) : page([], path, 0)
                );

                getFolderHierarchyByPath('/zzz/', SITE, mockDotFolderService).subscribe({
                    next: () => {
                        // One request per level ('/' and '/zzz/'), no follow-up lookup.
                        expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(2);
                        done();
                    },
                    error: done
                });
            });

            it('should leave the level untouched when the ancestor cannot be resolved', (done) => {
                // What a folder the user cannot READ looks like: filtered out of every response,
                // never a 403. It must not be pinned, and the readable siblings must still render.
                mockDotFolderService.searchFolders.mockImplementation(({ path, name }) =>
                    path === '/' && !name ? page(['a-one'], '/', 253) : page([], path, 0)
                );

                getFolderHierarchyByPath('/secret/', SITE, mockDotFolderService).subscribe({
                    next: (levels) => {
                        expect(levels[0].folders.map(({ path }) => path)).toEqual(['/a-one/']);
                        done();
                    },
                    error: done
                });
            });

            it('should leave the tree standing when the pin request itself fails', (done) => {
                // The pin is a best-effort extra request inside a forkJoin. Letting a transient
                // failure through would reject the whole hierarchy load, which loadFolders turns
                // into an empty tree — costing every readable folder to save one pin.
                mockDotFolderService.searchFolders.mockImplementation(({ path, name }) => {
                    if (path === '/' && name) {
                        return throwError(() => new Error('Service error'));
                    }

                    return path === '/' ? page(['a-one'], '/', 253) : page([], path, 0);
                });

                getFolderHierarchyByPath('/zzz/', SITE, mockDotFolderService).subscribe({
                    next: (levels) => {
                        expect(levels[0].folders.map(({ path }) => path)).toEqual(['/a-one/']);
                        done();
                    },
                    error: () => done(new Error('Should not have rejected the hierarchy load'))
                });
            });

            it('should derive nextPage from folders fetched, not from the pinned node', (done) => {
                const fullPage = Array.from(
                    { length: FOLDER_TREE_HIERARCHY_PAGE_SIZE },
                    (_, i) => `folder-${String(i).padStart(3, '0')}`
                );

                mockDotFolderService.searchFolders.mockImplementation(({ path, name }) =>
                    path === '/'
                        ? name
                            ? page(['zzz'], '/', 1)
                            : page(fullPage, '/', FOLDER_TREE_HIERARCHY_PAGE_SIZE + 53)
                        : page([], path, 0)
                );

                getFolderHierarchyByPath('/zzz/', SITE, mockDotFolderService).subscribe({
                    next: (levels) => {
                        // 200 fetched / 40 per load-more page + 1. The pinned node brings the
                        // rendered count to 201, which must not shift the resume point.
                        expect(levels[0].folders).toHaveLength(FOLDER_TREE_HIERARCHY_PAGE_SIZE + 1);
                        expect(levels[0].nextPage).toBe(
                            FOLDER_TREE_HIERARCHY_PAGE_SIZE / FOLDER_TREE_PAGE_SIZE + 1
                        );
                        done();
                    },
                    error: done
                });
            });
        });
    });

    describe('getFolderNodesByPath', () => {
        let mockDotFolderService: jest.Mocked<DotFolderService>;
        const SITE_ID = 'site-123';
        const HOSTNAME = 'test.com';
        const SITE = createFakeSite({ identifier: SITE_ID, hostname: HOSTNAME });

        const searchResult = (folders: FolderSearchView[]) =>
            of({ folders, pagination: {} as DotPagination });

        beforeEach(() => {
            mockDotFolderService = {
                searchFolders: jest.fn().mockReturnValue(searchResult([]))
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should request the given page of children with the paged size', (done) => {
            const testPath = '/main/sub-folder/';

            getFolderNodesByPath(testPath, SITE, mockDotFolderService, 3).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({
                            siteId: SITE_ID,
                            path: testPath,
                            recursive: false,
                            page: 3,
                            per_page: FOLDER_TREE_PAGE_SIZE
                        })
                    );
                    done();
                },
                error: done
            });
        });

        it('should default to page 1', (done) => {
            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ page: 1 })
                    );
                    done();
                },
                error: done
            });
        });

        it('should transform child folders into tree nodes', (done) => {
            const firstChild = createFakeFolderSearchView({
                id: 'child-1',
                inode: 'inode-1',
                name: 'child1',
                path: '/main/sub-folder/',
                addChildrenAllowed: true,
                hasChildren: true,
                permissions: [PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]
            });

            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([
                    firstChild,
                    createFakeFolderSearchView({
                        id: 'child-2',
                        inode: 'inode-2',
                        name: 'child2',
                        path: '/main/sub-folder/',
                        addChildrenAllowed: false,
                        hasChildren: false
                    })
                ])
            );

            getFolderNodesByPath('/main/sub-folder/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toHaveLength(2);
                    expect(result.folders[0]).toEqual({
                        key: 'child-1',
                        label: '/main/sub-folder/child1/',
                        data: {
                            id: 'child-1',
                            inode: 'inode-1',
                            hostname: HOSTNAME,
                            path: '/main/sub-folder/child1/',
                            type: 'folder',
                            // Carried so a right-click can gate the menu and fill the edit dialog.
                            name: 'child1',
                            title: firstChild.title,
                            sortOrder: firstChild.sortOrder,
                            filesMasks: firstChild.filesMasks,
                            defaultFileType: firstChild.defaultFileType,
                            showOnMenu: firstChild.showOnMenu,
                            defaultBaseType: firstChild.defaultBaseType,
                            permissions: [PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]
                        },
                        // hasChildren: true → expandable (chevron shown)
                        leaf: false
                    });
                    expect(result.folders[1].key).toBe('child-2');
                    expect(result.folders[1].label).toBe('/main/sub-folder/child2/');
                    // hasChildren: false → no chevron, cannot expand
                    expect(result.folders[1].leaf).toBe(true);
                    done();
                },
                error: done
            });
        });

        it('should request permissions so an expanded node can gate its context menu', (done) => {
            getFolderNodesByPath('/main/sub-folder/', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ includePermissions: true })
                    );
                    done();
                },
                error: done
            });
        });

        it('should normalize a parent path that is missing its trailing slash', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([createFakeFolderSearchView({ id: 'x', name: 'sub', path: '/main' })])
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    const folder = result.folders[0];
                    const data = folder?.data;

                    // Guard before isTreeNodeContentData — `data` is optional on TreeNode.
                    if (!data || !isTreeNodeContentData(data)) {
                        done(new Error('Expected a content folder node with path data'));

                        return;
                    }

                    // '/main' (no trailing slash) + 'sub' must yield '/main/sub/', not '/mainsub/'
                    expect(data.path).toBe('/main/sub/');
                    expect(folder.label).toBe('/main/sub/');
                    done();
                },
                error: done
            });
        });

        it('should return an empty folders array when the level has no children', (done) => {
            getFolderNodesByPath('/main/empty/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toEqual([]);
                    done();
                },
                error: done
            });
        });

        it('should surface the level total so the caller can decide if more remain', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [createFakeFolderSearchView({ path: '/main/' })],
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_PAGE_SIZE,
                        totalEntries: 120
                    }
                })
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.folders).toHaveLength(1);
                    expect(result.totalEntries).toBe(120);
                    done();
                },
                error: done
            });
        });

        it('should propagate service errors', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                throwError(() => new Error('Service error'))
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: () => done(new Error('Should have thrown an error')),
                error: (error) => {
                    expect(error.message).toBe('Service error');
                    done();
                }
            });
        });
    });

    describe('buildLoadMoreNode', () => {
        it('should build a non-selectable leaf load-more node carrying the paging cursor', () => {
            const node = buildLoadMoreNode('/main/', 'test.com', 2, 75);

            expect(node).toEqual({
                key: 'load-more:/main/',
                label: '',
                type: 'load-more',
                data: {
                    type: 'load-more',
                    path: '/main/',
                    hostname: 'test.com',
                    id: 'load-more:/main/',
                    nextPage: 2,
                    remaining: 75
                },
                leaf: true,
                selectable: false
            });
        });

        it('should set node.type and data.type to the same load-more value', () => {
            const node = buildLoadMoreNode('/main/', 'test.com', 2, 75);

            expect(node.type).toBe('load-more');
            expect(node.data?.type).toBe('load-more');
            expect(node.type).toBe(node.data?.type);
        });
    });

    describe('applyLoadMoreToHierarchy', () => {
        it('should append a load-more sentinel resuming at the level own nextPage', () => {
            const rootFolder = createTreeNode({
                id: 'root-1',
                inode: 'inode-1',
                hostName: 'test.com',
                path: '/main/',
                addChildrenAllowed: true
            });

            const roots = applyLoadMoreToHierarchy(
                [rootFolder],
                [
                    {
                        path: '/',
                        folders: [
                            {
                                id: 'root-1',
                                inode: 'inode-1',
                                hostName: 'test.com',
                                path: '/main/',
                                addChildrenAllowed: true
                            }
                        ],
                        totalEntries: 50,
                        // The hierarchy pages at 200 while load-more pages at 40, so a level that
                        // consumed one hierarchy page resumes at 40-sized page 6, not page 2.
                        nextPage: 6
                    }
                ],
                'test.com'
            );

            const loadMore = roots[roots.length - 1];
            expect(loadMore.type).toBe('load-more');
            expect(loadMore.data).toEqual(
                expect.objectContaining({
                    type: 'load-more',
                    nextPage: 6,
                    remaining: 49
                })
            );
        });

        it('should not append load-more when the hierarchy page already has all entries', () => {
            const rootFolder = createTreeNode({
                id: 'root-1',
                inode: 'inode-1',
                hostName: 'test.com',
                path: '/main/',
                addChildrenAllowed: true
            });

            const roots = applyLoadMoreToHierarchy(
                [rootFolder],
                [
                    {
                        path: '/',
                        folders: [
                            {
                                id: 'root-1',
                                inode: 'inode-1',
                                hostName: 'test.com',
                                path: '/main/',
                                addChildrenAllowed: true
                            }
                        ],
                        totalEntries: 1
                    }
                ],
                'test.com'
            );

            expect(roots).toHaveLength(1);
            expect(roots[0].type).not.toBe('load-more');
        });
    });

    describe('isFolder', () => {
        it('should return true for a folder item', () => {
            const folderItem: DotContentDriveFolder = {
                __icon__: 'folderIcon',
                defaultFileType: '',
                description: '',
                extension: 'folder',
                filesMasks: '',
                hasTitleImage: false,
                hostId: 'host-123',
                iDate: 1234567890,
                identifier: 'folder-123',
                inode: 'inode-123',
                mimeType: 'folder',
                modDate: 1234567890,
                name: 'Test Folder',
                owner: 'admin',
                parent: '/',
                path: '/test-folder/',
                permissions: [],
                showOnMenu: true,
                sortOrder: 0,
                title: 'Test Folder',
                type: 'folder'
            };

            expect(isFolder(folderItem)).toBe(true);
        });

        it('should return false for a contentlet item', () => {
            const contentletItem: DotCMSContentlet = {
                identifier: 'content-123',
                title: 'Test Content',
                baseType: 'CONTENT',
                contentType: 'Blog'
            } as DotCMSContentlet;

            expect(isFolder(contentletItem)).toBe(false);
        });

        it('should return false for an item without type property', () => {
            const itemWithoutType = {
                identifier: 'item-123',
                title: 'Test Item'
            } as DotContentDriveItem;

            expect(isFolder(itemWithoutType)).toBe(false);
        });

        it('should return false for an item with type property but not "folder"', () => {
            const itemWithWrongType = {
                identifier: 'item-123',
                title: 'Test Item',
                type: 'content'
            } as unknown as DotContentDriveItem;

            expect(isFolder(itemWithWrongType)).toBe(false);
        });

        it('should work as a type guard', () => {
            const folderItem: DotContentDriveFolder = {
                __icon__: 'folderIcon',
                defaultFileType: '',
                description: '',
                extension: 'folder',
                filesMasks: '',
                hasTitleImage: false,
                hostId: 'host-123',
                iDate: 1234567890,
                identifier: 'folder-123',
                inode: 'inode-123',
                mimeType: 'folder',
                modDate: 1234567890,
                name: 'Test Folder',
                owner: 'admin',
                parent: '/',
                path: '/test-folder/',
                permissions: [],
                showOnMenu: true,
                sortOrder: 0,
                title: 'Test Folder',
                type: 'folder'
            };

            const item: DotContentDriveItem = folderItem;

            if (isFolder(item)) {
                // TypeScript should narrow the type here
                expect(item.type).toBe('folder');
                expect(item.extension).toBe('folder');
            } else {
                fail('Type guard should have narrowed to DotContentDriveFolder');
            }
        });

        it('should return false for null or undefined', () => {
            expect(isFolder(null as unknown as DotContentDriveItem)).toBe(false);
            expect(isFolder(undefined as unknown as DotContentDriveItem)).toBe(false);
        });
    });
});

describe('User-searchable field helpers', () => {
    describe('decodeFilters - us.* keys', () => {
        it('should keep a us.* value raw without comma-splitting', () => {
            const result = decodeFilters('us.publishDate:2024-01-01,2024-12-31');

            expect(result['us.publishDate']).toBe('2024-01-01,2024-12-31');
        });

        it('should not trim/split even when the raw value has spaces and commas', () => {
            const result = decodeFilters('us.summary:hello, world');

            expect(result['us.summary']).toBe('hello, world');
        });
    });

    describe('getUserSearchableActive', () => {
        it('should return the field variables of us.* keys, in order, ignoring other filters', () => {
            expect(
                getUserSearchableActive({ 'us.title': 'x', baseType: ['1'], 'us.tags': 'a,b' })
            ).toEqual(['title', 'tags']);
        });

        it('should return an empty array when there are no us.* keys', () => {
            expect(getUserSearchableActive({ contentType: ['Blog'] })).toEqual([]);
        });
    });

    describe('isDateFieldFilterType', () => {
        it('should be true for Date, Date-and-Time and Time', () => {
            expect(isDateFieldFilterType('Date')).toBe(true);
            expect(isDateFieldFilterType('Date-and-Time')).toBe(true);
            expect(isDateFieldFilterType('Time')).toBe(true);
        });

        it('should be false for non-date types', () => {
            expect(isDateFieldFilterType('Text')).toBe(false);
            expect(isDateFieldFilterType('Select')).toBe(false);
        });
    });

    describe('isMultiValueFieldFilterType', () => {
        it('should be true for the multi-value types', () => {
            expect(isMultiValueFieldFilterType('Multi-Select')).toBe(true);
            expect(isMultiValueFieldFilterType('Checkbox')).toBe(true);
            expect(isMultiValueFieldFilterType('Tag')).toBe(true);
            expect(isMultiValueFieldFilterType('Category')).toBe(true);
        });

        it('should be false for single-value types', () => {
            expect(isMultiValueFieldFilterType('Text')).toBe(false);
            expect(isMultiValueFieldFilterType('Select')).toBe(false);
            expect(isMultiValueFieldFilterType('Radio')).toBe(false);
            // Relationship is single-value (one related identifier).
            expect(isMultiValueFieldFilterType('Relationship')).toBe(false);
        });
    });

    describe('isBinaryCheckboxField', () => {
        it('should be true for a single-option checkbox', () => {
            expect(isBinaryCheckboxField(createFakeCheckboxField({ values: '|true' }))).toBe(true);
        });

        it('should be false for a multi-option checkbox', () => {
            expect(isBinaryCheckboxField(createFakeCheckboxField({ values: 'A|a\r\nB|b' }))).toBe(
                false
            );
        });

        it('should be false for non-checkbox fields', () => {
            expect(isBinaryCheckboxField(createFakeSelectField({ values: 'A|a' }))).toBe(false);
        });
    });

    describe('parseUserSearchableValue', () => {
        it('should return undefined for an empty raw value', () => {
            expect(parseUserSearchableValue('', 'Text')).toBeUndefined();
        });

        it('should return the raw string for text/select', () => {
            expect(parseUserSearchableValue('hello', 'Text')).toBe('hello');
            expect(parseUserSearchableValue('published', 'Select')).toBe('published');
        });

        it('should split multi-value types into an array', () => {
            expect(parseUserSearchableValue('a,b,c', 'Multi-Select')).toEqual(['a', 'b', 'c']);
        });

        it('should round-trip a multi-value value that contains the separator', () => {
            // A tag label like "News, Press" must survive serialize → parse intact.
            const stored = serializeUserSearchableValue(['News, Press', 'cms'], 'Tag');

            expect(stored).not.toContain('News, Press');
            expect(parseUserSearchableValue(stored, 'Tag')).toEqual(['News, Press', 'cms']);
        });

        it('should reshape date types into a from/to range', () => {
            expect(parseUserSearchableValue('2024-01-01,2024-12-31', 'Date')).toEqual({
                from: '2024-01-01',
                to: '2024-12-31'
            });
        });

        describe('Key-Value translation', () => {
            it('should join a key:value shorthand into a key_value term (exact pair)', () => {
                expect(parseUserSearchableValue('color:red', 'Key-Value')).toBe('color_red');
            });

            it('should trim around the colon', () => {
                expect(parseUserSearchableValue(' color : red ', 'Key-Value')).toBe('color_red');
            });

            it('should pass a bare term through (loose match on a key or value)', () => {
                expect(parseUserSearchableValue('red', 'Key-Value')).toBe('red');
            });

            it('should fall back to the filled side when only one is given', () => {
                expect(parseUserSearchableValue('color:', 'Key-Value')).toBe('color');
                expect(parseUserSearchableValue(':red', 'Key-Value')).toBe('red');
            });

            it('should split on the first colon only, keeping colons in the value', () => {
                // A keyed colon-bearing value (URL / time) is preserved after the first colon.
                expect(parseUserSearchableValue('link:https://x', 'Key-Value')).toBe(
                    'link_https://x'
                );
                expect(parseUserSearchableValue('start:12:30', 'Key-Value')).toBe('start_12:30');
            });

            it('should lowercase the term to match the lowercased .key_value index', () => {
                // The index stores (key + "_" + value).toLowerCase(); the FE-typed case must not
                // cause a miss.
                expect(parseUserSearchableValue('Color:Red', 'Key-Value')).toBe('color_red');
                expect(parseUserSearchableValue('COLOR_RED', 'Key-Value')).toBe('color_red');
                expect(parseUserSearchableValue('Blue', 'Key-Value')).toBe('blue');
            });

            it('should return undefined for an empty value', () => {
                expect(parseUserSearchableValue('', 'Key-Value')).toBeUndefined();
                expect(parseUserSearchableValue('   ', 'Key-Value')).toBeUndefined();
            });
        });
    });

    describe('serializeUserSearchableValue', () => {
        it('should serialize null/undefined to an empty string', () => {
            expect(serializeUserSearchableValue(null, 'Text')).toBe('');
            expect(serializeUserSearchableValue(undefined, 'Text')).toBe('');
        });

        it('should join a multi-value array with commas', () => {
            expect(serializeUserSearchableValue(['a', 'b'], 'Multi-Select')).toBe('a,b');
        });

        it('should serialize a date range to from,to', () => {
            expect(
                serializeUserSearchableValue({ from: '2024-01-01', to: '2024-12-31' }, 'Date')
            ).toBe('2024-01-01,2024-12-31');
        });

        it('should serialize an empty date range to an empty string', () => {
            expect(serializeUserSearchableValue({ from: '', to: '' }, 'Date')).toBe('');
        });

        it('should stringify a single value', () => {
            expect(serializeUserSearchableValue('published', 'Select')).toBe('published');
        });

        it('should return an empty string for a non-range value on a Date field', () => {
            // Mismatched fieldType/value (a string where a range is expected) must not produce a
            // misleading partial range.
            expect(serializeUserSearchableValue('not-a-range', 'Date')).toBe('');
            expect(serializeUserSearchableValue(['a', 'b'], 'Date')).toBe('');
        });
    });

    describe('buildUserSearchablePayload', () => {
        it('should return undefined when there are no us.* entries', () => {
            const payload = buildUserSearchablePayload({ contentType: ['Blog'] }, []);

            expect(payload).toBeUndefined();
        });

        it('should reshape each field value by its type', () => {
            const filters: DotContentDriveFilters = {
                'us.title': 'review',
                'us.tags': 'angular,cms',
                'us.postingDate': '2024-01-01,2024-12-31'
            };
            const fields = [
                createFakeTextField({ variable: 'title' }),
                createFakeTagField({ variable: 'tags' }),
                createFakeDateField({ variable: 'postingDate' })
            ];

            const payload = buildUserSearchablePayload(filters, fields);

            expect(payload).toEqual({
                title: 'review',
                tags: ['angular', 'cms'],
                postingDate: { from: '2024-01-01', to: '2024-12-31' }
            });
        });

        it('should emit a boolean for a binary checkbox and always include it', () => {
            const fields = [createFakeCheckboxField({ variable: 'featured', values: '|true' })];

            expect(buildUserSearchablePayload({ 'us.featured': 'true' }, fields)).toEqual({
                featured: true
            });
            expect(buildUserSearchablePayload({ 'us.featured': 'false' }, fields)).toEqual({
                featured: false
            });
        });

        it('should skip empty non-binary values and fields without loaded metadata', () => {
            const fields = [createFakeTextField({ variable: 'title' })];

            // us.title is empty, and us.unknown has no field metadata → both skipped.
            const payload = buildUserSearchablePayload(
                { 'us.title': '', 'us.unknown': 'x' },
                fields
            );

            expect(payload).toBeUndefined();
        });
    });
});

describe('folderSearchViewToDotFolder', () => {
    it('should carry defaultBaseType through to the DotFolder', () => {
        const view = createFakeFolderSearchView({
            id: 'f1',
            name: 'app',
            path: '/',
            defaultBaseType: 'DOTASSET'
        });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder.defaultBaseType).toBe('DOTASSET');
    });

    it('should leave defaultBaseType undefined when the view has no preference', () => {
        const view = createFakeFolderSearchView({ id: 'f2', name: 'docs', path: '/' });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder.defaultBaseType).toBeUndefined();
    });

    it('should carry the fields the Edit-folder dialog reads', () => {
        const view = createFakeFolderSearchView({
            name: 'docs',
            path: '/',
            title: 'Documents',
            sortOrder: 3,
            filesMasks: '*.pdf,*.docx',
            defaultFileType: 'FileAsset',
            showOnMenu: true
        });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder).toEqual(
            expect.objectContaining({
                name: 'docs',
                title: 'Documents',
                sortOrder: 3,
                filesMasks: '*.pdf,*.docx',
                defaultFileType: 'FileAsset',
                showOnMenu: true
            })
        );
    });

    it('should carry granted permissions through unchanged', () => {
        const view = createFakeFolderSearchView({
            name: 'docs',
            path: '/',
            permissions: [PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]
        });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder.permissions).toEqual([PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]);
    });

    it('should keep an empty permissions array as a resolved "no grants" answer', () => {
        const view = createFakeFolderSearchView({ name: 'docs', path: '/', permissions: [] });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder.permissions).toEqual([]);
    });

    it('should turn a null permissions response into undefined ("not resolved")', () => {
        // The distinction matters: `[]` is final, `undefined` makes the sidebar resolve them on
        // demand before opening the context menu.
        const view = createFakeFolderSearchView({ name: 'docs', path: '/', permissions: null });

        const folder = folderSearchViewToDotFolder(view, 'demo.dotcms.com');

        expect(folder.permissions).toBeUndefined();
    });
});

describe('getPathLeafName', () => {
    it.each([
        ['/a/b/', 'b'],
        ['/a/b', 'b'],
        ['/b/', 'b'],
        ['/', ''],
        ['', '']
    ])('should resolve the own name of %s as %s', (path, expected) => {
        expect(getPathLeafName(path)).toBe(expected);
    });
});

describe('resolveHierarchyAncestor', () => {
    let mockDotFolderService: { searchFolders: jest.Mock };

    const searchResult = (folders: FolderSearchView[]) =>
        of({ folders, pagination: { totalEntries: folders.length } as DotPagination });

    beforeEach(() => {
        mockDotFolderService = { searchFolders: jest.fn().mockReturnValue(searchResult([])) };
    });

    it('should query the level with permissions, narrowed by the folder own name', (done) => {
        resolveHierarchyAncestor(
            '/main/',
            '/main/docs/',
            createFakeSite({ identifier: 'site-1', hostname: 'demo.dotcms.com' }),
            mockDotFolderService as unknown as DotFolderService
        ).subscribe({
            next: () => {
                expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                    expect.objectContaining({
                        siteId: 'site-1',
                        path: '/main/',
                        recursive: false,
                        name: 'docs',
                        per_page: FOLDER_TREE_HIERARCHY_PAGE_SIZE,
                        includePermissions: true
                    })
                );
                done();
            },
            error: done
        });
    });

    it('should omit the name filter when the folder name is too short for the endpoint', (done) => {
        resolveHierarchyAncestor(
            '/main/',
            '/main/a/',
            createFakeSite({ identifier: 'site-1' }),
            mockDotFolderService as unknown as DotFolderService
        ).subscribe({
            next: () => {
                expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                    expect.objectContaining({ name: undefined })
                );
                done();
            },
            error: done
        });
    });

    it('should return the folder matching the exact path, not a partial name match', (done) => {
        mockDotFolderService.searchFolders.mockReturnValue(
            searchResult([
                createFakeFolderSearchView({
                    id: 'other',
                    path: '/main/',
                    name: 'docs-archive',
                    permissions: [PERMISSIONS_TYPE.READ]
                }),
                createFakeFolderSearchView({
                    id: 'docs-id',
                    path: '/main/',
                    name: 'docs',
                    permissions: [PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]
                })
            ])
        );

        resolveHierarchyAncestor(
            '/main/',
            '/main/docs/',
            createFakeSite({ identifier: 'site-1', hostname: 'demo.dotcms.com' }),
            mockDotFolderService as unknown as DotFolderService
        ).subscribe({
            next: (folder) => {
                expect(folder?.id).toBe('docs-id');
                expect(folder?.permissions).toEqual([PERMISSIONS_TYPE.READ, PERMISSIONS_TYPE.EDIT]);
                done();
            },
            error: done
        });
    });

    it('should issue a single request and not page the level', (done) => {
        mockDotFolderService.searchFolders.mockReturnValue(
            of({
                folders: [createFakeFolderSearchView({ path: '/main/', name: 'someone-else' })],
                pagination: { totalEntries: 5000 } as DotPagination
            })
        );

        resolveHierarchyAncestor(
            '/main/',
            '/main/docs/',
            createFakeSite({ identifier: 'site-1', hostname: 'demo.dotcms.com' }),
            mockDotFolderService as unknown as DotFolderService
        ).subscribe({
            next: (folder) => {
                expect(folder).toBeUndefined();
                expect(mockDotFolderService.searchFolders).toHaveBeenCalledTimes(1);
                done();
            },
            error: done
        });
    });
});

describe('mergeFolderNodePage', () => {
    const node = (id: string, path: string) =>
        createTreeNode({
            id,
            inode: `inode-${id}`,
            hostName: 'test.com',
            path,
            addChildrenAllowed: true
        });

    const ids = (nodes: ReturnType<typeof node>[]) => nodes.map((item) => item.data?.id);

    it('should append the page when nothing overlaps', () => {
        const merged = mergeFolderNodePage([node('a', '/a/')], [node('b', '/b/')]);

        expect(ids(merged)).toEqual(['a', 'b']);
    });

    it('should render a folder once when the page repeats one already on screen', () => {
        // The hierarchy pinned `z` to the top; paging far enough returns it in sort order.
        const merged = mergeFolderNodePage(
            [node('z', '/z/'), node('a', '/a/')],
            [node('b', '/b/'), node('z', '/z/')]
        );

        expect(ids(merged)).toEqual(['a', 'b', 'z']);
    });

    it('should move the repeated folder from its pinned slot to where it belongs', () => {
        const merged = mergeFolderNodePage(
            [node('z', '/z/'), node('a', '/a/')],
            [node('z', '/z/'), node('b', '/b/')]
        );

        expect(ids(merged)).toEqual(['a', 'z', 'b']);
    });

    it('should keep the on-screen node, so its loaded children and expansion survive', () => {
        const pinned = node('z', '/z/');
        pinned.expanded = true;
        pinned.children = [node('inner', '/z/inner/')];

        const merged = mergeFolderNodePage([pinned, node('a', '/a/')], [node('z', '/z/')]);
        const retained = merged[merged.length - 1];

        // Identity matters: the incoming copy is a bare node with no children or expansion.
        expect(retained).toBe(pinned);
        expect(retained.expanded).toBe(true);
        expect(retained.children).toHaveLength(1);
    });

    it('should leave a load-more sentinel in place rather than treating it as a folder', () => {
        const loadMore = buildLoadMoreNode('/', 'test.com', 2, 5);

        const merged = mergeFolderNodePage([node('a', '/a/'), loadMore], [node('b', '/b/')]);

        expect(ids(merged)).toEqual(['a', loadMore.data?.id, 'b']);
    });
});
