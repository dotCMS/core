import type { DotCMSBasicContentlet } from '@dotcms/types';
import {
    __BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__,
    __DEFAULT_TINYMCE_CONFIG__
} from '@dotcms/uve/internal';

export type DOT_EDITABLE_TEXT_FORMAT = 'html' | 'text';

export type DOT_EDITABLE_TEXT_MODE = 'minimal' | 'full' | 'plain';

/**
 * Props for {@link DotCMSEditableText}.
 */
export interface DotCMSEditableTextProps<T extends DotCMSBasicContentlet = DotCMSBasicContentlet> {
    /** The contentlet whose field is edited. */
    contentlet: T;
    /** The field of the contentlet to edit. */
    fieldName: keyof T;
    /** The editor format — `text` or `html`. Defaults to `text`. */
    format?: DOT_EDITABLE_TEXT_FORMAT;
    /** The editor toolbar preset — `plain`, `minimal` or `full`. Defaults to `plain`. */
    mode?: DOT_EDITABLE_TEXT_MODE;
}

type TinyMCEInit = Record<string, unknown>;

const DEFAULT_TINYMCE_CONFIG: TinyMCEInit = {
    ...__DEFAULT_TINYMCE_CONFIG__,
    licenseKey: 'gpl' // Self-hosted GPL license.
};

/**
 * TinyMCE init config per editor mode, composed from the shared dotCMS presets.
 */
export const TINYMCE_CONFIG: Record<DOT_EDITABLE_TEXT_MODE, TinyMCEInit> = {
    full: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.full
    },
    plain: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.plain
    },
    minimal: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.minimal
    }
};
