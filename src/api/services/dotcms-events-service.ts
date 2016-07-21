import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Rx';
import {$WebSocket} from './websockets-service';

@Injectable()
export class DotcmsEventsService {
    ws: $WebSocket;

    constructor() {
    }

    connectWithSocket() {
        // TODO: how are we going to handle this url?
        this.ws = new $WebSocket("ws://localhost:8080/system/events");
        this.ws.send();
    }

    subscribeTo(clientEventType:string):Observable<any> {

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
                function(e) { console.log('Error: ' + e.message); },
                function() { console.log('Completed'); }
            );
        });

    }

}