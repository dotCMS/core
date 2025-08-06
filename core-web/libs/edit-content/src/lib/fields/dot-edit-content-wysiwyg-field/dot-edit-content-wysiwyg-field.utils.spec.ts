import { MD_SYNTAX } from './dot-edit-content-wysiwyg-field.constant';
import {
    CountOccurrences,
    isHtml,
    isJavascript,
    isMarkdown,
    isVelocity
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

    describe('Edge cases and integration scenarios', () => {
        describe('Empty and null inputs', () => {
            it('should handle empty strings for all detection functions', () => {
                expect(isVelocity('')).toBe(false);
                expect(isJavascript('')).toBe(false);
                expect(isHtml('')).toBe(false);
                expect(isMarkdown('')).toBe(false);
            });

            it('should handle whitespace-only strings', () => {
                const whitespace = '   \n\t  ';
                expect(isVelocity(whitespace)).toBe(false);
                expect(isJavascript(whitespace)).toBe(false);
                expect(isHtml(whitespace)).toBe(false);
                expect(isMarkdown(whitespace)).toBe(false);
            });
        });

        describe('Mixed content detection', () => {
            it('should prioritize Velocity over HTML when both are present', () => {
                const mixedContent =
                    '<div>#set($title = "Hello")</div> #if($title) <p>$title</p> #end';
                expect(isVelocity(mixedContent)).toBe(true);
                expect(isHtml(mixedContent)).toBe(true);
            });

            it('should detect JavaScript within HTML', () => {
                const mixedContent = '<script>function test() { const x = 5; return x; }</script>';
                expect(isJavascript(mixedContent)).toBe(true);
                expect(isHtml(mixedContent)).toBe(true);
            });

            it('should detect Markdown within HTML comments', () => {
                const mixedContent =
                    '<!-- # Header\n\n- List item\n\n```code``` --><div>content</div>';
                expect(isMarkdown(mixedContent)).toBe(true);
                expect(isHtml(mixedContent)).toBe(true);
            });
        });

        describe('Language detection priority scenarios', () => {
            it('should detect complex Velocity templates', () => {
                const velocityTemplate = `
                    #set($items = ["apple", "banana", "cherry"])
                    #foreach($item in $items)
                        #if($velocityCount > 1)
                            <li>$item</li>
                        #end
                    #end
                `;
                expect(isVelocity(velocityTemplate)).toBe(true);
            });

            it('should detect modern JavaScript syntax', () => {
                const modernJs = `
                    const data = await fetch('/api/data');
                    const items = data.map(item => ({ ...item, processed: true }));
                    export default class DataProcessor {
                        constructor() { this.items = items; }
                    }
                `;
                expect(isJavascript(modernJs)).toBe(true);
            });

            it('should detect complex HTML structures', () => {
                const complexHtml = `
                    <article>
                        <header><h1>Title</h1></header>
                        <section><p>Content</p></section>
                        <footer><nav><a href="#">Link</a></nav></footer>
                    </article>
                `;
                expect(isHtml(complexHtml)).toBe(true);
            });

            it('should detect advanced Markdown features', () => {
                const advancedMd = `
                    ## Table Example
                    
                    | Column 1 | Column 2 |
                    |----------|----------|
                    | Data     | More     |
                    
                    ### Code Block
                    \`\`\`javascript
                    function test() {}
                    \`\`\`
                    
                    - [ ] Todo item
                    - [x] Completed item
                `;
                expect(isMarkdown(advancedMd)).toBe(true);
            });
        });

        describe('False positive prevention', () => {
            it('should not detect Velocity in plain text with similar patterns', () => {
                const plainText = 'Email: user@domain.com, Price: $25.99, Note: Use #hashtag';
                expect(isVelocity(plainText)).toBe(false);
            });

            it('should not detect JavaScript in text with function-like words', () => {
                const plainText =
                    'The function of this device is to const-antly monitor variables.';
                expect(isJavascript(plainText)).toBe(false);
            });

            it('should not detect HTML in text with angle brackets', () => {
                const plainText = 'Value is < 5 and > 2, check if x < y or z > w.';
                expect(isHtml(plainText)).toBe(false);
            });

            it('should not detect Markdown in text with occasional symbols', () => {
                const plainText = 'Title: Important Notice * Please note: items 1. are available';
                expect(isMarkdown(plainText)).toBe(false);
            });
        });
    });
});
