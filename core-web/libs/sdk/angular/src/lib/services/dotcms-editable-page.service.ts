import { Observable, of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { finalize } from 'rxjs/operators';

import {
    UVEEventType,
    DotCMSPageResponse,
    DotCMSComposedPageResponse,
    DotCMSExtendedPageResponse
} from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE, updateNavigation } from '@dotcms/uve';

@Injectable({
    providedIn: 'root'
})
export class DotCMSEditablePageService {
    /**
     * Subject that emits the current editable page asset or null.
     * Used internally to track changes to the page data.
     *
     * @private
     * @type {Subject<DotCMSPageResponse | undefined>}
     */
    #responseSubject = new Subject<DotCMSPageResponse | undefined>();

    /**
     * Observable stream of the page asset changes.
     * Exposes the pageAssetSubject as an Observable for subscribers.
     *
     * @private
     * @type {Observable<DotCMSPageResponse | undefined>}
     */
    #response$ = this.#responseSubject.asObservable();

    /**
     * Listens for changes to an editable page and returns an Observable that emits the updated page data.
     * This method initializes the UVE (Universal Visual Editor) and sets up subscriptions to track content changes.
     *
     * @example
     * ```ts
     * // Import the service
     * import { DotCMSEditablePageService } from '@dotcms/angular';
     *
     * // Inject the service
     * constructor(private editablePageService: DotCMSEditablePageService) {}
     *
     * // Get the page data from your API call
     * const page = await client.page.get('/');
     *
     * // Listen for changes
     * const subscription = this.editablePageService.listen(page).subscribe(updatedPage => {
     *   if (updatedPage) {
     *     // Handle updated page data
     *     console.log('Page updated:', updatedPage);
     *   }
     * });
     *
     * // When done listening, unsubscribe
     * subscription.unsubscribe();
     * ```
     *
     * @param response Optional initial page data
     * @returns Observable that emits the updated page data or undefined
     */
    listen<T extends DotCMSExtendedPageResponse>(
        response?: DotCMSComposedPageResponse<T>
    ): Observable<DotCMSComposedPageResponse<T> | undefined> {
        if (!getUVEState()) {
            return of(response);
        }

        const pageURI = response?.pageAsset?.page?.pageURI;

        initUVE(response);

        // Update the navigation to the pageURI, when we have a pageURI
        // Sometimes the page is null due to permissions, so we don't want to update the navigation
        // And wait for the UVE to resolve the page
        if (pageURI) {
            updateNavigation(pageURI);
        }

        const unsubscribeUVEChanges = this.#listenUVEChanges();

        return this.#response$.pipe(
            finalize(() => {
                unsubscribeUVEChanges();
            })
        );
    }

    /**
     * Sets up a subscription to listen for UVE content changes and updates the page asset subject.
     * This is an internal method used by listenEditablePage() to handle UVE events.
     *
     * @returns {UVEUnsubscribeFunction} Function to unsubscribe from the UVE content changes
     * @private
     */
    #listenUVEChanges() {
        const { unsubscribe } = createUVESubscription(UVEEventType.CONTENT_CHANGES, (payload) => {
            this.#responseSubject.next(payload);
        });

        return unsubscribe;
    }
}
