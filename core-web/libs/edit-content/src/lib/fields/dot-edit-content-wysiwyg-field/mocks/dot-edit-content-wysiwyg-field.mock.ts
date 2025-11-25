import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

import { COMMENT_TINYMCE } from '../dot-edit-content-wysiwyg-field.constant';

export const WYSIWYG_VARIABLE_NAME = 'variable';

export const WYSIWYG_MOCK: DotCMSContentTypeField = {
    clazz: 'com.dotcms.contenttype.model.field.ImmutableWYSIWYGField',
    contentTypeId: '93ebaff75f3e3887bea73ecd04588dc9',
    dataType: 'TEXT',
    fieldType: 'WYSIWYG',
    fieldTypeLabel: 'WYSIWYG',
    fieldVariables: [],
    fixed: false,
    hint: 'A hint text',
    iDate: 1698291913000,
    id: '96909fa20a00497cd3b766b52edac0ec',
    indexed: false,
    listed: false,
    modDate: 1698291913000,
    name: 'WYSIWYG',
    readOnly: false,
    required: false,
    searchable: false,
    sortOrder: 1,
    unique: false,
    values: '<p>HELLO</p>',
    variable: WYSIWYG_VARIABLE_NAME
};

export const WYSIWYG_FIELD_CONTENTLET_MOCK_NO_CONTENT: DotCMSContentlet = {
    archived: false,
    baseType: 'CONTENT',
    contentType: 'Test2',
    creationDate: 1727121715503,
    [WYSIWYG_VARIABLE_NAME]: '',
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: false,
    hasTitleImage: false,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    identifier: '415d8f589845d8d1d32bc4b955d47b9e',
    inode: '871e129a-be77-4571-9800-51c9a5ad5ef6',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '1727121715472',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    ownerName: 'Admin User',
    sortOrder: 0,
    stInode: 'fbe4e03c0f4154e8e4fdc8d483fedcc0',
    title: '415d8f589845d8d1d32bc4b955d47b9e',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: '/content.871e129a-be77-4571-9800-51c9a5ad5ef6',
    working: true
};

export const WYSIWYG_FIELD_CONTENTLET_MOCK_WITH_WYSIWYG_CONTENT: DotCMSContentlet = {
    archived: false,
    baseType: 'CONTENT',
    contentType: 'Test2',
    creationDate: 1727121715503,
    [WYSIWYG_VARIABLE_NAME]: `${COMMENT_TINYMCE}<p>contenido</p>`,
    folder: 'SYSTEM_FOLDER',
    hasLiveVersion: false,
    hasTitleImage: false,
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    hostName: 'demo.dotcms.com',
    identifier: '415d8f589845d8d1d32bc4b955d47b9e',
    inode: '871e129a-be77-4571-9800-51c9a5ad5ef6',
    languageId: 1,
    live: false,
    locked: false,
    modDate: '1727121715472',
    modUser: 'dotcms.org.1',
    modUserName: 'Admin User',
    owner: 'dotcms.org.1',
    ownerName: 'Admin User',
    sortOrder: 0,
    stInode: 'fbe4e03c0f4154e8e4fdc8d483fedcc0',
    title: '415d8f589845d8d1d32bc4b955d47b9e',
    titleImage: 'TITLE_IMAGE_NOT_FOUND',
    url: '/content.871e129a-be77-4571-9800-51c9a5ad5ef6',
    working: true
};
