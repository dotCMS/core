export interface DropZoneMessage {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}

const DropZoneMessageMap = {
    default: {
        message: 'dot.binary.field.drag.and.drop.message',
        severity: 'info',
        icon: 'pi pi-upload'
    },
    couldNotLoad: {
        message: 'dot.binary.field.drag.and.drop.error.could.not.load.message',
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

export const getDropZoneMessage = (messageKey: string, ...args: string[]): DropZoneMessage => {
    const { message, severity, icon } = DropZoneMessageMap[messageKey];

    return {
        message,
        severity,
        icon,
        args
    };
};
