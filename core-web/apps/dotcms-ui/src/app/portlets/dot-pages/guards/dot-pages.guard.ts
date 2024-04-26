import { Observable } from 'rxjs';

import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn } from '@angular/router';

import { map, switchMap } from 'rxjs/operators';

import { DotContentTypeService, DotContentletService, DotRouterService } from '@dotcms/data-access';

/**
 * Check if the new edit content is enabled for a content an redirect the user to it.
 * Else, let the user to go to the old edit content.
 *
 * @param {ActivatedRouteSnapshot} route
 * @return {*}  {Observable<boolean>}
 */
export const newEditContentForContentletGuard: CanActivateFn = (
    route: ActivatedRouteSnapshot
): Observable<boolean> => {
    const inode = route.paramMap.get('asset');
    const dotContentletService = inject(DotContentletService);
    const dotContentTypeService = inject(DotContentTypeService);
    const dotRouterService = inject(DotRouterService); // Inject the Router service

    return dotContentletService.getContentletByInode(inode).pipe(
        switchMap((contentlet) => {
            return dotContentTypeService.getContentType(contentlet.contentType).pipe(
                map(({ metadata }) => {
                    const newEditorEnabled = metadata?.CONTENT_EDITOR2_ENABLED;
                    if (!newEditorEnabled) {
                        return true;
                    }

                    dotRouterService.goToURL(`content/${inode}`);

                    return false;
                })
            );
        })
    );
};

/**
 * Check if new edit content portlet is enabled for a content type and redirect the user to it.
 * Otherwise, allow the user to go to the old edit content portlet.
 *
 * @param {ActivatedRouteSnapshot} route
 * @return {*}  {Observable<boolean>}
 */
export const newEditContentForContentTypeGuard: CanActivateFn = (
    route: ActivatedRouteSnapshot
): Observable<boolean> => {
    const contentType = route.paramMap.get('contentType');
    const dotContentTypeService = inject(DotContentTypeService);
    const dotRouterService = inject(DotRouterService); // Inject the Router service

    return dotContentTypeService.getContentType(contentType).pipe(
        map(({ metadata }) => {
            const newEditorEnabled = metadata?.CONTENT_EDITOR2_ENABLED;
            if (!newEditorEnabled) {
                return true;
            }

            dotRouterService.goToURL(`content/new/${contentType}`);

            return false;
        })
    );
};
