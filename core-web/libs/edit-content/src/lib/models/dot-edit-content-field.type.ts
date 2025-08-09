import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { FIELD_TYPES } from './dot-edit-content-field.enum';

export type DotEditContentFieldSingleSelectableDataTypes = string | boolean | number;

export type ContentletIdentifier = string;

export type CurrentContentActionsWithScheme = Record<string, DotCMSWorkflowAction[]>;

/**
 * Represents the field type.
 */
export type FieldType = (typeof FIELD_TYPES)[keyof typeof FIELD_TYPES];
