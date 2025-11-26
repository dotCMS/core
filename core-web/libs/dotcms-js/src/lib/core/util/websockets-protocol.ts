import { Subject } from 'rxjs';

import { Protocol } from './protocol';

import { LoggerService } from '../logger.service';

enum WEB_SOCKET_PROTOCOL_CODE {
    NORMAL_CLOSE_CODE = 1000,
    GO_AWAY_CODE = 1001
}
export class WebSocketProtocol extends Protocol {
    dataStream: Subject<{}> = new Subject();
    private socket: WebSocket;
    private errorThrown: boolean;

    constructor(
        private url: string,
        loggerService: LoggerService
    ) {
        super(loggerService);

        const match = new RegExp('wss?://').test(url);
        if (!match) {
            throw new Error('Invalid url provided [' + url + ']');
        }
    }

    connect(): void {
        this.errorThrown = false;
        this.loggerService.debug('Connecting with Web socket', this.url);

        try {
            this.socket = new WebSocket(this.url);

            this.socket.onopen = (ev: Event) => {
                this.loggerService.debug('Web socket connected', this.url);
                this._open.next(ev);
            };

            this.socket.onmessage = (ev: MessageEvent) => {
                this._message.next(JSON.parse(ev.data));
            };

            this.socket.onclose = (ev: CloseEvent) => {
                if (!this.errorThrown) {
                    if (ev.code === WEB_SOCKET_PROTOCOL_CODE.NORMAL_CLOSE_CODE) {
                        this._close.next(ev);
                        this._message.complete();
                    } else {
                        this._error.next(ev);
                    }
                }
            };

            this.socket.onerror = (ev: ErrorEvent) => {
                this.errorThrown = true;
                this._error.next(ev);
            };
        } catch (error) {
            this.loggerService.debug('Web EventsSocket connection error', error);
            this._error.next(error);
        }
    }

    close(): void {
        if (this.socket && this.socket.readyState !== 3) {
            this.socket.close();
        }
    }
}
