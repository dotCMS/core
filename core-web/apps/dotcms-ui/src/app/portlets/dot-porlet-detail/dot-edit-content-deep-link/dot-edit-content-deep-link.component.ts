import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { take } from 'rxjs/operators';

import { DotContentletService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DOT_PANEL_URL_PARAM } from '@dotcms/utils';

/**
 * A path deep link to content (`/c/:id/:asset`, `/c/:id/new/:contentType`, `/pages/:asset`,
 * `/pages/new/:contentType`) redirects to its portlet's own URL with the side panel params,
 * replacing the path form in history (#38303, FR-026, FR-049). The portlet then opens the panel as
 * it does for any URL that names one, so a link is not a second way to open a panel. Velocity,
 * workflow emails and old bookmarks still emit the path form, so the routes stay.
 *
 * The portlet is the route's parent: `/c/<id>` for a legacy portlet, `/pages` for Pages. Its own
 * query params survive the redirect.
 */
@Component({
    selector: 'dot-edit-content-deep-link',
    template: ''
})
export class DotEditContentDeepLinkComponent implements OnInit {
    readonly #route = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #contentletService = inject(DotContentletService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    /**
     * Redirects for what the path names: content by inode, named in the URL by its identifier and
     * language, which this looks up; or a content type to create, in the folder `?folder` names.
     */
    ngOnInit(): void {
        const { asset, contentType } = this.#route.snapshot.params;

        if (!asset) {
            const folder = this.#route.snapshot.queryParams['folder'];

            this.#redirect({
                [DOT_PANEL_URL_PARAM.CREATE_CONTENT]: contentType,
                ...(folder ? { folder } : {})
            });

            return;
        }

        this.#contentletService
            .getContentletByInode(asset)
            .pipe(take(1))
            .subscribe({
                next: ({ identifier, languageId }) =>
                    this.#redirect({
                        [DOT_PANEL_URL_PARAM.EDIT_CONTENT]: identifier,
                        [DOT_PANEL_URL_PARAM.EDIT_CONTENT_LANG]: String(languageId)
                    }),
                // A bad inode, a deleted contentlet or no permission: the standard error, and the
                // portlet without a panel.
                error: (error) => {
                    this.#httpErrorManager.handle(error);
                    this.#redirect();
                }
            });
    }

    /**
     * Goes to the portlet's URL, the route's parent, with these panel params, in place of the path
     * form.
     *
     * @param queryParams The panel params, when there is a panel to name.
     */
    #redirect(queryParams?: Params): void {
        // `.` is the parent route's own URL; empty commands would keep the path the link came in on.
        void this.#router.navigate(['.'], {
            relativeTo: this.#route.parent,
            ...(queryParams ? { queryParams } : {}),
            queryParamsHandling: 'merge',
            replaceUrl: true
        });
    }
}
