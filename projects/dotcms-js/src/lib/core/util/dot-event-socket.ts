import { WebSocketProtocol } from './websockets-protocol';
import { Protocol } from './protocol';
import { LoggerService } from '../logger.service';
import { LongPollingProtocol } from './long-polling-protocol';
import { CoreWebService } from '../core-web.service';
import { ConfigParams } from '../dotcms-config.service';
import { Subject, Observable } from 'rxjs';
import { Url } from './models/url';
import { DotEventMessage } from './models/dot-event-message';

enum CONNECTION_STATUS {
    NONE,
    CONNECTING,
    CONNECTED,
    CLOSED
}

export class DotEventsSocket {
    private protocolImpl: Protocol;

    private status: CONNECTION_STATUS = CONNECTION_STATUS.NONE;
    private _message: Subject<DotEventMessage> = new Subject<DotEventMessage>();

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     * the Websocket parameters.
     */
    constructor(
        private url: Url,
        private configParams: ConfigParams,
        private loggerService: LoggerService,
        private coreWebService: CoreWebService
    ) {

        this.protocolImpl = this.isWebSocketsBrowserSupport() && !configParams.disabledWebsockets ?
            this.getWebSocketProtocol() : this.getLongPollingProtocol();
    }

    connect(): void {
        this.loggerService.debug('Creating a new socket connection', this.url.url);

        this.status = CONNECTION_STATUS.CONNECTING;
        this.connectProtocol();
    }

    destroy(): void {
        // On logout, meaning no authenticated user lets try to close the socket
        if (this.protocolImpl) {
            this.loggerService.debug('Closing socket');
            this.status = CONNECTION_STATUS.CLOSED;
            this.protocolImpl.close();
        }
    }

    messages(): Observable<DotEventMessage> {
        return this._message.asObservable();
    }

    private connectProtocol(): void {
        this.protocolImpl.connect();

        this.protocolImpl.open$().subscribe(() => {
            this.status = CONNECTION_STATUS.CONNECTED;
        });

        // tslint:disable-next-line:cyclomatic-complexity
        this.protocolImpl.error$().subscribe(() => {
            console.log('this.isWebSocketProtocol()', this.isWebSocketProtocol());
            console.log('this.status', this.status);
            if (this.isWebSocketProtocol() && this.status !== CONNECTION_STATUS.CONNECTED) {
                this.loggerService.info(
                    'Error connecting with Websockets, trying again with long polling'
                );

                this.protocolImpl = this.getLongPollingProtocol();
            }

            setTimeout(
                () => {
                    this.protocolImpl.close();
                    this.connect();
                },
                this.configParams.websocketReconnectTime
            );
        });

        this.protocolImpl.close$().subscribe((_event) => {
            this.status = CONNECTION_STATUS.CLOSED;
        });

        this.protocolImpl
            .message$()
            .subscribe(
                (res) => this._message.next(res),
                (e) =>
                    this.loggerService.debug(
                        'Error in the System Events service: ' + e.message
                    ),
                () => this.loggerService.debug('Completed')
            );
    }

    private getWebSocketProtocol(): WebSocketProtocol {
        return new WebSocketProtocol(this.url.url, this.loggerService);
    }

    private getLongPollingProtocol(): LongPollingProtocol {
        return new LongPollingProtocol(
            this.url.getHttpUrl(),
            this.loggerService,
            this.coreWebService
        );
    }

    private isWebSocketsBrowserSupport(): boolean {
        return 'WebSocket' in window || 'MozWebSocket' in window;
    }

    private isWebSocketProtocol(): boolean {

        return this.protocolImpl instanceof WebSocketProtocol;
    }
}
