import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import {
    DotContentletLockerService,
    DotContentletLockResponse
} from './dot-contentlet-locker.service';

describe('DotContentletLockerService', () => {
    let service: DotContentletLockerService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotContentletLockerService]
        });
        service = TestBed.inject(DotContentletLockerService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should lock a contentlet', () => {
        const mockResponse: DotContentletLockResponse = {
            id: 'test-id',
            inode: 'test-inode',
            message: 'Content locked'
        };

        service.lock('test-inode').subscribe((response: DotContentletLockResponse) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/content/lock/inode/test-inode');
        expect(req.request.method).toBe('PUT');
        req.flush({ bodyJsonObject: mockResponse });
    });

    it('should unlock a contentlet', () => {
        const mockResponse: DotContentletLockResponse = {
            id: 'test-id',
            inode: 'test-inode',
            message: 'Content unlocked'
        };

        service.unlock('test-inode').subscribe((response: DotContentletLockResponse) => {
            expect(response).toEqual(mockResponse);
        });

        const req = httpMock.expectOne('/api/content/unlock/inode/test-inode');
        expect(req.request.method).toBe('PUT');
        req.flush({ bodyJsonObject: mockResponse });
    });
});
