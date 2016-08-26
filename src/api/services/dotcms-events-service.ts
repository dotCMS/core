import {DotcmsConfig} from './system/dotcms-config';
import {Inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {$WebSocket} from './websockets-service';
import {User} from './login-service';
import {LoginService} from './login-service';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class DotcmsEventsService {

    ws: $WebSocket;

    private baseUrl: String;
    private protocol: String;
    private endPoint: String;

    private subjects: Subject<any>[] = [];

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     *                        the Websocket parameters.
     */
    constructor(@Inject('dotcmsConfig') private dotcmsConfig: DotcmsConfig, private loginService: LoginService) {
        this.protocol = dotcmsConfig.getWebsocketProtocol();
        this.baseUrl = dotcmsConfig.getWebsocketBaseUrl();
        this.endPoint = dotcmsConfig.getSystemEventsEndpoint();

        if (loginService.loginUser) {
            this.connectWithSocket( this.loginService.loginUser );
        }

        this.loginService.loginUser$.subscribe(user => this.connectWithSocket(user));
    }

    /**
     * Opens the Websocket connection with the System Events end-point.
     */
    connectWithSocket(user: User): void {
        this.ws = new $WebSocket(`${this.protocol}://${this.baseUrl}${this.endPoint}?userId=${user.userId}`);
        this.ws.connect();

        this.ws.getDataStream().subscribe(
            res => {
                let data = (JSON.parse(res.data));

                if (!this.subjects[data.event]) {
                    this.subjects[data.event] = new Subject();
                }
                this.subjects[data.event].next(data.payload);
            },
            function(e): void { console.log('Error in the System Events service: ' + e.message); },
            function(): void { console.log('Completed'); }
        );
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
        if ( !this.subjects[ clientEventType ] ) {
            this.subjects[ clientEventType ] = new Subject();
        }

        return this.subjects[ clientEventType].asObservable();
    }

}
