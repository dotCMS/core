import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'dot.file.field.action.choose.file': 'Choose File',
    'dot.file.field.action.create.new.file': 'Create New File',
    'dot.file.field.action.create.new.file.label': 'File Name',
    'dot.file.field.action.import.from.url.error.message':
        'The URL you requested is not valid. Please try again.',
    'dot.file.field.action.import.from.url': 'Import from URL',
    'dot.file.field.action.remove': 'Remove',
    'dot.file.field.dialog.create.new.file.header': 'File Details',
    'dot.file.field.dialog.import.from.url.header': 'URL',
    'dot.file.field.drag.and.drop.error.could.not.load.message':
        '<strong>Couldn&apos;t load the file.</strong> Please try again or',
    'dot.file.field.drag.and.drop.error.file.maxsize.exceeded.message':
        'The file weight <strong>exceeds the limits of {0}</strong>, please   reduce size before uploading.',
    'dot.file.field.drag.and.drop.error.file.not.supported.message':
        'This type of <strong>file is not supported</strong>, Please select a  {0} file.',
    'dot.file.field.drag.and.drop.error.multiple.files.dropped.message':
        'You can only upload one file at a time.',
    'dot.file.field.drag.and.drop.error.server.error.message':
        '<strong>Something went wrong</strong>, please try again or  contact our support team.',
    'dot.file.field.drag.and.drop.message': 'Drag and Drop or',
    'dot.file.field.error.type.file.not.extension': "Please add the file's extension",
    'dot.file.field.error.type.file.not.supported.message':
        'This type of file is not supported. Please use a {0} file.',
    'dot.file.field.file.bytes': 'Bytes',
    'dot.file.field.file.dimension': 'Dimension',
    'dot.file.field.file.size': 'File Size',
    'dot.file.field.import.from.url.error.file.not.supported.message':
        'This type of file is not supported, Please import a {0} file.',
    'dot.file.field.action.generate.with.dotai': 'Generate with dotAI',
    'dot.file.field.action.select.existing.file': 'Select Existing File',
    'dot.common.cancel': 'Cancel',
    'dot.common.edit': 'Edit',
    'dot.common.import': 'Import',
    'dot.common.remove': 'Remove',
    'dot.common.save': 'Save',
    'error.form.validator.required': 'This field is required'
};

export const MessageServiceMock = new MockDotMessageService(MESSAGES_MOCK);
