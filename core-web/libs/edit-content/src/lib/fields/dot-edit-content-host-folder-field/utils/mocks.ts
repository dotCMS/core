import { MockDotMessageService } from '@dotcms/utils-testing';

const FILE_MESSAGES_MOCK = {
    'dot.file.field.action.choose.file': 'Choose File',
    'dot.file.field.action.create.new.file': 'Create New File',
    'dot.file.field.action.create.new.file.label': 'File Name',
    'dot.file.field.action.import.from.url.error.message':
        'The URL you requested is not valid. Please try again.',
    'dot.file.field.action.import.from.url': 'Import from URL',
    'dot.file.field.action.remove': 'Remove',
    'dot.file.field.drag.and.drop.message': 'Drag and Drop or',
    'dot.file.field.action.select.existing.file': 'Select Existing File',
    'dot.common.cancel': 'Cancel',
    'dot.common.edit': 'Edit',
    'dot.common.import': 'Import',
    'dot.common.remove': 'Remove',
    'dot.common.save': 'Save',
    'error.form.validator.required': 'This field is required',
    'dot.file.field.host.folder.trigger.placeholder': 'Select Host/Folder',
    'dot.file.field.host.folder.sites.label': 'Sites',
    'dot.file.field.host.folder.sites.search.placeholder': 'Search sites...',
    'dot.file.field.host.folder.search.placeholder': 'Search folders in this site...',
    'dot.file.field.host.folder.action.select': 'Select',
    'dot.file.field.host.folder.action.load.more': 'Load {0} more',
    'dot.file.field.host.folder.copy.tooltip': 'Copy path',
    'dot.file.field.host.folder.empty.folders': 'No folders found',
    'dot.file.field.host.folder.error.sites': 'Unable to load sites. Please try again.',
    'dot.file.field.host.folder.error.folders': 'Unable to load folders. Please try again.',
    'dot.file.field.host.folder.error.search': 'Unable to search folders. Please try again.'
};

export const MessageServiceMock = new MockDotMessageService(FILE_MESSAGES_MOCK);
