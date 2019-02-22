import { Observable, of } from 'rxjs';
import { ResponseView } from './response-view';
import { StringUtils } from '../string-utils.service';
import { LoggerService } from '../logger.service';
import { DotEventsSocket } from './dot-event-socket';
import { Url } from './models/url';
import { ConfigParams } from '../dotcms-config.service';
import { CoreWebService } from '../core-web.service';
import { Server } from 'mock-socket';
import { RequestMethod } from '@angular/http';
import { DotEventMessage } from './models/dot-event-message';

class CoreWebServiceMock extends CoreWebService {

    constructor() {
        super(null, null, null, null, null);
    }

    public requestView(): Observable<ResponseView> {
        return null;
    }
}

describe('DotEventsSocket', () => {
    let coreWebServiceMock: CoreWebServiceMock;
    let dotEventsSocket: DotEventsSocket;
    const url = new Url('ws', 'testing', 'localhost');

    beforeEach(() => {
        coreWebServiceMock = new CoreWebServiceMock();

        const loggerService = new LoggerService(new StringUtils());
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
            websocketReconnectTime: 3,
            websocketsSystemEventsEndpoint: '',
        };

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

    xdescribe('LongPolling', () => {
        it('should connect', () => {
            const requestOpts = {
                method: RequestMethod.Get,
                url: url,
                params: {}
            };

            spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
                // dotEventsSocket.destroy();
                return of({
                    entity: {
                        message: 'message'
                    }
                });
            });

            dotEventsSocket.connect();

            expect(coreWebServiceMock.requestView).toHaveBeenCalledWith(requestOpts);
        });
    });
});
