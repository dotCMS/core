import { MockDotMessageService } from '@dotcms/utils-testing';

const MESSAGES_MOCK = {
    'contenttypes.content.edit.write.code': 'Write Code'
};

export const CONTENTTYPE_FIELDS_MESSAGE_MOCK = new MockDotMessageService(MESSAGES_MOCK);
