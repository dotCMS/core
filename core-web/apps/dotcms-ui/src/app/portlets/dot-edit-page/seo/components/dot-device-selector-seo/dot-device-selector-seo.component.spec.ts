import { Observable, of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { CoreWebService, CoreWebServiceMock } from '@dotcms/dotcms-js';
import { DotDevice } from '@dotcms/dotcms-models';

import { DotDeviceSelectorSeoComponent } from './dot-device-selector-seo.component';

@Injectable()
class mockDotDevicesService {
    get(): Observable<DotDevice[]> {
        return of([
            {
                name: 'Mobile Portrait',
                icon: 'pi pi-mobile',
                cssHeight: '390',
                cssWidth: '844',
                inode: '0',
                identifier: ''
            }
        ]);
    }
}

describe('DotDeviceSelectorSeoComponent', () => {
    let component: DotDeviceSelectorSeoComponent;
    let fixture: ComponentFixture<DotDeviceSelectorSeoComponent>;
    let mockDotMessageService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [DotDeviceSelectorSeoComponent, HttpClientTestingModule],
            providers: [
                { provide: DotDevicesService, useValue: mockDotDevicesService },
                { provide: DotMessageService, useValue: mockDotMessageService },
                {
                    provide: CoreWebService,
                    useClass: CoreWebServiceMock
                }
            ]
        });
        fixture = TestBed.createComponent(DotDeviceSelectorSeoComponent);
        component = fixture.componentInstance;
    });

    it('should emit selected device on change', (done) => {
        const device = {
            name: 'Mobile Portrait',
            icon: 'pi pi-mobile',
            cssHeight: '390',
            cssWidth: '844',
            inode: '0',
            identifier: ''
        };

        component.selected.subscribe((selectedDevice) => {
            expect(selectedDevice).toBe(device);
            done();
        });

        component.change(device);
    });
});
