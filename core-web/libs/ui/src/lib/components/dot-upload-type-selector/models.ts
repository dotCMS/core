import { TreeNodeData } from '@dotcms/dotcms-models';

import { UPLOAD_SELECTOR_OPTIONS } from './constants';

/**
 * Base type an upload is created as. Narrowed to what the selector actually offers
 * (`DOTASSET` / `FILEASSET`) rather than the full base-type enum.
 */
export type DotUploadBaseType = (typeof UPLOAD_SELECTOR_OPTIONS)[number]['baseType'];

/** What the Asset/File prompt needs to render. `files` is set only in the drag-and-drop flow. */
export interface DotUploadSelectorPayload {
    targetFolder?: TreeNodeData;
    files?: FileList;
}

/** The user's answer to the prompt, plus the context needed to run the upload. */
export interface DotUploadSelection {
    baseType: DotUploadBaseType;
    targetFolder?: TreeNodeData;
    files?: FileList;
}

/** Files dropped on a target, emitted by the dropzone and the folder tree. */
export interface DotUploadFiles {
    files: FileList;
    targetFolder?: TreeNodeData;
}
