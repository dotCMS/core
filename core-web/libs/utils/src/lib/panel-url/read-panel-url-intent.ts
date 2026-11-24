import { isDotIdentifier } from '../dot-utils';

/**
 * The query params that name an open side panel, the same on every surface (#38303, FR-044).
 * Content Drive's names, kept so the links authors already share keep opening.
 *
 * In `@dotcms/utils`, not beside the opener in `@dotcms/edit-content`, so a screen of the admin app
 * can read its URL without loading the editor library, which the app loads only on demand.
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

/**
 * Why a URL's panel params open nothing: they name more than one panel (`conflict`), or one of
 * them has a shape it cannot take (`malformed`). Either way it is a link the author cannot have
 * meant, and every surface treats both alike: it opens nothing, tells the author, and clears the
 * params (`DotPanelUrlStateService.refuse`).
 */
export type DotPanelUrlRefusal = 'conflict' | 'malformed';

/** What a URL asks to open when a surface loads. */
export type DotPanelUrlIntent =
    | { kind: 'none' }
    | { kind: 'edit'; identifier: string; languageId?: number }
    | { kind: 'create'; contentType: string }
    | { kind: 'editCode'; inode: string }
    | { kind: 'refused'; reason: DotPanelUrlRefusal };

/**
 * What `createContent` may name: a content type variable or id. Letters, digits, `_` and `-` only,
 * so the value cannot reshape the REST paths it reaches.
 */
const CONTENT_TYPE_REF_PATTERN = /^[A-Za-z0-9_-]+$/;

/** A panel param present in the URL with a value of a shape it cannot take. */
const MALFORMED = Symbol('malformed');

/**
 * Reads what a URL asks to open, once, when a surface loads (#38303, FR-049).
 *
 * Values from the URL reach Lucene queries and REST paths, so only the shapes they can take are
 * accepted: an identifier for `editContent`, a positive integer for `editContentLang`, a content
 * type variable or id for `createContent`, an inode (the same UUID shape as an identifier) for
 * `editCode`. A panel param of any other shape, or repeated, is `refused` as `malformed`, even
 * beside a valid one, so the surface can tell the author rather than open nothing in silence; the
 * value never goes further (T172). The retired `editContent=new` create marker is one: an old
 * bookmark gets the error. A URL naming more than one panel is `refused` as a `conflict`. An empty
 * value names nothing and reads as absent.
 *
 * @param queryParams The route's query params.
 * @returns The one panel the URL names, `none`, or `refused` with the reason.
 */
export function readPanelUrlIntent(queryParams: Record<string, unknown>): DotPanelUrlIntent {
    /**
     * A param's value as `accept` takes it; `undefined` when the URL does not carry it, or carries
     * it empty; `MALFORMED` when it carries something `accept` turns down, or repeats it (an array).
     */
    const readParam = (
        key: string,
        accept: (value: string) => string | undefined
    ): string | undefined | typeof MALFORMED => {
        const value = queryParams[key];

        if (value === undefined || value === null || value === '') {
            return undefined;
        }

        return typeof value === 'string' ? (accept(value) ?? MALFORMED) : MALFORMED;
    };

    // Trimmed, as `isDotIdentifier` tests it: the value reaches a Lucene query.
    const uuid = (value: string) => (isDotIdentifier(value) ? value.trim() : undefined);
    const contentTypeRef = (value: string) =>
        CONTENT_TYPE_REF_PATTERN.test(value) ? value : undefined;
    const positiveInteger = (value: string) => {
        const number = Number(value);

        return Number.isInteger(number) && number > 0 ? String(number) : undefined;
    };

    const identifier = readParam(DOT_PANEL_URL_PARAM.EDIT_CONTENT, uuid);
    const language = readParam(DOT_PANEL_URL_PARAM.EDIT_CONTENT_LANG, positiveInteger);
    const contentType = readParam(DOT_PANEL_URL_PARAM.CREATE_CONTENT, contentTypeRef);
    const inode = readParam(DOT_PANEL_URL_PARAM.EDIT_CODE, uuid);

    if ([identifier, language, contentType, inode].includes(MALFORMED)) {
        return { kind: 'refused', reason: 'malformed' };
    }

    if ([identifier, contentType, inode].filter(Boolean).length > 1) {
        return { kind: 'refused', reason: 'conflict' };
    }

    if (typeof identifier === 'string') {
        return {
            kind: 'edit',
            identifier,
            languageId: typeof language === 'string' ? Number(language) : undefined
        };
    }

    if (typeof contentType === 'string') {
        return { kind: 'create', contentType };
    }

    if (typeof inode === 'string') {
        return { kind: 'editCode', inode };
    }

    return { kind: 'none' };
}

/**
 * The same for the same panel, whatever else the URL carries. A surface that follows its URL on
 * every router navigation opens a panel once by comparing these: an app navigation that changes
 * only the surface's own params still carries the panel params the router last saw, which may name
 * a panel already open or one the author closed.
 *
 * @param intent What a URL asks to open.
 * @returns A key naming that panel.
 */
export function panelUrlIntentKey(intent: DotPanelUrlIntent): string {
    switch (intent.kind) {
        case 'edit':
            return `edit:${intent.identifier}:${intent.languageId ?? ''}`;
        case 'create':
            return `create:${intent.contentType}`;
        case 'editCode':
            return `editCode:${intent.inode}`;
        case 'refused':
            return `refused:${intent.reason}`;
        default:
            return intent.kind;
    }
}
