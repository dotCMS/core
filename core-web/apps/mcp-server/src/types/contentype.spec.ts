import { ContentTypeSchema } from './contentype';

describe('ContentType Types', () => {
    describe('FieldClazzEnum validation', () => {
        it('should accept known field types', () => {
            const mockContentType = {
                id: '1',
                name: 'Test Content Type',
                baseType: 'CONTENT' as const,
                clazz: 'test.class',
                defaultType: false,
                fixed: false,
                folder: 'folder',
                folderPath: '/folder',
                host: 'host123',
                iDate: 123456789,
                layout: [
                    {
                        divider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                            contentTypeId: 'ct1',
                            dataType: 'SYSTEM',
                            fieldType: 'Row',
                            fieldTypeLabel: 'Row',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 123456789,
                            indexed: false,
                            listed: false,
                            modDate: 123456789,
                            name: 'Row1',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 1,
                            unique: false
                        },
                        columns: [
                            {
                                columnDivider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                    contentTypeId: 'ct1',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Column',
                                    fieldTypeLabel: 'Column',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 123456789,
                                    indexed: false,
                                    listed: false,
                                    modDate: 123456789,
                                    name: 'Column1',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 2,
                                    unique: false
                                },
                                fields: [
                                    {
                                        clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                        contentTypeId: 'ct1',
                                        dataType: 'TEXT',
                                        fieldType: 'Text',
                                        fieldTypeLabel: 'Text',
                                        fieldVariables: [],
                                        fixed: false,
                                        forceIncludeInApi: false,
                                        iDate: 123456789,
                                        id: 'text-field-id',
                                        indexed: false,
                                        listed: false,
                                        modDate: 123456789,
                                        name: 'Title',
                                        readOnly: false,
                                        required: false,
                                        searchable: false,
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
                modDate: 123456789,
                multilingualable: false,
                sortOrder: 1,
                system: false,
                variable: 'testContentType',
                versionable: true
            };

            const result = ContentTypeSchema.safeParse(mockContentType);
            expect(result.success).toBe(true);
        });

        it('should accept deprecated field types like ImmutableRelationshipsTabField', () => {
            const mockContentType = {
                id: '1',
                name: 'Legacy Content Type',
                baseType: 'CONTENT' as const,
                clazz: 'test.class',
                defaultType: false,
                fixed: false,
                folder: 'folder',
                folderPath: '/folder',
                host: 'host123',
                iDate: 123456789,
                layout: [
                    {
                        divider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                            contentTypeId: 'ct1',
                            dataType: 'SYSTEM',
                            fieldType: 'Row',
                            fieldTypeLabel: 'Row',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 123456789,
                            indexed: false,
                            listed: false,
                            modDate: 123456789,
                            name: 'Row1',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 1,
                            unique: false
                        },
                        columns: [
                            {
                                columnDivider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                    contentTypeId: 'ct1',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Column',
                                    fieldTypeLabel: 'Column',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 123456789,
                                    indexed: false,
                                    listed: false,
                                    modDate: 123456789,
                                    name: 'Column1',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 2,
                                    unique: false
                                },
                                fields: [
                                    {
                                        clazz: 'com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField',
                                        contentTypeId: 'ct1',
                                        dataType: 'SYSTEM',
                                        fieldType: 'RelationshipsTab',
                                        fieldTypeLabel: 'Relationships Tab',
                                        fieldVariables: [],
                                        fixed: false,
                                        forceIncludeInApi: false,
                                        iDate: 123456789,
                                        id: 'deprecated-field-id',
                                        indexed: false,
                                        listed: false,
                                        modDate: 123456789,
                                        name: 'Relationships',
                                        readOnly: false,
                                        required: false,
                                        searchable: false,
                                        sortOrder: 3,
                                        unique: false,
                                        variable: 'relationships'
                                    }
                                ]
                            }
                        ]
                    }
                ],
                metadata: {},
                modDate: 123456789,
                multilingualable: false,
                sortOrder: 1,
                system: false,
                variable: 'legacyContentType',
                versionable: true
            };

            const result = ContentTypeSchema.safeParse(mockContentType);
            expect(result.success).toBe(true);
            if (result.success) {
                const firstField = result.data.layout[0]?.columns[0]?.fields[0];
                expect(firstField?.clazz).toBe('com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField');
            }
        });

        it('should reject invalid field clazz that does not match the pattern', () => {
            const mockContentType = {
                id: '1',
                name: 'Invalid Content Type',
                baseType: 'CONTENT' as const,
                clazz: 'test.class',
                defaultType: false,
                fixed: false,
                folder: 'folder',
                folderPath: '/folder',
                host: 'host123',
                iDate: 123456789,
                layout: [
                    {
                        divider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                            contentTypeId: 'ct1',
                            dataType: 'SYSTEM',
                            fieldType: 'Row',
                            fieldTypeLabel: 'Row',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 123456789,
                            indexed: false,
                            listed: false,
                            modDate: 123456789,
                            name: 'Row1',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 1,
                            unique: false
                        },
                        columns: [
                            {
                                columnDivider: {
                                    clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                    contentTypeId: 'ct1',
                                    dataType: 'SYSTEM',
                                    fieldType: 'Column',
                                    fieldTypeLabel: 'Column',
                                    fieldVariables: [],
                                    fixed: false,
                                    forceIncludeInApi: false,
                                    iDate: 123456789,
                                    indexed: false,
                                    listed: false,
                                    modDate: 123456789,
                                    name: 'Column1',
                                    readOnly: false,
                                    required: false,
                                    searchable: false,
                                    sortOrder: 2,
                                    unique: false
                                },
                                fields: [
                                    {
                                        clazz: 'invalid.field.class.NotADotCMSField',
                                        contentTypeId: 'ct1',
                                        dataType: 'TEXT',
                                        fieldType: 'Text',
                                        fieldTypeLabel: 'Text',
                                        fieldVariables: [],
                                        fixed: false,
                                        forceIncludeInApi: false,
                                        iDate: 123456789,
                                        id: 'invalid-field-id',
                                        indexed: false,
                                        listed: false,
                                        modDate: 123456789,
                                        name: 'Invalid Field',
                                        readOnly: false,
                                        required: false,
                                        searchable: false,
                                        sortOrder: 3,
                                        unique: false,
                                        variable: 'invalidField'
                                    }
                                ]
                            }
                        ]
                    }
                ],
                metadata: {},
                modDate: 123456789,
                multilingualable: false,
                sortOrder: 1,
                system: false,
                variable: 'invalidContentType',
                versionable: true
            };

            const result = ContentTypeSchema.safeParse(mockContentType);
            expect(result.success).toBe(false);
            if (!result.success) {
                expect(result.error.issues.some(issue => 
                    issue.message.includes('Field clazz must be a valid dotCMS field class')
                )).toBe(true);
            }
        });

        it('should accept other deprecated field types that follow the pattern', () => {
            const deprecatedFieldTypes = [
                'com.dotcms.contenttype.model.field.ImmutableRelationshipsTabField',
                'com.dotcms.contenttype.model.field.ImmutableSomeOtherDeprecatedField',
                'com.dotcms.contenttype.model.field.ImmutableAnotherLegacyField'
            ];

            deprecatedFieldTypes.forEach(clazz => {
                const mockContentType = {
                    id: '1',
                    name: 'Test Content Type',
                    baseType: 'CONTENT' as const,
                    clazz: 'test.class',
                    defaultType: false,
                    fixed: false,
                    folder: 'folder',
                    folderPath: '/folder',
                    host: 'host123',
                    iDate: 123456789,
                    layout: [
                        {
                            divider: {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                                contentTypeId: 'ct1',
                                dataType: 'SYSTEM',
                                fieldType: 'Row',
                                fieldTypeLabel: 'Row',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 123456789,
                                indexed: false,
                                listed: false,
                                modDate: 123456789,
                                name: 'Row1',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 1,
                                unique: false
                            },
                            columns: [
                                {
                                    columnDivider: {
                                        clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                                        contentTypeId: 'ct1',
                                        dataType: 'SYSTEM',
                                        fieldType: 'Column',
                                        fieldTypeLabel: 'Column',
                                        fieldVariables: [],
                                        fixed: false,
                                        forceIncludeInApi: false,
                                        iDate: 123456789,
                                        indexed: false,
                                        listed: false,
                                        modDate: 123456789,
                                        name: 'Column1',
                                        readOnly: false,
                                        required: false,
                                        searchable: false,
                                        sortOrder: 2,
                                        unique: false
                                    },
                                    fields: [
                                        {
                                            clazz: clazz,
                                            contentTypeId: 'ct1',
                                            dataType: 'SYSTEM',
                                            fieldType: 'Deprecated',
                                            fieldTypeLabel: 'Deprecated Field',
                                            fieldVariables: [],
                                            fixed: false,
                                            forceIncludeInApi: false,
                                            iDate: 123456789,
                                            id: 'deprecated-field-id',
                                            indexed: false,
                                            listed: false,
                                            modDate: 123456789,
                                            name: 'Deprecated',
                                            readOnly: false,
                                            required: false,
                                            searchable: false,
                                            sortOrder: 3,
                                            unique: false,
                                            variable: 'deprecated'
                                        }
                                    ]
                                }
                            ]
                        }
                    ],
                    metadata: {},
                    modDate: 123456789,
                    multilingualable: false,
                    sortOrder: 1,
                    system: false,
                    variable: 'testContentType',
                    versionable: true
                };

                const result = ContentTypeSchema.safeParse(mockContentType);
                expect(result.success).toBe(true);
            });
        });
    });
});