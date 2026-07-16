import {
    HTML_TAGS,
    JS_KEYWORDS,
    MD_SYNTAX,
    VELOCITY_PATTERNS
} from './dot-edit-content-wysiwyg-field.constant';

/**
 * Escapes special characters in a string for use in a regular expression.
 * @param {string} string - The string to escape.
 * @returns {string} The escaped string.
 */
const escapeRegExp = (string: string) => {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
};

/**
 * Counts the occurrences of a substring within a string.
 * @param {string} str - The string to search in.
 * @param {string} searchStr - The substring to search for.
 * @returns {number} The number of occurrences.
 */
export const CountOccurrences = (str: string, searchStr: string) => {
    const escapedSearchStr = escapeRegExp(searchStr);

    return (str.match(new RegExp(escapedSearchStr, 'gi')) || []).length;
};

/**
 * Determines if the default editor should be used based on the content.
 * @param {unknown} content - The content to check.
 * @returns {boolean} True if the default editor should be used, false otherwise.
 */
export const shouldUseDefaultEditor = (content: unknown): boolean => {
    return !content || typeof content !== 'string' || content.trim() === '';
};

/**
 * Checks if the content is likely to be Velocity code.
 * @param {string} content - The content to check.
 * @returns {boolean} True if the content is likely Velocity, false otherwise.
 */
export const isVelocity = (content: string): boolean => {
    const velocityScore = VELOCITY_PATTERNS.reduce(
        (score, pattern) => score + (pattern.test(content) ? 1 : 0),
        0
    );

    return velocityScore > 2;
};

/**
 * Checks if the content is likely to be JavaScript code.
 * @param {string} content - The content to check.
 * @returns {boolean} True if the content is likely JavaScript, false otherwise.
 */
export const isJavascript = (content: string): boolean => {
    return JS_KEYWORDS.some((keyword) => content.includes(keyword));
};

/**
 * Checks if the content is likely to be HTML.
 * @param {string} content - The content to check.
 * @returns {boolean} True if the content is likely HTML, false otherwise.
 */
export const isHtml = (content: string): boolean => {
    return HTML_TAGS.some((tag) => content.indexOf(tag) !== -1);
};

/**
 * Checks if the content is likely to be Markdown.
 * @param {string} content - The content to check.
 * @returns {boolean} True if the content is likely Markdown, false otherwise.
 */
export const isMarkdown = (content: string): boolean => {
    const mdScore = MD_SYNTAX.reduce(
        (score, syntax) => score + CountOccurrences(content, syntax),
        0
    );

    return mdScore > 2;
};
