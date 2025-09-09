import { DestroyRef, inject, Injectable, Signal, signal } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import {
  DotCMSExtendedPageResponse,
  DotCMSPageRequestParams,
} from '@dotcms/types';
import { filter, startWith, switchMap, tap } from 'rxjs/operators';
import { PageService } from './page.service';
import { DotCMSEditablePageService } from '@dotcms/angular';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { getUVEState } from '@dotcms/uve';
import { ComposedPageResponse, PageState } from '../shared/models';

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

  readonly #context = signal<PageState<T>>({
    status: 'idle',
  });

  /**
   * Initializes the page by loading and managing page content based on the current route.
   * This method:
   * 1. Sets initial loading state
   * 2. Listens for route changes
   * 3. Fetches page content for the current URL
   * 4. Handles vanity URLs and redirects
   * 5. Manages UVE (Universal Visual Editor) integration
   * 6. Updates page state based on responses
   *
   * @param {DotCMSPageRequestParams} extraParams - Additional parameters to include in the page request
   * @returns {Signal<PageState<T>>} A signal containing the current page state
   */
  initializePage(
    extraParams: DotCMSPageRequestParams = {},
  ): Signal<PageState<T>> {
    this.#setLoading();

    // Wait for the router to navigate to the page
    this.#router.events
      .pipe(
        filter(
          (event): event is NavigationEnd => event instanceof NavigationEnd,
        ),
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

          // Fetch the page asset
          return this.#pageService.getPageAsset<T>(url, extraParams);
        }),
      )
      .subscribe((response) => {
        const vanityUrl = response.pageAsset?.vanityUrl;
        const action = vanityUrl?.action ?? 0;

        if (action > 200) {
          this.#router.navigate([vanityUrl?.forwardTo]);
          return;
        }

        if (response.error && !getUVEState()) {
          this.#setError({
            message: response.error.message,
            status: 'Error',
          });
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
      });

    return this.#context.asReadonly();
  }

  /**
   * Set the page content
   * @param page
   */
  #setPageContent(page?: ComposedPageResponse<T>) {
    this.#context.set({
      pageResponse: page,
      status: 'success',
      error: undefined,
    });
  }

  /**
   * Set the loading state
   */
  #setLoading() {
    this.#context.update((state) => ({
      ...state,
      status: 'loading',
      error: undefined,
    }));
  }

  /**
   * Set the error state
   * @param error
   */
  #setError(error: PageState<T>['error']) {
    this.#context.update((state) => ({
      ...state,
      error: error,
      status: 'error',
    }));
  }
}
