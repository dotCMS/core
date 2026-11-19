import { Observable } from 'rxjs';

import { ActivatedRoute, Params, Router } from '@angular/router';

import { distinctUntilChanged, filter, map } from 'rxjs/operators';

import type { DotEditContentRequest } from '@dotcms/edit-content';
import { isDotIdentifier, panelUrlIntentKey, readPanelUrlIntent } from '@dotcms/utils';

/**
 * The legacy portlet's create link carries the folder beside the panel params
 * (`/c/:id/new/:contentType?folder=<inode>` redirects here with it). Content Drive's folder is its
 * `path`, so the key is the legacy portlets' own.
 */
export const PORTLET_FOLDER_PARAM = 'folder';

/** What a portlet URL asks the side panel to show, keyed so the same URL is told apart. */
export interface DotPortletUrlPanel {
    /** The same for the same panel, whatever else the URL carries. */
    key: string;
    /** What to open; absent when the URL names nothing, or names a panel the author cannot mean. */
    request?: DotEditContentRequest;
    /**
     * The URL names a panel param of the wrong shape, or two panels: open nothing, show the
     * standard error, and clear them.
     */
    refused?: boolean;
}

/**
 * Reads what a legacy portlet URL asks the side panel to show (#38303, FR-049): an edit by
 * identifier and language, or a create with the folder an inode-shaped `folder` names.
 *
 * @param params The route's query params.
 */
export function panelFromPortletUrl(params: Params): DotPortletUrlPanel {
    const intent = readPanelUrlIntent(params);
    const key = panelUrlIntentKey(intent);

    if (intent.kind === 'edit') {
        const { identifier, languageId } = intent;

        return {
            key,
            request: { mode: 'edit', identifier, ...(languageId ? { languageId } : {}) }
        };
    }

    if (intent.kind === 'create') {
        const folder = params[PORTLET_FOLDER_PARAM];
        // It reaches the legacy create screen's URL, so only an inode-shaped value is used.
        const folderInode =
            typeof folder === 'string' && isDotIdentifier(folder) ? folder.trim() : null;

        return {
            // The folder is this portlet's own, beside the shared key.
            key: `${key}:${folderInode ?? ''}`,
            request: {
                mode: 'new',
                contentType: intent.contentType,
                ...(folderInode ? { folderInode } : {})
            }
        };
    }

    return intent.kind === 'refused' ? { key, refused: true } : { key };
}

/**
 * What a legacy portlet's URL asks the side panel to show, on subscribe and on every navigation of
 * the app's own that changes it (#38303, FR-049).
 *
 * A navigation the browser's Back or Forward started is skipped: those belong to the panel URL
 * service (FR-048), and the router replays an entry that may name a panel still open. A URL whose
 * panel did not change is skipped too, since an app navigation that changes other params carries
 * the panel params the router last saw.
 *
 * @param route The route whose query params name the panel.
 * @param router The app's router, to tell a Back or Forward apart.
 * @param when Read on every URL, before the dedup: a URL it turns down is never the one the next
 * is compared to.
 */
export function portletPanelUrl$(
    route: ActivatedRoute,
    router: Router,
    when: () => boolean = () => true
): Observable<DotPortletUrlPanel> {
    return route.queryParams.pipe(
        filter(() => router.currentNavigation()?.trigger !== 'popstate' && when()),
        map(panelFromPortletUrl),
        distinctUntilChanged((previous, next) => previous.key === next.key)
    );
}
