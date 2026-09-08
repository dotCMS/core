import type TurndownService from 'turndown';

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
 * Configuration for Turndown service (HTML to Markdown conversion).
 *
 * Derived from Turndown's own `Options` so the five keys we set keep exactly the
 * literal unions Turndown accepts — `bulletListMarker` and `emDelimiter` were
 * declared as plain `string` here, which made the config unassignable to the
 * `TurndownService` constructor.
 */
export type MarkdownConfig = Required<
    Pick<
        TurndownService.Options,
        'headingStyle' | 'hr' | 'bulletListMarker' | 'codeBlockStyle' | 'emDelimiter'
    >
>;

/**
 * Platform detection type
 */
export type Platform = 'mac' | 'pc';
