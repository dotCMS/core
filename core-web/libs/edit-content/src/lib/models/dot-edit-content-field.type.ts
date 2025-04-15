import { DotCMSWorkflowAction } from '@dotcms/dotcms-models';

export type DotEditContentFieldSingleSelectableDataTypes = string | boolean | number;

export type ContentletIdentifier = string;

export type CurrentContentActionsWithScheme = Record<string, DotCMSWorkflowAction[]>;
