import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DotDevice } from '@dotcms/dotcms-models';

import { DotDevicesService } from './dot-devices.service';

describe('DotDevicesService', () => {
    let service: DotDevicesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting(), DotDevicesService]
        });
        service = TestBed.inject(DotDevicesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    it('should get devices', () => {
        const mockDevices: DotDevice[] = [
            {
                cssHeight: '100',
                cssWidth: '100',
                inode: '123',
                identifier: '123',
                name: 'Test Device',
                stInode: 'abc'
            }
        ];

        service.get().subscribe((devices: DotDevice[]) => {
            expect(devices).toEqual(mockDevices);
        });

        const expectedUrl =
            '/api/content/respectFrontendRoles/false/render/false/query/+contentType:previewDevice +live:true +deleted:false +working:true/limit/40/orderby/title';
        const req = httpMock.expectOne(expectedUrl);
        expect(req.request.method).toBe('GET');
        req.flush({ contentlets: mockDevices });
    });
});
