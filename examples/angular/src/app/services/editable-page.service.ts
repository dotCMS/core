import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { DotCMSExtendedPageResponse, DotCMSPageRequestParams } from '@dotcms/types';
import { filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { PageService } from './page.service';
import { DotCMSEditablePageService } from '@dotcms/angular/next';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getUVEState } from '@dotcms/uve';
import { ComposedPageResponse, PageRender } from '../shared/models';
import { Observable } from 'rxjs';

/**
 * Service that handles page loading and management for DotCMS pages
 * This service should be provided at the component level, not in root,
 * to ensure each page component has its own isolated instance.
 */
@Injectable()
export class EditablePageService<T extends DotCMSExtendedPageResponse> {
    #router = inject(Router);
    #pageService = inject(PageService);
    #dotcmsEditablePageService = inject(DotCMSEditablePageService);
    #destroyRef = inject(DestroyRef);
    #activatedRoute = inject(ActivatedRoute);

    readonly $context = signal<PageRender<T>>({
        status: 'idle'
    });

    /**
     * Initialize page loading for the current route
     * Call this method from a component's ngOnInit
     * @param route Optional override for the current route
     * @returns Observable that completes when initial page load is done
     */
    initializePage(extraParams?: DotCMSPageRequestParams): Observable<void> {
        this.#setLoading();

        // Wait for the router to navigate to the page
        return this.#router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            startWith(null), // Trigger initial load
            tap(() => {
                this.#setLoading();
            }),
            takeUntilDestroyed(this.#destroyRef),
            switchMap(() => {
                // Get the path from the current route
                const path = this.#activatedRoute.snapshot.url
                    .map((segment) => segment.path)
                    .join('/');

                // If the path is empty, use the root path
                const url = path || '/';

                // Get the query params from the current route
                const queryParams = this.#activatedRoute.snapshot.queryParams;

                // Combine the query params with the extra params
                const fullParams = {
                    ...queryParams,
                    ...extraParams
                };

                // Fetch the page asset
                return this.#pageService.getPageAsset<T>(url, fullParams);
            }),
            tap(({ response, error }) => {
                if (error) {
                    this.#setError(error);
                    return;
                }

                // If UVE is not enabled, set the page content
                if (!getUVEState()) {
                    this.#setPageContent(response as ComposedPageResponse<T>);
                    return;
                }

                // If UVE is enabled, listen for changes
                this.#dotcmsEditablePageService
                    .listen(response)
                    .pipe(takeUntilDestroyed(this.#destroyRef))
                    .subscribe((page) => {
                        // Set the page content every time it changes
                        this.#setPageContent(page as ComposedPageResponse<T>);
                    });
            }),
            // Transform to void to simplify the API
            map(() => undefined)
        );
    }

    /**
     * Set the page content
     * @param page
     */
    #setPageContent(page?: ComposedPageResponse<T>) {
        this.$context.set({
            pageResponse: page,
            status: 'success',
            error: undefined
        });
    }

    /**
     * Set the loading state
     */
    #setLoading() {
        this.$context.update((state) => ({
            ...state,
            status: 'loading',
            error: undefined
        }));
    }

    /**
     * Set the error state
     * @param error
     */
    #setError(error: PageRender<T>['error']) {
        this.$context.update((state) => ({
            ...state,
            error: error,
            status: 'error'
        }));
    }
}
