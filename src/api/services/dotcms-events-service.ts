import {DotcmsConfig} from './system/dotcms-config';
import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {$WebSocket} from './websockets-service';
import {LoginService} from './login-service';
import {Subject} from 'rxjs/Subject';

@Injectable()
export class DotcmsEventsService {

    ws: $WebSocket;

    private baseUrl: String;
    private protocol: String;
    private endPoint: String;

    private closedOnLogout: boolean = false;

    private subjects: Subject<any>[] = [];
    private timeWaitToReconnect: number;

    /**
     * Initializes this service with the configuration properties that are
     * necessary for opening the Websocket with the System Events end-point.
     *
     * @param dotcmsConfig - The dotCMS configuration properties that include
     *                        the Websocket parameters.
     */
    constructor(private dotcmsConfig: DotcmsConfig, loginService: LoginService) {
        dotcmsConfig.getConfig().subscribe(dotcmsConfig => {
            this.protocol = dotcmsConfig.getWebsocketProtocol();
            this.baseUrl = dotcmsConfig.getWebsocketBaseUrl();
            this.endPoint = dotcmsConfig.getSystemEventsEndpoint();
            this.timeWaitToReconnect = dotcmsConfig.getTimeToWaitToReconnect();
        });

        // Subscribe to changes on the logged user, only start the socket connection when the user is authenticated
        loginService.watchUser(auth => {
            if (auth.user) {
                // The user exist, lets try to create the socket connection
                this.connectWithSocket();
            }
        });

        // Subscribe to changes in order to know when there is not a logged user
        loginService.logout$.subscribe(() => {

            // On logout, meaning no authenticated user lets try to close the socket
            if (this.ws) {

                /*
                 We need to turn on this closedOnLogout flag in order to avoid reconnections as we explicitly
                 closed the socket
                 */
                this.closedOnLogout = true;
                this.ws.close(true);
            }
        });
    }

    /**
     * Opens the Websocket connection with the System Events end-point.
     */
    connectWithSocket(): void {
        if (!this.ws) {
            this.ws = new $WebSocket(`${this.protocol}://${this.baseUrl}${this.endPoint}`);
            this.ws.connect();

            this.ws.onClose(() => {

                if (this.closedOnLogout) { // We explicitly closed the socket

                    // If we closed the socket for a logout we need to reset the closedOnLogout flag
                    this.closedOnLogout = false;
                    this.ws = null; // Cleaning up the socket as we explicitly closed the socket

                } else { // Something happened and we need to try a reconnection
                    setTimeout(this.reconnect.bind(this), this.timeWaitToReconnect);
                }
            });

            this.ws.getDataStream().subscribe(
                res => {
                    let data = (JSON.parse(res.data));
                    console.log('DATA', data);
                    if (!this.subjects[data.event]) {
                        this.subjects[data.event] = new Subject();
                    }
                    this.subjects[data.event].next(data.payload);
                },
                function (e): void {
                    console.log('Error in the System Events service: ' + e.message);
                },
                function (): void {
                    console.log('Completed');
                }
            );
        }
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
        if (!this.subjects[clientEventType]) {
            this.subjects[clientEventType] = new Subject();
        }

        return this.subjects[clientEventType].asObservable();
    }

    subscribeToEvents(clientEventTypes: string[]): Observable<any> {
        let subject: Subject = new Subject();

        clientEventTypes.forEach( eventType => this.subscribeTo(eventType).subscribe(data =>  subject.next(data)) );

        return subject.asObservable();
    }

    private reconnect(): void {
        this.ws = null;
        this.connectWithSocket();
    }
}
