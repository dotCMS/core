import { MESSAGES_TYPES, UIMessagesMap } from '../models';

export const UiMessageMap: UIMessagesMap = {
    DEFAULT: {
        message: 'dot.file.field.drag.and.drop.message',
        severity: 'info',
        icon: 'pi pi-upload'
    },
    SERVER_ERROR: {
        message: 'dot.file.field.drag.and.drop.error.server.error.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    FILE_TYPE_MISMATCH: {
        message: 'dot.file.field.drag.and.drop.error.file.not.supported.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    MAX_FILE_SIZE_EXCEEDED: {
        message: 'dot.file.field.drag.and.drop.error.file.maxsize.exceeded.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    MULTIPLE_FILES_DROPPED: {
        message: 'dot.file.field.drag.and.drop.error.multiple.files.dropped.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    }
};

export function getUiMessage(key: MESSAGES_TYPES) {
    return UiMessageMap[key];
}
