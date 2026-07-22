import { DotCMSContentlet } from '@dotcms/dotcms-models';

/**
 * Configuration data passed to the edit content dialog
 */
export interface EditContentDialogData {
    /**
     * Mode determines whether we're creating new content or editing existing
     */
    mode: 'new' | 'edit';

    /**
     * For new content: The content type variable name or ID
     */
    contentTypeId?: string;

    /**
     * For edit content: The inode of the content to edit
     */
    contentletInode?: string;

    /**
     * Depth for loading existing content (defaults to TWO)
     */
    depth?: number;

    /**
     * Optional header label for the side panel (e.g. the content title when editing, or the
     * content type name when creating). Shown in the panel header; ignored by the dialog.
     */
    title?: string;

    /**
     * For new content: pre-fills a Host-or-Folder field with this `hostname/path`, so content
     * created from a folder context (e.g. Content Drive) lands in that folder. Mirrors the
     * `folderPath` query param the full-screen editor reads.
     */
    folderPath?: string;

    /**
     * Optional relationship information when creating content for relationships
     */
    relationshipInfo?: {
        parentContentletId: string;
        relationshipName: string;
        isParent: boolean;
    };

    /**
     * Optional callback for when content is successfully created or updated
     */
    onContentSaved?: (contentlet: DotCMSContentlet) => void;

    /**
     * Optional callback for when dialog is cancelled
     */
    onCancel?: () => void;
}
