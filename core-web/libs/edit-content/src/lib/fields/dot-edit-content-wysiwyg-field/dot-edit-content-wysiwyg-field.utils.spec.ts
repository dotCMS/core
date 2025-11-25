import { COMMENT_TINYMCE, MdSyntax } from './dot-edit-content-wysiwyg-field.constant';
import { CountOccurrences, shouldUseDefaultEditor } from './dot-edit-content-wysiwyg-field.utils';

describe('CountOccurrences', () => {
    it('should correctly count markdown syntax occurrences', () => {
        const markdownContent = '# Heading\n\n- List item\n\n```code block```';
        const mdScore = MdSyntax.reduce(
            (score, syntax) => score + CountOccurrences(markdownContent, syntax),
            0
        );
        expect(mdScore).toBeGreaterThan(2);
    });

    it('should return low score for non-markdown content', () => {
        const nonMarkdownContent = 'This is just plain text without any special syntax.';
        const mdScore = MdSyntax.reduce(
            (score, syntax) => score + CountOccurrences(nonMarkdownContent, syntax),
            0
        );
        expect(mdScore).toBeLessThanOrEqual(2);
    });

    it('should handle multiple occurrences of the same syntax', () => {
        const repeatedSyntaxContent = '# Heading 1\n## Heading 2\n### Heading 3';
        const headingScore = CountOccurrences(repeatedSyntaxContent, '#');
        expect(headingScore).toBe(6); // 1 + 2 + 3 = 6 occurrences of '#'
    });
});

describe('shouldUseDefaultEditor', () => {
    it('should return true for null or undefined', () => {
        expect(shouldUseDefaultEditor(null)).toBe(true);
        expect(shouldUseDefaultEditor(undefined)).toBe(true);
    });

    it('should return true for non-string types', () => {
        expect(shouldUseDefaultEditor(123)).toBe(true);
        expect(shouldUseDefaultEditor({})).toBe(true);
        expect(shouldUseDefaultEditor([])).toBe(true);
    });

    it('should return true for empty string', () => {
        expect(shouldUseDefaultEditor('')).toBe(true);
    });

    it('should return true for string with only whitespace', () => {
        expect(shouldUseDefaultEditor('   ')).toBe(true);
    });

    it('should return true for COMMENT_TINYMCE', () => {
        expect(shouldUseDefaultEditor(COMMENT_TINYMCE)).toBe(true);
    });

    it('should return false for non-empty strings', () => {
        expect(shouldUseDefaultEditor('hello')).toBe(false);
    });
});
