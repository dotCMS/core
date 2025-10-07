import { Subject, Observable, timer } from 'rxjs';

import { Injectable, inject } from '@angular/core';

import { tap } from 'rxjs/operators';

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
 * It is a socket to receive notifications when a event is triggered by the server, first this try to establish a web socket
 * connection if it fails then try a Long polling connection instead.
 *
 * If the connection is lost at any point it will try to reconnect automatically after a time set by configuration parameters.
 * It implements an exponential backoff strategy with jitter for reconnection attempts.
 *
 * @export
 */
@Injectable()
export class DotEventsSocket {
    private dotEventsSocketURL = inject(DotEventsSocketURL);
    private dotcmsConfigService = inject(DotcmsConfigService);
    private loggerService = inject(LoggerService);
    private coreWebService = inject(CoreWebService);

    private protocolImpl: Protocol;

    private status: ConnectionStatus = ConnectionStatus.NONE;
    private _message: Subject<DotEventMessage> = new Subject<DotEventMessage>();
    private _open: Subject<boolean> = new Subject<boolean>();
    private webSocketConfigParams: WebSocketConfigParams;
    private readonly MAX_RETRIES = 100000;
    private readonly INITIAL_RETRY_DELAY = 1000;
    private readonly MAX_RETRY_DELAY = 30000; // 30 seconds max delay
    private retryCount = 0;

    /**
     * Connect to a Event socket using  Web Socket protocol,
     * if a Web Socket connection can be stablish then try again with a Long Polling connection.
     *
     * @returns Observable<ConfigParams>
     * @memberof DotEventsSocket
     */
    connect(): Observable<ConfigParams> {
        // Using the init method and making sure the return type is correct
        return this.dotcmsConfigService.getConfig().pipe(
            tap((config) => {
                this.webSocketConfigParams = config.websocket;
                this.protocolImpl =
                    this.isWebSocketsBrowserSupport() &&
                    !this.webSocketConfigParams.disabledWebsockets
                        ? this.getWebSocketProtocol()
                        : this.getLongPollingProtocol();
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

    private connectProtocol(): void {
        this.protocolImpl.open$().subscribe(() => {
            this.status = ConnectionStatus.CONNECTED;
            this._open.next(true);
            // Reset retry counter on successful connection
            this.retryCount = 0;
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
                this.reconnect();
            }
        });

        this.protocolImpl.close$().subscribe((_event) => {
            if (this.status !== ConnectionStatus.CLOSED) {
                this.loggerService.info('Connection closed unexpectedly, attempting to reconnect');
                this.reconnect();
            } else {
                this.loggerService.debug('Connection closed normally');
            }
        });

        this.protocolImpl.message$().subscribe(
            (res) => this._message.next(res),
            (e) => {
                this.loggerService.debug('Error in the System Events service: ' + e.message);
                this.reconnect();
            },
            () => this.loggerService.debug('Completed')
        );

        this.protocolImpl.connect();
    }

    private reconnect(): void {
        // Don't attempt more reconnections than MAX_RETRIES (which is a big number because we always want to reconnect)
        if (this.retryCount >= this.MAX_RETRIES) {
            this.loggerService.info(
                `Maximum reconnection attempts (${this.MAX_RETRIES}) reached. Giving up.`
            );
            this.status = ConnectionStatus.CLOSED;

            return;
        }

        this.status = this.getAfterErrorStatus();
        this.retryCount++;

        const delay = this.calculateReconnectDelay();

        this.loggerService.info(
            `Scheduling reconnection attempt ${this.retryCount}/${this.MAX_RETRIES} in ${delay}ms`
        );

        timer(delay).subscribe(() => {
            if (this.status !== ConnectionStatus.CLOSED) {
                this.loggerService.info('Attempting to reconnect');
                this.protocolImpl.connect();
            }
        });
    }

    private calculateReconnectDelay(): number {
        // Use configured time or default delay with a random jitter to prevent thundering herd
        const baseDelay =
            this.webSocketConfigParams?.websocketReconnectTime || this.INITIAL_RETRY_DELAY;

        // Exponential backoff with jitter, capped at MAX_RETRY_DELAY
        const exponentialDelay = Math.min(
            baseDelay * Math.pow(2, Math.min(this.retryCount, 10)), // Cap exponential growth
            this.MAX_RETRY_DELAY
        );

        // Add random jitter to prevent reconnection thundering herd problems
        const jitter = Math.random() * 1000; // Up to 1 second of jitter

        return exponentialDelay + jitter;
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
