import { BehaviorSubject, Observable, Subject } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { switchMap, take, takeUntil } from 'rxjs/operators';

import { SiteService } from '@dotcms/dotcms-js';

/** The two globals this service and the Google Maps callback share on `window`. */
interface MapsApiWindow extends Window {
    mapsApi$: BehaviorSubject<MapsApiState>;
    mapsApiReady: () => void;
}

interface MapsApiState {
    ready: boolean;
    error?: unknown;
}

const mapsWindow = window as unknown as MapsApiWindow;

mapsWindow.mapsApi$ = new BehaviorSubject<MapsApiState>({ ready: false });

mapsWindow.mapsApiReady = () => {
    mapsWindow.mapsApi$.next({ ready: true });
    mapsWindow.mapsApi$.complete();
};

@Injectable()
export class GoogleMapService {
    private siteService = inject(SiteService);

    mapsApi$: BehaviorSubject<MapsApiState>;
    private destroy$ = new Subject<boolean>();
    constructor() {
        this.loadApi(this.siteService.currentSite.identifier).subscribe();
        this.mapsApi$ = mapsWindow.mapsApi$;
        this.mapsApi$.subscribe();

        this.siteService.currentSite$
            .pipe(
                takeUntil(this.destroy$),
                switchMap(({ identifier }) => this.loadApi(identifier))
            )
            .subscribe();
    }

    //this method gets the Google key from the current site and loads the Google Maps API
    loadApi(siteId: string): Observable<boolean> {
        return this.siteService.getSiteById(siteId).pipe(
            take(1),
            switchMap((site) => {
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
