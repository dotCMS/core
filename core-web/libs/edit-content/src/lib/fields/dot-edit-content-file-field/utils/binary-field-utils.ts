import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { UiMessageI, UiMessageMap } from '../interfaces';

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
    },
    MULTIPLE_FILES_DROPPED: {
        message: 'dot.binary.field.drag.and.drop.error.multiple.files.dropped.message',
        severity: 'error',
        icon: 'pi pi-exclamation-triangle'
    }
};

export const getUiMessage = (messageKey: string, ...args: string[]): UiMessageI => {
    return {
        ...UiMessageMap[messageKey],
        args
    };
};

export const getFileMetadata = (contentlet: DotCMSContentlet) => {
    const { metaData, fieldVariable } = contentlet;

    const metadata = metaData || contentlet[`${fieldVariable}MetaData`];

    return metadata || {};
};

export const getFieldVersion = (contentlet: DotCMSContentlet) => {
    const { fileAssetVersion, fieldVariable } = contentlet;

    return fileAssetVersion || contentlet[`${fieldVariable}Version`];
};
