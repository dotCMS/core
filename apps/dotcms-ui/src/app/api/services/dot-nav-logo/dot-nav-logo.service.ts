import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
@Injectable({
    providedIn: 'root'
})
export class DotNavLogoService {
    navBarLogo$: BehaviorSubject<string> = new BehaviorSubject('');

    constructor() {}

    /**
     * Sets a logo for the nav bar
     *
     * @param {string} navLogo
     * @return {*}  {void}
     * @memberof DotNavLogoService
     */
    setLogo(navLogo: string): void {
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
