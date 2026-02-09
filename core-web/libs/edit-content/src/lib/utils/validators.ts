import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Checks if a Block Editor JSON content is actually empty (no text content).
 * A block editor is considered empty if it has no text nodes with content,
 * even if it has structural JSON like {"type":"doc","content":[{"type":"paragraph"}]}.
 *
 * @param content - The block editor content (string or object)
 * @returns true if the content is empty, false otherwise
 */
function isBlockEditorEmpty(content: unknown): boolean {
    if (!content) {
        return true;
    }

    let jsonContent: unknown;

    // Parse if it's a string
    if (typeof content === 'string') {
        try {
            jsonContent = JSON.parse(content);
        } catch {
            // If it's a plain string with content, it's not empty
            return content.trim().length === 0;
        }
    } else {
        jsonContent = content;
    }

    // Recursively check for text content
    return !hasTextContent(jsonContent);
}

/**
 * Recursively checks if a JSON structure has any actual text content.
 *
 * @param node - The JSON node to check
 * @returns true if any text content is found, false otherwise
 */
function hasTextContent(node: unknown): boolean {
    if (!node || typeof node !== 'object') {
        return false;
    }

    const obj = node as Record<string, unknown>;

    // Check if this is a text node with content
    if (
        obj['type'] === 'text' &&
        typeof obj['text'] === 'string' &&
        obj['text'].trim().length > 0
    ) {
        return true;
    }

    // Check content array recursively
    if (Array.isArray(obj['content'])) {
        return obj['content'].some((child) => hasTextContent(child));
    }

    return false;
}

/**
 * Custom validator for Block Editor required fields.
 * Unlike Validators.required, this checks if the block editor actually has text content,
 * not just if it has a JSON structure.
 *
 * @returns ValidatorFn that returns { required: true } if empty, null if valid
 *
 * @example
 * ```typescript
 * const control = new FormControl('', blockEditorRequiredValidator());
 * ```
 */
export function blockEditorRequiredValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
        const isEmpty = isBlockEditorEmpty(control.value);

        return isEmpty ? { required: true } : null;
    };
}
