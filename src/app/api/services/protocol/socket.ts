
import {WebSocketProtocol} from './websockets-protocol';
import {Protocol, Url, QueryParams} from './protocol';
import {LoggerService} from '../logger.service';
import {LongPollingProtocol} from './long-polling-protocol';
import {CoreWebService} from '../core-web-service';
import {ConfigParams} from '../system/dotcms-config';

enum CONNECTION_STATUS {
    NONE,
    CONNECTING,
    CONNECTED,
    CLOSED
}

enum WEB_SOCKET_SERVER_SUPPORT {
    NOT_SUPPORTED,
    SUPPORTED,
    DONT_NOW
}

export class EventsSocket extends Protocol {

    private protocolImpl: Protocol;

    private closedOnLogout = false;
    private isWebSocketServerSupport: WEB_SOCKET_SERVER_SUPPORT = WEB_SOCKET_SERVER_SUPPORT.DONT_NOW;
    private status: CONNECTION_STATUS = CONNECTION_STATUS.NONE;

    private lastcallback: number;

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     * the Websocket parameters.
     */
    constructor(private url: Url, configParams: ConfigParams, loggerService: LoggerService,
                private coreWebService: CoreWebService) {

        super(loggerService, {
            timeWaitToReconnect: configParams.websocketReconnectTime
        });

        this.isWebSocketServerSupport = configParams.disabledWebsockets ? WEB_SOCKET_SERVER_SUPPORT.NOT_SUPPORTED
            : WEB_SOCKET_SERVER_SUPPORT.DONT_NOW;
    }

    start(): void {
        if (!this.protocolImpl && this.url) {
            this.loggerService.debug('Creating a new socket connection', this.url.url);

            this.protocolImpl = this.getProtocol();
            this.status = CONNECTION_STATUS.CONNECTING;
            this.protocolImpl.start();

            this.protocolImpl.open$().subscribe( () => {
                if (this.isWebSocketProtocol()) {
                    this.isWebSocketServerSupport = WEB_SOCKET_SERVER_SUPPORT.SUPPORTED;
                }
                this.status = CONNECTION_STATUS.CONNECTED;
                this._open.next();
            });

            this.protocolImpl.error$().subscribe( error => {

                if ( this.status === CONNECTION_STATUS.CONNECTING && this.isWebSocketProtocol()) {
                    this.loggerService.info('Error connecting with Websockets, trying again with long polling');
                    this.status = CONNECTION_STATUS.NONE;
                    this.isWebSocketServerSupport = WEB_SOCKET_SERVER_SUPPORT.NOT_SUPPORTED;
                    this.protocolImpl = null;
                    this.start();
                } else {
                    this._error.next(error);
                }
            });

            this.protocolImpl.close$().subscribe(event => {

                if (this.closedOnLogout) { // We explicitly closed the socket

                    // If we closed the socket for a logout we need to reset the closedOnLogout flag
                    this.closedOnLogout = false;
                    this.protocolImpl = null; // Cleaning up the socket as we explicitly closed the socket

                } else if (this.status === CONNECTION_STATUS.CONNECTED) {
                    // Something happened and we need to try a reconnection
                    this.reconnect();
                }

                this.status = CONNECTION_STATUS.CLOSED;
            });

            this.protocolImpl.message$().subscribe(
                res => this._message.next(res),
                e =>  this.loggerService.debug('Error in the System Events service: ' + e.message),
                () => this.loggerService.debug('Completed')
            );
        }
    }

    destroy(): void {
        // On logout, meaning no authenticated user lets try to close the socket
        if (this.protocolImpl) {
            this.loggerService.debug('Closing socket');
            this.closedOnLogout = true;
            this.protocolImpl.destroy();
            this._close.next();
        }
    }

    private getProtocol(): Protocol {
        let isWebSocketBrowserSupport = this.isWebSocketsBrowserSupport();
        if (isWebSocketBrowserSupport && this.isWebSocketServerSupport !== WEB_SOCKET_SERVER_SUPPORT.NOT_SUPPORTED) {
            return new WebSocketProtocol(this.url, this.loggerService);
        }else {
            return new LongPollingProtocol(this.url.getHttpUrl(), this.loggerService, this.coreWebService,
                data => {
                    let result: QueryParams = {};

                    if (data.length > 0) {
                        this.lastcallback = data[data.length - 1].creationDate + 1;
                        result.lastcallback = this.lastcallback;
                    }else if (this.lastcallback) {
                        result.lastcallback = this.lastcallback;
                    }

                    return result;
                });
        }
    }

    private isWebSocketsBrowserSupport(): boolean {
        return 'WebSocket' in window || 'MozWebSocket' in window;
    }

    private isWebSocketProtocol(): boolean {
        return this.protocolImpl instanceof WebSocketProtocol;
    }
}
