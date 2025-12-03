/**
 * Field Editor Preferences Utility
 *
 * Manages editor preferences for DotCMS content fields using the `disabledWYSIWYG`
 * contentlet attribute. This replaces content-based editor detection with explicit
 * user preferences that persist across sessions.
 *
 * ## Entry Formats
 *
 * ### Text Area Fields
 * - No entry: Plain text editor (default)
 * - `FIELD@ToggleEditor`: Monaco code editor
 *
 * ### WYSIWYG Fields
 * - No entry: TinyMCE editor (default)
 * - `FIELD`: Monaco code editor
 * - `FIELD@PLAIN`: Legacy format (maps to Monaco)
 *
 * ## Usage Examples
 *
 * ```typescript
 * // Parse entry
 * const result = parseDisabledEditorEntry('myField@ToggleEditor');
 *
 * // Get current editor
 * const editor = getCurrentEditorFromDisabled('myField', ['myField@ToggleEditor'], true);
 *
 * // Update on editor switch
 * const updated = updateDisabledWYSIWYGOnEditorSwitch(
 *     'myField',
 *     AvailableEditorTextArea.Monaco,
 *     [],
 *     true
 * );
 * ```
 *
 */
import { AvailableEditorTextArea } from '../../dot-edit-content-text-area/dot-edit-content-text-area.constants';
import { AvailableEditor } from '../../dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.constant';

/**
 * Enum representing the different editor types that can be disabled via disabledWYSIWYG attribute
 */
export enum DisabledEditorType {
    /** Monaco/Code editor for textarea fields - format: FIELD_VARIABLE_NAME@ToggleEditor */
    MonacoTextArea = 'ToggleEditor',
    /** Monaco/Code editor for WYSIWYG fields - format: FIELD_VARIABLE_NAME */
    MonacoWYSIWYG = 'Monaco',
    /** Legacy PLAIN editor for WYSIWYG fields - format: FIELD_VARIABLE_NAME@PLAIN (maps to Monaco) */
    PlainLegacy = 'PLAIN'
}

/**
 * Interface representing a parsed disabledWYSIWYG entry
 */
export interface DisabledEditorEntry {
    fieldVariable: string;
    editorType: DisabledEditorType | null;
    /** The full entry string as it appears in disabledWYSIWYG array */
    fullEntry: string;
}

/**
 * Parses a disabledWYSIWYG entry string into its components
 *
 * Entry formats:
 * - FIELD_VARIABLE_NAME@ToggleEditor (Monaco for textarea)
 * - FIELD_VARIABLE_NAME (Monaco for WYSIWYG)
 * - FIELD_VARIABLE_NAME@PLAIN (Legacy PLAIN for WYSIWYG)
 *
 * @param entry - The disabledWYSIWYG entry string to parse
 * @returns Parsed entry with field variable and editor type
 */
export const parseDisabledEditorEntry = (entry: string): DisabledEditorEntry => {
    if (!entry || typeof entry !== 'string') {
        return { fieldVariable: '', editorType: null, fullEntry: entry };
    }

    const parts = entry.split('@');
    const fieldVariable = parts[0] || '';

    if (parts.length === 1) {
        // Just field variable name means Monaco for WYSIWYG
        return {
            fieldVariable,
            editorType: DisabledEditorType.MonacoWYSIWYG,
            fullEntry: entry
        };
    }

    const editorSuffix = parts[1];
    let editorType: DisabledEditorType | null = null;

    switch (editorSuffix) {
        case DisabledEditorType.MonacoTextArea:
            editorType = DisabledEditorType.MonacoTextArea;
            break;
        case DisabledEditorType.PlainLegacy:
            editorType = DisabledEditorType.PlainLegacy;
            break;
        default:
            // Unknown suffix, treat as null
            editorType = null;
            break;
    }

    return { fieldVariable, editorType, fullEntry: entry };
};

/**
 * Gets the current editor type for a field based on disabledWYSIWYG entries
 *
 * @param fieldVariable - The field variable name to check
 * @param disabledWYSIWYG - Array of disabled WYSIWYG entries from contentlet
 * @param isTextAreaField - Whether this is a textarea field (true) or WYSIWYG field (false)
 * @returns The current editor type for the field
 */
export const getCurrentEditorFromDisabled = (
    fieldVariable: string,
    disabledWYSIWYG: string[] = [],
    isTextAreaField: boolean
): AvailableEditorTextArea | AvailableEditor => {
    const relevantEntry = disabledWYSIWYG
        .map(parseDisabledEditorEntry)
        .find((entry) => entry.fieldVariable === fieldVariable);

    if (!relevantEntry) {
        // No entry found - use default editors
        return isTextAreaField ? AvailableEditorTextArea.PlainText : AvailableEditor.TinyMCE;
    }

    if (isTextAreaField) {
        // For textarea fields
        switch (relevantEntry.editorType) {
            case DisabledEditorType.MonacoTextArea:
                return AvailableEditorTextArea.Monaco;
            default:
                return AvailableEditorTextArea.PlainText;
        }
    } else {
        // For WYSIWYG fields
        switch (relevantEntry.editorType) {
            case DisabledEditorType.MonacoWYSIWYG:
                return AvailableEditor.Monaco;
            case DisabledEditorType.PlainLegacy:
                // Legacy PLAIN editor maps to Monaco code editor
                return AvailableEditor.Monaco;
            default:
                return AvailableEditor.TinyMCE;
        }
    }
};

/**
 * Updates the disabledWYSIWYG array when switching editors
 *
 * @param fieldVariable - The field variable name
 * @param newEditor - The new editor being switched to
 * @param currentDisabledWYSIWYG - Current disabledWYSIWYG array
 * @param isTextAreaField - Whether this is a textarea field (true) or WYSIWYG field (false)
 * @returns Updated disabledWYSIWYG array
 */
export const updateDisabledWYSIWYGOnEditorSwitch = (
    fieldVariable: string,
    newEditor: AvailableEditorTextArea | AvailableEditor,
    currentDisabledWYSIWYG: string[] = [],
    isTextAreaField: boolean
): string[] => {
    // Remove any existing entries for this field
    const filteredEntries = currentDisabledWYSIWYG.filter((entry) => {
        const parsed = parseDisabledEditorEntry(entry);
        return parsed.fieldVariable !== fieldVariable;
    });

    if (isTextAreaField) {
        // For textarea fields
        if (newEditor === AvailableEditorTextArea.Monaco) {
            // Add Monaco entry for textarea
            return [...filteredEntries, `${fieldVariable}@${DisabledEditorType.MonacoTextArea}`];
        }
        // PlainText doesn't need an entry
        return filteredEntries;
    } else {
        // For WYSIWYG fields
        if (newEditor === AvailableEditor.Monaco) {
            // Add Monaco entry for WYSIWYG
            return [...filteredEntries, fieldVariable];
        }
        // TinyMCE doesn't need an entry
        return filteredEntries;
    }
};
