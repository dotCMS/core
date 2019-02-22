import { Subject } from 'rxjs';
import { LoggerService } from '../logger.service';
import { Protocol } from './protocol';

export class WebSocketProtocol extends Protocol {
    dataStream: Subject<{}> = new Subject();
    private socket: WebSocket;

    private readonly NORMAL_CLOSE_CODE = 1000;

    constructor(private url: string, loggerService: LoggerService) {
        super(loggerService);

        const match = new RegExp('wss?://').test(url);
        if (!match) {
            throw new Error('Invalid url provided [' + url + ']');
        }
    }

    connect(): void {
        this.loggerService.debug('Connecting with Web socket', this.url);

        try {
            this.socket = new WebSocket(this.url);

            this.socket.onopen = (ev: Event) => {
                this.loggerService.debug('Web socket connected', this.url);
                this._open.next(ev);
            };

            this.socket.onmessage = (ev: MessageEvent) => {
                console.log('ev', ev);
                this._message.next(JSON.parse(ev.data));
            };

            this.socket.onclose = (ev: CloseEvent) => {
                if (ev.code === this.NORMAL_CLOSE_CODE) {
                    this._close.next(ev);
                    this._message.complete();
                } else {
                    this._error.next(ev);
                }
            };

            this.socket.onerror = (ev: ErrorEvent) => {
                this.close();
                this._error.next(ev);
            };
        } catch (error) {
            this.loggerService.debug('Web EventsSocket connection error', error);
            console.log('Web EventsSocket connection error', error);
            this._error.next(error);
        }
    }

    close(): void {
        if (this.socket && !this.socket.bufferedAmount) {
            this.socket.close();
        }
    }
}
