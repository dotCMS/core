/* eslint-disable @typescript-eslint/no-explicit-any */

import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotContentletLockerService } from './dot-contentlet-locker.service';

describe('DotContentletLockerService', () => {
    let injector: TestBed;
    let dotContentletLockerService: DotContentletLockerService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotContentletLockerService
            ]
        });
        injector = getTestBed();
        dotContentletLockerService = injector.get(DotContentletLockerService);
        httpTestingController = injector.get(HttpTestingController);
    });

    it('should lock a content asset', () => {
        const inode = '123';
        const mockLock = {
            entity: {
                message: 'locked'
            }
        };
        dotContentletLockerService.lock(inode).subscribe((lockInfo: any) => {
            expect(lockInfo).toEqual({ message: 'locked' });
        });

        const req = httpTestingController.expectOne(`/api/content/lock/inode/${inode}`);
        expect(req.request.method).toBe('PUT');
        req.flush(mockLock);
    });

    it('should unlock a content asset', () => {
        const inode = '123';
        const mockLock = {
            entity: {
                message: 'locked'
            }
        };
        dotContentletLockerService.unlock(inode).subscribe((lockInfo: any) => {
            expect(lockInfo).toEqual({ message: 'locked' });
        });

        const req = httpTestingController.expectOne(`/api/content/unlock/inode/${inode}`);
        expect(req.request.method).toBe('PUT');
        req.flush(mockLock);
    });

    afterEach(() => {
        httpTestingController.verify();
    });
});
