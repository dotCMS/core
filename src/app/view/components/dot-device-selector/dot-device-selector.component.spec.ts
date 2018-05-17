import { ComponentFixture } from '@angular/core/testing';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotDevicesService } from '../../../api/services/dot-devices/dot-devices.service';
import { DotDevicesServiceMock } from '../../../test/dot-device-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { mockDotDevice } from '../../../test/dot-device.mock';
import { DotDevice } from '../../../shared/models/dot-device/dot-device.model';

describe('DotDeviceSelectorComponent', () => {
    let component: DotDeviceSelectorComponent;
    let fixture: ComponentFixture<DotDeviceSelectorComponent>;
    let de: DebugElement;
    const defaultDevice: DotDevice = {
        name: 'Desktop',
        cssHeight: '',
        cssWidth: '',
        inode: '0'
    };
    const messageServiceMock = new MockDotMessageService({
        'editpage.viewas.default.device': 'Desktop'
    });

    beforeEach(() => {
        DOTTestBed.configureTestingModule({
            declarations: [DotDeviceSelectorComponent],
            imports: [BrowserAnimationsModule],
            providers: [
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        });

        fixture = DOTTestBed.createComponent(DotDeviceSelectorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
        de = fixture.debugElement;
    });

    it('should emmit the selected Device', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: mockDotDevice });

        expect(component.change).toHaveBeenCalledWith(mockDotDevice);
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotDevice);
    });

    it('should add Default Device as first position', () => {
        fixture.detectChanges();
        component.options.subscribe((devices: DotDevice[]) => {
            expect(devices[0]).toEqual(defaultDevice);
        });
    });
});
