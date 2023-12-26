import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { FormControl } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';

import { EditEmaPaletteContentTypeComponent } from './edit-ema-palette-content-type.component';

const CONTENTTYPE_MOCK = [
    {
        baseType: 'CONTENT',
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        description: 'Activities available at desitnations',
        detailPage: 'e5f131d2-1952-4596-bbbf-28fb28021b68',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        folderPath: '/',
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        iDate: 1567778770000,
        icon: 'paragliding',
        id: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
        layout: [
            {
                divider: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                    dataType: 'SYSTEM',
                    fieldContentTypeProperties: [],
                    fieldType: 'Row',
                    fieldTypeLabel: 'Row',
                    fieldVariables: [],
                    fixed: false,
                    forceIncludeInApi: false,
                    iDate: 1567778770000,
                    id: '92660884-13cf-47d1-88aa-cae114e74f3e',
                    indexed: false,
                    listed: false,
                    modDate: 1703020623000,
                    name: 'fields-0',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 0,
                    unique: false,
                    variable: 'fields0'
                },
                columns: [
                    {
                        columnDivider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                            dataType: 'SYSTEM',
                            fieldContentTypeProperties: [],
                            fieldType: 'Column',
                            fieldTypeLabel: 'Column',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1567778770000,
                            id: 'a70e9c49-d33b-46dd-b44b-3e9e6669cccb',
                            indexed: false,
                            listed: false,
                            modDate: 1703020623000,
                            name: 'fields-1',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 1,
                            unique: false,
                            variable: 'fields1'
                        },
                        fields: [
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'SYSTEM',
                                fieldType: 'Host-Folder',
                                fieldTypeLabel: 'Site or Folder',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778788000,
                                id: '3589acb1-a542-49d3-b794-21c1780d34fe',
                                indexed: true,
                                listed: false,
                                modDate: 1703020623000,
                                name: 'Host',
                                readOnly: false,
                                required: true,
                                searchable: false,
                                sortOrder: 2,
                                unique: false,
                                variable: 'contentHost'
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'TEXT',
                                fieldType: 'Text',
                                fieldTypeLabel: 'Text',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778844000,
                                id: 'e1f99107-fd0e-49d4-a099-1cc10aa284d8',
                                indexed: true,
                                listed: true,
                                modDate: 1703020623000,
                                name: 'Title',
                                readOnly: false,
                                required: true,
                                searchable: true,
                                sortOrder: 3,
                                unique: false,
                                variable: 'title'
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableCustomField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'LONG_TEXT',
                                fieldType: 'Custom-Field',
                                fieldTypeLabel: 'Custom Field',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778884000,
                                id: '9ae4fc76-0d0f-4612-88d1-e178fa1a4a11',
                                indexed: true,
                                listed: false,
                                modDate: 1703020623000,
                                name: 'URL Title',
                                readOnly: false,
                                required: true,
                                searchable: false,
                                sortOrder: 4,
                                unique: true,
                                values: "#dotParse('/application/vtl/custom-fields/url-title.vtl')",
                                variable: 'urlTitle'
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'LONG_TEXT',
                                fieldType: 'Textarea',
                                fieldTypeLabel: 'Textarea',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778909000,
                                id: '806f5a87-48d5-40b3-8df8-8e1f8dbfc6ea',
                                indexed: false,
                                listed: false,
                                modDate: 1703020624000,
                                name: 'Description',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 5,
                                unique: false,
                                variable: 'description'
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTagField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'SYSTEM',
                                fieldType: 'Tag',
                                fieldTypeLabel: 'Tag',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778923000,
                                id: 'cbca2814-9f43-42df-bc6f-8ce1a151390b',
                                indexed: true,
                                listed: false,
                                modDate: 1703020624000,
                                name: 'Tags',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 6,
                                unique: false,
                                variable: 'tags'
                            }
                        ]
                    },
                    {
                        columnDivider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                            dataType: 'SYSTEM',
                            fieldContentTypeProperties: [],
                            fieldType: 'Column',
                            fieldTypeLabel: 'Column',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1567778801000,
                            id: 'd80efc2d-d968-42f8-a15c-067c2447a20e',
                            indexed: false,
                            listed: false,
                            modDate: 1703020624000,
                            name: 'fields-2',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 7,
                            unique: false,
                            variable: 'fields2'
                        },
                        fields: [
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableBinaryField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'SYSTEM',
                                fieldType: 'Binary',
                                fieldTypeLabel: 'Binary',
                                fieldVariables: [
                                    {
                                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                                        fieldId: 'd4a6bff9-7459-417a-ac57-8c0f9d2dc6d1',
                                        id: '50f8dfdc-08c6-44fc-bbd5-85ca5a54d9b7',
                                        key: 'accept',
                                        value: 'image/*'
                                    }
                                ],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778809000,
                                id: 'd4a6bff9-7459-417a-ac57-8c0f9d2dc6d1',
                                indexed: false,
                                listed: false,
                                modDate: 1703020624000,
                                name: 'Image',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 8,
                                unique: false,
                                variable: 'image'
                            },
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'TEXT',
                                fieldType: 'Text',
                                fieldTypeLabel: 'Text',
                                fieldVariables: [],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1567778825000,
                                id: 'e977f782-53e7-4c5c-a8ed-ba15bf1d4f45',
                                indexed: false,
                                listed: false,
                                modDate: 1703020624000,
                                name: 'Alt Tag',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 9,
                                unique: false,
                                variable: 'altTag'
                            }
                        ]
                    }
                ]
            },
            {
                divider: {
                    clazz: 'com.dotcms.contenttype.model.field.ImmutableRowField',
                    contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                    dataType: 'SYSTEM',
                    fieldContentTypeProperties: [],
                    fieldType: 'Row',
                    fieldTypeLabel: 'Row',
                    fieldVariables: [],
                    fixed: false,
                    forceIncludeInApi: false,
                    iDate: 1568315901000,
                    id: '7dad4390-3fad-498d-ae91-20aee43d5b19',
                    indexed: false,
                    listed: false,
                    modDate: 1703020624000,
                    name: 'fields-3',
                    readOnly: false,
                    required: false,
                    searchable: false,
                    sortOrder: 10,
                    unique: false,
                    variable: 'fields3'
                },
                columns: [
                    {
                        columnDivider: {
                            clazz: 'com.dotcms.contenttype.model.field.ImmutableColumnField',
                            contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                            dataType: 'SYSTEM',
                            fieldContentTypeProperties: [],
                            fieldType: 'Column',
                            fieldTypeLabel: 'Column',
                            fieldVariables: [],
                            fixed: false,
                            forceIncludeInApi: false,
                            iDate: 1568315901000,
                            id: '54699dae-f65a-477d-875e-06019a544fd4',
                            indexed: false,
                            listed: false,
                            modDate: 1703020624000,
                            name: 'fields-4',
                            readOnly: false,
                            required: false,
                            searchable: false,
                            sortOrder: 11,
                            unique: false,
                            variable: 'fields4'
                        },
                        fields: [
                            {
                                clazz: 'com.dotcms.contenttype.model.field.ImmutableWysiwygField',
                                contentTypeId: '778f3246-9b11-4a2a-a101-e7fdf111bdad',
                                dataType: 'LONG_TEXT',
                                fieldType: 'WYSIWYG',
                                fieldTypeLabel: 'WYSIWYG',
                                fieldVariables: [
                                    {
                                        clazz: 'com.dotcms.contenttype.model.field.ImmutableFieldVariable',
                                        fieldId: 'b3b7d7c2-3dcd-4fca-af31-8eda00363eab',
                                        id: 'e7e6144a-e4b2-47ad-aa17-555293aaa36a',
                                        key: 'tinymceprops',
                                        value: "#set($dontShowIcon=true) #dotParse('/application/wysiwyg/wysiwyg-minimal.vtl')"
                                    }
                                ],
                                fixed: false,
                                forceIncludeInApi: false,
                                iDate: 1568315854000,
                                id: 'b3b7d7c2-3dcd-4fca-af31-8eda00363eab',
                                indexed: false,
                                listed: false,
                                modDate: 1703020624000,
                                name: 'Body',
                                readOnly: false,
                                required: false,
                                searchable: false,
                                sortOrder: 12,
                                unique: false,
                                variable: 'body'
                            }
                        ]
                    }
                ]
            }
        ],
        modDate: 1703020624000,
        multilingualable: false,
        nEntries: 10,
        name: 'Activity',
        siteName: 'demo.dotcms.com',
        sortOrder: 0,
        system: false,
        systemActionMappings: [
            {
                archived: false,
                creationDate: 1703026814428,
                defaultScheme: false,
                description: '',
                entryActionId: null,
                id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                mandatory: false,
                modDate: 1703020631984,
                name: 'System Workflow',
                system: true
            }
        ],
        urlMapPattern: '/activities/{urlTitle}',
        variable: 'Activity',
        versionable: true,
        workflows: [
            {
                archived: false,
                creationDate: 1703026814428,
                defaultScheme: false,
                description: '',
                entryActionId: null,
                id: 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2',
                mandatory: false,
                modDate: 1703020631984,
                name: 'System Workflow',
                system: true
            }
        ]
    }
];

describe('EditEmaPaletteContentTypeComponent', () => {
    let spectator: Spectator<EditEmaPaletteContentTypeComponent>;

    const createComponent = createComponentFactory({
        component: EditEmaPaletteContentTypeComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: {
                    get() {
                        return 'Sample';
                    }
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                contentTypes: CONTENTTYPE_MOCK,
                filter: '',
                control: new FormControl(''),
                isLoading: false
            }
        });
    });

    it('should emit dragStart event on drag start', () => {
        const event = {} as DragEvent;
        const dragSpy = jest.spyOn(spectator.component.dragStart, 'emit');

        spectator.component.onDragStart(event);
        expect(dragSpy).toHaveBeenCalledWith(event);
    });

    it('should emit dragEnd event on drag end', () => {
        const event = {} as DragEvent;
        const dragSpy = jest.spyOn(spectator.component.dragEnd, 'emit');

        spectator.component.onDragEnd(event);
        expect(dragSpy).toHaveBeenCalledWith(event);
    });

    it('should emit showContentlets event with contentTypeName', () => {
        const contentTypeName = 'exampleContentType';

        const spy = jest.spyOn(spectator.component.showContentlets, 'emit');
        spectator.component.showContentletsFromContentType(contentTypeName);
        expect(spy).toHaveBeenCalledWith(contentTypeName);
    });

    it('should render the content type list', () => {
        expect(spectator.query('.contenttype-card')).not.toBeNull();
    });

    it('should the content type list hace data-item attribute', () => {
        expect(spectator.query('.contenttype-card')).toHaveAttribute('data-item');
    });
});
