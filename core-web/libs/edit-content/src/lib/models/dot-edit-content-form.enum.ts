import { DotCMSFieldType, DotCMSFieldTypes } from '@dotcms/dotcms-models';

/**
 * Field types that get no form control of their own.
 *
 * Rows, columns and tab dividers arrange other fields rather than holding a value; constant and
 * hidden fields hold one the editor never edits.
 */
export const NON_FORM_CONTROL_FIELD_TYPES: DotCMSFieldType[] = [
    DotCMSFieldTypes.ROW,
    DotCMSFieldTypes.COLUMN,
    DotCMSFieldTypes.TAB_DIVIDER,
    DotCMSFieldTypes.CONSTANT,
    DotCMSFieldTypes.HIDDEN
];

/**
 * Field types that never render as a field component — they exist to arrange the layout.
 *
 * `LINE_DIVIDER` is deliberately absent: it is a layout field in the content model but it does
 * render, so it has a component like any other.
 */
export const LAYOUT_ONLY_FIELD_TYPES: DotCMSFieldType[] = [
    DotCMSFieldTypes.ROW,
    DotCMSFieldTypes.COLUMN,
    DotCMSFieldTypes.TAB_DIVIDER,
    DotCMSFieldTypes.COLUMN_BREAK
];
