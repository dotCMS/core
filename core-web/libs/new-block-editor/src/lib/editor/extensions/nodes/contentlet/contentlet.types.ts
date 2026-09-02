import type { DotCMSContentlet } from '@dotcms/dotcms-models';

/** TipTap JSON `type` for embedded dotCMS contentlets (slash menu → content type → contentlet). */
export const DOT_CONTENTLET_NODE_NAME = 'dotContent' as const;

/**
 * HTML tag used in {@link renderHTML} / {@link parseHTML} for clipboard and server-side HTML.
 * Live editing uses an Angular node view; persisted truth is ProseMirror JSON (`dotContent` attrs).
 */
export const CONTENTLET_HTML_HOST_TAG = 'dot-contentlet' as const;

/** Payload emitted by the editor when the user clicks "Edit contentlet". */
export interface ContentletEditEvent {
    identifier: string;
    inode: string;
    contentType: string;
    title: string;
}

/**
 * Skinny shape persisted to disk. The dotCMS backend filters all other fields on save and
 * re-hydrates the full contentlet on read, so the editor only needs to transmit the reference.
 */
export interface ContentletDataRef {
    identifier: string;
    languageId: number;
}

export type ContentletData = ContentletDataRef & Partial<DotCMSContentlet>;

/** Tailwind classes for the contentlet card shell (node view host + static HTML export). */
export const CONTENTLET_CARD_HOST_CLASS =
    'not-prose my-4 block rounded-lg border border-gray-200 bg-gray-50 p-4 shadow-sm dark:border-gray-700 dark:bg-gray-900/40';
