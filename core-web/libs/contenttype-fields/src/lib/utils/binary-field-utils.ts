const UiMessage = {
    default: {
        message: 'dot.binary.field.drag.and.drop.message',
        severity: 'info',
        icon: 'pi pi-upload'
    },
    serverError: {
        message: 'dot.binary.field.drag.and.drop.error.server.error.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    fileTypeMismatch: {
        message: 'dot.binary.field.drag.and.drop.error.file.not.supported.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    maxFileSizeExceeded: {
        message: 'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    }
};

export enum UI_MESSAGE_KEYS {
    DEFAULT = 'default',
    SERVER_ERROR = 'serverError',
    FILE_TYPE_MISMATCH = 'fileTypeMismatch',
    MAX_FILE_SIZE_EXCEEDED = 'maxFileSizeExceeded'
}

export interface UiMessageI {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}

export const getUiMessage = (messageKey: string, ...args: string[]): UiMessageI => {
    const { message, severity, icon } = UiMessage[messageKey];

    return {
        message,
        severity,
        icon,
        args
    };
};
