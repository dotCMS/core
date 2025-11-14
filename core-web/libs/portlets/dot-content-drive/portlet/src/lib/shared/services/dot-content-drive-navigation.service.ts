import { Location } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotContentTypeService, DotRouterService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveItem,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { mapQueryParamsToCDParams } from '@dotcms/utils';

@Injectable({
    providedIn: 'root'
})
export class DotContentDriveNavigationService {
    readonly #router = inject(Router);
    readonly #location = inject(Location);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);
    /**
     * Navigates to the appropriate editor based on the content type.
     * Routes to the page editor for HTML pages, or the contentlet editor for other types.
     *
     * @param contentlet - The content item to edit
     */
    editContent(contentlet: DotContentDriveItem) {
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
    editPage(contentlet: DotContentDriveItem) {
        const url = contentlet.urlMap || contentlet.url;

        this.#dotRouterService.goToEditPage({ url, language_id: contentlet.languageId });
    }

    /**
     * Navigates to the contentlet editor.
     * Determines whether to use the new or legacy content editor based on
     * the content type's feature flag settings.
     *
     * @param contentlet - The contentlet to edit
     */
    #editContentlet(contentlet: DotContentDriveItem) {
        const currentPath = this.#location.path(true);
        const currentQueryParams = new URL(currentPath, window.location.origin).searchParams;

        this.#dotContentTypeService
            .getContentType(contentlet.contentType)
            .pipe(take(1))
            .subscribe((contentType) => {
                const shouldRedirectToOldContentEditor =
                    !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];

                const mappedQueryParams = mapQueryParamsToCDParams(currentQueryParams);

                if (shouldRedirectToOldContentEditor) {
                    this.#router.navigate([`c/content/${contentlet.inode}`], {
                        queryParams: mappedQueryParams
                    });
                    return;
                }

                this.#router.navigate([`content/${contentlet.inode}`]);
            });
    }
}
