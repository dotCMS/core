import { isDotIdentifier } from '@dotcms/utils';

/**
 * The query params that name an open side panel, the same on every surface (#38303, FR-044).
 * Content Drive's names, kept so the links authors already share keep opening.
 */
export const DOT_PANEL_URL_PARAM = {
    /** Identifier of the content open for editing. */
    EDIT_CONTENT: 'editContent',
    /** Language id of the version open for editing. */
    EDIT_CONTENT_LANG: 'editContentLang',
    /** Variable or id of the content type being created. */
    CREATE_CONTENT: 'createContent',
    /** Inode of the file open in Edit Source. */
    EDIT_CODE: 'editCode'
} as const;

/** What a URL asks to open when a surface loads. */
export type DotPanelUrlIntent =
    | { kind: 'none' }
    | { kind: 'edit'; identifier: string; languageId?: number }
    | { kind: 'create'; contentType: string }
    | { kind: 'editCode'; inode: string }
    | { kind: 'conflict' };

/**
 * What `createContent` may name: a content type variable or id. Letters, digits, `_` and `-` only,
 * so the value cannot reshape the REST paths it reaches.
 */
const CONTENT_TYPE_REF_PATTERN = /^[A-Za-z0-9_-]+$/;

/**
 * Reads what a URL asks to open, once, when a surface loads (#38303, FR-049).
 *
 * Values from the URL reach Lucene queries and REST paths, so only the shapes they can take are
 * accepted and anything else reads as absent: an identifier for `editContent`, a positive integer
 * for `editContentLang`, a content type variable or id for `createContent`, an inode (the same UUID
 * shape as an identifier) for `editCode`. The retired `editContent=new` create marker is not an
 * identifier, so an old link opens nothing rather than an empty form. A URL naming more than one
 * panel is a `conflict`: the surface opens nothing and clears the params.
 *
 * @param queryParams The route's query params.
 * @returns The one panel the URL names, `none`, or `conflict`.
 */
export function readPanelUrlIntent(queryParams: Record<string, unknown>): DotPanelUrlIntent {
    const read = (key: string): string | undefined => {
        const value = queryParams[key];

        // A param repeated in the URL arrives as an array, which names nothing.
        return typeof value === 'string' && value.length > 0 ? value : undefined;
    };

    const readUuid = (key: string): string | undefined => {
        const value = read(key);

        return isDotIdentifier(value) ? value : undefined;
    };

    const identifier = readUuid(DOT_PANEL_URL_PARAM.EDIT_CONTENT);
    const createContent = read(DOT_PANEL_URL_PARAM.CREATE_CONTENT);
    const contentType =
        createContent && CONTENT_TYPE_REF_PATTERN.test(createContent) ? createContent : undefined;
    const inode = readUuid(DOT_PANEL_URL_PARAM.EDIT_CODE);

    if ([identifier, contentType, inode].filter(Boolean).length > 1) {
        return { kind: 'conflict' };
    }

    if (identifier) {
        const languageId = Number(read(DOT_PANEL_URL_PARAM.EDIT_CONTENT_LANG));

        return {
            kind: 'edit',
            identifier,
            languageId: Number.isInteger(languageId) && languageId > 0 ? languageId : undefined
        };
    }

    if (contentType) {
        return { kind: 'create', contentType };
    }

    if (inode) {
        return { kind: 'editCode', inode };
    }

    return { kind: 'none' };
}
