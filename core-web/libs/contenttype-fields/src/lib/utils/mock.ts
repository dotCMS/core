import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'dot.binary.field.action.choose.file': 'Choose File',
    'dot.binary.field.action.create.new.file': 'Create New File',
    'dot.binary.field.action.import.from.url': 'Import from URL',
    'dot.binary.field.action.remove': 'Remove',
    'dot.binary.field.dialog.create.new.file.header': 'File Details',
    'dot.binary.field.dialog.import.from.url.header': 'URL',
    'dot.binary.field.drag.and.drop.message': 'Drag and Drop or',
    'dot.binary.field.drag.and.drop.error.could.not.load.message':
        '<strong>Couldn&apos;t load the file.</strong> Please try again or<br/>',
    'dot.binary.field.drag.and.drop.error.file.not.supported.message':
        'This type of <strong>file is not supported</strong>, Please select a <br /> {0} file.',
    'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message':
        'The file weight <strong>exceeds the limits of {0}</strong>, please  <br /> reduce size before uploading.',
    'dot.binary.field.drag.and.drop.error.server.error.message':
        '<strong>Something went wrong</strong>, please try again or <br/> contact our support team.',
    'dot.binary.field.action.cancel': 'Cancel',
    'dot.binary.field.action.import': 'Import'
};

export const CONTENTTYPE_FIELDS_MESSAGE_MOCK = new MockDotMessageService(MESSAGES_MOCK);
