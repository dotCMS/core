/* eslint-disable @typescript-eslint/no-explicit-any */

import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { CoreWebServiceMock } from '@dotcms/utils-testing';

import { DotContentletLockerService } from './dot-contentlet-locker.service';

describe('DotContentletLockerService', () => {
    let dotContentletLockerService: DotContentletLockerService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotContentletLockerService
            ]
        });
        dotContentletLockerService = TestBed.inject(DotContentletLockerService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should lock a content asset', () => {
        const inode = '123';
        dotContentletLockerService.lock(inode).subscribe((lockInfo: any) => {
            expect(lockInfo).toEqual({ message: 'locked' });
        });

        const req = httpMock.expectOne(`/api/content/lock/inode/${inode}`);
        expect(req.request.method).toBe('PUT');
        req.flush({
            message: 'locked'
        });
    });

    it('should unlock a content asset', () => {
        const inode = '123';
        dotContentletLockerService.unlock(inode).subscribe((lockInfo: any) => {
            expect(lockInfo).toEqual({ message: 'locked' });
        });

        const req = httpMock.expectOne(`/api/content/unlock/inode/${inode}`);
        expect(req.request.method).toBe('PUT');
        req.flush({
            message: 'locked'
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
