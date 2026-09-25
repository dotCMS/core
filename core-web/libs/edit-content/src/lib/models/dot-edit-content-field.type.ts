import { DotCMSFieldType, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

export type DotEditContentFieldSingleSelectableDataTypes = string | boolean | number;

export type ContentletIdentifier = string;

export type CurrentContentActionsWithScheme = Record<string, DotCMSWorkflowAction[]>;

/**
 * Represents the field type.
 *
 * Kept as an alias so the many call sites that speak of a "FieldType" still read naturally,
 * but it is now the workspace's single field-type vocabulary rather than a second copy of it
 * derived from a local constant object (issue #37670, FR-005).
 */
export type FieldType = DotCMSFieldType;
