import { WebSocketProtocol } from './websockets-protocol';
import { Protocol } from './protocol';
import { LoggerService } from '../logger.service';
import { LongPollingProtocol } from './long-polling-protocol';
import { CoreWebService } from '../core-web.service';
import { ConfigParams } from '../dotcms-config.service';
import { Subject, Observable } from 'rxjs';
import { DotEventsSocketURL } from './models/dot-event-socket-url';
import { DotEventMessage } from './models/dot-event-message';

enum ConnectionStatus {
    NONE,
    CONNECTING,
    RECONNECTING,
    CONNECTED,
    CLOSED
}

export class DotEventsSocket {
    private protocolImpl: Protocol;

    private status: ConnectionStatus = ConnectionStatus.NONE;
    private _message: Subject<DotEventMessage> = new Subject<DotEventMessage>();
    private _open: Subject<boolean> = new Subject<boolean>();

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     * the Websocket parameters.
     */
    constructor(
        private dotEventsSocketURL: DotEventsSocketURL,
        private configParams: ConfigParams,
        private loggerService: LoggerService,
        private coreWebService: CoreWebService
    ) {

        this.protocolImpl = this.isWebSocketsBrowserSupport() && !configParams.disabledWebsockets ?
            this.getWebSocketProtocol() : this.getLongPollingProtocol();
    }

    /**
     * Connect to a Event socket using  Web Socket protocol,
     * if a Web Socket connection can be stablish then try again with a Long Polling connection.
     */
    connect(): void {
        this.loggerService.debug('Creating a new socket connection', this.dotEventsSocketURL.url);

        this.status = ConnectionStatus.CONNECTING;
        this.connectProtocol();
    }

    /**
     * Destroy the Event socket
     */
    destroy(): void {
        // On logout, meaning no authenticated user lets try to close the socket
        if (this.protocolImpl) {
            this.loggerService.debug('Closing socket');
            this.status = ConnectionStatus.CLOSED;
            this.protocolImpl.close();
        }
    }

    /**
     * Trigger when a message is received
     */
    messages(): Observable<DotEventMessage> {
        return this._message.asObservable();
    }

    /**
     * Trigger when a connect is open
     */
    open(): Observable<boolean> {
        return this._open.asObservable();
    }

    private connectProtocol(): void {
        this.protocolImpl.open$().subscribe(() => {
            this.status = ConnectionStatus.CONNECTED;
            this._open.next(true);
        });

        this.protocolImpl.error$().subscribe(() => {
            if (this.shouldTryWithLongPooling()) {
                this.loggerService.info(
                    'Error connecting with Websockets, trying again with long polling'
                );

                this.protocolImpl.destroy();
                this.protocolImpl = this.getLongPollingProtocol();
                this.connectProtocol();
            } else {
                setTimeout(
                () => {
                        this.status = this.getAfterErrorStatus();
                        this.protocolImpl.connect();
                    },
                    this.configParams.websocketReconnectTime
                );
            }
        });

        this.protocolImpl.close$().subscribe((_event) => {
            this.status = ConnectionStatus.CLOSED;
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

        this.protocolImpl.connect();
    }

    private getAfterErrorStatus(): ConnectionStatus {
        return this.status === ConnectionStatus.CONNECTING ? ConnectionStatus.CONNECTING : ConnectionStatus.RECONNECTING;
    }

    private shouldTryWithLongPooling(): boolean {
        return this.isWebSocketProtocol() && this.status !== ConnectionStatus.CONNECTED && this.status !== ConnectionStatus.RECONNECTING;
    }

    private getWebSocketProtocol(): WebSocketProtocol {
        return new WebSocketProtocol(this.dotEventsSocketURL.url, this.loggerService);
    }

    private getLongPollingProtocol(): LongPollingProtocol {
        return new LongPollingProtocol(
            this.dotEventsSocketURL.getHttpUrl(),
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
