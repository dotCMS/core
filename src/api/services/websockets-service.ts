import _ from 'lodash';
import {Injectable} from '@angular/core';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class $WebSocket {
    private static sockets:WebSocket[] = [];

    private reconnectAttempts = 0;
    private sendQueue = [];
    private onOpenCallbacks = [];
    private onMessageCallbacks = [];
    private onErrorCallbacks = [];
    private onCloseCallbacks = [];
    private readyStateConstants = {
        'CONNECTING': 0,
        'OPEN': 1,
        'CLOSING': 2,
        'CLOSED': 3,
        'RECONNECT_ABORTED': 4
    };
    private normalCloseCode = 1000;
    private reconnectableStatusCodes = [4000];
    private socket: WebSocket;
    private dataStream: Subject<any>;
    private internalConnectionState: number;

    constructor(private url:string, private protocols?:Array<string>, private config?:WebSocketConfig  ) {
        var match = new RegExp('wss?:\/\/').test(url);
        if (!match) {
            throw new Error('Invalid url provided');
        }
        this.config = config || { initialTimeout: 500, maxTimeout : 300000, reconnectIfNotNormalClose : false };
        this.dataStream = new Subject();
    }

    connect(force:boolean = false) {
        var self = this;
        if (force || !this.socket || this.socket.readyState !== this.readyStateConstants.OPEN) {
            self.socket = this.protocols ? new WebSocket(this.url, this.protocols) : new WebSocket(this.url);

            self.socket.onopen = (ev: Event) => {
                //console.log('onOpen: %s', ev);
                this.onOpenHandler(ev);
            };
            self.socket.onmessage = (ev: MessageEvent) => {
                //console.log('onNext: %s', ev.data);
                self.onMessageHandler(ev);
                this.dataStream.next(ev);
            };
            this.socket.onclose = (ev: CloseEvent) => {
                //console.log('onClose, completed');
                self.onCloseHandler(ev);
                this.dataStream.complete()
            };

            this.socket.onerror = (ev: ErrorEvent) => {
                //console.log('onError', ev);
                self.onErrorHandler(ev);
                this.dataStream.error(ev);
            };

            $WebSocket.sockets.push(this.socket);

        }
    }

    send(data) {
        var self = this;
        if (this.getReadyState() != this.readyStateConstants.OPEN && this.getReadyState() != this.readyStateConstants.CONNECTING) {
            this.connect();
        }
        // TODO: change this for an observer
        return new Promise((resolve, reject) => {
            if (self.socket.readyState === self.readyStateConstants.RECONNECT_ABORTED) {
                reject('Socket connection has been closed');
            }
            else {
                self.sendQueue.push({message: data});
                self.fireQueue();
            }
        });
    }

    getDataStream():Subject<any> {
        return this.dataStream;
    }

    onOpenHandler(event: Event) {
        this.reconnectAttempts = 0;
        this.notifyOpenCallbacks(event);
        this.fireQueue();
    }

    notifyOpenCallbacks(event) {
        for (let i = 0; i < this.onOpenCallbacks.length; i++) {
            this.onOpenCallbacks[i].call(this, event);
        }
    }

    fireQueue() {
        while (this.sendQueue.length && this.socket.readyState === this.readyStateConstants.OPEN) {
            var data = this.sendQueue.shift();

            this.socket.send(
                _.isString(data.message) ? data.message : JSON.stringify(data.message)
            );
            data.deferred.resolve();
        }
    }

    notifyCloseCallbacks(event) {
        for (let i = 0; i < this.onCloseCallbacks.length; i++) {
            this.onCloseCallbacks[i].call(this, event);
        }
    }

    notifyErrorCallbacks(event) {
        for (var i = 0; i < this.onErrorCallbacks.length; i++) {
            this.onErrorCallbacks[i].call(this, event);
        }
    }

    onOpen(cb) {
        this.onOpenCallbacks.push(cb);
        return this;
    }

    onClose(cb) {
        this.onCloseCallbacks.push(cb);
        return this;
    }

    onError(cb) {
        this.onErrorCallbacks.push(cb);
        return this;
    }

    onMessage(callback, options) {
        if (!_.isFunction(callback)) {
            throw new Error('Callback must be a function');
        }

        this.onMessageCallbacks.push({
            fn: callback,
            pattern: options ? options.filter : undefined,
            autoApply: options ? options.autoApply : true
        });
        return this;
    }

    onMessageHandler(message: MessageEvent) {
        var pattern;
        var self = this;
        var currentCallback;
        for (var i = 0; i < self.onMessageCallbacks.length; i++) {
            currentCallback = self.onMessageCallbacks[i];
            currentCallback.fn.apply(self, message);
        }

    }

    onCloseHandler(event: CloseEvent) {
        this.notifyCloseCallbacks(event);
        if ((this.config.reconnectIfNotNormalClose && event.code !== this.normalCloseCode) || this.reconnectableStatusCodes.indexOf(event.code) > -1) {
            this.reconnect();
        }
    }

    onErrorHandler(event) {
        this.notifyErrorCallbacks(event);
    }

    reconnect() {
        this.close(true);
        var backoffDelay = this.getBackoffDelay(++this.reconnectAttempts);
        var backoffDelaySeconds = backoffDelay / 1000;
        // console.log('Reconnecting in ' + backoffDelaySeconds + ' seconds');
        setTimeout( this.connect(), backoffDelay);
        return this;
    }

    close(force: boolean) {
        if (force || !this.socket.bufferedAmount) {
            this.socket.close();
        }
        return this;
    }

    // Exponential Backoff Formula by Prof. Douglas Thain
    // http://dthain.blogspot.co.uk/2009/02/exponential-backoff-in-distributed.html
    getBackoffDelay(attempt) {
        var R = Math.random() + 1;
        var T = this.config.initialTimeout;
        var F = 2;
        var N = attempt;
        var M = this.config.maxTimeout;

        return Math.floor(Math.min(R * T * Math.pow(F, N), M));
    }

    setInternalState(state) {
        if (Math.floor(state) !== state || state < 0 || state > 4) {
            throw new Error('state must be an integer between 0 and 4, got: ' + state);
        }

        this.internalConnectionState = state;

    }

    /**
     * Could be -1 if not initzialized yet
     * @returns {number}
     */
    getReadyState() {
        if (this.socket == null)
        {
            return -1;
        }
        return this.internalConnectionState || this.socket.readyState;
    }

    /**
     * Close all the Web sockets connections
     */
    public static closeAllSockets():void{
        $WebSocket.sockets.forEach(ws => ws.close());
     }

}

export interface WebSocketConfig {
    initialTimeout:number;
    maxTimeout:number ;
    reconnectIfNotNormalClose: boolean
}