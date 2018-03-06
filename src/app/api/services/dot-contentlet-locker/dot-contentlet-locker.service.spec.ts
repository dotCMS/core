import { ConnectionBackend, ResponseOptions, Response } from '@angular/http';
import { MockBackend } from '@angular/http/testing';

import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotContentletLockerService } from './dot-contentlet-locker.service';

describe('DotContentletLockerService', () => {
    let service: DotContentletLockerService;
    let backend: MockBackend;
    let lastConnection;
    let injector;

    beforeEach(() => {
        lastConnection = [];

        injector = DOTTestBed.configureTestingModule({
            providers: [DotContentletLockerService]
        });

        service = injector.get(DotContentletLockerService);
        backend = injector.get(ConnectionBackend) as MockBackend;
        backend.connections.subscribe((connection: any) => {
            lastConnection.push(connection);
        });
    });

    it('should lock a content asset', () => {
        let result: any;
        service.lock('123').subscribe((lockInfo: any) => (result = lockInfo));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        message: 'locked'
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/content/lock/inode/123');
        expect(result).toEqual({ message: 'locked' });
    });

    it('should unlock a content asset', () => {
        let result: any;
        service.unlock('123').subscribe((lockInfo: any) => (result = lockInfo));

        lastConnection[0].mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        message: 'locked'
                    }
                })
            )
        );
        expect(lastConnection[0].request.url).toContain('/api/content/unlock/inode/123');
        expect(result).toEqual({ message: 'locked' });
    });
});
