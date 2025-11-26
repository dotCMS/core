import {
    DotCMSClazzes,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow
} from '@dotcms/dotcms-models';

import { DotContentletItem } from '../models/dot-contentlet-item.model';

export const basicField: DotCMSContentTypeField = {
    clazz: DotCMSClazzes.TEXT,
    contentTypeId: '',
    dataType: '',
    defaultValue: '',
    fieldType: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: true,
    hint: '',
    iDate: 100,
    id: '',
    indexed: true,
    listed: true,
    modDate: 100,
    name: '',
    readOnly: true,
    regexCheck: '',
    required: true,
    searchable: true,
    sortOrder: 100,
    unique: true,
    values: '',
    variable: ''
};

export const dotFormLayoutMock: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        variable: 'textfield1',
                        required: true,
                        name: 'TexField',
                        fieldType: 'Text'
                    }
                ]
            }
        ]
    },
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2'
                    }
                ]
            },
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: '2',
                        fieldType: 'Select',
                        name: 'Dropdwon',
                        required: false,
                        values: '|,labelA|1,labelB|2,labelC|3',
                        variable: 'dropdown3'
                    }
                ]
            }
        ]
    }
];

export const fieldMockNotRequired: DotCMSContentTypeLayoutRow[] = [
    {
        divider: {
            ...basicField
        },
        columns: [
            {
                columnDivider: {
                    ...basicField
                },
                fields: [
                    {
                        ...basicField,
                        defaultValue: 'key|value,llave|valor',
                        fieldType: 'Key-Value',
                        name: 'Key Value:',
                        required: false,
                        variable: 'keyvalue2'
                    }
                ]
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
