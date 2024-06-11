import { Subject, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { tap, pluck } from 'rxjs/operators';

import { LongPollingProtocol } from './long-polling-protocol';
import { DotEventMessage } from './models/dot-event-message';
import { DotEventsSocketURL } from './models/dot-event-socket-url';
import { Protocol } from './protocol';
import { WebSocketProtocol } from './websockets-protocol';

import { CoreWebService } from '../core-web.service';
import { ConfigParams, DotcmsConfigService, WebSocketConfigParams } from '../dotcms-config.service';
import { LoggerService } from '../logger.service';

enum ConnectionStatus {
    NONE,
    CONNECTING,
    RECONNECTING,
    CONNECTED,
    CLOSED
}

/**
 * It is a socket to receive notifications when a event is tiggered by the server, first this try to stablish a web socket
 * connection if it fail then try to a Long polling connection instead.
 *
 * If the connection is lost in any point the it try to reconnect automaticatly after a time set by configuration parameters.
 *
 * @export
 */
@Injectable()
export class DotEventsSocket {
    private protocolImpl: Protocol;

    private status: ConnectionStatus = ConnectionStatus.NONE;
    private _message: Subject<DotEventMessage> = new Subject<DotEventMessage>();
    private _open: Subject<boolean> = new Subject<boolean>();
    private webSocketConfigParams: WebSocketConfigParams;

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfigService - The dotCMS configuration properties that include
     * the Websocket parameters.
     */
    constructor(
        private dotEventsSocketURL: DotEventsSocketURL,
        private dotcmsConfigService: DotcmsConfigService,
        private loggerService: LoggerService,
        private coreWebService: CoreWebService
    ) {}

    /**
     * Connect to a Event socket using  Web Socket protocol,
     * if a Web Socket connection can be stablish then try again with a Long Polling connection.
     *
     * @returns Observable<ConfigParams>
     * @memberof DotEventsSocket
     */
    connect(): Observable<ConfigParams> {
        return this.init().pipe(
            tap(() => {
                this.status = ConnectionStatus.CONNECTING;
                this.connectProtocol();
            })
        );
    }

    /**
     * Destroy the Event socket
     *
     * @memberof DotEventsSocket
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
     *
     * @returns Observable<DotEventMessage>
     * @memberof DotEventsSocket
     */
    messages(): Observable<DotEventMessage> {
        return this._message.asObservable();
    }

    /**
     * Trigger when a connect is open
     *
     * @returns Observable<boolean>
     * @memberof DotEventsSocket
     */
    open(): Observable<boolean> {
        return this._open.asObservable();
    }

    /**
     * Return true if the socket is connected otherwise return false
     *
     * @returns boolean
     * @memberof DotEventsSocket
     */
    isConnected(): boolean {
        return this.status === ConnectionStatus.CONNECTED;
    }

    private init(): Observable<any> {
        return this.dotcmsConfigService.getConfig().pipe(
            pluck('websocket'),
            tap((webSocketConfigParams: WebSocketConfigParams) => {
                this.webSocketConfigParams = webSocketConfigParams;
                this.protocolImpl =
                    this.isWebSocketsBrowserSupport() && !webSocketConfigParams.disabledWebsockets
                        ? this.getWebSocketProtocol()
                        : this.getLongPollingProtocol();
            })
        );
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
                setTimeout(() => {
                    this.status = this.getAfterErrorStatus();
                    this.protocolImpl.connect();
                }, this.webSocketConfigParams.websocketReconnectTime);
            }
        });

        this.protocolImpl.close$().subscribe((_event) => {
            this.status = ConnectionStatus.CLOSED;
        });

        this.protocolImpl.message$().subscribe(
            (res) => this._message.next(res),
            (e) => this.loggerService.debug('Error in the System Events service: ' + e.message),
            () => this.loggerService.debug('Completed')
        );

        this.protocolImpl.connect();
    }

    private getAfterErrorStatus(): ConnectionStatus {
        return this.status === ConnectionStatus.CONNECTING
            ? ConnectionStatus.CONNECTING
            : ConnectionStatus.RECONNECTING;
    }

    private shouldTryWithLongPooling(): boolean {
        return (
            this.isWebSocketProtocol() &&
            this.status !== ConnectionStatus.CONNECTED &&
            this.status !== ConnectionStatus.RECONNECTING
        );
    }

    private getWebSocketProtocol(): WebSocketProtocol {
        return new WebSocketProtocol(this.dotEventsSocketURL.getWebSocketURL(), this.loggerService);
    }

    private getLongPollingProtocol(): LongPollingProtocol {
        return new LongPollingProtocol(
            this.dotEventsSocketURL.getLongPoolingURL(),
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
