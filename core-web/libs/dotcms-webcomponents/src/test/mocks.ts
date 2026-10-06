import {
    ContentTypeColumnField,
    ContentTypeKeyValueField,
    ContentTypeRowField,
    ContentTypeSelectField,
    ContentTypeTextField,
    DotCMSClazzes,
    DotCMSContentTypeLayoutRow,
    DotCMSDataTypes,
    DotCMSFieldTypes
} from '@dotcms/dotcms-models';

import { DotContentletItem } from '../models/dot-contentlet-item.model';

/**
 * Properties every field mock shares. The discriminating trio — `clazz`, `dataType` and
 * `fieldType` — is deliberately left out so each mock below pins its own arm of the
 * `DotCMSContentTypeField` union.
 */
const commonFieldProperties = {
    contentTypeId: '',
    defaultValue: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: true,
    forceIncludeInApi: false,
    hint: '',
    iDate: 100,
    id: '',
    indexed: true,
    listed: true,
    modDate: 100,
    name: '',
    readOnly: true,
    required: true,
    searchable: true,
    sortOrder: 100,
    unique: true,
    values: '',
    variable: ''
};

export const basicField: ContentTypeTextField = {
    ...commonFieldProperties,
    clazz: DotCMSClazzes.TEXT,
    dataType: DotCMSDataTypes.TEXT,
    fieldType: DotCMSFieldTypes.TEXT,
    regexCheck: ''
};

const rowDivider: ContentTypeRowField = {
    ...commonFieldProperties,
    clazz: DotCMSClazzes.ROW,
    dataType: DotCMSDataTypes.SYSTEM,
    fieldType: DotCMSFieldTypes.ROW
};

const columnDivider: ContentTypeColumnField = {
    ...commonFieldProperties,
    clazz: DotCMSClazzes.COLUMN,
    dataType: DotCMSDataTypes.SYSTEM,
    fieldType: DotCMSFieldTypes.COLUMN
};

export const textFieldMock: ContentTypeTextField = {
    ...basicField,
    variable: 'textfield1',
    required: true,
    name: 'TexField'
};

export const keyValueFieldMock: ContentTypeKeyValueField = {
    ...commonFieldProperties,
    clazz: DotCMSClazzes.KEY_VALUE,
    dataType: DotCMSDataTypes.LONG_TEXT,
    fieldType: DotCMSFieldTypes.KEY_VALUE,
    defaultValue: 'key|value,llave|valor',
    name: 'Key Value:',
    required: false,
    variable: 'keyvalue2'
};

export const selectFieldMock: ContentTypeSelectField = {
    ...commonFieldProperties,
    clazz: DotCMSClazzes.SELECT,
    dataType: DotCMSDataTypes.TEXT,
    fieldType: DotCMSFieldTypes.SELECT,
    defaultValue: '2',
    name: 'Dropdwon',
    required: false,
    values: '|,labelA|1,labelB|2,labelC|3',
    variable: 'dropdown3'
};

export const dotFormLayoutMock: DotCMSContentTypeLayoutRow[] = [
    {
        divider: { ...rowDivider },
        columns: [
            {
                columnDivider: { ...columnDivider },
                fields: [textFieldMock]
            }
        ]
    },
    {
        divider: { ...rowDivider },
        columns: [
            {
                columnDivider: { ...columnDivider },
                fields: [keyValueFieldMock]
            },
            {
                columnDivider: { ...columnDivider },
                fields: [selectFieldMock]
            }
        ]
    }
];

export const fieldMockNotRequired: DotCMSContentTypeLayoutRow[] = [
    {
        divider: { ...rowDivider },
        columns: [
            {
                columnDivider: { ...columnDivider },
                fields: [keyValueFieldMock]
            }
        ]
    }
];

export const contentletMock: DotContentletItem = {
    typeVariable: 'Image',
    modDate: '2/5/2020 11:50AM',
    __wfstep__: 'Published',
    baseType: 'FILEASSET',
    inode: 'c68db8ec-b523-41b7-82bd-fcb7533d3cfa',
    __title__: 'pinos.jpg',
    Identifier: '10885ceb-7457-4571-bdbe-b2a2c0198bd1',
    permissions:
        'P654b0931-1027-41f7-ad4d-173115ed8ec1.2P P654b0931-1027-41f7-ad4d-173115ed8ec1.1P ',
    contentStructureType: '4',
    working: 'true',
    locked: 'false',
    live: 'true',
    owner: 'dotcms.org.1',
    identifier: '10885ceb-7457-4571-bdbe-b2a2c0198bd1',
    wfActionMapList: '[]',
    languageId: '1',
    __icon__: 'jpgIcon',
    statusIcons: '<span></span>',
    hasLiveVersion: 'false',
    deleted: 'false',
    structureInode: 'd5ea385d-32ee-4f35-8172-d37f58d9cd7a',
    __type__: '<div></div>',
    ownerCanRead: 'false',
    hasTitleImage: 'true',
    modUser: 'Admin User',
    ownerCanWrite: 'false',
    ownerCanPublish: 'false',
    title: '',
    sysPublishDate: '',
    mediaType: '',
    language: '',
    mimeType: '',
    titleImage: 'fileAsset',
    modDateMilis: 23434252456
};
