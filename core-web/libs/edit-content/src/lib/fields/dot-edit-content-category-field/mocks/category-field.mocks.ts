import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { DotCategoryFieldCategory } from '../models/dot-category-field.models';

export const CATEGORY_FIELD_VARIABLE_NAME = 'categorias';

export const CATEGORY_FIELD_CONTENTLET_MOCK: DotCMSContentlet = {
    modDate: '',
    archived: false,
    baseType: 'CONTENT',
    [CATEGORY_FIELD_VARIABLE_NAME]: [
        {
            '11111': 'Cleaning Supplies'
        },
        {
            '22222': 'Concrete & Cement'
        }
    ],
    contentType: 'TEST',
    creationDate: 1719237692960,
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: true,
    hasTitleImage: false,
    host: '8a7d5e23-da1e-420a-b4f0-471e7da8ea2d',
    hostName: 'default',
    identifier: '389e76f7c51714b8087fa9950a8f271b',
    inode: '5c42dfca-bddc-4d1e-a0e1-2dfeca952c5e',
    languageId: 1,
    live: true,
    locked: false,
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    ownerName: 'Admin User',
    publishDate: 1719237693091,
    publishUser: 'dotcms.org.1',
    publishUserName: 'Admin User',
    sortOrder: 0,
    stInode: '61226fd915b7f025da020fc1f5856ab7',
    title: '389e76f7c51714b8087fa9950a8f271b',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: '/content.5c42dfca-bddc-4d1e-a0e1-2dfeca952c5e',
    working: true
};

export const CATEGORY_FIELD_MOCK: DotCMSContentTypeField = {
    categories: {
        categoryName: 'Categorias',
        description: null,
        inode: 'b3da6475e34655bed79919984bc34fc4',
        key: 'categorias',
        keywords: '',
        sortOrder: 0
    },
    clazz: 'com.dotcms.contenttype.model.field.ImmutableCategoryField',
    contentTypeId: '61226fd915b7f025da020fc1f5856ab7',
    dataType: 'SYSTEM',
    fieldType: 'Category',
    fieldTypeLabel: 'Category',
    fieldVariables: [],
    fixed: false,
    forceIncludeInApi: false,
    iDate: 1718916209000,
    id: '76f9f11132d288260e712056a4f950c5',
    indexed: true,
    listed: false,
    modDate: 1718998303000,
    name: 'Categorias',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 2,
    unique: false,
    values: 'b3da6475e34655bed79919984bc34fc4',
    variable: 'categorias'
};

export const CATEGORIES_WITH_CHILD_MOCK: DotCategoryFieldCategory[] = [
    {
        active: true,
        categoryName: 'Adhesives & Sealants',
        categoryVelocityVarName: '1f208488057007cedda0e0b5d52ee3b3',
        childrenCount: 10,
        description: null,
        iDate: 1719242808952,
        identifier: null,
        inode: '11111',
        key: '1f208488057007cedda0e0b5d52ee3b3',
        keywords: null,
        modDate: 1718916179985,
        owner: '',
        sortOrder: 0,
        type: 'category',
        checked: false
    }
];

export const CATEGORIES_MOCK: DotCategoryFieldCategory[][] = [
    [
        {
            active: true,
            categoryName: 'Cleaning Supplies',
            categoryVelocityVarName: '1f208488057007cedda0e0b5d52ee3b3',
            childrenCount: 1, // This make show the caret of has children
            description: null,
            iDate: 1719275768170,
            identifier: null,
            inode: '8259cf9bb5b895196fc2b4127e929f02',
            key: '1f208488057007cedda0e0b5d52ee3b3',
            keywords: null,
            modDate: 1718916179985,
            owner: '',
            sortOrder: 0,
            type: 'category',
            checked: true
        }
    ],
    [
        {
            active: true,
            categoryName: 'Concrete & Cement',
            categoryVelocityVarName: 'd2fb8e67c390e3b84cd613fa15aad5d4',
            childrenCount: 0,
            description: null,
            iDate: 1719275768170,
            identifier: null,
            inode: '22222',
            key: 'd2fb8e67c390e3b84cd613fa15aad5d4',
            keywords: null,
            modDate: 1718916180738,
            owner: '',
            sortOrder: 0,
            type: 'category',
            checked: false
        }
    ]
];

export const SELECTED_MOCK = [CATEGORIES_MOCK[0][0].inode];
