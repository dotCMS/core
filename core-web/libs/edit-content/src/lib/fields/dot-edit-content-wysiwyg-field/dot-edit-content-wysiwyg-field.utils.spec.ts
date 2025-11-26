import { MD_SYNTAX } from './dot-edit-content-wysiwyg-field.constant';
import {
    CountOccurrences,
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity,
    shouldUseDefaultEditor
} from './dot-edit-content-wysiwyg-field.utils';

describe('WYSIWYG Field Utils', () => {
    describe('CountOccurrences', () => {
        it('should correctly count markdown syntax occurrences', () => {
            const markdownContent = '# Heading\n\n- List item\n\n```code block```';
            const mdScore = MD_SYNTAX.reduce(
                (score, syntax) => score + CountOccurrences(markdownContent, syntax),
                0
            );
            expect(mdScore).toBeGreaterThan(2);
        });

        it('should return low score for non-markdown content', () => {
            const nonMarkdownContent = 'This is just plain text without any special syntax.';
            const mdScore = MD_SYNTAX.reduce(
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

        it('should return false for non-empty strings', () => {
            expect(shouldUseDefaultEditor('hello')).toBe(false);
        });
    });

    describe('isVelocity', () => {
        it('should return true for content with more than 2 Velocity patterns', () => {
            const velocityContent = '#set($var = "value") #if($condition) $!variable #end';
            expect(isVelocity(velocityContent)).toBe(true);
        });

        it('should return false for content with 2 or fewer Velocity patterns', () => {
            const nonVelocityContent = '#set($var = "value") Some other content';
            expect(isVelocity(nonVelocityContent)).toBe(false);
        });
    });

    describe('isJavascript', () => {
        it('should return true if content includes JavaScript keywords', () => {
            const jsContent = 'function example() { const x = 10; return x; }';
            expect(isJavascript(jsContent)).toBe(true);
        });

        it('should return false if content does not include JavaScript keywords', () => {
            const nonJsContent = 'This is just some plain text.';
            expect(isJavascript(nonJsContent)).toBe(false);
        });
    });

    describe('isHtml', () => {
        it('should return true if content includes HTML tags', () => {
            const htmlContent = '<div><p>This is a paragraph</p></div>';
            expect(isHtml(htmlContent)).toBe(true);
        });

        it('should return false if content does not include HTML tags', () => {
            const nonHtmlContent = 'This is just some plain text.';
            expect(isHtml(nonHtmlContent)).toBe(false);
        });
    });

    describe('isMarkdown', () => {
        it('should return true for content with more than 2 Markdown syntax occurrences', () => {
            const markdownContent = '# Heading\n\n- List item\n\n```code block```';
            expect(isMarkdown(markdownContent)).toBe(true);
        });

        it('should return false for content with 2 or fewer Markdown syntax occurrences', () => {
            const nonMarkdownContent = 'This is just plain text with *one* emphasis.';
            expect(isMarkdown(nonMarkdownContent)).toBe(false);
        });
    });
});
