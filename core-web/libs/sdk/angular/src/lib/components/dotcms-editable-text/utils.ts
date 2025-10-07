import { EditorComponent } from '@tinymce/tinymce-angular';

import {
    __BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__,
    __DEFAULT_TINYMCE_CONFIG__
} from '@dotcms/uve/internal';

export type DOT_EDITABLE_TEXT_MODE = 'minimal' | 'full' | 'plain';

export type DOT_EDITABLE_TEXT_FORMAT = 'html' | 'text';

const DEFAULT_TINYMCE_CONFIG: EditorComponent['init'] = {
    ...__DEFAULT_TINYMCE_CONFIG__,
    license_key: 'gpl' // Using self-hosted license key
};

export const TINYMCE_CONFIG: {
    [key in DOT_EDITABLE_TEXT_MODE]: EditorComponent['init'];
} = {
    minimal: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.minimal
    },
    full: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.full
    },
    plain: {
        ...DEFAULT_TINYMCE_CONFIG,
        ...__BASE_TINYMCE_CONFIG_WITH_NO_DEFAULT__.plain
    }
};
