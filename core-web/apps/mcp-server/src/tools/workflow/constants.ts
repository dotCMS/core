/**
 * Action messages mapping for different workflow actions
 * Maps workflow action types to user-friendly success messages
 */
export const ACTION_MESSAGES = {
    PUBLISH: 'Content published successfully!',
    UNPUBLISH: 'Content unpublished successfully!',
    ARCHIVE: 'Content archived successfully!',
    UNARCHIVE: 'Content unarchived successfully!',
    DELETE: 'Content deleted successfully!'
} as const;

/**
 * Type for action message keys
 */
export type ActionType = keyof typeof ACTION_MESSAGES;
