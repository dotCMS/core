import {
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity
} from '../../dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.utils';
import { AvailableEditorTextArea } from '../dot-edit-content-text-area.constants';

/**
 * Determines which editor to use based on content analysis.
 * Uses existing language detectors to determine if content needs Monaco editor.
 *
 * The content will be considered code (and use Monaco) if:
 * - It contains Velocity syntax
 * - It contains JavaScript code
 * - It contains HTML markup
 * - It contains Markdown syntax
 *
 * Otherwise, it will use the plain text editor.
 *
 * @param content - The content to analyze
 * @returns The appropriate editor type to use
 */
export const detectEditorType = (content: string): AvailableEditorTextArea => {
    // First check if it contains any code syntax
    const isCode = [isVelocity, isJavascript, isHtml, isMarkdown].some((detector) =>
        detector(content)
    );

    // If it's code, use Monaco
    if (isCode) {
        return AvailableEditorTextArea.Monaco;
    }

    // Otherwise use plain text
    return AvailableEditorTextArea.PlainText;
};
