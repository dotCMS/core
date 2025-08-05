import { MarkdownConfig, ShortcutConfig } from './context-menu.interfaces';

/**
 * Keyboard shortcuts for context menu actions
 */
export const SHORTCUTS: Record<string, ShortcutConfig> = {
    CUT: { mac: '⌘X', pc: 'Ctrl+X' },
    COPY: { mac: '⌘C', pc: 'Ctrl+C' },
    PASTE: { mac: '⌘V', pc: 'Ctrl+V' },
    PASTE_WITHOUT_FORMAT: { mac: '⌘⇧V', pc: 'Ctrl+Shift+V' }
} as const;

/**
 * Menu item labels
 */
export const MENU_LABELS = {
    CUT: 'Cut',
    COPY: 'Copy',
    COPY_MARKDOWN: 'Copy as Markdown',
    PASTE: 'Paste',
    PASTE_WITHOUT_FORMAT: 'Paste without format',
    PASTE_MARKDOWN: 'Paste from Markdown'
} as const;

/**
 * Turndown service configuration for HTML to Markdown conversion
 */
export const MARKDOWN_CONFIG: MarkdownConfig = {
    headingStyle: 'atx',
    hr: '---',
    bulletListMarker: '-',
    codeBlockStyle: 'fenced',
    emDelimiter: '_'
} as const;

/**
 * Platform detection regex patterns
 */
export const PLATFORM_PATTERNS = {
    MAC: /MAC/i
} as const;
