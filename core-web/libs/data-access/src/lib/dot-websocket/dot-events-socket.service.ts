import { BehaviorSubject, Observable, Subject, Subscription, timer } from 'rxjs';

import { Injectable } from '@angular/core';

import { distinctUntilChanged, filter, map } from 'rxjs/operators';

import { DotEventMessage } from './dot-event-message.model';

export type WebSocketStatus = 'connecting' | 'reconnecting' | 'connected' | 'closed';

/**
 * Manages the WebSocket connection to the dotCMS server-sent events endpoint.
 *
 * Features:
 * - Native WebSocket (no long-polling fallback — all modern browsers support WS)
 * - Exponential backoff with jitter on reconnection
 * - `status$()` — reactive connection state for UI indicators
 * - `on<T>(eventType)` — typed event subscription
 */
@Injectable({ providedIn: 'root' })
export class DotEventsSocket {
    private readonly socketURL = (() => {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        return `${protocol}//${window.location.host}/api/ws/v1/system/events`;
    })();

    private socket: WebSocket | null = null;
    private status: WebSocketStatus = 'connecting';
    private retryCount = 0;
    private destroyed = false;
    private reconnectTimer: Subscription | null = null;

    private readonly _message = new Subject<DotEventMessage>();
    private readonly _status = new BehaviorSubject<WebSocketStatus>('connecting');

    private readonly MAX_RETRIES = 100_000;
    private readonly INITIAL_RETRY_DELAY = 1_000;
    private readonly MAX_RETRY_DELAY = 30_000;

    /**
     * Opens the WebSocket connection. Call once at app startup.
     * Returns an Observable that completes immediately after initiating the connection —
     * subscribe in GlobalStore's withWebSocket feature.
     */
    connect(): Observable<void> {
        return new Observable<void>((subscriber) => {
            this.destroyed = false;
            this.openSocket();
            subscriber.next();
            subscriber.complete();
        });
    }

    /** Closes the connection permanently (e.g. on logout). */
    destroy(): void {
        this.destroyed = true;
        this.reconnectTimer?.unsubscribe();
        this.reconnectTimer = null;
        this.setStatus('closed');
        this.socket?.close();
        this.socket = null;
    }

    /** Emits the typed payload of a specific event type. */
    on<T>(eventType: string): Observable<T> {
        return this._message.asObservable().pipe(
            filter((msg) => msg.event === eventType),
            map((msg) => msg.payload?.data as T)
        );
    }

    /** All raw messages from the server. */
    messages(): Observable<DotEventMessage> {
        return this._message.asObservable();
    }

    isConnected(): boolean {
        return this.status === 'connected';
    }

    /** Emits only when the status actually changes. */
    status$(): Observable<WebSocketStatus> {
        return this._status.asObservable().pipe(distinctUntilChanged());
    }

    private openSocket(): void {
        if (this.destroyed || this.socket?.readyState === WebSocket.CONNECTING) {
            return;
        }

        this.setStatus(this.retryCount === 0 ? 'connecting' : 'reconnecting');

        try {
            this.socket = new WebSocket(this.socketURL);
        } catch {
            this.scheduleReconnect();
            return;
        }

        this.socket.onopen = () => {
            this.retryCount = 0;
            this.setStatus('connected');
        };

        this.socket.onmessage = (ev: MessageEvent) => {
            try {
                this._message.next(JSON.parse(ev.data) as DotEventMessage);
            } catch {
                // Ignore unparseable messages
            }
        };

        this.socket.onclose = (ev: CloseEvent) => {
            if (!this.destroyed) {
                // 1000 = normal closure (server-initiated clean close) — still reconnect
                // since dotCMS may restart
                if (ev.code !== 1001) {
                    this.scheduleReconnect();
                }
            }
        };

        this.socket.onerror = () => {
            // onerror is always followed by onclose, so let onclose drive reconnection
        };
    }

    private scheduleReconnect(): void {
        if (this.retryCount >= this.MAX_RETRIES || this.destroyed) {
            this.setStatus('closed');
            return;
        }

        this.retryCount++;
        this.setStatus('reconnecting');

        this.reconnectTimer?.unsubscribe();
        this.reconnectTimer = timer(this.calculateDelay()).subscribe(() => {
            this.reconnectTimer = null;
            if (!this.destroyed) {
                this.openSocket();
            }
        });
    }

    private calculateDelay(): number {
        const exponential = Math.min(
            this.INITIAL_RETRY_DELAY * Math.pow(2, Math.min(this.retryCount, 10)),
            this.MAX_RETRY_DELAY
        );

        return exponential + Math.random() * 1_000;
    }

    private setStatus(status: WebSocketStatus): void {
        this.status = status;
        this._status.next(status);
    }
}
