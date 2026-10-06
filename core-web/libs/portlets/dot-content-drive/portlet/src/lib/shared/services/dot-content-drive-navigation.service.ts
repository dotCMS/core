import { EMPTY, Observable, of } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, map, switchMap, take } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { EditContentDialogData } from '@dotcms/edit-content';
import { mapQueryParamsToCDParams } from '@dotcms/utils';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/** Shape of the `/api/content/_search` entity we read the resolved contentlet from. */
interface ContentSearchEntity {
    jsonObjectView: { contentlets: DotCMSContentlet[] };
}

// Provided at the Content Drive shell level (not `root`) so it can inject the shell-scoped
// DotContentDriveStore and read the side-panel feature flag from it.
@Injectable()
export class DotContentDriveNavigationService {
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #contentSearch = inject(DotContentSearchService);
    readonly #store = inject(DotContentDriveStore);

    /**
     * Feature flag gating the side panel. When off, the new editor opens via full-screen route
     * navigation (the previous behavior); when on, it opens in the side panel. Read from the
     * store's `withFlags` slice (batch-fetched once on init, degrades to `false` on a failed config
     * read) — defaults to `false` until it resolves, so the safe/previous behavior is used meanwhile.
     */
    readonly $sidePanelEnabled = computed(
        () => this.#store.flags()[FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL] ?? false
    );

    readonly #editPanelRequest = signal<EditContentDialogData | null>(null);

    /**
     * The content to show in the Edit Content side panel, or `null` when it is closed. Set when
     * the new editor should open for a content type/inode (instead of navigating to the
     * full-screen route); the shell renders the panel while this is set.
     */
    readonly $editPanelRequest = this.#editPanelRequest.asReadonly();
    /**
     * Navigates to the appropriate editor based on the content type.
     * Routes to the page editor for HTML pages, or the contentlet editor for other types.
     *
     * @param contentlet - The content item to edit
     */
    editContent(contentlet: DotCMSContentlet) {
        if (contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE) {
            this.editPage(contentlet);
        } else {
            this.#editContentlet(contentlet);
        }
    }

    /**
     * Navigates to the edit page editor for a page contentlet.
     * Uses the contentlet's URL map or URL along with the language ID for routing.
     *
     * @param contentlet - The page content item to edit
     */
    editPage(contentlet: DotCMSContentlet) {
        const url = contentlet['urlMap'] || contentlet.url;

        this.#dotRouterService.goToEditPage({ url, language_id: contentlet.languageId });
    }

    /**
     * Navigates to the content editor to CREATE a new content of the given type.
     * Mirrors the edit flow ({@link editContent}): the new content editor is only used when it
     * is enabled for the selected content type (CONTENT_EDITOR2 flag); otherwise it falls back
     * to the legacy create editor.
     *
     * @param contentTypeVariable - The variable name of the content type to create
     * @param folder - The folder the user is currently browsing, so the new content is created
     * there. `folderPath` (`hostname/path`) pre-selects the Host/Folder field in the new editor;
     * `folderInode` pre-selects the target folder in the legacy editor.
     */
    createContent(
        contentTypeVariable: string,
        folder: { folderPath?: string; folderInode?: string } = {}
    ): void {
        const currentPath = this.#location.path(true);
        // Parse the query string directly — avoids depending on window.location (SSR/tests).
        const currentQueryParams = new URLSearchParams(currentPath?.split('?')[1] ?? '');

        this.#dotContentTypeService
            .getContentType(contentTypeVariable)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentType) => {
                const shouldRedirectToOldContentEditor =
                    !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];

                if (shouldRedirectToOldContentEditor) {
                    // Carry the current Content Drive params (filters/path) as CD_-prefixed query
                    // params so closing the legacy editor returns the user to Content Drive with
                    // their filters preserved — same mechanism as #editContentlet.
                    const mappedQueryParams = mapQueryParamsToCDParams(currentQueryParams);

                    // The legacy editor pre-selects the target folder from a `folder=<inode>` param
                    // on its action URL. DotCreateContentletResolver reads this route param and
                    // appends it to the resolved action URL loaded in the iframe.
                    if (folder.folderInode) {
                        mappedQueryParams['folder'] = folder.folderInode;
                    }

                    this.#router.navigate([`c/content/new/${contentTypeVariable}`], {
                        queryParams: mappedQueryParams
                    });
                    return;
                }

                if (this.$sidePanelEnabled()) {
                    // New editor in a side panel over Content Drive. Forward `folderPath` so the
                    // content is created in the folder being browsed.
                    this.#editPanelRequest.set({
                        mode: 'new',
                        contentTypeId: contentTypeVariable,
                        folderPath: folder.folderPath,
                        title: contentType.name
                    });

                    return;
                }

                // Side panel disabled: navigate to the full-screen new-content editor (previous
                // behavior). It pre-selects the Host/Folder field from the `folderPath` query param.
                this.#router.navigate([`content/new/${contentTypeVariable}`], {
                    queryParams: folder.folderPath ? { folderPath: folder.folderPath } : {}
                });
            });
    }

    /** Closes the Edit Content side panel. */
    closeEditPanel(): void {
        this.#editPanelRequest.set(null);
    }

    /**
     * Navigates to the contentlet editor.
     * Determines whether to use the new or legacy content editor based on
     * the content type's feature flag settings.
     *
     * @param contentlet - The contentlet to edit
     */
    #editContentlet(contentlet: DotCMSContentlet) {
        const currentPath = this.#location.path(true);
        // Parse the query string directly — avoids depending on window.location (SSR/tests).
        const currentQueryParams = new URLSearchParams(currentPath?.split('?')[1] ?? '');

        this.#dotContentTypeService
            .getContentType(contentlet.contentType)
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentType) => {
                const shouldRedirectToOldContentEditor =
                    !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];

                if (shouldRedirectToOldContentEditor) {
                    const mappedQueryParams = mapQueryParamsToCDParams(currentQueryParams);
                    this.#router.navigate([`c/content/${contentlet.inode}`], {
                        queryParams: mappedQueryParams
                    });
                    return;
                }

                if (this.$sidePanelEnabled()) {
                    // New editor in a side panel over Content Drive (keeps the list/filters).
                    this.#editPanelRequest.set({
                        mode: 'edit',
                        contentletInode: contentlet.inode,
                        identifier: contentlet.identifier,
                        languageId: contentlet.languageId,
                        title: contentlet.title
                    });

                    return;
                }

                // Side panel disabled: navigate to the full-screen editor (previous behavior).
                this.#router.navigate([`content/${contentlet.inode}`]);
            });
    }

    /**
     * Opens the Edit Content editor for a content addressed by its stable `identifier` (e.g. from a
     * shared `?editContent=<identifier>` URL). Resolves the identifier to its current working inode
     * (the editor loads by inode), then routes by the side-panel flag: on → the panel; off → the
     * full-screen editor. The flag is gated here (not skipped) because the param can outlive the
     * flag being on — a shared link, a bookmark, or a URL that travels staging→prod — and AC15
     * requires full-screen when the flag is off. No-op when the content can't be resolved (deleted,
     * no permission, bad id).
     *
     * The flag is read from `$sidePanelEnabled` (the store's `withFlags` slice) after the resolve.
     * On a cold deep-link load the flag is usually resolved by then (the config fetch starts on
     * store init, before this search); in the rare case it hasn't, this falls back to the
     * full-screen editor — safe and functional, just not the panel.
     */
    openEditByIdentifier(identifier: string, languageId?: number): void {
        const anyVersion = `+identifier:${identifier} +working:true`;
        const preferred = this.#preferredLanguageId(languageId);

        this.#resolveWorkingVersion(
            preferred ? `${anyVersion} +languageId:${preferred}` : anyVersion
        )
            .pipe(
                // A link to content with no version in the preferred language must still open, so
                // "nothing" falls back to any version rather than being read as "do not open". Only
                // reached when that language is genuinely missing: the link carries the language that
                // was open, so the first lookup normally hits.
                switchMap((version) =>
                    version || !preferred ? of(version) : this.#resolveWorkingVersion(anyVersion)
                ),
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentlet) => {
                if (!contentlet?.inode) {
                    return;
                }

                if (!this.$sidePanelEnabled()) {
                    // Flag off: full-screen new editor (the panel only ever opened for
                    // CONTENT_EDITOR2 content, so its full-screen equivalent is `content/<inode>`).
                    this.#router.navigate([`content/${contentlet.inode}`]);

                    return;
                }

                this.#editPanelRequest.set({
                    mode: 'edit',
                    contentletInode: contentlet.inode,
                    identifier,
                    languageId: contentlet.languageId,
                    title: contentlet.title
                });
            });
    }

    /**
     * Resolves an identifier query to the single working contentlet it matches, or `undefined`.
     *
     * @param query The Lucene query to run.
     *
     * @return {*} {Observable<DotCMSContentlet | undefined>} The matched contentlet, if any.
     */
    #resolveWorkingVersion(query: string): Observable<DotCMSContentlet | undefined> {
        return this.#contentSearch
            .get<ContentSearchEntity>({ query, limit: 1 })
            .pipe(map((entity) => entity?.jsonObjectView?.contentlets?.[0]));
    }

    /**
     * Which language version the deep link should open, most authoritative first.
     *
     * One identifier has one inode PER LANGUAGE, so the identifier alone does not name a version.
     * `languageId` comes from the URL that opened the panel and names the exact one, so it wins. It is
     * only absent on a link written before the language was recorded; the rest is a best guess for that
     * case — the drive's active Locale filter (its first language, if several are selected), then the
     * environment default. Note both are usually still unresolved here: this runs from the shell's
     * constructor while the store's languages request is in flight, which is exactly why the URL
     * carrying the language matters.
     *
     * @param languageId The language the URL asked for, when it carried one.
     *
     * @return {*} {number | undefined} The language to look for, or `undefined` when none is known.
     */
    #preferredLanguageId(languageId?: number): number | undefined {
        if (languageId) {
            return languageId;
        }

        const [selected] = (this.#store.getFilterValue('languageId') as string[]) ?? [];

        return Number(selected) || this.#store.defaultLanguageId() || undefined;
    }
}
