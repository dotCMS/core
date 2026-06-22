import type { ContentletData, ContentletDataRef } from '../contentlet/contentlet.types';

/**
 * TipTap JSON `type` for an inline dotCMS contentlet reference (`@`-mention picker).
 *
 * IMMUTABLE: this string is persisted as the node `type` in customer documents. Renaming it
 * would make TipTap drop stored inline references on load. See `new-block-editor/CLAUDE.md`
 * ("TipTap Node Names Are Immutable").
 */
export const DOT_INLINE_CONTENT_NODE_NAME = 'dotInlineContent' as const;

/**
 * HTML tag used in {@link renderHTML} / {@link parseHTML} for clipboard and server-side HTML.
 * Live editing uses an Angular node view; persisted truth is ProseMirror JSON
 * (`dotInlineContent` attrs). Inline counterpart to the block `dot-contentlet` tag.
 */
export const INLINE_CONTENT_HTML_HOST_TAG = 'dot-inline-content' as const;

/** Host classes for the inline reference token (node view + static HTML export). */
export const INLINE_CONTENT_HOST_CLASS = 'dot-inline-content-token' as const;

// The inline reference reuses the block contentlet's attribute shape: full contentlet at
// runtime, skinny `{ identifier, languageId }` ref on disk (the backend re-hydrates the rest).
export type { ContentletData, ContentletDataRef };
