export interface BinaryFieldMessage {
    message: string;
    severity: string;
    icon: string;
    args?: string[];
}

const BinaryFieldMessageMap = {
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

export const getBinaryFieldMessage = (
    messageKey: string,
    ...args: string[]
): BinaryFieldMessage => {
    const { message, severity, icon } = BinaryFieldMessageMap[messageKey];

    return {
        message,
        severity,
        icon,
        args
    };
};
