import {
    MONACO_MARKER,
    addMonacoMarker,
    removeMonacoMarker,
    hasMonacoMarker
} from './monaco-marker.util';

describe('Monaco Marker Utils', () => {
    describe('MONACO_MARKER constant', () => {
        it('should be the invisible Unicode character', () => {
            expect(MONACO_MARKER).toBe('\u2060');
        });

        it('should be invisible when displayed', () => {
            // The character should have zero width
            expect(MONACO_MARKER.length).toBe(1);
            // It should be the Word Joiner Unicode character
            expect(MONACO_MARKER.charCodeAt(0)).toBe(8288);
        });
    });

    describe('addMonacoMarker', () => {
        it('should add marker to content that does not have it', () => {
            const content = 'function test() { return true; }';
            const result = addMonacoMarker(content);
            expect(result).toBe(MONACO_MARKER + content);
        });

        it('should not add marker if content already has it', () => {
            const content = MONACO_MARKER + 'function test() { return true; }';
            const result = addMonacoMarker(content);
            expect(result).toBe(content);
        });

        it('should handle empty string', () => {
            const result = addMonacoMarker('');
            expect(result).toBe(MONACO_MARKER);
        });

        it('should handle string with only whitespace', () => {
            const content = '   ';
            const result = addMonacoMarker(content);
            expect(result).toBe(MONACO_MARKER + content);
        });

        it('should handle null or undefined gracefully', () => {
            // The function should handle these cases without throwing
            expect(() => addMonacoMarker(null)).not.toThrow();
            expect(() => addMonacoMarker(undefined)).not.toThrow();

            // Should return marker + empty string for null/undefined
            expect(addMonacoMarker(null)).toBe(MONACO_MARKER);
            expect(addMonacoMarker(undefined)).toBe(MONACO_MARKER);
        });
    });

    describe('removeMonacoMarker', () => {
        it('should remove marker from content that has it', () => {
            const originalContent = 'function test() { return true; }';
            const markedContent = MONACO_MARKER + originalContent;
            const result = removeMonacoMarker(markedContent);
            expect(result).toBe(originalContent);
        });

        it('should not modify content that does not have marker', () => {
            const content = 'function test() { return true; }';
            const result = removeMonacoMarker(content);
            expect(result).toBe(content);
        });

        it('should remove all instances of marker', () => {
            const content = MONACO_MARKER + 'test' + MONACO_MARKER + 'content';
            const result = removeMonacoMarker(content);
            expect(result).toBe('testcontent');
        });

        it('should handle empty string', () => {
            const result = removeMonacoMarker('');
            expect(result).toBe('');
        });

        it('should handle string with only marker', () => {
            const result = removeMonacoMarker(MONACO_MARKER);
            expect(result).toBe('');
        });

        it('should handle null or undefined gracefully', () => {
            expect(() => removeMonacoMarker(null)).not.toThrow();
            expect(() => removeMonacoMarker(undefined)).not.toThrow();

            // Should return empty string for null/undefined
            expect(removeMonacoMarker(null)).toBe('');
            expect(removeMonacoMarker(undefined)).toBe('');
        });
    });

    describe('hasMonacoMarker', () => {
        it('should return true for content that starts with marker', () => {
            const content = MONACO_MARKER + 'function test() { return true; }';
            expect(hasMonacoMarker(content)).toBe(true);
        });

        it('should return false for content that does not start with marker', () => {
            const content = 'function test() { return true; }';
            expect(hasMonacoMarker(content)).toBe(false);
        });

        it('should return false for content that has marker in the middle', () => {
            const content = 'function' + MONACO_MARKER + ' test() { return true; }';
            expect(hasMonacoMarker(content)).toBe(false);
        });

        it('should return true for content that only contains marker', () => {
            expect(hasMonacoMarker(MONACO_MARKER)).toBe(true);
        });

        it('should return false for empty string', () => {
            expect(hasMonacoMarker('')).toBe(false);
        });

        it('should handle null or undefined gracefully', () => {
            expect(() => hasMonacoMarker(null)).not.toThrow();
            expect(() => hasMonacoMarker(undefined)).not.toThrow();
            expect(hasMonacoMarker(null)).toBe(false);
            expect(hasMonacoMarker(undefined)).toBe(false);
        });
    });

    describe('Integration scenarios', () => {
        it('should work correctly in add-check-remove cycle', () => {
            const originalContent = 'const message = "Hello World!";';

            // Add marker
            const markedContent = addMonacoMarker(originalContent);
            expect(hasMonacoMarker(markedContent)).toBe(true);

            // Remove marker
            const cleanContent = removeMonacoMarker(markedContent);
            expect(cleanContent).toBe(originalContent);
            expect(hasMonacoMarker(cleanContent)).toBe(false);
        });

        it('should be idempotent for add operations', () => {
            const content = 'let x = 5;';
            const marked1 = addMonacoMarker(content);
            const marked2 = addMonacoMarker(marked1);
            const marked3 = addMonacoMarker(marked2);

            expect(marked1).toBe(marked2);
            expect(marked2).toBe(marked3);
        });

        it('should handle content with special characters', () => {
            const content = 'const emoji = "🚀"; const unicode = "αβγ";';
            const marked = addMonacoMarker(content);

            expect(hasMonacoMarker(marked)).toBe(true);
            expect(removeMonacoMarker(marked)).toBe(content);
        });
    });
});
