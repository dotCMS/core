import { Observable, of, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { updateNavigation } from '@dotcms/client';
import { UVEEventType, DotCMSEditablePage, UVEUnsubscribeFunction } from '@dotcms/types';
import { createUVESubscription, getUVEState, initUVE } from '@dotcms/uve';

@Injectable({
    providedIn: 'root'
})
export class DotCMSEditablePageService {
    #pageAssetSubject = new Subject<DotCMSEditablePage | null>();
    #pageAsset$ = this.#pageAssetSubject.asObservable();

    #uveUnsubscribe: UVEUnsubscribeFunction | null = null;

    listenEditablePage(pageAsset?: DotCMSEditablePage): Observable<DotCMSEditablePage | null> {
        if (!getUVEState()) {
            return of(pageAsset || null);
        }

        const pageURI = pageAsset?.page?.pageURI ?? '/';

        initUVE(pageAsset);
        updateNavigation(pageURI);

        this.#uveUnsubscribe = this.#listenUVEChanges();

        return this.#pageAsset$;
    }

    #listenUVEChanges() {
        const { unsubscribe } = createUVESubscription(UVEEventType.CONTENT_CHANGES, (payload) => {
            this.#pageAssetSubject.next(payload);
        });

        return unsubscribe;
    }

    unsubscribeEditablePage() {
        if (this.#uveUnsubscribe) {
            this.#uveUnsubscribe();
            this.#uveUnsubscribe = null;
        }

        this.#pageAssetSubject.next(null);
    }
}
