import { EMPTY } from 'rxjs';

import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { catchError, take } from 'rxjs/operators';

import {
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

@Injectable({
    providedIn: 'root'
})
export class DotContentDriveNavigationService {
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

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

                // New editor: open it in a side panel over Content Drive instead of navigating.
                // Forward `folderPath` so the content is created in the folder being browsed.
                this.#editPanelRequest.set({
                    mode: 'new',
                    contentTypeId: contentTypeVariable,
                    folderPath: folder.folderPath,
                    title: contentType.name
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

                // New editor: open it in a side panel over Content Drive instead of navigating.
                this.#editPanelRequest.set({
                    mode: 'edit',
                    contentletInode: contentlet.inode,
                    title: contentlet.title
                });
            });
    }
}
