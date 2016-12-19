import {ApiRoot} from '../../persistence/ApiRoot';
import {CoreWebService} from "../core-web-service";
import {Http, RequestMethod} from '@angular/http';
import {Injectable} from '@angular/core';
import {Observable, Observer} from 'rxjs/Rx';

/**
 * Created by josecastro on 7/29/16.
 *
 * Wraps the configuration properties for dotCMS in order to provide an
 * easier way to access the information.
 *
 */
const DEFAULT_REST_PAGE_COUNT = 'DEFAULT_REST_PAGE_COUNT';
const DOTCMS_WEBSOCKET_RECONNECT_TIME = 'dotcms.websocket.reconnect.time';
const DOTCMS_WEBSOCKET_ENDPOINTS = 'dotcms.websocket.endpoints';
const WEBSOCKET_SYSTEMEVENTS_ENDPOINT ='websocket.systemevents.endpoint';
const DOTCMS_WEBSOCKET_BASEURL = 'dotcms.websocket.baseurl';
const DOTCMS_WEBSOCKET_PROTOCOL = 'dotcms.websocket.protocol';

@Injectable()
export class DotcmsConfig extends CoreWebService {

    private waiting: Observer[] = [];
    private configParams: any;
    private configUrl: string;

    /**
     * Initializes this class with the dotCMS core configuration parameters.
     *
     * @param configParams - The configuration properties for the current instance.
     */
    constructor(apiRoot: ApiRoot, http: Http) {
        super(apiRoot, http);
        this.configUrl = 'v1/appconfiguration';
        this.loadConfig();
    }

    getConfig(): Observable<DotcmsConfig> {
        return Observable.create( obs => {
            if (this.configParams) {
                obs.next(this);
            } else {
                this.waiting.push(obs);
            }
        });
    }

    loadConfig(): void {
        this.requestView({
            method: RequestMethod.Get,
            url: this.configUrl
        }).pluck('entity').subscribe(res => {
            this.configParams = res;
            this.waiting.forEach(obs => obs.next(this));
            this.waiting = null;
            return res;
        });
    }

    /**
     * Returns the specified protocol for Websocket connections. Defaults to "ws".
     *
     * @returns {String} The Websocket protocol.
     */
    getWebsocketProtocol(): String {
        return this.configParams.config[DOTCMS_WEBSOCKET_PROTOCOL];
    }

    /**
     * Returns the base URL (domain name) used by clients to connect to the server
     * end-point that will receive and handle connection requests.
     *
     * @returns {String} The Websocket base URL.
     */
    getWebsocketBaseUrl(): String {
        return this.configParams.config[DOTCMS_WEBSOCKET_BASEURL];
    }

    /**
     * Returns the URL to the System Events end-point that clients will use to get
     * notifications on events created by dotCMS or custom code.
     *
     * @returns {String} The System Events end-point URL.
     */
    getSystemEventsEndpoint(): String {
        return this.configParams.config[DOTCMS_WEBSOCKET_ENDPOINTS][WEBSOCKET_SYSTEMEVENTS_ENDPOINT];
    }

    getTimeToWaitToReconnect(): number {
        return this.configParams.config[DOTCMS_WEBSOCKET_RECONNECT_TIME];
    }

    /**
     * Returns the elements that make up the main navigation menu in the back-end. The
     * items in the menu depend on the roles and permissions of the logged-in user.
     *
     * @returns {Array<any>} The menu items and sub-items.
     */
    getNavigationMenu(): Array<any> {
        return this.configParams.menu;
    }

    /**
     * Returns the default rest page count to display.
     *
     * @returns <number> The max number of sites to display after a search.
     */
    getDefaultRestPageCount(): number {
        return this.configParams.config[DEFAULT_REST_PAGE_COUNT];
    }
}
