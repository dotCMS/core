import { ComponentFixture } from '@angular/core/testing';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';
import { DebugElement } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotDevicesServiceMock } from '../../../test/dot-device-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { mockDotDevices } from '../../../test/dot-device.mock';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { Dropdown } from 'primeng/primeng';

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

        pDropDown.triggerEventHandler('onChange', { value: mockDotDevices });

        expect(component.change).toHaveBeenCalledWith(mockDotDevices);
        expect(component.selected.emit).toHaveBeenCalledWith(mockDotDevices);
    });

    it('should add Default Device as first position', () => {
        fixture.detectChanges();
        expect(component.options[0]).toEqual(defaultDevice);
    });

    it('should set devices that have Width & Height bigger than 0', () => {
        fixture.detectChanges();
        const devicesMock = mockDotDevices.filter(
            (device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0
        );
        expect(component.options.length).toEqual(devicesMock.length + 1);
    });

    it('shoudl set fixed width to dropdown', () => {
        fixture.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.style).toEqual({ width: '100px' });
    });
});
