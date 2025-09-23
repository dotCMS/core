import { DotCMSClazzes, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeSelectField } from '@dotcms/utils-testing';

import { resolutionValue } from './dot-edit-content-form-resolutions';

import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { getRelationshipFromContentlet } from '../../utils/relationshipFromContentlet';

jest.mock('../../utils/relationshipFromContentlet', () => ({
    getRelationshipFromContentlet: jest.fn()
}));

describe('DotEditContentFormResolutions', () => {
    const mockField: DotCMSContentTypeField = {
        variable: 'testField',
        defaultValue: 'default value',
        clazz: DotCMSClazzes.TEXT,
        contentTypeId: 'test-content-type',
        dataType: 'text',
        fieldType: 'text',
        fieldTypeLabel: 'Text',
        fixed: false,
        indexed: true,
        listed: true,
        readOnly: false,
        required: false,
        searchable: true,
        sortOrder: 0,
        unique: false,
        fieldVariables: [],
        iDate: 1710892800000, // 2024-03-20T00:00:00.000Z
        id: 'test-field-id',
        modDate: 1710892800000, // 2024-03-20T00:00:00.000Z
        name: 'Test Field'
    };

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

    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('defaultResolutionFn', () => {
        it('should return field value from contentlet', () => {
            const result = resolutionValue[FIELD_TYPES.TEXTAREA](mockContentlet, mockField);
            expect(result).toBe('test value');
        });

        it('should return defaultValue when field value is not in contentlet', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.testField;

            const result = resolutionValue[FIELD_TYPES.TEXTAREA](contentlet, mockField);
            expect(result).toBe('default value');
        });

        it('should return defaultValue when contentlet is null', () => {
            const result = resolutionValue[FIELD_TYPES.TEXTAREA](null, mockField);
            expect(result).toBe('default value');
        });
    });

    describe('textFieldResolutionFn', () => {
        it('should not modify non-URL values', () => {
            const contentlet = {
                ...mockContentlet,
                testField: 'test-value'
            };

            const result = resolutionValue[FIELD_TYPES.TEXT](contentlet, mockField);
            expect(result).toBe('test-value');
        });

        it('should handle null values', () => {
            const result = resolutionValue[FIELD_TYPES.TEXT](null, mockField);
            expect(result).toBe('default value');
        });
    });

    describe('hostFolderResolutionFn', () => {
        it('should construct host folder path from hostName and url', () => {
            const result = resolutionValue[FIELD_TYPES.HOST_FOLDER](mockContentlet, mockField);
            expect(result).toBe('https://example.com');
        });

        it('should return defaultValue when hostName is missing', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.hostName;

            const result = resolutionValue[FIELD_TYPES.HOST_FOLDER](contentlet, mockField);
            expect(result).toBe('default value');
        });

        it('should return defaultValue when url is missing', () => {
            const contentlet = { ...mockContentlet };
            delete contentlet.url;

            const result = resolutionValue[FIELD_TYPES.HOST_FOLDER](contentlet, mockField);
            expect(result).toBe('default value');
        });
    });

    describe('categoryResolutionFn', () => {
        it('should extract keys from array of objects', () => {
            const contentlet = {
                ...mockContentlet,
                testField: [{ key1: 'value1' }, { key2: 'value2' }]
            };

            const result = resolutionValue[FIELD_TYPES.CATEGORY](contentlet, mockField);
            expect(result).toEqual(['key1', 'key2']);
        });

        it('should return defaultValue when field is not an array', () => {
            const contentlet = {
                ...mockContentlet,
                testField: 'not-an-array'
            };

            const result = resolutionValue[FIELD_TYPES.CATEGORY](contentlet, mockField);
            expect(result).toBe('default value');
        });

        it('should return empty array when no defaultValue is provided', () => {
            const field = { ...mockField };
            delete field.defaultValue;

            const result = resolutionValue[FIELD_TYPES.CATEGORY](mockContentlet, field);
            expect(result).toEqual([]);
        });
    });

    describe('relationshipResolutionFn', () => {
        const mockRelationships = [{ identifier: 'id1' }, { identifier: 'id2' }];

        beforeEach(() => {
            (getRelationshipFromContentlet as jest.Mock).mockReturnValue(mockRelationships);
        });

        it('should join relationship identifiers with commas', () => {
            const result = resolutionValue[FIELD_TYPES.RELATIONSHIP](mockContentlet, mockField);
            expect(result).toBe('id1,id2');
        });

        it('should call getRelationshipFromContentlet with correct parameters', () => {
            resolutionValue[FIELD_TYPES.RELATIONSHIP](mockContentlet, mockField);
            expect(getRelationshipFromContentlet).toHaveBeenCalledWith({
                contentlet: mockContentlet,
                variable: mockField.variable
            });
        });

        it('should handle empty relationships', () => {
            (getRelationshipFromContentlet as jest.Mock).mockReturnValue([]);
            const result = resolutionValue[FIELD_TYPES.RELATIONSHIP](mockContentlet, mockField);
            expect(result).toBe('');
        });
    });

    describe('emptyResolutionFn', () => {
        it('should always return empty string', () => {
            const result = resolutionValue[FIELD_TYPES.LINE_DIVIDER](mockContentlet, mockField);
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
            const result = resolutionValue[FIELD_TYPES.SELECT](mockContentlet, mockField);
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
            const result = resolutionValue[FIELD_TYPES.SELECT](mockContentlet, mockField);
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
            const result = resolutionValue[FIELD_TYPES.SELECT](mockContentlet, mockField);
            expect(result).toBe('Option 2');
        });
    });

    describe('field type mappings', () => {
        it('should have resolution functions for all field types', () => {
            Object.values(FIELD_TYPES).forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBeDefined();
            });
        });

        it('should use defaultResolutionFn for most field types', () => {
            const defaultFieldTypes = [
                FIELD_TYPES.BINARY,
                FIELD_TYPES.FILE,
                FIELD_TYPES.IMAGE,
                FIELD_TYPES.BLOCK_EDITOR,
                FIELD_TYPES.CHECKBOX,
                FIELD_TYPES.CONSTANT,
                FIELD_TYPES.CUSTOM_FIELD,
                FIELD_TYPES.HIDDEN,
                FIELD_TYPES.JSON,
                FIELD_TYPES.KEY_VALUE,
                FIELD_TYPES.MULTI_SELECT,
                FIELD_TYPES.RADIO,
                FIELD_TYPES.TAG,
                FIELD_TYPES.TEXTAREA,
                FIELD_TYPES.WYSIWYG
            ];

            defaultFieldTypes.forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBe(resolutionValue[FIELD_TYPES.TEXTAREA]);
            });
        });

        it('should use dateResolutionFn for date field types', () => {
            const dateFieldTypes = [FIELD_TYPES.DATE, FIELD_TYPES.DATE_AND_TIME, FIELD_TYPES.TIME];

            dateFieldTypes.forEach((fieldType) => {
                expect(resolutionValue[fieldType]).toBe(resolutionValue[FIELD_TYPES.DATE]);
            });
        });
    });
});
