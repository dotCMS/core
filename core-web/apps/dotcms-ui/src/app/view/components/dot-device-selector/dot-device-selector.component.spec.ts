/* eslint-disable @typescript-eslint/no-explicit-any */

/* eslint-disable @typescript-eslint/no-explicit-any */

import { of } from 'rxjs';

import { Component, CUSTOM_ELEMENTS_SCHEMA, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import {
    DotDevicesServiceMock,
    mockDotDevices,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';

@Component({
    selector: 'dot-test-host-component',
    template: ` <dot-device-selector [value]="value"></dot-device-selector> `
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
    let de: DebugElement;

    const defaultDevice: DotDevice = {
        identifier: '',
        name: 'Desktop',
        cssHeight: '',
        cssWidth: '',
        inode: '0'
    };
    const messageServiceMock = new MockDotMessageService({
        'editpage.viewas.default.device': 'Desktop',
        'editpage.viewas.label.device': 'Device'
    });

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [TestHostComponent, DotDeviceSelectorComponent],
            imports: [BrowserAnimationsModule, DotIconModule, DotMessagePipe],
            providers: [
                {
                    provide: DotDevicesService,
                    useClass: DotDevicesServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA]
        }).compileComponents();
    });
    beforeEach(() => {
        fixtureHost = TestBed.createComponent(TestHostComponent);
        deHost = fixtureHost.debugElement;
        componentHost = fixtureHost.componentInstance;
        de = deHost.query(By.css('dot-device-selector'));
        component = de.componentInstance;
        dotDeviceService = TestBed.inject(DotDevicesService);
    });

    it('should have icon', () => {
        fixtureHost.detectChanges();
        const icon = de.query(By.css('dot-icon'));
        expect(icon.attributes.name).toBe('devices');
        expect(icon.attributes.big).toBeDefined();
    });

    it('should emmit the selected Device', () => {
        const pDropDown: DebugElement = de.query(By.css('p-dropdown'));

        spyOn(component.selected, 'emit');
        spyOn(component, 'change').and.callThrough();

        pDropDown.triggerEventHandler('onChange', { value: mockDotDevices });

        expect<any>(component.change).toHaveBeenCalledWith(mockDotDevices);
        expect<any>(component.selected.emit).toHaveBeenCalledWith(mockDotDevices);
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

    it('should reload options when value change', () => {
        spyOn(dotDeviceService, 'get').and.callThrough();

        componentHost.value = {
            ...mockDotDevices[1]
        };
        fixtureHost.detectChanges();
        expect(dotDeviceService.get).toHaveBeenCalledTimes(1);
    });

    describe('disabled', () => {
        beforeEach(() => {
            spyOn(dotDeviceService, 'get').and.returnValue(of([]));
            fixtureHost.detectChanges();
        });
        it('should disabled dropdown when just have just one device', () => {
            const pDropDown: DebugElement = de.query(By.css('p-dropdown'));
            expect(pDropDown.componentInstance.disabled).toBe(true);
        });

        it('should add class to the host when disabled', () => {
            expect(de.nativeElement.classList.contains('disabled')).toBe(true);
        });
    });
});
