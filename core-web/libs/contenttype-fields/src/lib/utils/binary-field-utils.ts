export enum UI_MESSAGE_KEYS {
    DEFAULT = 'DEFAULT',
    SERVER_ERROR = 'SERVER_ERROR',
    FILE_TYPE_MISMATCH = 'FILE_TYPE_MISMATCH',
    MAX_FILE_SIZE_EXCEEDED = 'MAX_FILE_SIZE_EXCEEDED'
}

export interface UiMessageI {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}

type UiMessageMap = {
    [key in UI_MESSAGE_KEYS]: UiMessageI;
};

const UiMessageMap: UiMessageMap = {
    DEFAULT: {
        message: 'dot.binary.field.drag.and.drop.message',
        severity: 'info',
        icon: 'pi pi-upload'
    },
    SERVER_ERROR: {
        message: 'dot.binary.field.drag.and.drop.error.server.error.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    FILE_TYPE_MISMATCH: {
        message: 'dot.binary.field.drag.and.drop.error.file.not.supported.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    },
    MAX_FILE_SIZE_EXCEEDED: {
        message: 'dot.binary.field.drag.and.drop.error.file.maxsize.exceeded.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    }
};

export const getUiMessage = (messageKey: string, ...args: string[]): UiMessageI => {
    const { message, severity, icon } = UiMessageMap[messageKey];

    return {
        message,
        severity,
        icon,
        args
    };
};
