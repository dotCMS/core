// tslint:disable:typedef
import { BehaviorSubject, Observable, Subject } from 'rxjs';

import { Injectable } from '@angular/core';

import { switchMap, take, takeUntil, tap } from 'rxjs/operators';

import { DotSiteService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';

window['mapsApi$'] = new BehaviorSubject({ ready: false });

window['mapsApiReady'] = () => {
    window['mapsApi$'].next({ ready: true });
    window['mapsApi$'].complete();
};

@Injectable()
export class GoogleMapService {
    apiReady = false;
    loadingApi = false;
    mapsApi$: BehaviorSubject<{ ready: boolean; error?: any }>;
    private destroy$ = new Subject<boolean>();
    constructor(private siteService: SiteService, private dotSiteService: DotSiteService) {
        console.log('GoogleMapService');
        this.loadApi(this.siteService.currentSite.identifier).subscribe(() => {
            this.loadingApi = false;
        });
        this.mapsApi$ = window['mapsApi$'];
        this.mapsApi$.subscribe((gMapApi) => {
            if (gMapApi != null) {
                this.apiReady = true;
            }
        });

        this.siteService.switchSite$
            .pipe(
                takeUntil(this.destroy$),
                switchMap(({ identifier }) => this.loadApi(identifier))
            )
            .subscribe(() => {
                this.loadingApi = false;
            });
    }

    //this method gets the Google key from the current site and loads the Google Maps API
    loadApi(siteId): Observable<boolean> {
        return this.siteService.getSiteById(siteId).pipe(
            take(1),
            switchMap((site) => {
                this.loadingApi = true;
                const url = `https://maps.googleapis.com/maps/api/js?key=${
                    site.googleMap || ''
                }&callback=mapsApiReady`;

                return this.addScript(url);
            })
        );
    }

    private addScript(url: string): Observable<boolean> {
        const id = 'google-maps-api';
        const scriptLoad$ = new Subject<boolean>();
        let script = document.getElementById(id) as HTMLScriptElement;

        document.getElementById(id)?.remove();

        script = document.createElement('script');
        script.id = id;
        script.type = 'text/javascript';
        script.src = url;
        document.body.appendChild(script);

        script.onload = () => scriptLoad$.next(true);

        return scriptLoad$.asObservable();
    }
}
