import { IAllProps } from '@tinymce/tinymce-react';

import { DotCMSBasicContentlet } from '@dotcms/types';
import {
    __BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__,
    __DEFAULT_TINYMCE_CONFIG__
} from '@dotcms/uve/internal';
export type DOT_EDITABLE_TEXT_FORMAT = 'html' | 'text';

export type DOT_EDITABLE_TEXT_MODE = 'minimal' | 'full' | 'plain';

export interface DotCMSEditableTextProps<T extends DotCMSBasicContentlet> {
    /**
     * Represents the field name of the `contentlet` that can be edited
     *
     * @memberof DotCMSEditableTextProps
     */
    fieldName: keyof T;
    /**
     * Represents the format of the editor which can be `text` or `html`
     *
     * @type {DOT_EDITABLE_TEXT_FORMAT}
     * @memberof DotCMSEditableTextProps
     */
    format?: DOT_EDITABLE_TEXT_FORMAT;
    /**
     * Represents the mode of the editor which can be `plain`, `minimal`, or `full`
     *
     * @type {DOT_EDITABLE_TEXT_MODE}
     * @memberof DotCMSEditableTextProps
     */
    mode?: DOT_EDITABLE_TEXT_MODE;
    /**
     * Represents the `contentlet` that can be inline edited
     *
     * @type {DotCMSBasicContentlet}
     * @memberof DotCMSEditableTextProps
     */
    contentlet: T;
}

const DEFAULT_TINYMCE_CONFIG: IAllProps['init'] = {
    ...__DEFAULT_TINYMCE_CONFIG__,
    licenseKey: 'gpl' // Using self-hosted license key
};

export const TINYMCE_CONFIG: {
    [key in DOT_EDITABLE_TEXT_MODE]: IAllProps['init'];
} = {
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
