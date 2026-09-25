import { DotCMSContentlet, DotCMSContentTypeBaseField } from '@dotcms/dotcms-models';

import { ContainerPayload } from '../../../shared/models';

/** Sentinel value emitted by the UVE SDK when hovering over an empty container. */
export const TEMP_EMPTY_CONTENTLET_TYPE = 'TEMP_EMPTY_CONTENTLET_TYPE';

export const CopyMode = {
    ALL_PAGES: 'all-pages',
    THIS_PAGE: 'this-page'
} as const;

export type CopyMode = (typeof CopyMode)[keyof typeof CopyMode];

/**
 * Pick only the fields needed for the quick-edit form from
 * the content model. Extends with `options` for dropdown / checkbox
 * / radio rendering.
 */
export type ContentletField = Pick<
    DotCMSContentTypeBaseField,
    | 'name'
    | 'variable'
    | 'clazz'
    | 'required'
    | 'readOnly'
    | 'dataType'
    | 'defaultValue'
    | 'fieldVariables'
    | 'fieldType'
> & {
    // Picked from the base rather than the union, and `regexCheck` declared separately: it is
    // not a property every field has — only Text, TextArea, WYSIWYG and Custom declare one —
    // so `keyof` the union does not include it.
    regexCheck?: string;
    options?: Array<{ label: string; value: string }>;
};

export interface ContentletEditData {
    container: ContainerPayload | undefined;
    contentlet: DotCMSContentlet;
}
