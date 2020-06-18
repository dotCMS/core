import { ComponentFixture } from '@angular/core/testing';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';
import { DebugElement, Component } from '@angular/core';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { DotDevicesService } from '@services/dot-devices/dot-devices.service';
import { DotDevicesServiceMock } from '../../../test/dot-device-service.mock';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { By } from '@angular/platform-browser';
import { mockDotDevices } from '../../../test/dot-device.mock';
import { DotDevice } from '@models/dot-device/dot-device.model';
import { Dropdown } from 'primeng/primeng';
import { of } from 'rxjs/internal/observable/of';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';

@Component({
    selector: 'dot-test-host-component',
    template: `
        <dot-device-selector [value]="value"></dot-device-selector>
    `
})
class TestHostComponent {
    value: DotDevice = mockDotDevices[0];
}

describe('DotDeviceSelectorComponent', () => {
    let fixtureHost: ComponentFixture<TestHostComponent>;
    let componentHost: TestHostComponent;
    let deHost: DebugElement;
    let dotDeviceService;
    let component: DotDeviceSelectorComponent;
    // let fixture: ComponentFixture<DotDeviceSelectorComponent>;
    let de: DebugElement;

    const defaultDevice: DotDevice = {
        name: 'Desktop',
        cssHeight: '',
        cssWidth: '',
        inode: '0'
    };
    const messageServiceMock = new MockDotMessageService({
        'editpage.viewas.default.device': 'Desktop',
        'editpage.viewas.label.device': 'Device'
    });

    beforeEach(() => {
        const testbed = DOTTestBed.configureTestingModule({
            declarations: [TestHostComponent, DotDeviceSelectorComponent],
            imports: [BrowserAnimationsModule, DotIconModule],
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

        fixtureHost = DOTTestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-device-selector'));
        component = de.componentInstance;

        dotDeviceService = testbed.get(DotDevicesService);
    });

    it('should have icon', () => {
        fixtureHost.detectChanges();
        const icon = de.query(By.css('dot-icon'));
        expect(icon.attributes.name).toBe('devices');
        expect(icon.attributes.big).toBeDefined();
    });

    it('should have label', () => {
        fixtureHost.detectChanges();
        const label = de.query(By.css('label')).nativeElement;
        expect(label.textContent).toBe('Device');
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
        fixtureHost.detectChanges();
        expect(component.options[0]).toEqual(defaultDevice);
    });

    it('should set devices that have Width & Height bigger than 0', () => {
        fixtureHost.detectChanges();
        const devicesMock = mockDotDevices.filter(
            (device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0
        );
        expect(component.options.length).toEqual(2);
        expect(component.options[0]).toEqual(defaultDevice);
        expect(component.options[1]).toEqual(devicesMock[0]);
    });

    it('should set fixed width to dropdown', () => {
        fixtureHost.detectChanges();
        const pDropDown: Dropdown = de.query(By.css('p-dropdown')).componentInstance;
        expect(pDropDown.style).toEqual({ width: '120px' });
    });

    it('should reload options when value change', () => {
        spyOn(dotDeviceService, 'get').and.callThrough();

        fixtureHost.detectChanges();
        componentHost.value = {
            ...mockDotDevices[1]
        };
        fixtureHost.detectChanges();

        expect(dotDeviceService.get).toHaveBeenCalledTimes(2);
    });

    describe('disabled', () => {
        it('should disabled dropdown when just have just one device', () => {
            spyOn(dotDeviceService, 'get').and.returnValue(of([]));
            fixtureHost.detectChanges();

            const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
            expect(pDropDown.componentInstance.disabled).toBe(true);
        });

        it('should add class to the host when disabled', () => {
            spyOn(dotDeviceService, 'get').and.returnValue(of([]));
            fixtureHost.detectChanges();
            expect(de.nativeElement.classList.contains('disabled')).toBe(true);
        });
    });
});
