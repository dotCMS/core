import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { DYNAMIC_COMPONENTS } from '../components';
import {
    DotcmsNavigationItem,
    DotCMSPageAsset,
    DotCMSUVEAction,
    DotCMSPageRequestParams
} from '@dotcms/types';
import { filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { PageService } from './page.service';
import { DotCMSEditablePageService } from '@dotcms/angular/next';
import { BASE_EXTRA_QUERIES } from '../../shared/constants';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getUVEState, sendMessageToUVE } from '@dotcms/uve';
import { ComposedPageResponse, ExtraContent, PageRender } from '../../shared/models';
import { Observable } from 'rxjs';

/**
 * Service that handles page loading and management for DotCMS pages
 * This service should be provided at the component level, not in root,
 * to ensure each page component has its own isolated instance.
 */
@Injectable()
export class EditablePageService<
    TPage extends DotCMSPageAsset = DotCMSPageAsset,
    TContent = ExtraContent
> {
    #router = inject(Router);
    #pageService = inject(PageService);
    #dotcmsEditablePageService = inject(DotCMSEditablePageService);

    readonly $context = signal<PageRender<TPage, TContent>>({
        status: 'idle'
    });

    readonly $components = signal<any>({});

    /**
     * Initialize page loading for the current route
     * Call this method from a component's ngOnInit
     * @param route Optional override for the current route
     * @returns Observable that completes when initial page load is done
     */
    initializePage({
        activateRoute,
        destroyRef,
        components = DYNAMIC_COMPONENTS,
        extraQuery = BASE_EXTRA_QUERIES
    }: {
        activateRoute: ActivatedRoute;
        destroyRef: DestroyRef;
        components?: any;
        extraQuery?: DotCMSPageRequestParams['graphql'];
    }): Observable<void> {
        this.$components.set(components);

        this.#setLoading();

        return this.#router.events.pipe(
            filter((event): event is NavigationEnd => event instanceof NavigationEnd),
            startWith(null), // Trigger initial load
            tap(() => {
                this.#setLoading();
            }),
            switchMap(() => {
                return this.#pageService.getPageAsset<TPage, TContent>(activateRoute, extraQuery);
            }),
            tap(({ response, error }) => {
                if (error) {
                    this.#setError(error);
                    return;
                }

                console.log('response', response);

                if (getUVEState()) {
                    this.#dotcmsEditablePageService
                        .listen(response)
                        .pipe(takeUntilDestroyed(destroyRef))
                        .subscribe((page) => {
                            this.#setPageContent(page as ComposedPageResponse<TPage, TContent>);
                        });
                } else {
                    this.#setPageContent(response as ComposedPageResponse<TPage, TContent>);
                }
            }),
            // Transform to void to simplify the API
            map(() => undefined)
        );
    }

    #setPageContent(page?: ComposedPageResponse<TPage, TContent>) {
        this.$context.set({
            pageResponse: page,
            status: 'success',
            error: undefined
        });
    }

    #setLoading() {
        this.$context.update((state) => ({
            ...state,
            status: 'loading',
            error: undefined
        }));
    }

    #setError(error: PageRender<TPage, TContent>['error']) {
        this.$context.update((state) => ({
            ...state,
            error: error,
            status: 'error'
        }));

        /**
         * Send a message to the editor to let it know that the client is ready.
         * This is a temporary workaround to avoid the editor to be stuck in the loading state.
         * This will be removed once the editor is able to detect when the client is ready without use DotcmsLayoutComponent.
         */

        // REMIND ME TO REVISIT THIS
        sendMessageToUVE({ action: DotCMSUVEAction.CLIENT_READY, payload: {} });
    }
}
