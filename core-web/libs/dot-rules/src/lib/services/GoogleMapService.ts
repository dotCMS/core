// tslint:disable:typedef
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { SiteService } from '@dotcms/dotcms-js';
import { DotSiteService } from '@dotcms/data-access';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

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

    constructor(private siteService: SiteService, private dotSiteService: DotSiteService) {
        this.getApiKey();
        this.mapsApi$ = window['mapsApi$'];
        this.mapsApi$.subscribe((gMapApi) => {
            if (gMapApi != null) {
                this.apiReady = true;
            }
        });
    }

    //this method gets the Google key from the current sie
    private getApiKey(): void {
        const siteId = this.siteService.currentSite.identifier;

        this.siteService
            .getSiteById(siteId)
            .pipe(takeUntilDestroyed())
            .subscribe((site) => {
                this.apiKey = site.googleMap || null;
            });
    }

    loadApi(): void {
        if (!this.loadingApi) {
            this.loadingApi = true;
            let url: string;
            if (this.apiKey) {
                url = `https://maps.googleapis.com/maps/api/js?key=${this.apiKey}&callback=mapsApiReady`;
            } else {
                url = `https://maps.googleapis.com/maps/api/js?callback=mapsApiReady`;
            }
            this.addScript(url);
        }
    }

    addScript(url, callback?): void {
        const script = document.createElement('script');
        if (callback) {
            script.onload = callback;
        }
        script.type = 'text/javascript';
        script.src = url;
        document.body.appendChild(script);
    }
}
