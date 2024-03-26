// tslint:disable:typedef
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { DotSiteService } from '@dotcms/data-access';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { map, skip, takeUntil, tap } from 'rxjs/operators';

window['mapsApi$'] = new BehaviorSubject({ ready: false });

window['mapsApiReady'] = () => {
    window['mapsApi$'].next({ ready: true });
    window['mapsApi$'].complete();
};

@Injectable()
export class GoogleMapService {
    apiKey: string = null;

    apiReady: Boolean = false;
    loadingApi: Boolean = false;
    mapsApi$: BehaviorSubject<{ ready: boolean; error?: any }>;
    private destroy$ = new Subject<boolean>();
    constructor(private siteService: SiteService, private dotSiteService: DotSiteService) {
        this.getApiKey(this.siteService.currentSite.identifier);
        this.mapsApi$ = window['mapsApi$'];
        this.mapsApi$.subscribe((gMapApi) => {
            if (gMapApi != null) {
                this.apiReady = true;
            }
        });

        this.siteService.switchSite$
            .pipe(takeUntil(this.destroy$))
            .subscribe(({ identifier }) => this.getApiKey(identifier));
    }

    //this method gets the Google key from the current site
    getApiKey(siteId): void {
        this.siteService
            .getSiteById(siteId)
            .pipe(map((site) => site.googleMap || null))
            .subscribe((apiKey) => {
                this.apiKey = apiKey;
                console.log('API KEY', this.apiKey);
            });
    }

    loadApi(): void {
        this.loadingApi = true;
        let url: string;
        if (this.apiKey) {
            url = `https://maps.googleapis.com/maps/api/js?key=${this.apiKey}&callback=mapsApiReady`;
        } else {
            url = `https://maps.googleapis.com/maps/api/js?callback=mapsApiReady`;
        }
        console.log('URL to render', url);
        this.addScript(url);
    }

    addScript(url, callback?): void {
        const id = 'google-maps-api';

        if (document.getElementById(id)) {
            document.getElementById(id).remove();
        }

        const script = document.createElement('script');
        if (callback) {
            script.onload = callback;
        }
        script.id = id;
        script.type = 'text/javascript';
        script.src = url;

        document.body.appendChild(script);
    }
}
