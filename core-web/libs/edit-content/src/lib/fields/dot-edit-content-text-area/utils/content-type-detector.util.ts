import { hasMonacoMarker } from '../../../shared/dot-edit-content-monaco-editor-control/monaco-marker.util';
import { AvailableEditorTextArea } from '../dot-edit-content-text-area.constants';

/**
 * Determines which editor to use based on content analysis.
 *
 * This function checks if the content has a Monaco marker (Zero Width Space character)
 * to determine the appropriate editor type.
 *
 * @param content - The content to analyze
 * @returns The appropriate editor type:
 *   - AvailableEditorTextArea.Monaco if content has Monaco marker
 *   - AvailableEditorTextArea.PlainText otherwise
 */
export const detectEditorType = (content: string): AvailableEditorTextArea => {
    if (hasMonacoMarker(content)) {
        return AvailableEditorTextArea.Monaco;
    }

    // Otherwise use plain text
    return AvailableEditorTextArea.PlainText;
};
