import { describe, it, expect } from '@jest/globals';
import { of, Observable } from 'rxjs';

import { DotFolderService } from '@dotcms/data-access';
import {
    DotFolder,
    DotContentDriveFolder,
    DotContentDriveItem,
    DotCMSContentlet,
    DotCMSContentTypeField
} from '@dotcms/dotcms-models';

import {
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
    buildUserSearchablePayload
} from './functions';

import { DotContentDriveFilters } from '../shared/models';

/** Minimal content-type field fixture for the user-searchable helpers. */
const field = (overrides: Partial<DotCMSContentTypeField> = {}): DotCMSContentTypeField =>
    ({
        variable: 'aField',
        fieldType: 'Text',
        dataType: 'TEXT',
        values: '',
        ...overrides
    }) as DotCMSContentTypeField;

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

        beforeEach(() => {
            mockDotFolderService = {
                getFolders: jest.fn()
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should fetch folders for all parent paths', (done) => {
            const testPath = '/main/sub-folder/inner-folder';
            const mockFolders1: DotFolder[] = [
                { id: '1', hostName: 'test.com', path: '/main/', addChildrenAllowed: true }
            ];
            const mockFolders2: DotFolder[] = [
                {
                    id: '2',
                    hostName: 'test.com',
                    path: '/main/sub-folder/',
                    addChildrenAllowed: true
                }
            ];
            const mockFolders3: DotFolder[] = [
                {
                    id: '3',
                    hostName: 'test.com',
                    path: '/main/sub-folder/inner-folder/',
                    addChildrenAllowed: true
                }
            ];

            // Mock the service to return different folders for each path
            mockDotFolderService.getFolders
                .mockReturnValueOnce(of(mockFolders1))
                .mockReturnValueOnce(of(mockFolders2))
                .mockReturnValueOnce(of(mockFolders3));

            getFolderHierarchyByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result).toEqual([mockFolders1, mockFolders2, mockFolders3]);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledTimes(3);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith('/main/');
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith(
                        '/main/sub-folder/'
                    );
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith(
                        '/main/sub-folder/inner-folder/'
                    );
                    done();
                },
                error: done
            });
        });

        it('should handle single level path', (done) => {
            const testPath = '/main';
            const mockFolders: DotFolder[] = [
                { id: '1', hostName: 'test.com', path: '/main/', addChildrenAllowed: true }
            ];

            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderHierarchyByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result).toEqual([mockFolders]);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledTimes(1);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith('/main/');
                    done();
                },
                error: done
            });
        });

        it('should handle service errors gracefully', (done) => {
            const testPath = '/main';
            const errorMessage = 'Service error';

            mockDotFolderService.getFolders.mockReturnValue(
                new Observable((observer) => observer.error(new Error(errorMessage)))
            );

            getFolderHierarchyByPath(testPath, mockDotFolderService).subscribe({
                next: () => {
                    done(new Error('Should have thrown an error'));
                },
                error: (error) => {
                    expect(error.message).toBe(errorMessage);
                    done();
                }
            });
        });

        it('should handle root path', (done) => {
            const testPath = '/';
            // Root path has no parent paths, so generateAllParentPaths returns empty array
            // and the function should return empty array without calling service

            getFolderHierarchyByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result).toEqual([]);
                    expect(mockDotFolderService.getFolders).not.toHaveBeenCalled();
                    done();
                },
                error: done
            });
        });

        it('should handle empty path', (done) => {
            const testPath = '';
            const mockFolders: DotFolder[] = [];

            // Empty path should result in no paths generated, so no service calls
            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderHierarchyByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    // Empty path generates no parent paths, so result should be empty array
                    expect(result).toEqual([]);
                    expect(mockDotFolderService.getFolders).not.toHaveBeenCalled();
                    done();
                },
                error: done
            });
        });
    });

    describe('getFolderNodesByPath', () => {
        let mockDotFolderService: jest.Mocked<DotFolderService>;

        beforeEach(() => {
            mockDotFolderService = {
                getFolders: jest.fn()
            } as unknown as jest.Mocked<DotFolderService>;
        });

        it('should return parent folder and child tree nodes', (done) => {
            const testPath = '/main/sub-folder';
            const mockParentFolder: DotFolder = {
                id: 'parent-1',
                hostName: 'test.com',
                path: '/main/sub-folder/',
                addChildrenAllowed: true
            };
            const mockChildFolder1: DotFolder = {
                id: 'child-1',
                hostName: 'test.com',
                path: '/main/sub-folder/child1/',
                addChildrenAllowed: true
            };
            const mockChildFolder2: DotFolder = {
                id: 'child-2',
                hostName: 'test.com',
                path: '/main/sub-folder/child2/',
                addChildrenAllowed: false
            };

            const mockFolders = [mockParentFolder, mockChildFolder1, mockChildFolder2];
            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.parent).toEqual(mockParentFolder);
                    expect(result.folders).toHaveLength(2);

                    // Verify first child tree node
                    expect(result.folders[0]).toEqual({
                        key: 'child-1',
                        label: '/main/sub-folder/child1/',
                        data: {
                            id: 'child-1',
                            hostname: 'test.com',
                            path: '/main/sub-folder/child1/',
                            type: 'folder'
                        },
                        leaf: false
                    });

                    // Verify second child tree node
                    expect(result.folders[1]).toEqual({
                        key: 'child-2',
                        label: '/main/sub-folder/child2/',
                        data: {
                            id: 'child-2',
                            hostname: 'test.com',
                            path: '/main/sub-folder/child2/',
                            type: 'folder'
                        },
                        leaf: false
                    });

                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith(testPath);
                    done();
                },
                error: done
            });
        });

        it('should handle folder with no children', (done) => {
            const testPath = '/main/empty-folder';
            const mockParentFolder: DotFolder = {
                id: 'parent-1',
                hostName: 'test.com',
                path: '/main/empty-folder/',
                addChildrenAllowed: true
            };

            const mockFolders = [mockParentFolder]; // Only parent, no children
            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.parent).toEqual(mockParentFolder);
                    expect(result.folders).toEqual([]);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith(testPath);
                    done();
                },
                error: done
            });
        });

        it('should handle empty folder response', (done) => {
            const testPath = '/non-existent';
            const mockFolders: DotFolder[] = [];

            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.parent).toBeUndefined();
                    expect(result.folders).toEqual([]);
                    expect(mockDotFolderService.getFolders).toHaveBeenCalledWith(testPath);
                    done();
                },
                error: done
            });
        });

        it('should handle service errors gracefully', (done) => {
            const testPath = '/main';
            const errorMessage = 'Service error';

            mockDotFolderService.getFolders.mockReturnValue(
                new Observable((observer) => observer.error(new Error(errorMessage)))
            );

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: () => {
                    done(new Error('Should have thrown an error'));
                },
                error: (error) => {
                    expect(error.message).toBe(errorMessage);
                    done();
                }
            });
        });

        it('should handle root path', (done) => {
            const testPath = '/';
            const mockRootFolder: DotFolder = {
                id: 'root',
                hostName: 'test.com',
                path: '/',
                addChildrenAllowed: true
            };
            const mockChildFolder: DotFolder = {
                id: 'child-1',
                hostName: 'test.com',
                path: '/child1/',
                addChildrenAllowed: true
            };

            const mockFolders = [mockRootFolder, mockChildFolder];
            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.parent).toEqual(mockRootFolder);
                    expect(result.folders).toHaveLength(1);
                    expect(result.folders[0].key).toBe('child-1');
                    done();
                },
                error: done
            });
        });

        it('should transform folders with correct tree node structure', (done) => {
            const testPath = '/test';
            const mockParentFolder: DotFolder = {
                id: 'parent-123',
                hostName: 'example.com',
                path: '/test/',
                addChildrenAllowed: true
            };
            const mockChildFolder: DotFolder = {
                id: 'child-456',
                hostName: 'example.com',
                path: '/test/subfolder/',
                addChildrenAllowed: false
            };

            const mockFolders = [mockParentFolder, mockChildFolder];
            mockDotFolderService.getFolders.mockReturnValue(of(mockFolders));

            getFolderNodesByPath(testPath, mockDotFolderService).subscribe({
                next: (result) => {
                    expect(result.parent).toEqual(mockParentFolder);
                    expect(result.folders).toHaveLength(1);

                    const treeNode = result.folders[0];
                    expect(treeNode.key).toBe('child-456');
                    expect(treeNode.label).toBe('/test/subfolder/');
                    expect(treeNode.leaf).toBe(false);
                    expect(treeNode.data).toEqual({
                        id: 'child-456',
                        hostname: 'example.com',
                        path: '/test/subfolder/',
                        type: 'folder'
                    });
                    done();
                },
                error: done
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
            expect(isBinaryCheckboxField(field({ fieldType: 'Checkbox', values: '|true' }))).toBe(
                true
            );
        });

        it('should be false for a multi-option checkbox', () => {
            expect(
                isBinaryCheckboxField(field({ fieldType: 'Checkbox', values: 'A|a\r\nB|b' }))
            ).toBe(false);
        });

        it('should be false for non-checkbox fields', () => {
            expect(isBinaryCheckboxField(field({ fieldType: 'Select', values: 'A|a' }))).toBe(
                false
            );
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
                field({ variable: 'title', fieldType: 'Text' }),
                field({ variable: 'tags', fieldType: 'Tag' }),
                field({ variable: 'postingDate', fieldType: 'Date' })
            ];

            const payload = buildUserSearchablePayload(filters, fields);

            expect(payload).toEqual({
                title: 'review',
                tags: ['angular', 'cms'],
                postingDate: { from: '2024-01-01', to: '2024-12-31' }
            });
        });

        it('should emit a boolean for a binary checkbox and always include it', () => {
            const fields = [
                field({ variable: 'featured', fieldType: 'Checkbox', values: '|true' })
            ];

            expect(buildUserSearchablePayload({ 'us.featured': 'true' }, fields)).toEqual({
                featured: true
            });
            expect(buildUserSearchablePayload({ 'us.featured': 'false' }, fields)).toEqual({
                featured: false
            });
        });

        it('should skip empty non-binary values and fields without loaded metadata', () => {
            const fields = [field({ variable: 'title', fieldType: 'Text' })];

            // us.title is empty, and us.unknown has no field metadata → both skipped.
            const payload = buildUserSearchablePayload(
                { 'us.title': '', 'us.unknown': 'x' },
                fields
            );

            expect(payload).toBeUndefined();
        });
    });
});
