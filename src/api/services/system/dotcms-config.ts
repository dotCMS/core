import {Injectable} from '@angular/core';

/**
 * Created by josecastro on 7/29/16.
 *
 * Wraps the configuration properties for dotCMS in order to provide an
 * easier way to access the information.
 *
 */
@Injectable()
export class DotcmsConfig {

    private configParams: any;

    /**
     * Initializes this class with the dotCMS core configuration parameters.
     *
     * @param configParams - The configuration properties for the current instance.
     */
    constructor(configParams: any) {
        this.configParams = configParams;
    }

    /**
     * Returns the specified protocol for Websocket connections. Defaults to "ws".
     *
     * @returns {String} The Websocket protocol.
     */
    getWebsocketProtocol(): String {
        return this.configParams.config['dotcms.websocket.protocol'];
    }

    /**
     * Returns the base URL (domain name) used by clients to connect to the server
     * end-point that will receive and handle connection requests.
     *
     * @returns {String} The Websocket base URL.
     */
    getWebsocketBaseUrl(): String {
        return this.configParams.config['dotcms.websocket.baseurl'];
    }

    /**
     * Returns the URL to the System Events end-point that clients will use to get
     * notifications on events created by dotCMS or custom code.
     *
     * @returns {String} The System Events end-point URL.
     */
    getSystemEventsEndpoint(): String {
        return this.configParams.config['dotcms.websocket.endpoints']['websocket.systemevents.endpoint'];
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

}
