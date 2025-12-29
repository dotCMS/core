import { Server } from 'mock-socket';
import { noop, of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotEventsSocket } from './dot-event-socket';
import { DotEventMessage } from './models/dot-event-message';
import { DotEventsSocketURL } from './models/dot-event-socket-url';

import { ConfigParams, DotcmsConfigService } from '../dotcms-config.service';
import { LoggerService } from '../logger.service';

class DotcmsConfigMock {
    getConfig() {
        return of({
            colors: {
                primary: '#000',
                secondary: '#fff',
                background: '#fff'
            },
            emailRegex: '',
            license: {
                displayServerId: '',
                isCommunity: false,
                level: 0,
                levelName: ''
            },
            logos: {
                loginScreen: '',
                navBar: ''
            },
            menu: [],
            paginatorLinks: 1,
            paginatorRows: 2,
            releaseInfo: {
                buildDate: '',
                version: ''
            },
            websocket: {
                websocketReconnectTime: 0,
                disabledWebsockets: false
            },
            systemTimezone: {
                id: '',
                label: '',
                offset: ''
            }
        } as ConfigParams);
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
                LoggerService,
                DotEventsSocket
            ]
        });

        dotEventsSocket = TestBed.inject(DotEventsSocket);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
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

            dotEventsSocket.connect().subscribe(noop);
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
            dotEventsSocket.connect().subscribe(noop);
        });

        afterEach(() => {
            mockwebSocketServer.close();
        });
    });

    describe('LongPolling', () => {
        const longPollingUrl = 'http://localhost/testing';

        it('should connect', (done) => {
            dotEventsSocket.connect().subscribe(noop);

            dotEventsSocket.open().subscribe(() => {
                dotEventsSocket.destroy();
                done();
            });

            const req = httpTesting.expectOne(longPollingUrl);
            expect(req.request.method).toBe('GET');
            req.flush({ entity: { message: 'message' } });
        });

        it('should catch a message', (done) => {
            dotEventsSocket.connect().subscribe(noop);

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });
                done();
            });

            const req = httpTesting.expectOne(longPollingUrl);
            expect(req.request.method).toBe('GET');
            req.flush({
                entity: {
                    event: 'event',
                    payload: 'message'
                }
            });
        });

        it('should reconnect after error', (done) => {
            dotEventsSocket.connect().subscribe(noop);

            dotEventsSocket.messages().subscribe((message) => {
                dotEventsSocket.destroy();
                expect(message).toEqual({
                    event: 'event',
                    payload: 'message'
                });
                done();
            });

            // First request fails
            const firstReq = httpTesting.expectOne(longPollingUrl);
            firstReq.error(new ProgressEvent('error'));

            // After reconnect delay, the second request should succeed
            setTimeout(() => {
                const secondReq = httpTesting.expectOne(longPollingUrl);
                secondReq.flush({
                    entity: {
                        event: 'event',
                        payload: 'message'
                    }
                });
            }, 1500);
        });
    });
});
