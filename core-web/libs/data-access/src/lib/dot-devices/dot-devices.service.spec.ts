import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CoreWebService } from '@dotcms/dotcms-js';
import { DotDevice } from '@dotcms/dotcms-models';
import { CoreWebServiceMock, mockDotDevices } from '@dotcms/utils-testing';

import { DotDevicesService } from './dot-devices.service';

describe('DotDevicesService', () => {
    let dotDevicesService: DotDevicesService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotDevicesService
            ]
        });
        dotDevicesService = TestBed.inject(DotDevicesService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    it('should get Devices', () => {
        const url = [
            `api/`,
            `content/respectFrontendRoles/false/render/false/query/+contentType:previewDevice `,
            `+live:true `,
            `+deleted:false `,
            `+working:true`,
            `/limit/40/orderby/title`
        ].join('');

        dotDevicesService.get().subscribe((devices: DotDevice[]) => {
            expect(devices).toEqual(mockDotDevices);
        });

        const req = httpMock.expectOne(url);
        expect(req.request.method).toBe('GET');
        req.flush({ contentlets: mockDotDevices });
    });

    afterEach(() => {
        httpMock.verify();
    });
});
