import { of, throwError } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LongPollingProtocol } from './long-polling-protocol';

import { LoggerService } from '../logger.service';
import { StringUtils } from '../string-utils.service';

describe('LongPollingProtocol', () => {
    let httpClient: HttpClient;
    let httpTesting: HttpTestingController;
    let longPollingProtocol: LongPollingProtocol;
    const url = 'http://testing';

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), StringUtils, LoggerService]
        });

        httpClient = TestBed.inject(HttpClient);
        httpTesting = TestBed.inject(HttpTestingController);

        const loggerService = TestBed.inject(LoggerService);
        longPollingProtocol = new LongPollingProtocol(url, loggerService, httpClient);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should connect', () => {
        longPollingProtocol.connect();

        const req = httpTesting.expectOne(url);
        expect(req.request.method).toBe('GET');

        longPollingProtocol.close();
        req.flush({ entity: { message: 'message' } });
    });

    it('should trigger message', (done) => {
        longPollingProtocol.message$().subscribe((message) => {
            expect(message).toEqual({ message: 'message' });
            done();
        });

        longPollingProtocol.connect();

        const req = httpTesting.expectOne(url);
        longPollingProtocol.close();
        req.flush({ entity: { message: 'message' } });
    });

    it('should trigger message with lastCallback', (done) => {
        let countRequest = 0;

        longPollingProtocol.message$().subscribe((message) => {
            expect(message).toEqual({ message: 'message', creationDate: 1 });
            countRequest++;

            if (countRequest === 2) {
                longPollingProtocol.close();
                done();
            }
        });

        longPollingProtocol.connect();

        const req1 = httpTesting.expectOne(url);
        req1.flush({ entity: [{ message: 'message', creationDate: 1 }] });

        const req2 = httpTesting.expectOne(`${url}?lastCallBack=2`);
        longPollingProtocol.close();
        req2.flush({ entity: [{ message: 'message', creationDate: 1 }] });
    });

    it('should reconnect after a message', () => {
        longPollingProtocol.connect();

        const req1 = httpTesting.expectOne(url);
        req1.flush({ entity: [{ message: 'message' }] });

        const req2 = httpTesting.expectOne(url);
        longPollingProtocol.close();
        req2.flush({ entity: [{ message: 'message' }] });
    });

    it('should trigger a error', (done) => {
        longPollingProtocol.error$().subscribe(() => {
            done();
        });

        longPollingProtocol.connect();

        const req = httpTesting.expectOne(url);
        req.flush(null, { status: 500, statusText: 'Server Error' });
    });
});
