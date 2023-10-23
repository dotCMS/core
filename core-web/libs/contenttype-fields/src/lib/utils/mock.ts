import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'dot.binary.field.action.choose.file': 'Choose File',
    'dot.binary.field.action.create.new.file': 'Create New File',
    'dot.binary.field.action.import.from.url': 'Import from URL',
    'dot.binary.field.action.import.from.url.error.message':
        'The URL you requested is not valid. Please try again.',
    'dot.binary.field.action.remove': 'Remove',
    'dot.binary.field.dialog.create.new.file.header': 'File Details',
    'dot.binary.field.dialog.import.from.url.header': 'URL',
    'dot.binary.field.drag.and.drop.message': 'Drag and Drop or',
    'dot.binary.field.drag.and.drop.error.could.not.load.message':
        '<strong>Couldn&apos;t load the file.</strong> Please try again or<br/>',
    'dot.binary.field.drag.and.drop.error.file.not.supported.message':
        'This type of <strong>file is not supported</strong>, Please select a <br /> {0} file.',
    'dot.binary.field.error.type.file.not.supported.message':
        'This type of file is not supported. Please use a {0} file.',
    'dot.binary.field.error.type.file.not.extension': "Please add the file's extension",
    'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message':
        'The file weight <strong>exceeds the limits of {0}</strong>, please  <br /> reduce size before uploading.',
    'dot.binary.field.drag.and.drop.error.server.error.message':
        '<strong>Something went wrong</strong>, please try again or <br/> contact our support team.',
    'dot.common.cancel': 'Cancel',
    'dot.common.import': 'Import',
    'dot.common.save': 'Save',
    'error.form.validator.required': 'This field is required'
};

export const CONTENTTYPE_FIELDS_MESSAGE_MOCK = new MockDotMessageService(MESSAGES_MOCK);

const TEMP_IMAGE_MOCK = {
    fileName: 'Image.jpg',
    folder: 'folder',
    id: 'tempFileId',
    image: true,
    length: 10000,
    mimeType: 'image/jpeg',
    referenceUrl: '',
    thumbnailUrl:
        'https://images.unsplash.com/photo-1575936123452-b67c3203c357?auto=format&fit=crop&q=80&w=1000&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8aW1hZ2V8ZW58MHx8MHx8fDA%3D'
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
    referenceUrl: 'https://raw.githubusercontent.com/angular/angular/master/README.md',
    thumbnailUrl: '',
    content: 'HOLA'
};

export const TEMP_FILES_MOCK = [TEMP_IMAGE_MOCK, TEMP_VIDEO_MOCK, TEMP_FILE_MOCK];
