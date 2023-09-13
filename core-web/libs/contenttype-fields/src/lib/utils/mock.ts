import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'contenttypes.content.edit.write.code': 'Write Code',
    'dot.binary.field.drag.and.drop.message':
        'Drag and Drop or <a id="choose-file">Choose File</a> to upload',
    'dot.binary.field.drag.and.drop.error.could.not.load.message':
        '<strong>Couldn&apos;t load the file.</strong> Please try again or <br/> <a id="choose-file">Choose File</a> to upload',
    'dot.binary.field.drag.and.drop.error.file.not.supported.message':
        'This type of <strong>file is not supported</strong>, Please select a <br /> {0} file. <a id="choose-file">Choose File</a>',
    'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message':
        'The file weight <strong>exceeds the limits of {0}</strong>, please  <br /> reduce size before uploading. <a id="choose-file">Choose File</a>',
    'dot.binary.field.drag.and.drop.error.unknown.error.message':
        '<strong>Something went wrong</strong>, please try again or <br/> contact our support team. <a id="choose-file">Choose File</a>'
};

export const CONTENTTYPE_FIELDS_MESSAGE_MOCK = new MockDotMessageService(MESSAGES_MOCK);
