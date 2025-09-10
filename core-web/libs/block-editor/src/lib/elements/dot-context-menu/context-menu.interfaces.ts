/**
 * Interface for context menu items
 */
export interface ContextMenuItem {
    label?: string;
    command?: () => void | Promise<void>;
    shortcut?: string;
    disabled?: boolean;
    separator?: boolean;
}

/**
 * Configuration for keyboard shortcuts
 */
export interface ShortcutConfig {
    mac: string;
    pc: string;
}

/**
 * Configuration for Turndown service (HTML to Markdown conversion)
 */
export interface MarkdownConfig {
    headingStyle: 'setext' | 'atx';
    hr: string;
    bulletListMarker: string;
    codeBlockStyle: 'indented' | 'fenced';
    emDelimiter: string;
}

/**
 * Platform detection type
 */
export type Platform = 'mac' | 'pc';
