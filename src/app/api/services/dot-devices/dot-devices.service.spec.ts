import { DotDevicesService } from './dot-devices.service';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpTestingController, HttpClientTestingModule } from '@angular/common/http/testing';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { mockDotDevices } from '@tests/dot-device.mock';

describe('DotDevicesService', () => {
    let injector: TestBed;
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
        injector = getTestBed();
        dotDevicesService = injector.get(DotDevicesService);
        httpMock = injector.get(HttpTestingController);
    });

    it('should get Devices', () => {
        const url = [
            `content/respectFrontendRoles/false/render/false/query/+contentType:previewDevice `,
            `+live:true `,
            `+deleted:false `,
            `+working:true`
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
