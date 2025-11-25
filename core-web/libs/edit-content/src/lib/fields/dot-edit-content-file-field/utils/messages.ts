import { MESSAGES_TYPES, UIMessage, UIMessagesMap } from '../models';

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

/**
 * Returns a uiMessage given its key
 * @param key The key of the uiMessage
 * @returns The uiMessage
 */
export function getUiMessage(key: MESSAGES_TYPES): UIMessage {
    const uiMessage = UiMessageMap[key];

    if (!uiMessage) {
        throw new Error(`Key ${key} not found in UiMessageMap`);
    }

    return uiMessage;
}
