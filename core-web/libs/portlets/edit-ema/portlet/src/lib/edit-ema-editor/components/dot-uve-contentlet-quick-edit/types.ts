import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';

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
 * DotCMSContentTypeField. Extends with `options` for dropdown / checkbox
 * / radio rendering.
 */
export type ContentletField = Pick<
    DotCMSContentTypeField,
    | 'name'
    | 'variable'
    | 'clazz'
    | 'required'
    | 'readOnly'
    | 'regexCheck'
    | 'dataType'
    | 'defaultValue'
    | 'fieldVariables'
    | 'fieldType'
> & {
    options?: Array<{ label: string; value: string }>;
};

export interface ContentletEditData {
    container: ContainerPayload | undefined;
    contentlet: DotCMSContentlet;
}
