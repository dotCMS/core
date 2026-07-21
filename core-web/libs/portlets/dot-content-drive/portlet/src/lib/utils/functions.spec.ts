import { describe, it, expect } from '@jest/globals';
import { of, throwError } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import {
    DotContentDriveFolder,
    DotContentDriveItem,
    DotCMSContentlet,
    DotPagination,
    FolderSearchView
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
    buildLoadMoreNode,
    decodeFilters,
    encodeFilters,
    decodeByFilterKey,
    getFolderHierarchyByPath,
    getFolderNodesByPath,
    isFolder,
    parseWorkflowToken,
    workflowEntryToToken,
    parseWorkflowFilter,
    isDateFieldFilterType,
    isMultiValueFieldFilterType,
    isBinaryCheckboxField,
    parseUserSearchableValue,
    serializeUserSearchableValue,
    buildUserSearchablePayload,
    getUserSearchableActive
} from './functions';

import { FOLDER_TREE_PAGE_SIZE, FOLDER_TREE_SEARCH_PAGE_SIZE } from '../shared/constants';
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

        it('should handle empty arrays by encoding them', () => {
            const result = encodeFilters({ contentType: [], baseType: ['1'] });
            // Empty arrays are encoded as "key:" (empty value after colon) since join(',') on empty array returns ''
            expect(result).toBe('contentType:;baseType:1');
        });

        it('should handle filters with null or undefined values', () => {
            const result = encodeFilters({
                contentType: ['Blog'],
                status: undefined,
                title: null as unknown as string
            });
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
                status: 'published',
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

        it('should search the root and every parent path with an uncapped page size', (done) => {
            const folderPath = '/main/sub-folder/inner-folder';

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
                                per_page: FOLDER_TREE_SEARCH_PAGE_SIZE
                            })
                        );
                    });
                    done();
                },
                error: done
            });
        });

        it('should adapt search results into DotFolder full paths with the site hostname', (done) => {
            mockDotFolderService.searchFolders.mockReturnValueOnce(
                searchResult([
                    createFakeFolderSearchView({
                        id: 'm',
                        inode: 'im',
                        name: 'main',
                        path: '/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    })
                ])
            );

            getFolderHierarchyByPath('/main', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0][0]).toEqual({
                        id: 'm',
                        inode: 'im',
                        hostName: HOSTNAME,
                        path: '/main/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    });
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

        it('should return every folder in a level without a 40-item cap', (done) => {
            const many = Array.from({ length: 45 }, (_, i) =>
                createFakeFolderSearchView({ id: `f${i}`, name: `folder-${i}`, path: '/' })
            );
            mockDotFolderService.searchFolders.mockReturnValue(searchResult(many));

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: (levels) => {
                    expect(levels[0]).toHaveLength(45);
                    expect(mockDotFolderService.searchFolders).toHaveBeenCalledWith(
                        expect.objectContaining({ per_page: FOLDER_TREE_SEARCH_PAGE_SIZE })
                    );
                    done();
                },
                error: done
            });
        });

        it('should warn (not silently truncate) when a level exceeds the page size', (done) => {
            const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => undefined);
            mockDotFolderService.searchFolders.mockReturnValue(
                of({
                    folders: [createFakeFolderSearchView({ path: '/' })],
                    pagination: {
                        currentPage: 1,
                        perPage: FOLDER_TREE_SEARCH_PAGE_SIZE,
                        totalEntries: FOLDER_TREE_SEARCH_PAGE_SIZE + 1
                    }
                })
            );

            getFolderHierarchyByPath('/', SITE, mockDotFolderService).subscribe({
                next: () => {
                    expect(warnSpy).toHaveBeenCalledWith(
                        expect.stringContaining(String(FOLDER_TREE_SEARCH_PAGE_SIZE + 1))
                    );
                    warnSpy.mockRestore();
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
            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([
                    createFakeFolderSearchView({
                        id: 'child-1',
                        inode: 'inode-1',
                        name: 'child1',
                        path: '/main/sub-folder/',
                        addChildrenAllowed: true,
                        hasChildren: true
                    }),
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
                            type: 'folder'
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

        it('should normalize a parent path that is missing its trailing slash', (done) => {
            mockDotFolderService.searchFolders.mockReturnValue(
                searchResult([createFakeFolderSearchView({ id: 'x', name: 'sub', path: '/main' })])
            );

            getFolderNodesByPath('/main/', SITE, mockDotFolderService).subscribe({
                next: (result) => {
                    // '/main' (no trailing slash) + 'sub' must yield '/main/sub/', not '/mainsub/'
                    expect(result.folders[0].data.path).toBe('/main/sub/');
                    expect(result.folders[0].label).toBe('/main/sub/');
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
                label: 'content-drive.tree.load-more',
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
