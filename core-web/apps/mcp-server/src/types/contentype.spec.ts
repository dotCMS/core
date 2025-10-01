import { ContentTypeSchema } from './contentype';

describe('ContentType Validation', () => {
    describe('Field Clazz Validation through Content Types', () => {
        it('should validate content types with known field types', () => {
            const knownFieldTypes = [
                'com.dotcms.contenttype.model.field.ImmutableTextField',
                'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
                'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                'com.dotcms.contenttype.model.field.ImmutableImageField',
                'com.dotcms.contenttype.model.field.ImmutableFileField',
                'com.dotcms.contenttype.model.field.ImmutableDateField',
                'com.dotcms.contenttype.model.field.ImmutableDateTimeField',
                'com.dotcms.contenttype.model.field.ImmutableTimeField',
                'com.dotcms.contenttype.model.field.ImmutableSelectField',
                'com.dotcms.contenttype.model.field.ImmutableRadioField',
                'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
                'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
                'com.dotcms.contenttype.model.field.ImmutableCategoryField',
                'com.dotcms.contenttype.model.field.ImmutableTagField',
                'com.dotcms.contenttype.model.field.ImmutableRelationshipField',
                'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
                'com.dotcms.contenttype.model.field.ImmutableHiddenField',
                'com.dotcms.contenttype.model.field.ImmutableConstantField',
                'com.dotcms.contenttype.model.field.ImmutableCustomField',
                'com.dotcms.contenttype.model.field.ImmutableKeyValueField',
                'com.dotcms.contenttype.model.field.ImmutableJSONField',
                'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                'com.dotcms.contenttype.model.field.ImmutableLineDividerField'
            ];

            knownFieldTypes.forEach((fieldType) => {
                const contentTypeData = {
                    baseType: 'CONTENT' as const,
                    clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                    defaultType: false,
                    fixed: false,
                    folder: 'SYSTEM_FOLDER',
                    folderPath: '/SYSTEM_FOLDER',
                    host: 'test-host',
                    iDate: 1234567890,
                    id: 'test-content-type',
                    layout: [],
                    metadata: {},
                    modDate: 1234567890,
                    multilingualable: true,
                    name: 'Test Content Type',
                    sortOrder: 1,
                    system: false,
                    variable: 'testContentType',
                    versionable: true,
                    fields: [
                        {
                            clazz: fieldType,
                            contentTypeId: 'test-content-type',
                            dataType: 'TEXT',
                            fieldType: 'Text',
                            fieldTypeLabel: 'Text Field',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1234567890,
                            id: 'test-field-id',
                            indexed: true,
                            listed: true,
                            modDate: 1234567890,
                            name: 'Test Field',
                            readOnly: false,
                            required: false,
                            searchable: true,
                            sortOrder: 1,
                            unique: false,
                            variable: 'testField'
                        }
                    ]
                };

                expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
            });
        });

        it('should validate content types with deprecated field types like ImmutableBinaryField', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
                        contentTypeId: 'test-content-type',
                        dataType: 'BINARY',
                        fieldType: 'Binary',
                        fieldTypeLabel: 'Binary Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1234567890,
                        id: 'test-field-id',
                        indexed: false,
                        listed: false,
                        modDate: 1234567890,
                        name: 'Test Binary Field',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 1,
                        unique: false,
                        variable: 'testBinaryField'
                    }
                ]
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
        });

        it('should validate content types with other unknown field types that follow the dotCMS pattern', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableSomeNewField',
                        contentTypeId: 'test-content-type',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1234567890,
                        id: 'test-field-id',
                        indexed: true,
                        listed: true,
                        modDate: 1234567890,
                        name: 'Test Field',
                        readOnly: false,
                        required: false,
                        searchable: true,
                        sortOrder: 1,
                        unique: false,
                        variable: 'testField'
                    }
                ]
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
        });

        it('should reject content types with invalid field clazz values', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.invalid.field.ImmutableInvalidField',
                        contentTypeId: 'test-content-type',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1234567890,
                        id: 'test-field-id',
                        indexed: true,
                        listed: true,
                        modDate: 1234567890,
                        name: 'Test Field',
                        readOnly: false,
                        required: false,
                        searchable: true,
                        sortOrder: 1,
                        unique: false,
                        variable: 'testField'
                    }
                ]
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).toThrow();
        });

        it('should reject content types with field clazz that does not end with Field', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableSomethingElse',
                        contentTypeId: 'test-content-type',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1234567890,
                        id: 'test-field-id',
                        indexed: true,
                        listed: true,
                        modDate: 1234567890,
                        name: 'Test Field',
                        readOnly: false,
                        required: false,
                        searchable: true,
                        sortOrder: 1,
                        unique: false,
                        variable: 'testField'
                    }
                ]
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).toThrow();
        });
    });

    describe('Content Type Clazz Validation', () => {
        it('should validate content types with known clazz types', () => {
            const knownContentTypeClazzes = [
                'com.dotcms.contenttype.model.type.ImmutableDotAssetContentType',
                'com.dotcms.contenttype.model.type.ImmutableFileAssetContentType',
                'com.dotcms.contenttype.model.type.ImmutableFormContentType',
                'com.dotcms.contenttype.model.type.ImmutableKeyValueContentType',
                'com.dotcms.contenttype.model.type.ImmutablePageContentType',
                'com.dotcms.contenttype.model.type.ImmutablePersonaContentType',
                'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                'com.dotcms.contenttype.model.type.ImmutableVanityUrlContentType',
                'com.dotcms.contenttype.model.type.ImmutableWidgetContentType'
            ];

            knownContentTypeClazzes.forEach((clazz) => {
                const contentTypeData = {
                    baseType: 'CONTENT' as const,
                    clazz: clazz,
                    defaultType: false,
                    fixed: false,
                    folder: 'SYSTEM_FOLDER',
                    folderPath: '/SYSTEM_FOLDER',
                    host: 'test-host',
                    iDate: 1234567890,
                    id: 'test-content-type',
                    layout: [],
                    metadata: {},
                    modDate: 1234567890,
                    multilingualable: true,
                    name: 'Test Content Type',
                    sortOrder: 1,
                    system: false,
                    variable: 'testContentType',
                    versionable: true
                };

                expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
            });
        });

        it('should validate content types with unknown clazz types that follow the dotCMS pattern', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSomeNewContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
        });

        it('should reject content types with invalid clazz values', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.invalid.type.ImmutableInvalidContentType',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).toThrow();
        });

        it('should reject content types with clazz that does not end with ContentType', () => {
            const contentTypeData = {
                baseType: 'CONTENT' as const,
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSomethingElse',
                defaultType: false,
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: 'test-host',
                iDate: 1234567890,
                id: 'test-content-type',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).toThrow();
        });
    });

    describe('Content Type with Deprecated Fields', () => {
        it('should validate a complete content type with deprecated binary field', () => {
            const contentTypeData = {
                baseType: 'CONTENT',
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                description: 'Test content type with deprecated field',
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                iDate: 1234567890,
                id: 'test-content-type-id',
                layout: [],
                metadata: {},
                modDate: 1234567890,
                multilingualable: true,
                name: 'Test Content Type',
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
                        contentTypeId: 'test-content-type-id',
                        dataType: 'BINARY',
                        fieldType: 'Binary',
                        fieldTypeLabel: 'Binary Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1234567890,
                        id: 'test-field-id',
                        indexed: false,
                        listed: false,
                        modDate: 1234567890,
                        name: 'Test Binary Field',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 1,
                        unique: false,
                        variable: 'testBinaryField'
                    }
                ]
            };

            expect(() => ContentTypeSchema.parse(contentTypeData)).not.toThrow();
        });

        it('should validate a realistic dotCMS API response with deprecated ImmutableBinaryField', () => {
            // Simulate a real dotCMS API response from an old instance
            const realWorldResponse = {
                entity: [
                    {
                        baseType: 'CONTENT',
                        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                        defaultType: false,
                        description: 'Legacy content type with binary field',
                        fixed: false,
                        folder: 'SYSTEM_FOLDER',
                        folderPath: '/SYSTEM_FOLDER',
                        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                        iDate: 1640995200000,
                        id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
                        layout: [
                            {
                                divider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                                    contentTypeId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
                                    dataType: 'SYSTEM_FIELD',
                                    fieldType: 'Row',
                                    fieldTypeLabel: 'Row',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 1640995200000,
                                    indexed: false,
                                    listed: false,
                                    modDate: 1640995200000,
                                    name: 'Row',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 0,
                                    unique: false,
                                    variable: 'row'
                                },
                                columns: [
                                    {
                                        columnDivider: {
                                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                            contentTypeId: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
                                            dataType: 'SYSTEM_FIELD',
                                            fieldType: 'Column',
                                            fieldTypeLabel: 'Column',
                                            fieldVariables: [],
                                            fixed: false,
                                            forceIncludeInApi: false,
                                            iDate: 1640995200000,
                                            indexed: false,
                                            listed: false,
                                            modDate: 1640995200000,
                                            name: 'Column',
                                            readOnly: false,
                                            required: false,
                                            searchable: false,
                                            sortOrder: 0,
                                            unique: false,
                                            variable: 'column'
                                        },
                                        fields: [
                                            {
                                                clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
                                                contentTypeId:
                                                    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
                                                dataType: 'BINARY',
                                                defaultValue: '',
                                                fieldType: 'Binary',
                                                fieldTypeLabel: 'Binary',
                                                fieldVariables: [],
                                                fixed: false,
                                                forceIncludeInApi: false,
                                                hint: 'Upload a file',
                                                iDate: 1640995200000,
                                                id: 'binary-field-id',
                                                indexed: false,
                                                listed: false,
                                                modDate: 1640995200000,
                                                name: 'Document Upload',
                                                readOnly: false,
                                                required: false,
                                                searchable: false,
                                                sortOrder: 1,
                                                unique: false,
                                                variable: 'documentUpload'
                                            },
                                            {
                                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                                contentTypeId:
                                                    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
                                                dataType: 'TEXT',
                                                defaultValue: '',
                                                fieldType: 'Text',
                                                fieldTypeLabel: 'Text',
                                                fieldVariables: [],
                                                fixed: false,
                                                forceIncludeInApi: false,
                                                hint: 'Enter document title',
                                                iDate: 1640995200000,
                                                id: 'text-field-id',
                                                indexed: true,
                                                listed: true,
                                                modDate: 1640995200000,
                                                name: 'Document Title',
                                                readOnly: false,
                                                required: true,
                                                searchable: true,
                                                sortOrder: 2,
                                                unique: false,
                                                variable: 'documentTitle'
                                            }
                                        ]
                                    }
                                ]
                            }
                        ],
                        metadata: {
                            'com.dotcms.contenttype.builder.ImmutableFieldType': 'Binary'
                        },
                        modDate: 1640995200000,
                        multilingualable: true,
                        name: 'Legacy Document',
                        sortOrder: 1,
                        system: false,
                        variable: 'legacyDocument',
                        versionable: true,
                        workflows: [
                            {
                                archived: false,
                                creationDate: 1640995200000,
                                defaultScheme: true,
                                description: 'Default workflow',
                                entryActionId: 'entry-action-id',
                                id: 'workflow-id',
                                mandatory: false,
                                modDate: 1640995200000,
                                name: 'Default Workflow',
                                system: true,
                                variableName: 'defaultWorkflow'
                            }
                        ]
                    }
                ]
            };

            // Validate that the entire response structure can be parsed without errors
            expect(() => {
                const contentType = realWorldResponse.entity[0];
                ContentTypeSchema.parse(contentType);
            }).not.toThrow();

            // Specifically validate that the deprecated binary field is accepted
            const contentType = realWorldResponse.entity[0];
            const parsedContentType = ContentTypeSchema.parse(contentType);

            expect(parsedContentType.layout[0].columns[0].fields[0].clazz).toBe(
                'com.dotcms.contenttype.model.field.ImmutableBinaryField'
            );
            expect(parsedContentType.layout[0].columns[0].fields[0].fieldType).toBe('Binary');
            expect(parsedContentType.layout[0].columns[0].fields[0].variable).toBe(
                'documentUpload'
            );
        });

        it('should handle multiple deprecated field types in the same content type', () => {
            const contentTypeWithMultipleDeprecatedFields = {
                baseType: 'CONTENT',
                clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
                defaultType: false,
                description: 'Content type with multiple deprecated fields',
                fixed: false,
                folder: 'SYSTEM_FOLDER',
                folderPath: '/SYSTEM_FOLDER',
                host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
                iDate: 1640995200000,
                id: 'multi-deprecated-content-type',
                layout: [],
                metadata: {},
                modDate: 1640995200000,
                multilingualable: true,
                name: 'Multi Deprecated Fields',
                sortOrder: 1,
                system: false,
                variable: 'multiDeprecatedFields',
                versionable: true,
                fields: [
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
                        contentTypeId: 'multi-deprecated-content-type',
                        dataType: 'BINARY',
                        fieldType: 'Binary',
                        fieldTypeLabel: 'Binary Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1640995200000,
                        id: 'binary-field-1',
                        indexed: false,
                        listed: false,
                        modDate: 1640995200000,
                        name: 'Legacy Binary Field',
                        readOnly: false,
                        required: false,
                        searchable: false,
                        sortOrder: 1,
                        unique: false,
                        variable: 'legacyBinary'
                    },
                    {
                        clazz: 'com.dotcms.contenttype.model.field.ImmutableSomeOtherDeprecatedField',
                        contentTypeId: 'multi-deprecated-content-type',
                        dataType: 'TEXT',
                        fieldType: 'Text',
                        fieldTypeLabel: 'Text Field',
                        fieldVariables: [],
                        fixed: false,
                        forceIncludeInApi: false,
                        iDate: 1640995200000,
                        id: 'deprecated-field-2',
                        indexed: true,
                        listed: true,
                        modDate: 1640995200000,
                        name: 'Another Deprecated Field',
                        readOnly: false,
                        required: false,
                        searchable: true,
                        sortOrder: 2,
                        unique: false,
                        variable: 'anotherDeprecated'
                    }
                ]
            };

            expect(() =>
                ContentTypeSchema.parse(contentTypeWithMultipleDeprecatedFields)
            ).not.toThrow();

            const parsed = ContentTypeSchema.parse(contentTypeWithMultipleDeprecatedFields);
            expect(parsed.fields).toBeDefined();
            expect(parsed.fields).toHaveLength(2);

            if (parsed.fields) {
                expect(parsed.fields[0].clazz).toBe(
                    'com.dotcms.contenttype.model.field.ImmutableBinaryField'
                );
                expect(parsed.fields[1].clazz).toBe(
                    'com.dotcms.contenttype.model.field.ImmutableSomeOtherDeprecatedField'
                );
            }
        });
    });
});
