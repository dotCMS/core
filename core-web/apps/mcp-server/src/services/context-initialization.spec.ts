/**
 * Integration test demonstrating how the deprecated field fix works
 * in the context of the MCP server's context initialization process
 */

import { ContentTypeService } from '../services/contentType';
import { mockFetch } from '../test-setup';

describe('Context Initialization with Deprecated Fields', () => {
    let service: ContentTypeService;

    beforeEach(() => {
        service = new ContentTypeService();
    });

    it('should successfully process content types with deprecated fields during context initialization', async () => {
        // Mock response simulating what dotCMS API returns for a customer with deprecated fields
        const mockResponseWithDeprecatedFields = {
            json: jest.fn().mockResolvedValue({
                entity: [
                    {
                        id: 'ct-legacy-1',
                        name: 'Customer Legacy Content Type',
                        baseType: 'CONTENT',
                        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                        defaultType: false,
                        fixed: false,
                        folder: 'SYSTEM_FOLDER',
                        folderPath: '/System folder',
                        host: 'demo.dotcms.com',
                        iDate: 1640995200000,
                        layout: [
                            {
                                divider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                                    contentTypeId: 'ct-legacy-1',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Row',
                                    fieldTypeLabel: 'Row',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 1640995200000,
                                    indexed: false,
                                    listed: false,
                                    modDate: 1640995200000,
                                    name: 'fields-0',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 0,
                                    unique: false
                                },
                                columns: [
                                    {
                                        columnDivider: {
                                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                            contentTypeId: 'ct-legacy-1',
                                            dataType: 'SYSTEM',
                                            fieldType: 'Column',
                                            fieldTypeLabel: 'Column',
                                            fieldVariables: [],
                                            fixed: false,
                                            forceIncludeInApi: false,
                                            iDate: 1640995200000,
                                            indexed: false,
                                            listed: false,
                                            modDate: 1640995200000,
                                            name: 'fields-1',
                                            readOnly: false,
                                            required: false,
                                            searchable: false,
                                            sortOrder: 1,
                                            unique: false
                                        },
                                        fields: [
                                            {
                                                // This is the problematic deprecated field that was causing failures
                                                clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField',
                                                contentTypeId: 'ct-legacy-1',
                                                dataType: 'SYSTEM',
                                                fieldType: 'RelationshipsTab',
                                                fieldTypeLabel: 'Relationships Tab',
                                                fieldVariables: [],
                                                fixed: false,
                                                forceIncludeInApi: false,
                                                iDate: 1640995200000,
                                                id: 'relationships-tab-field',
                                                indexed: false,
                                                listed: false,
                                                modDate: 1640995200000,
                                                name: 'Relationships',
                                                readOnly: false,
                                                required: false,
                                                searchable: false,
                                                sortOrder: 2,
                                                unique: false,
                                                variable: 'relationships'
                                            },
                                            {
                                                // Standard field that should also continue to work
                                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                                contentTypeId: 'ct-legacy-1',
                                                dataType: 'TEXT',
                                                fieldType: 'Text',
                                                fieldTypeLabel: 'Text',
                                                fieldVariables: [],
                                                fixed: false,
                                                forceIncludeInApi: false,
                                                iDate: 1640995200000,
                                                id: 'title-field',
                                                indexed: true,
                                                listed: true,
                                                modDate: 1640995200000,
                                                name: 'Title',
                                                readOnly: false,
                                                required: true,
                                                searchable: true,
                                                sortOrder: 3,
                                                unique: false,
                                                variable: 'title'
                                            }
                                        ]
                                    }
                                ]
                            }
                        ],
                        metadata: {},
                        modDate: 1640995200000,
                        multilingualable: false,
                        sortOrder: 0,
                        system: false,
                        variable: 'customerLegacyContentType',
                        versionable: true
                    },
                    {
                        // Another content type with only standard fields to ensure mixed scenarios work
                        id: 'ct-standard-1',
                        name: 'Standard Content Type',
                        baseType: 'CONTENT',
                        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                        defaultType: false,
                        fixed: false,
                        folder: 'SYSTEM_FOLDER',
                        folderPath: '/System folder',
                        host: 'demo.dotcms.com',
                        iDate: 1640995200000,
                        layout: [
                            {
                                divider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                                    contentTypeId: 'ct-standard-1',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Row',
                                    fieldTypeLabel: 'Row',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 1640995200000,
                                    indexed: false,
                                    listed: false,
                                    modDate: 1640995200000,
                                    name: 'fields-0',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 0,
                                    unique: false
                                },
                                columns: [
                                    {
                                        columnDivider: {
                                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                            contentTypeId: 'ct-standard-1',
                                            dataType: 'SYSTEM',
                                            fieldType: 'Column',
                                            fieldTypeLabel: 'Column',
                                            fieldVariables: [],
                                            fixed: false,
                                            forceIncludeInApi: false,
                                            iDate: 1640995200000,
                                            indexed: false,
                                            listed: false,
                                            modDate: 1640995200000,
                                            name: 'fields-1',
                                            readOnly: false,
                                            required: false,
                                            searchable: false,
                                            sortOrder: 1,
                                            unique: false
                                        },
                                        fields: [
                                            {
                                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                                contentTypeId: 'ct-standard-1',
                                                dataType: 'TEXT',
                                                fieldType: 'Text',
                                                fieldTypeLabel: 'Text',
                                                fieldVariables: [],
                                                fixed: false,
                                                forceIncludeInApi: false,
                                                iDate: 1640995200000,
                                                id: 'description-field',
                                                indexed: true,
                                                listed: true,
                                                modDate: 1640995200000,
                                                name: 'Description',
                                                readOnly: false,
                                                required: false,
                                                searchable: true,
                                                sortOrder: 2,
                                                unique: false,
                                                variable: 'description'
                                            }
                                        ]
                                    }
                                ]
                            }
                        ],
                        metadata: {},
                        modDate: 1640995200000,
                        multilingualable: false,
                        sortOrder: 1,
                        system: false,
                        variable: 'standardContentType',
                        versionable: true
                    }
                ]
            })
        };

        mockFetch.mockResolvedValue(mockResponseWithDeprecatedFields);

        // This should now succeed instead of failing with validation errors
        const result = await service.getContentTypesSchema();

        expect(mockFetch).toHaveBeenCalledWith(
            '/api/v1/contenttype?page=1&per_page=100&orderby=name&direction=ASC',
            { method: 'GET' }
        );

        // Verify we got both content types
        expect(result).toHaveLength(2);

        // Verify the content type with deprecated field was processed successfully
        const legacyContentType = result.find((ct) => ct.variable === 'customerLegacyContentType');
        expect(legacyContentType).toBeDefined();
        expect(legacyContentType!.name).toBe('Customer Legacy Content Type');
        expect(legacyContentType!.fields).toHaveLength(2);

        // Verify the deprecated field is present and has the correct clazz
        const deprecatedField = legacyContentType!.fields.find(
            (f) => f.variable === 'relationships'
        );
        expect(deprecatedField).toBeDefined();
        expect(deprecatedField!.clazz).toBe(
            'com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField'
        );
        expect(deprecatedField!.name).toBe('Relationships');

        // Verify the standard field in the same content type still works
        const titleField = legacyContentType!.fields.find((f) => f.variable === 'title');
        expect(titleField).toBeDefined();
        expect(titleField!.clazz).toBe('com.dotcms.contenttype.model.field.ImmutableTextField');
        expect(titleField!.name).toBe('Title');

        // Verify the standard content type still works
        const standardContentType = result.find((ct) => ct.variable === 'standardContentType');
        expect(standardContentType).toBeDefined();
        expect(standardContentType!.name).toBe('Standard Content Type');
        expect(standardContentType!.fields).toHaveLength(1);
        expect(standardContentType!.fields?.[0]?.clazz).toBe(
            'com.dotcms.contenttype.model.field.ImmutableTextField'
        );
    });
});
