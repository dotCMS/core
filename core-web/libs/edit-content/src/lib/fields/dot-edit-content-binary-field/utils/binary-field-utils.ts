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
    // The preview falls back here when a temp file has no metadata (image-editor saves
    // come back with `metadata: null` and a null contentlet), so guard the destructure.
    if (!contentlet) {
        return {};
    }

    const { metaData, fieldVariable } = contentlet;

    const metadata = metaData || contentlet[`${fieldVariable}MetaData`];

    return metadata || {};
};

export const getFieldVersion = (contentlet: DotCMSContentlet) => {
    const { fileAssetVersion, fieldVariable } = contentlet;

    return fileAssetVersion || contentlet[`${fieldVariable}Version`];
};

/**
 * Parses a focal point string from a binary field's metadata (`"x,y"`, normalized 0..1)
 * into a point the image editor can seed its marker with. The backend uses `"0.0"` / `(0,0)`
 * to mean "no focal point", so that — like an absent or invalid value — yields `undefined`,
 * letting the editor open centred.
 *
 * @param value The raw `focalPoint` metadata value, e.g. `"0.88,0.31"`
 * @returns The parsed point, or `undefined` when unset/invalid
 */
export const parseFocalPoint = (
    value: string | undefined | null
): { x: number; y: number } | undefined => {
    if (!value) {
        return undefined;
    }

    const [x, y] = value.split(',').map(Number);

    if (!Number.isFinite(x) || !Number.isFinite(y) || (x === 0 && y === 0)) {
        return undefined;
    }

    return { x, y };
};
