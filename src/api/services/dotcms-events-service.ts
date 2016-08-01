import {DotcmsConfig} from './system/dotcms-config';
import {Inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {$WebSocket} from './websockets-service';

@Injectable()
export class DotcmsEventsService {

    ws: $WebSocket;

    private baseUrl: String;
    private protocol: String;
    private endPoint: String;

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     *                        the Websocket parameters.
     */
    constructor(@Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig) {
        this.protocol = dotcmsConfig.getWebsocketProtocol();
        this.baseUrl = dotcmsConfig.getWebsocketBaseUrl();
        this.endPoint = dotcmsConfig.getSystemEventsEndpoint();
    }

    /**
     * Opens the Websocket connection with the System Events end-point.
     */
    connectWithSocket(): void {
        this.ws = new $WebSocket(this.protocol + '://' + this.baseUrl + this.endPoint);
        this.ws.send();
    }

    /**
     * This method will be called by clients that want to receive notifications
     * regarding incoming system events. The events they will receive will be
     * based on the type of event clients register for.
     *
     * @param clientEventType - The type of event clients will get. For example,
     *                          "notification" will allow a client to receive the
     *                          messages in the Notification section.
     * @returns {any} The system events that a client will receive.
     */
    subscribeTo(clientEventType: string): Observable<any> {

        return Observable.create(observer => {
            if (!this.ws) {
                this.connectWithSocket();
            }

            this.ws.getDataStream().subscribe(
                res => {
                    let data = (JSON.parse(res.data));
                    if (data.event === clientEventType) {
                        observer.next(data.payload);
                    }
                },
                function(e): void { console.log('Error in the System Events service: ' + e.message); },
                function(): void { console.log('Completed'); }
            );
        });

    }

}
