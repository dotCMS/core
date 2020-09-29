import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { DotContentletLockerService } from './dot-contentlet-locker.service';

describe('DotContentletLockerService', () => {
    let injector: TestBed;
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
        injector = getTestBed();
        dotContentletLockerService = injector.get(DotContentletLockerService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should lock a content asset', () => {
        const inode = '123';
        dotContentletLockerService.lock(inode).subscribe((lockInfo: any) => {
            expect(lockInfo).toEqual({ message: 'locked' });
        });

        const req = httpMock.expectOne(`content/lock/inode/${inode}`);
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

        const req = httpMock.expectOne(`content/unlock/inode/${inode}`);
        expect(req.request.method).toBe('PUT');
        req.flush({
            message: 'locked'
        });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
