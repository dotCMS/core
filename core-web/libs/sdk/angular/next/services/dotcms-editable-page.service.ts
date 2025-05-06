import { Observable, of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { finalize } from 'rxjs/operators';

import { updateNavigation } from '@dotcms/client';
import { UVEEventType, DotCMSPageResponse } from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE } from '@dotcms/uve';

@Injectable({
    providedIn: 'root'
})
export class DotCMSEditablePageService {
    /**
     * Subject that emits the current editable page asset or null.
     * Used internally to track changes to the page data.
     *
     * @private
     * @type {Subject<DotCMSPageResponse | null>}
     */
    #pageAssetSubject = new Subject<DotCMSPageResponse | null>();

    /**
     * Observable stream of the page asset changes.
     * Exposes the pageAssetSubject as an Observable for subscribers.
     *
     * @private
     * @type {Observable<DotCMSPageResponse | null>}
     */
    #pageAsset$ = this.#pageAssetSubject.asObservable();

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
     * @param pageAsset Optional initial page data
     * @returns Observable that emits the updated page data or null
     */
    listen(pageAsset?: DotCMSPageResponse): Observable<DotCMSPageResponse | null> {
        if (!getUVEState()) {
            return of(pageAsset || null);
        }

        const pageURI = pageAsset?.page?.pageURI ?? '/';

        initUVE(pageAsset);
        updateNavigation(pageURI);

        const unsubscribeUVEChanges = this.#listenUVEChanges();

        return this.#pageAsset$.pipe(
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
            this.#pageAssetSubject.next(payload);
        });

        return unsubscribe;
    }
}
