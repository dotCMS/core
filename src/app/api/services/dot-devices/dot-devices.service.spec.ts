import { DotDevicesService } from './dot-devices.service';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { Response, ConnectionBackend, ResponseOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { mockDotDevices } from '../../../test/dot-device.mock';

describe('DotDevicesService', () => {
    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([DotDevicesService]);
        this.dotDevicesService = this.injector.get(DotDevicesService);
        this.backend = this.injector.get(ConnectionBackend) as MockBackend;
        this.backend.connections.subscribe((connection: any) => {
            this.lastConnection = connection;
        });
    });

    it('should get Devices', () => {
        const url = [
            `content/respectFrontendRoles/false/render/false/query/+contentType:previewDevice `,
            `+live:true `,
            `+deleted:false `,
            `+working:true`
        ].join('');

        this.dotDevicesService.get().subscribe((devices: DotDevice[]) => {
            expect(devices).toEqual(mockDotDevices);
        });

        this.lastConnection.mockRespond(
            new Response(
                new ResponseOptions({
                    body: {
                        contentlets: mockDotDevices
                    }
                })
            )
        );

        expect(this.lastConnection.request.method).toBe(0); // 0 is GET method
        expect(this.lastConnection.request.url).toContain(url);
    });
});
