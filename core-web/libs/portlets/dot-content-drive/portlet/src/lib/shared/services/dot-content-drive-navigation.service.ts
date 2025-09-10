import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotContentTypeService, DotRouterService } from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotContentDriveItem,
    FeaturedFlags
} from '@dotcms/dotcms-models';

@Injectable({
    providedIn: 'root'
})
export class DotContentDriveNavigationService {
    readonly #router = inject(Router);
    readonly #dotContentTypeService = inject(DotContentTypeService);
    readonly #dotRouterService = inject(DotRouterService);

    /**
     * Navigates to the appropriate editor based on the content type
     * @param contentlet The content item to edit
     */
    editContent(contentlet: DotContentDriveItem) {
        if (contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE) {
            this.#editPage(contentlet);
        } else {
            this.#editContentlet(contentlet);
        }
    }

    #editPage(contentlet: DotContentDriveItem) {
        const url = contentlet.urlMap || contentlet.url;

        this.#dotRouterService.goToEditPage({ url, language_id: contentlet.languageId });
    }

    #editContentlet(contentlet: DotContentDriveItem) {
        this.#dotContentTypeService
            .getContentType(contentlet.contentType)
            .pipe(take(1))
            .subscribe((contentType) => {
                const shouldRedirectToOldContentEditor =
                    !contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED];

                if (shouldRedirectToOldContentEditor) {
                    this.#router.navigate([`c/content/${contentlet.inode}`]);
                    return;
                }

                this.#router.navigate([`content/${contentlet.inode}`]);
            });
    }
}
