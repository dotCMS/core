/**
 * Monaco Editor Marker Utilities
 *
 * Uses an invisible Unicode character (\u2060) to mark content edited with Monaco editor.
 * This helps distinguish between TinyMCE/Textarea and Monaco content.
 */

/**
 * Invisible Unicode character used to mark Monaco editor content
 */
export const MONACO_MARKER = '\u2060';

/**
 * Adds Monaco marker to content if not already present
 * @param content - The content to mark
 * @returns Content with Monaco marker prefix
 */
export const addMonacoMarker = (content: string): string => {
    if (!content) {
        return MONACO_MARKER + (content || '');
    }

    if (content.startsWith(MONACO_MARKER)) {
        return content;
    }
    return MONACO_MARKER + content;
};

/**
 * Removes Monaco marker from content
 * @param content - The content to clean
 * @returns Content without Monaco marker
 */
export const removeMonacoMarker = (content: string): string => {
    if (!content) {
        return content || '';
    }
    return content.replace(new RegExp(MONACO_MARKER, 'g'), '');
};

/**
 * Checks if content has Monaco marker
 * @param content - The content to check
 * @returns True if content has Monaco marker
 */
export const hasMonacoMarker = (content: string): boolean => {
    if (!content) {
        return false;
    }
    return content.startsWith(MONACO_MARKER);
};
