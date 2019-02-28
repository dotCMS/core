import { Observable, of, throwError } from 'rxjs';
import { StringUtils } from '../string-utils.service';
import { LoggerService } from '../logger.service';
import { DotEventsSocket } from './dot-event-socket';
import { DotEventsSocketURL } from './models/dot-event-socket-url';
import { ConfigParams } from '../dotcms-config.service';
import { CoreWebService } from '../core-web.service';
import { Server } from 'mock-socket';
import { RequestMethod } from '@angular/http';
import { DotEventMessage } from './models/dot-event-message';
class CoreWebServiceMock  extends CoreWebService {

    constructor() {
        super(null, null, null, null, null);
    }

    public requestView(): Observable<any> {
        return null;
    }
}

describe('DotEventsSocket', () => {
    let coreWebServiceMock;
    let dotEventsSocket: DotEventsSocket;
    const url = new DotEventsSocketURL('ws', 'localhost', '/testing');

    beforeEach(() => {
        coreWebServiceMock = new CoreWebServiceMock();

        const configParamsMock: ConfigParams = {
            colors: {},
            disabledWebsockets: '',
            emailRegex: '',
            license: {},
            menu: [],
            paginatorLinks: 1,
            paginatorRows: 2,
            websocketBaseURL: '',
            websocketEndpoints: '',
            websocketProtocol: '',
            websocketReconnectTime: 0,
            websocketsSystemEventsEndpoint: '',
        };

        const loggerService = new LoggerService(new StringUtils());

        dotEventsSocket = new DotEventsSocket(url, configParamsMock, loggerService, coreWebServiceMock);
    });

    describe('WebSocket', () => {
        let mockServer: Server;

        beforeEach(() => {
            mockServer = new Server(url.url);
        });

        it('should connect', (done) => {
            mockServer.on('connection', () => {
                done();
            });

            dotEventsSocket.connect();
        });

        it('should catch a message', (done) => {
            const expectedMessage: DotEventMessage = {
                event: 'event',
                payload: 'message'
            };

            mockServer.on('connection', socket => {
                socket.send(
                    JSON.stringify(expectedMessage)
                );
                done();
            });

            dotEventsSocket.messages().subscribe((message) => {
                expect(message).toEqual(expectedMessage);
            });
            dotEventsSocket.connect();
        });

        afterEach(() => {
            mockServer.close();
        });
    });

    describe('LongPolling', () => {

        const requestOpts = {
            method: RequestMethod.Get,
            url: 'http://localhost/testing',
            params: {}
        };

        it('should connect', (done) => {
            spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
                dotEventsSocket.destroy();

                return of({
                    entity: {
                        message: 'message'
                    }
                });
            });

            dotEventsSocket.connect();

            dotEventsSocket.open().subscribe(() => {
                expect(coreWebServiceMock.requestView).toHaveBeenCalledWith(requestOpts);
                done();
            });
        });

        it('should catch a message', (done) => {
            spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
                return of({
                    entity: {
                        event: 'event',
                        payload: 'message'
                    }
                });
            });

            dotEventsSocket.connect();

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });
                done();
            });
        });

        it('should catch a message', (done) => {
            let firstTime = true;
            let countMessage = 0;

            spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
                if (firstTime) {
                    firstTime = false;
                    return throwError('ERROR');
                } else {
                    return of({
                        entity: {
                            event: 'event',
                            payload: 'message'
                        }
                    });
                }
            });

            dotEventsSocket.connect();

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();

                countMessage++;
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });

                setTimeout(() => {
                    if (countMessage === 1) {
                        done();
                    } else if (countMessage === 2) {
                        expect(true).toBe(false, 'should be call just one');
                    }
                }, 10);
            });
        });
    });
});
