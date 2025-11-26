import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'dot.binary.field.action.choose.file': 'Choose File',
    'dot.binary.field.action.create.new.file': 'Create New File',
    'dot.binary.field.action.create.new.file.label': 'File Name',
    'dot.binary.field.action.import.from.url.error.message':
        'The URL you requested is not valid. Please try again.',
    'dot.binary.field.action.import.from.url': 'Import from URL',
    'dot.binary.field.action.remove': 'Remove',
    'dot.binary.field.dialog.create.new.file.header': 'File Details',
    'dot.binary.field.dialog.import.from.url.header': 'URL',
    'dot.binary.field.drag.and.drop.error.could.not.load.message':
        '<strong>Couldn&apos;t load the file.</strong> Please try again or',
    'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message':
        'The file weight <strong>exceeds the limits of {0}</strong>, please   reduce size before uploading.',
    'dot.binary.field.drag.and.drop.error.file.not.supported.message':
        'This type of <strong>file is not supported</strong>, Please select a  {0} file.',
    'dot.binary.field.drag.and.drop.error.multiple.files.dropped.message':
        'You can only upload one file at a time.',
    'dot.binary.field.drag.and.drop.error.server.error.message':
        '<strong>Something went wrong</strong>, please try again or  contact our support team.',
    'dot.binary.field.drag.and.drop.message': 'Drag and Drop or',
    'dot.binary.field.error.type.file.not.extension': "Please add the file's extension",
    'dot.binary.field.error.type.file.not.supported.message':
        'This type of file is not supported. Please use a {0} file.',
    'dot.binary.field.file.bytes': 'Bytes',
    'dot.binary.field.file.dimension': 'Dimension',
    'dot.binary.field.file.size': 'File Size',
    'dot.binary.field.import.from.url.error.file.not.supported.message':
        'This type of file is not supported, Please import a {0} file.',
    'dot.common.cancel': 'Cancel',
    'dot.common.edit': 'Edit',
    'dot.common.import': 'Import',
    'dot.common.remove': 'Remove',
    'dot.common.save': 'Save',
    'error.form.validator.required': 'This field is required'
};

export const CONTENTTYPE_FIELDS_MESSAGE_MOCK = new MockDotMessageService(MESSAGES_MOCK);

const TEMP_IMAGE_MOCK: DotCMSTempFile = {
    fileName: 'Image.jpg',
    folder: 'folder',
    id: 'tempFileId',
    image: true,
    length: 10000,
    mimeType: 'image/jpeg',
    referenceUrl: '',
    thumbnailUrl:
        'https://images.unsplash.com/photo-1575936123452-b67c3203c357?auto=format&fit=crop&q=80&w=1000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8aW1hZ2V8ZW58MHx8MHx8fDA%3D',
    metadata: {
        contentType: 'image/jpeg',
        fileSize: 12312,
        length: 12312,
        isImage: true,
        modDate: 12312,
        name: 'image.png',
        sha256: '12345',
        title: 'Asset',
        version: 1,
        height: 100,
        width: 100,
        editableAsText: false
    }
};

const TEMP_VIDEO_MOCK = {
    fileName: 'video.mp4',
    folder: 'folder',
    id: 'tempFileId',
    image: false,
    length: 10000,
    mimeType: 'video/mp4',
    referenceUrl: 'https://www.w3schools.com/tags/movie.mp4',
    thumbnailUrl: ''
};

const TEMP_FILE_MOCK = {
    fileName: 'template.html',
    folder: 'folder',
    id: 'tempFileId',
    image: false,
    length: 10000,
    mimeType: 'text/html',
    referenceUrl: 'https://raw.githubusercontent.com/angular/angular/main/README.md',
    thumbnailUrl: '',
    content: 'HOLA'
};

export const TEMP_FILES_MOCK = [TEMP_IMAGE_MOCK, TEMP_VIDEO_MOCK, TEMP_FILE_MOCK];

export const CONTENTLET = {
    publishDate: '2023-10-24 13:21:49.682',
    inode: 'b22aa2f3-12af-4ea8-9d7d-164f98ea30b1',
    binaryField2: '/dA/af9294c29906dea7f4a58d845f569219/binaryField2/New-Image.png',
    host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
    binaryField2Version: '/dA/b22aa2f3-12af-4ea8-9d7d-164f98ea30b1/binaryField2/New-Image.png',
    locked: false,
    stInode: 'd1901a41d38b6686dd5ed8f910346d7a',
    contentType: 'BinaryField',
    identifier: 'af9294c29906dea7f4a58d845f569219',
    folder: 'SYSTEM_FOLDER',
    hasTitleImage: true,
    sortOrder: 0,
    binaryField2MetaData: {
        modDate: 1698153707197,
        sha256: 'e84030fe91978e483e34242f0631a81903cf53a945475d8dcfbb72da484a28d5',
        length: 29848,
        title: 'New-Image.png',
        version: 20220201,
        isImage: true,
        fileSize: 29848,
        name: 'New-Image.png',
        width: 738,
        contentType: 'image/png',
        height: 435
    },
    hostName: 'demo.dotcms.com',
    modDate: '2023-10-24 13:21:49.682',
    title: 'af9294c29906dea7f4a58d845f569219',
    baseType: 'CONTENT',
    archived: false,
    working: true,
    live: true,
    owner: 'dotcms.org.1',
    binaryField2ContentAsset: 'af9294c29906dea7f4a58d845f569219/binaryField2',
    languageId: 1,
    url: '/content.b22aa2f3-12af-4ea8-9d7d-164f98ea30b1',
    titleImage: 'binaryField2',
    modUserName: 'Admin User',
    hasLiveVersion: true,
    modUser: 'dotcms.org.1',
    __icon__: 'contentIcon',
    contentTypeIcon: 'event_note',
    variant: 'DEFAULT'
};

export const fileMetaData = {
    contentType: 'image/png',
    fileSize: 12312,
    length: 12312,
    modDate: 12312,
    name: 'image.png',
    sha256: '12345',
    title: 'Asset',
    version: 1,
    height: 100,
    width: 100,
    editableAsText: false,
    isImage: true
};
