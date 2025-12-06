import { Server } from 'mock-socket';
import { Observable, of, throwError } from 'rxjs';

import { ReflectiveInjector, Injectable } from '@angular/core';

import { DotEventsSocket } from './dot-event-socket';
import { DotEventMessage } from './models/dot-event-message';
import { DotEventsSocketURL } from './models/dot-event-socket-url';

import { CoreWebService } from '../core-web.service';
import { ConfigParams, DotcmsConfigService } from '../dotcms-config.service';
import { LoggerService } from '../logger.service';
import { StringUtils } from '../string-utils.service';

@Injectable()
class CoreWebServiceMock extends CoreWebService {
    constructor() {
        super(null, null, null, null, null);
    }

    public requestView(): Observable<any> {
        return null;
    }
}

@Injectable()
class DotcmsConfigMock {
    getConfig(): Observable<ConfigParams> {
        return of({
            colors: {},
            emailRegex: '',
            license: {},
            menu: [],
            paginatorLinks: 1,
            paginatorRows: 2,
            websocket: {
                websocketReconnectTime: 0,
                disabledWebsockets: false
            }
        });
    }
}

describe('DotEventsSocket', () => {
    const coreWebServiceMock = new CoreWebServiceMock();
    const dotcmsConfig: DotcmsConfigMock = new DotcmsConfigMock();
    let dotEventsSocket: DotEventsSocket;
    const url = new DotEventsSocketURL('localhost/testing', false);

    beforeEach(() => {
        const injector = ReflectiveInjector.resolveAndCreate([
            { provide: CoreWebService, useValue: coreWebServiceMock },
            { provide: DotcmsConfigService, useValue: dotcmsConfig },
            { provide: DotEventsSocketURL, useValue: url },
            StringUtils,
            LoggerService,
            DotEventsSocket
        ]);

        dotEventsSocket = injector.get(DotEventsSocket);
    });

    describe('WebSocket', () => {
        let mockwebSocketServer: Server;

        beforeEach(() => {
            mockwebSocketServer = new Server(url.getWebSocketURL());
        });

        it('should connect', (done) => {
            mockwebSocketServer.on('connection', () => {
                done();
            });

            dotEventsSocket.connect().subscribe(() => {});
        });

        it('should catch a message', (done) => {
            const expectedMessage: DotEventMessage = {
                event: 'event',
                payload: 'message'
            };

            mockwebSocketServer.on('connection', (socket) => {
                socket.send(JSON.stringify(expectedMessage));
                done();
            });

            dotEventsSocket.messages().subscribe((message) => {
                expect(message).toEqual(expectedMessage);
            });
            dotEventsSocket.connect().subscribe(() => {});
        });

        afterEach(() => {
            mockwebSocketServer.close();
        });
    });

    describe('LongPolling', () => {
        const requestOpts = {
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

            dotEventsSocket.connect().subscribe(() => {});

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

            dotEventsSocket.connect().subscribe(() => {});

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

                    return throwError(() => 'ERROR');
                } else {
                    return of({
                        entity: {
                            event: 'event',
                            payload: 'message'
                        }
                    });
                }
            });

            dotEventsSocket.connect().subscribe(() => {});

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
