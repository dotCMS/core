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
