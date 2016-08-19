import {Injectable} from '@angular/core';
import {ApiRoot} from "../persistence/ApiRoot";
import {CoreWebService} from "./core-web-service";
import {Observable} from 'rxjs/Rx';
import {RequestMethod} from '@angular/http';
import {Observer} from "rxjs/Observer";

@Injectable()
export class SiteService  {

    private allSiteUrl:string;
    private switchSiteUrl:string;

    private currentSite:Site;
    private sites:Site[];

    private switchSiteObservable:Observable<Site>;
    private switchSiteObserver:Observer<Site>;

    constructor(_apiRoot: ApiRoot, private coreWebService: CoreWebService) {
        this.allSiteUrl = `${_apiRoot.baseUrl}api/v1/site/currentSite`;
        this.switchSiteUrl = `${_apiRoot.baseUrl}api/v1/site/switch`;

        this.switchSiteObservable = Observable.create( observer => {
            this.switchSiteObserver = observer;

            if ( this.currentSite ) {
                this.switchSiteObserver.next(this.currentSite);
            }
        });
    }

    public getAllSites(): Observable<{currentSite:Site, sites:Site[]}> {

        return Observable.create(observer => {
            this.coreWebService.requestView({
                method: RequestMethod.Get,
                url: this.allSiteUrl
            }).subscribe( response =>{
                console.log('RESPONSE', response);
                this.sites = response.entity.sites;
                this.setCurrentSite( response.entity.currentSite );

                observer.next({
                    currentSite: this.currentSite,
                    sites: response.entity.sites
                });
            }, error => observer.next( error ));
        });
    }

    private setCurrentSite(siteIdentifier:string){
        this.currentSite = this.sites.filter( site => site.identifier === siteIdentifier)[0];

        if (this.switchSiteObserver) {
            this.switchSiteObserver.next(this.currentSite);
        }
    }

    switchSite(siteId:String):Observable<any> {
        return Observable.create(observer => {
            this.coreWebService.requestView({
                method: RequestMethod.Put,
                url: `${this.switchSiteUrl}/${siteId}`
            }).subscribe( response => this.setCurrentSite( siteId ),
                error => observer.error( error ));
        });
    }

    public subscribeSwitchSite():Observable<Site>{
        return this.switchSiteObservable;
    }
}

export interface Site{
    hostName:string
    type:string
    identifier:string
}
