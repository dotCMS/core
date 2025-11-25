import { Observable, of, throwError } from 'rxjs';

import { ReflectiveInjector } from '@angular/core';

import { LongPollingProtocol } from './long-polling-protocol';
import { ResponseView } from './response-view';

import { CoreWebService } from '../core-web.service';
import { LoggerService } from '../logger.service';
import { StringUtils } from '../string-utils.service';

class CoreWebServiceMock {
    public requestView(): Observable<ResponseView> {
        return null;
    }
}

describe('LongPollingProtocol', () => {
    let injector: ReflectiveInjector;
    let coreWebServiceMock: CoreWebServiceMock;
    let longPollingProtocol: LongPollingProtocol;
    const url = 'http://testing';

    beforeEach(() => {
        coreWebServiceMock = new CoreWebServiceMock();

        injector = ReflectiveInjector.resolveAndCreate([
            { provide: CoreWebService, useValue: coreWebServiceMock },
            StringUtils,
            LoggerService
        ]);

        const loggerService = injector.get(LoggerService);

        longPollingProtocol = new LongPollingProtocol(
            url,
            loggerService,
            injector.get(CoreWebService)
        );
    });

    it('should connect', () => {
        const requestOpts = {
            url: url,
            params: {}
        };

        spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
            longPollingProtocol.close();

            return of({
                entity: {
                    message: 'message'
                }
            });
        });

        longPollingProtocol.connect();

        expect(coreWebServiceMock.requestView).toHaveBeenCalledWith(requestOpts);
    });

    it('should trigger message', (done) => {
        const requestOpts = {
            url: url,
            params: {}
        };

        spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
            longPollingProtocol.close();

            return of({
                entity: {
                    message: 'message'
                }
            });
        });

        longPollingProtocol.message$().subscribe((message) => {
            expect(message).toEqual({
                message: 'message'
            });
            done();
        });

        longPollingProtocol.connect();

        expect(coreWebServiceMock.requestView).toHaveBeenCalledWith(requestOpts);
    });

    it('should trigger message with lastCallback', (done) => {
        let countRequest = 0;

        spyOn(coreWebServiceMock, 'requestView').and.callFake((opts) => {
            countRequest++;

            if (countRequest === 2) {
                expect(opts).toEqual({
                    url: url,
                    params: {
                        lastCallBack: 2
                    }
                });
            }

            return of({
                entity: [
                    {
                        message: 'message',
                        creationDate: 1
                    }
                ]
            });
        });

        longPollingProtocol.message$().subscribe((message) => {
            expect(message).toEqual({
                message: 'message',
                creationDate: 1
            });

            if (countRequest === 2) {
                longPollingProtocol.close();
                done();
            }
        });

        longPollingProtocol.connect();
    });

    it('should reconnect after a message', () => {
        let firstMessage = true;
        const requestOpts = {
            url: url,
            params: {}
        };

        spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
            if (!firstMessage) {
                longPollingProtocol.close();
                expect(coreWebServiceMock.requestView).toHaveBeenCalledWith(requestOpts);
                expect(coreWebServiceMock.requestView).toHaveBeenCalledTimes(2);
            } else {
                firstMessage = false;
            }

            return of({
                entity: [
                    {
                        message: 'message'
                    }
                ]
            });
        });

        longPollingProtocol.connect();
    });

    it('should trigger a error', (done) => {
        spyOn(coreWebServiceMock, 'requestView').and.callFake(() => {
            return throwError({
                entity: {}
            });
        });

        longPollingProtocol.error$().subscribe(() => {
            done();
        });
        longPollingProtocol.connect();
    });
});
