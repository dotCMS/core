import { Mock, vi } from 'vitest';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSFieldTypes
} from '@dotcms/dotcms-models';
import {
    createFakeBlockEditorField,
    createFakeCategoryField,
    createFakeCheckboxField,
    createFakeContentlet,
    createFakeDateField,
    createFakeHostFolderField,
    createFakeKeyValueField,
    createFakeLineDividerField,
    createFakeMultiSelectField,
    createFakeRelationshipField,
    createFakeSelectField,
    createFakeTextAreaField,
    createFakeTextField
} from '@dotcms/utils-testing';

import { resolutionValue, ResolvedFormValue } from './dot-edit-content-form-resolutions';

import { attachOrderedFields, parsePreservingKeyOrder } from '../../utils/key-value-order.util';
import { getRelationshipFromContentlet } from '../../utils/relationshipFromContentlet';

vi.mock('../../utils/relationshipFromContentlet', () => ({
    getRelationshipFromContentlet: vi.fn()
}));

// Ensure resolutionValue is properly initialized before each test
let originalResolutionValue: typeof resolutionValue;

describe('DotEditContentFormResolutions', () => {
    // Shared across the arms below. `clazz`, `dataType` and `fieldType` are deliberately absent:
    // each arm fixes all three together, and it was a single flat mock declaring them as
    // lower-case 'text' that let one field stand in for every resolver.
    const MOCK_FIELD_OVERRIDES = {
        variable: 'testField',
        defaultValue: 'default value',
        contentTypeId: 'test-content-type',
        id: 'test-field-id',
        name: 'Test Field',
        iDate: 1710892800000, // 2024-03-20T00:00:00.000Z
        modDate: 1710892800000 // 2024-03-20T00:00:00.000Z
    };

    const mockTextField = createFakeTextField(MOCK_FIELD_OVERRIDES);
    const mockTextAreaField = createFakeTextAreaField(MOCK_FIELD_OVERRIDES);
    const mockHostFolderField = createFakeHostFolderField(MOCK_FIELD_OVERRIDES);
    const mockCategoryField = createFakeCategoryField(MOCK_FIELD_OVERRIDES);
    const mockRelationshipField = createFakeRelationshipField(MOCK_FIELD_OVERRIDES);
    const mockLineDividerField = createFakeLineDividerField(MOCK_FIELD_OVERRIDES);

    const mockContentlet: DotCMSContentlet = {
        testField: 'test value',
        hostName: 'https://example.com',
        url: '/content/test',
        archived: false,
        baseType: 'CONTENT',
        contentType: 'test-content-type',
        folder: 'test-folder',
        hasTitleImage: false,
        host: 'test-host',
        identifier: 'test-identifier',
        inode: 'test-inode',
        languageId: 1,
        live: true,
        locked: false,
        modDate: '2024-03-20T00:00:00.000Z',
        modUser: 'test-user',
        modUserName: 'Test User',
        owner: 'test-owner',
        publishDate: '2024-03-20T00:00:00.000Z',
        sortOrder: 0,
        stInode: 'test-st-inode',
        title: 'Test Title',
        titleImage: 'test-image.jpg',
        working: true
    };

    beforeAll(() => {
        // Capture the original resolutionValue state
        originalResolutionValue = { ...resolutionValue };
    });

    beforeEach(() => {
        vi.clearAllMocks();

        // Restore resolutionValue to its original state before each test. This prevents test
        // contamination from other tests. Copied wholesale rather than key by key: the map is
        // keyed by the field-type discriminant, so a `string` cannot index it.
        Object.assign(resolutionValue, originalResolutionValue);
    });

    describe('defaultResolutionFn', () => {
        it('should return field value from contentlet', () => {
            const result = resolutionValue[DotCMSFieldTypes.TEXTAREA](
                mockContentlet,
                mockTextAreaField
            );
            expect(result).toBe('test value');
        });

        it('should return defaultValue when field value is not in contentlet', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.testField;

            const result = resolutionValue[DotCMSFieldTypes.TEXTAREA](
                contentlet,
                mockTextAreaField
            );
            expect(result).toBe('default value');
        });

        it('should return defaultValue when contentlet is null', () => {
            const result = resolutionValue[DotCMSFieldTypes.TEXTAREA](null, mockTextAreaField);
            expect(result).toBe('default value');
        });

        it('should return null when contentlet is null and isManualTranslation is true', () => {
            const result = resolutionValue[DotCMSFieldTypes.TEXTAREA](
                null,
                mockTextAreaField,
                undefined,
                true
            );
            expect(result).toBeNull();
        });
    });

    // A Checkbox or Multi-Select the user cleared comes back from the API with its key absent.
    // On saved content that absence means "nothing selected", so the Content Type default must
    // not be re-applied — otherwise the box re-checks itself and a save writes the default back.
    // See https://github.com/dotCMS/core/issues/35416
    describe('selectionResolutionFn', () => {
        // One fixture per arm, each from its own factory, so a Checkbox never carries another
        // type's clazz or dataType — castResolvedValue dispatches on fieldType. Each resolver is
        // bound to its own field so the call is checked against that arm, not the union.
        const selectionOverrides = {
            ...MOCK_FIELD_OVERRIDES,
            variable: 'showOnMenu',
            defaultValue: 'true'
        };
        const checkboxField = createFakeCheckboxField(selectionOverrides);
        const multiSelectField = createFakeMultiSelectField(selectionOverrides);

        const SELECTION_CASES: ReadonlyArray<{
            fieldType: string;
            variable: string;
            resolve: (
                contentlet: DotCMSContentlet | null,
                isManualTranslation?: boolean
            ) => ResolvedFormValue;
        }> = [
            {
                fieldType: checkboxField.fieldType,
                variable: checkboxField.variable,
                resolve: (contentlet, isManualTranslation) =>
                    resolutionValue[DotCMSFieldTypes.CHECKBOX](
                        contentlet,
                        checkboxField,
                        undefined,
                        isManualTranslation
                    )
            },
            {
                fieldType: multiSelectField.fieldType,
                variable: multiSelectField.variable,
                resolve: (contentlet, isManualTranslation) =>
                    resolutionValue[DotCMSFieldTypes.MULTI_SELECT](
                        contentlet,
                        multiSelectField,
                        undefined,
                        isManualTranslation
                    )
            }
        ];

        describe.each(SELECTION_CASES)('$fieldType', ({ variable, resolve }) => {
            // null, not '': castResolvedValue flattens these types with split(','), and ''
            // would become [''] — length 1, which Validators.required accepts.
            it('should return null when the key is absent on a saved contentlet', () => {
                const contentlet = createFakeContentlet({ identifier: 'saved-123' });
                delete contentlet[variable];
                // createFakeContentlet does not set this key, so the delete is defensive —
                // assert it so the test fails loudly if the fake ever grows one.
                expect(contentlet[variable]).toBeUndefined();

                expect(resolve(contentlet)).toBeNull();
            });

            it('should return the stored value when the contentlet has one', () => {
                const contentlet = createFakeContentlet({
                    identifier: 'saved-123',
                    [variable]: 'true'
                });

                expect(resolve(contentlet)).toBe('true');
            });

            it('should return the defaultValue for new content', () => {
                expect(resolve(null)).toBe('true');
            });

            it('should return null for new content during manual translation', () => {
                expect(resolve(null, true)).toBeNull();
            });
        });
    });

    describe('textFieldResolutionFn', () => {
        it('should not modify non-URL values', () => {
            const contentlet = {
                ...mockContentlet,
                testField: 'test-value'
            };

            const result = resolutionValue[DotCMSFieldTypes.TEXT](contentlet, mockTextField);
            expect(result).toBe('test-value');
        });

        it('should return defaultValue when contentlet is null', () => {
            const result = resolutionValue[DotCMSFieldTypes.TEXT](null, mockTextField);
            expect(result).toBe('default value');
        });

        it('should return null when contentlet is null and isManualTranslation is true', () => {
            const result = resolutionValue[DotCMSFieldTypes.TEXT](
                null,
                mockTextField,
                undefined,
                true
            );
            expect(result).toBeNull();
        });

        it('should remove leading slash from URL field in HTMLPAGE content', () => {
            const contentlet = {
                ...mockContentlet,
                baseType: 'HTMLPAGE',
                url: '/test-url'
            };
            const urlField = { ...mockTextField, variable: 'url' };

            const result = resolutionValue[DotCMSFieldTypes.TEXT](contentlet, urlField);
            expect(result).toBe('test-url');
        });

        it('should NOT remove leading slash from URL field in non-HTMLPAGE content', () => {
            const contentlet = {
                ...mockContentlet,
                myVariable: '/content-url'
            };
            const urlField = { ...mockTextField, variable: 'myVariable' };

            const result = resolutionValue[DotCMSFieldTypes.TEXT](contentlet, urlField);
            expect(result).toBe('/content-url');
        });
    });

    describe('hostFolderResolutionFn', () => {
        beforeEach(() => {
            // Ensure the resolution function exists before each test in this describe block
            expect(resolutionValue[DotCMSFieldTypes.HOST_FOLDER]).toBeDefined();
        });

        it('should construct host folder path from hostName and url for non-file assets', () => {
            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                mockContentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com');
        });

        it('should handle file assets by removing filename from path', () => {
            const contentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                hostName: 'https://example.com',
                url: '/path/to/file.jpg'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com/path/to');
        });

        it('should handle file assets with single segment path', () => {
            const contentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                hostName: 'https://example.com',
                url: '/file.jpg'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com');
        });

        it('should handle HTMLPAGE by removing last path segment to get directory', () => {
            const contentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                hostName: 'https://example.com',
                url: '/content/blog/my-page'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com/content/blog');
        });

        it('should handle HTMLPAGE with single segment path', () => {
            const contentlet = {
                ...mockContentlet,
                baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
                hostName: 'https://example.com',
                url: '/my-page'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com');
        });

        it('should extract path up to /content for non-file assets', () => {
            const contentlet = {
                ...mockContentlet,
                type: 'content',
                hostName: 'https://example.com',
                url: '/content/test-page'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com');
        });

        it('should return full path when /content is not found', () => {
            const contentlet = {
                ...mockContentlet,
                type: 'content',
                hostName: 'https://example.com',
                url: '/some/other/path'
            };

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('https://example.com/some/other/path');
        });

        it('should return defaultValue when hostName is missing', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.hostName;

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('default value');
        });

        it('should return defaultValue when url is missing', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.url;

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('default value');
        });

        it('should return defaultValue when hostName is not a string', () => {
            const contentlet = {
                ...mockContentlet,
                hostName: 123
            } as unknown as DotCMSContentlet;

            // Ensure the resolution function exists before calling it
            expect(resolutionValue[DotCMSFieldTypes.HOST_FOLDER]).toBeDefined();

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('default value');
        });

        it('should return defaultValue when url is not a string', () => {
            const contentlet = {
                ...mockContentlet,
                url: 123
            } as unknown as DotCMSContentlet;

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                contentlet,
                mockHostFolderField
            );
            expect(result).toBe('default value');
        });

        it('should return defaultValue when contentlet is null', () => {
            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](null, mockHostFolderField);
            expect(result).toBe('default value');
        });

        it('should return defaultValue when contentlet is undefined', () => {
            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                undefined,
                mockHostFolderField
            );
            expect(result).toBe('default value');
        });

        it('should return empty string when field has no defaultValue', () => {
            const field = { ...mockHostFolderField };
            delete field.defaultValue;

            const contentlet = { ...mockContentlet };
            delete contentlet.hostName;

            const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](contentlet, field);
            expect(result).toBe('');
        });

        describe('with queryParams', () => {
            it('should return folderPath from queryParams when contentlet is null', () => {
                const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                    null,
                    mockHostFolderField,
                    {
                        folderPath: 'default/level1/level2/'
                    }
                );
                expect(result).toBe('default/level1/level2/');
            });

            it('should return folderPath from queryParams when contentlet is undefined', () => {
                const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                    undefined,
                    mockHostFolderField,
                    {
                        folderPath: 'default/level1/'
                    }
                );
                expect(result).toBe('default/level1/');
            });

            it('should prefer folderPath over field defaultValue when contentlet is null', () => {
                const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                    null,
                    mockHostFolderField,
                    {
                        folderPath: 'myhost/folder1/'
                    }
                );
                expect(result).toBe('myhost/folder1/');
            });

            it('should fall back to defaultValue when queryParams has no folderPath', () => {
                const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                    null,
                    mockHostFolderField,
                    {}
                );
                expect(result).toBe('default value');
            });

            it('should ignore queryParams when contentlet has valid hostName and url', () => {
                const result = resolutionValue[DotCMSFieldTypes.HOST_FOLDER](
                    mockContentlet,
                    mockHostFolderField,
                    {
                        folderPath: 'default/should-be-ignored/'
                    }
                );
                expect(result).toBe('https://example.com');
            });
        });
    });

    describe('categoryResolutionFn', () => {
        it('should extract keys from array of objects', () => {
            const contentlet = {
                ...mockContentlet,
                testField: [{ key1: 'value1' }, { key2: 'value2' }]
            };

            const result = resolutionValue[DotCMSFieldTypes.CATEGORY](
                contentlet,
                mockCategoryField
            );
            expect(result).toEqual(['key1', 'key2']);
        });

        it('should return defaultValue when field is not an array', () => {
            const contentlet = {
                ...mockContentlet,
                testField: 'not-an-array'
            };

            const result = resolutionValue[DotCMSFieldTypes.CATEGORY](
                contentlet,
                mockCategoryField
            );
            expect(result).toBe('default value');
        });

        it('should return empty array when no defaultValue is provided', () => {
            const field = { ...mockCategoryField };
            delete field.defaultValue;

            const result = resolutionValue[DotCMSFieldTypes.CATEGORY](mockContentlet, field);
            expect(result).toEqual([]);
        });

        it('should return empty array when contentlet is null and isManualTranslation is true', () => {
            const result = resolutionValue[DotCMSFieldTypes.CATEGORY](
                null,
                mockCategoryField,
                undefined,
                true
            );
            expect(result).toEqual([]);
        });

        it('should return empty array instead of defaultValue when isManualTranslation is true', () => {
            const contentlet = { ...mockContentlet, testField: 'not-an-array' };
            const result = resolutionValue[DotCMSFieldTypes.CATEGORY](
                contentlet,
                mockCategoryField,
                undefined,
                true
            );
            expect(result).toEqual([]);
        });
    });

    describe('relationshipResolutionFn', () => {
        const mockRelationships = [{ identifier: 'id1' }, { identifier: 'id2' }];

        beforeEach(() => {
            (getRelationshipFromContentlet as Mock).mockReturnValue(mockRelationships);
        });

        it('should join relationship identifiers with commas', () => {
            const result = resolutionValue[DotCMSFieldTypes.RELATIONSHIP](
                mockContentlet,
                mockRelationshipField
            );
            expect(result).toBe('id1,id2');
        });

        it('should call getRelationshipFromContentlet with correct parameters', () => {
            resolutionValue[DotCMSFieldTypes.RELATIONSHIP](mockContentlet, mockRelationshipField);
            expect(getRelationshipFromContentlet).toHaveBeenCalledWith({
                contentlet: mockContentlet,
                variable: mockRelationshipField.variable
            });
        });

        it('should handle empty relationships', () => {
            (getRelationshipFromContentlet as Mock).mockReturnValue([]);
            const result = resolutionValue[DotCMSFieldTypes.RELATIONSHIP](
                mockContentlet,
                mockRelationshipField
            );
            expect(result).toBe('');
        });
    });

    describe('emptyResolutionFn', () => {
        it('should always return empty string', () => {
            const result = resolutionValue[DotCMSFieldTypes.LINE_DIVIDER](
                mockContentlet,
                mockLineDividerField
            );
            expect(result).toBe('');
        });
    });

    describe('selectResolutionFn', () => {
        it('should return the first option when the value and defaultValue are empty', () => {
            const mockContentlet = createFakeContentlet({
                testField: null
            });
            const mockField = createFakeSelectField({
                values: 'Option 1|1\r\nOption 2|2\r\nOption 3|3',
                defaultValue: null,
                variable: 'testField'
            });
            const result = resolutionValue[DotCMSFieldTypes.SELECT](mockContentlet, mockField);
            expect(result).toBe('1');
        });

        it('should return the default value when the value is empty', () => {
            const mockContentlet = createFakeContentlet({
                testField: null
            });
            const mockField = createFakeSelectField({
                defaultValue: 'Option 2',
                variable: 'testField'
            });
            const result = resolutionValue[DotCMSFieldTypes.SELECT](mockContentlet, mockField);
            expect(result).toBe('Option 2');
        });

        it('should return the value when the value is not empty', () => {
            const mockContentlet = createFakeContentlet({
                testField: 'Option 2'
            });
            const mockField = createFakeSelectField({
                variable: 'testField',
                defaultValue: null
            });
            const result = resolutionValue[DotCMSFieldTypes.SELECT](mockContentlet, mockField);
            expect(result).toBe('Option 2');
        });

        it('should return null when contentlet is null and isManualTranslation is true', () => {
            const field = createFakeSelectField({
                values: 'Option 1|1\r\nOption 2|2',
                defaultValue: 'Option 1',
                variable: 'testField'
            });
            const result = resolutionValue[DotCMSFieldTypes.SELECT](null, field, undefined, true);
            expect(result).toBeNull();
        });
    });

    describe('field type mappings', () => {
        it('should have resolution functions for all field types', () => {
            Object.values(DotCMSFieldTypes).forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBeDefined();
            });
        });

        it('should use defaultResolutionFn for most field types', () => {
            const defaultFieldTypes = [
                DotCMSFieldTypes.BINARY,
                DotCMSFieldTypes.FILE,
                DotCMSFieldTypes.IMAGE,
                // CHECKBOX and MULTI_SELECT have their own resolver — for saved content an
                // absent key means "nothing selected", so no default is applied.
                DotCMSFieldTypes.CONSTANT,
                DotCMSFieldTypes.CUSTOM_FIELD,
                DotCMSFieldTypes.HIDDEN,
                DotCMSFieldTypes.JSON,
                // KEY_VALUE has its own resolver — it recovers the response's key order.
                DotCMSFieldTypes.RADIO,
                DotCMSFieldTypes.TAG,
                DotCMSFieldTypes.TEXTAREA,
                DotCMSFieldTypes.WYSIWYG
            ];

            defaultFieldTypes.forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBe(resolutionValue[DotCMSFieldTypes.TEXTAREA]);
            });
        });

        // Keeps the table above self-checking: without this, removing CHECKBOX and MULTI_SELECT
        // from the default list leaves only a comment recording where they went.
        it('should route CHECKBOX and MULTI_SELECT to their own shared resolver', () => {
            expect(resolutionValue[DotCMSFieldTypes.CHECKBOX]).toBe(
                resolutionValue[DotCMSFieldTypes.MULTI_SELECT]
            );
            expect(resolutionValue[DotCMSFieldTypes.CHECKBOX]).not.toBe(
                resolutionValue[DotCMSFieldTypes.TEXTAREA]
            );
        });

        describe('blockEditorResolutionFn', () => {
            const blockField = createFakeBlockEditorField({
                ...MOCK_FIELD_OVERRIDES,
                variable: 'blockContent'
            });

            it('should parse JSON string values from the API', () => {
                const jsonObj = { type: 'doc', content: [{ type: 'paragraph' }] };
                const contentlet = {
                    ...mockContentlet,
                    blockContent: JSON.stringify(jsonObj)
                };
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](
                    contentlet,
                    blockField
                );
                expect(result).toEqual(jsonObj);
            });

            it('should return object values as-is', () => {
                const jsonObj = { type: 'doc', content: [{ type: 'paragraph' }] };
                const contentlet = { ...mockContentlet, blockContent: jsonObj };
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](
                    contentlet as unknown as DotCMSContentlet,
                    blockField
                );
                expect(result).toEqual(jsonObj);
            });

            it('should return non-JSON strings as-is', () => {
                const contentlet = { ...mockContentlet, blockContent: 'plain text' };
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](
                    contentlet,
                    blockField
                );
                expect(result).toBe('plain text');
            });

            it('should return invalid JSON-looking strings as-is', () => {
                const contentlet = { ...mockContentlet, blockContent: '{invalid' };
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](
                    contentlet,
                    blockField
                );
                expect(result).toBe('{invalid');
            });

            it('should return defaultValue when contentlet is null', () => {
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](null, blockField);
                expect(result).toBe(blockField.defaultValue);
            });

            it('should return null when contentlet is null and isManualTranslation is true', () => {
                const result = resolutionValue[DotCMSFieldTypes.BLOCK_EDITOR](
                    null,
                    blockField,
                    undefined,
                    true
                );
                expect(result).toBeNull();
            });
        });

        it('should use dateResolutionFn for date field types', () => {
            const dateFieldTypes = [
                DotCMSFieldTypes.DATE,
                DotCMSFieldTypes.DATE_AND_TIME,
                DotCMSFieldTypes.TIME
            ];

            dateFieldTypes.forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBe(resolutionValue[DotCMSFieldTypes.DATE]);
            });
        });

        describe('dateResolutionFn', () => {
            const dateField = createFakeDateField({
                ...MOCK_FIELD_OVERRIDES,
                variable: 'campoDate'
            });

            it('should return numeric timestamp from contentlet', () => {
                const contentlet = {
                    ...mockContentlet,
                    campoDate: 1736899200000
                };

                expect(resolutionValue[DotCMSFieldTypes.DATE](contentlet, dateField)).toBe(
                    1736899200000
                );
            });

            it('should parse ISO date strings from contentlet', () => {
                const isoDate = '2025-01-15T10:30:00.000Z';
                const contentlet = {
                    ...mockContentlet,
                    campoDate: isoDate
                };

                expect(resolutionValue[DotCMSFieldTypes.DATE](contentlet, dateField)).toBe(
                    Date.parse(isoDate)
                );
            });

            it('should parse formatted date strings from contentlet', () => {
                const formattedDate = '2025-01-15';
                const contentlet = {
                    ...mockContentlet,
                    campoDate: formattedDate
                };

                expect(resolutionValue[DotCMSFieldTypes.DATE](contentlet, dateField)).toBe(
                    Date.parse(formattedDate)
                );
            });

            it('should return null for empty date values', () => {
                const contentlet = {
                    ...mockContentlet,
                    campoDate: ''
                };

                expect(resolutionValue[DotCMSFieldTypes.DATE](contentlet, dateField)).toBeNull();
            });
        });
    });
});

describe('KEY_VALUE resolution', () => {
    const resolve = (contentlet: unknown, variable = 'keyValue') =>
        resolutionValue[DotCMSFieldTypes.KEY_VALUE](
            contentlet as DotCMSContentlet,
            createFakeKeyValueField({ variable, defaultValue: undefined }),
            undefined,
            false
        );

    /** A contentlet as `getContentById` hands it over. */
    const loaded = (raw: string) => {
        const { entity } = JSON.parse(raw);
        const ordered = parsePreservingKeyOrder(raw) as { entity: unknown };

        return attachOrderedFields(entity, ordered.entity);
    };

    it('should hand the form the order the response carried, not the object order', () => {
        const raw = '{"entity":{"keyValue":{"orden 1":"a","123":"b","zzz":"c"}}}';

        // The plain object has already hoisted `123`; the resolver must not use it.
        expect(Object.keys(JSON.parse(raw).entity.keyValue)[0]).toBe('123');
        expect(resolve(loaded(raw))).toBe('{"orden 1":"a","123":"b","zzz":"c"}');
    });

    it('should fall back to the stored value when nothing was recovered', () => {
        // Contentlets reaching the form by another path carry no recovered copy.
        const plain = { keyValue: { a: '1' } };

        expect(resolve(plain)).toEqual({ a: '1' });
    });

    it('should resolve only its own field', () => {
        const raw = '{"entity":{"first":{"9":"a","b":"c"},"second":{"z":"1"}}}';

        expect(resolve(loaded(raw), 'first')).toBe('{"9":"a","b":"c"}');
        expect(resolve(loaded(raw), 'second')).toBe('{"z":"1"}');
    });

    it('should leave a binary field alone even though it is reachable', () => {
        // Only the declared field type routes here, which is the whole point: a
        // shape-based guess used to rewrite `metaData` and break file previews.
        const raw = '{"entity":{"metaData":{"name":"f.txt","fileSize":42}}}';
        const contentlet = loaded(raw);

        expect(contentlet.metaData).toEqual({ name: 'f.txt', fileSize: 42 });
    });
});
