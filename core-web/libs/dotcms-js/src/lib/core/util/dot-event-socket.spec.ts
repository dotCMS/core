import { Server } from 'mock-socket';
import { Observable, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotEventsSocket } from './dot-event-socket';
import { DotEventMessage } from './models/dot-event-message';
import { DotEventsSocketURL } from './models/dot-event-socket-url';

import { ConfigParams, DotcmsConfigService } from '../dotcms-config.service';
import { LoggerService } from '../logger.service';
import { StringUtils } from '../string-utils.service';

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
    let dotEventsSocket: DotEventsSocket;
    let httpTesting: HttpTestingController;
    const url = new DotEventsSocketURL('localhost/testing', false);

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotcmsConfigService, useClass: DotcmsConfigMock },
                { provide: DotEventsSocketURL, useValue: url },
                StringUtils,
                LoggerService,
                DotEventsSocket
            ]
        });

        dotEventsSocket = TestBed.inject(DotEventsSocket);
        httpTesting = TestBed.inject(HttpTestingController);
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
        const longPollingUrl = 'http://localhost/testing';

        it('should connect', (done) => {
            dotEventsSocket.connect().subscribe(() => {});

            dotEventsSocket.open().subscribe(() => {
                done();
            });

            const req = httpTesting.expectOne(longPollingUrl);
            dotEventsSocket.destroy();
            req.flush({ entity: { message: 'message' } });
        });

        it('should catch a message', (done) => {
            dotEventsSocket.connect().subscribe(() => {});

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });
                done();
            });

            const req = httpTesting.expectOne(longPollingUrl);
            req.flush({ entity: { event: 'event', payload: 'message' } });
        });

        it('should fallback to long polling after websocket error and catch message', (done) => {
            dotEventsSocket.connect().subscribe(() => {});

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });
                done();
            });

            const req1 = httpTesting.expectOne(longPollingUrl);
            req1.flush(null, { status: 500, statusText: 'Server Error' });

            const req2 = httpTesting.expectOne(longPollingUrl);
            req2.flush({ entity: { event: 'event', payload: 'message' } });
        });
    });
});
