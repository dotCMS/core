import { BehaviorSubject } from 'rxjs';

import { Injectable } from '@angular/core';
@Injectable({
    providedIn: 'root'
})
export class DotNavLogoService {
    /**
     * `| null` because `setLogo` publishes null for anything that is not a `/dA` asset path — that
     * is how "no custom logo" is spelled, and the nav header's template branches on it.
     */
    navBarLogo$: BehaviorSubject<string | null> = new BehaviorSubject<string | null>('');

    /**
     * Sets a logo for the nav bar
     *
     * @param {string} navLogo
     * @return {*}  {void}
     * @memberof DotNavLogoService
     */
    setLogo(navLogo: string | null): void {
        if (navLogo?.startsWith('/dA')) {
            this.navBarLogo$.next(this.setUrlProperty(navLogo));
        } else {
            this.navBarLogo$.next(null);
        }
    }

    private setUrlProperty(navLogo: string): string {
        return `url("${navLogo}")`;
    }
}
