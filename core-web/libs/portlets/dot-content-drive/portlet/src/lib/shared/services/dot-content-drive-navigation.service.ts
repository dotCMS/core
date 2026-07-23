import { EMPTY } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';

import { catchError, take } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotPropertiesService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { EditContentDialogData } from '@dotcms/edit-content';
import { mapQueryParamsToCDParams } from '@dotcms/utils';

/** Shape of the `/api/content/_search` entity we read the resolved contentlet from. */
interface ContentSearchEntity {
    jsonObjectView: { contentlets: DotCMSContentlet[] };
}

@Injectable({
    providedIn: 'root'
})
export class DotContentDriveNavigationService {
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #contentSearch = inject(DotContentSearchService);
    readonly #dotPropertiesService = inject(DotPropertiesService);

    /**
     * Feature flag gating the side panel. When off, the new editor opens via full-screen route
     * navigation (the previous behavior); when on, it opens in the side panel. Defaults to `false`
     * until the flag resolves, so the safe/previous behavior is used meanwhile.
     */
    readonly sidePanelEnabled = toSignal(
        this.#dotPropertiesService.getFeatureFlag(
            FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL
        ),
        { initialValue: false }
    );

    readonly #editPanelRequest = signal<EditContentDialogData | null>(null);

    /**
     * The content to show in the Edit Content side panel, or `null` when it is closed. Set when
     * the new editor should open for a content type/inode (instead of navigating to the
     * full-screen route); the shell renders the panel while this is set.
     */
    readonly editPanelRequest = this.#editPanelRequest.asReadonly();
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
        const url = contentlet.urlMap || contentlet.url;

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

                if (this.sidePanelEnabled()) {
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

                if (this.sidePanelEnabled()) {
                    // New editor in a side panel over Content Drive (keeps the list/filters).
                    this.#editPanelRequest.set({
                        mode: 'edit',
                        contentletInode: contentlet.inode,
                        identifier: contentlet.identifier,
                        title: contentlet.title
                    });

                    return;
                }

                // Side panel disabled: navigate to the full-screen editor (previous behavior).
                this.#router.navigate([`content/${contentlet.inode}`]);
            });
    }

    /**
     * Opens the Edit Content side panel for a content addressed by its stable `identifier`
     * (e.g. from a shared `?editContent=<identifier>` URL). Resolves the identifier to its
     * current working inode — the editor loads by inode — and opens the panel. No-op when the
     * content can't be resolved (deleted, no permission, bad id).
     */
    openEditByIdentifier(identifier: string): void {
        // No flag gate here on purpose: the `?editContent` param is only ever written while the
        // side panel is active (flag on), so its presence already implies a panel context — and
        // the flag may not have resolved yet on this cold load.
        this.#contentSearch
            .get<ContentSearchEntity>({
                query: `+identifier:${identifier} +working:true`,
                limit: 1
            })
            .pipe(
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((entity) => {
                const contentlet = entity?.jsonObjectView?.contentlets?.[0];
                if (!contentlet?.inode) {
                    return;
                }

                this.#editPanelRequest.set({
                    mode: 'edit',
                    contentletInode: contentlet.inode,
                    identifier,
                    title: contentlet.title
                });
            });
    }
}
