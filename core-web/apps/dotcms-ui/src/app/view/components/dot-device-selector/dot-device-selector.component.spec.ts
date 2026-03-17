/* eslint-disable @typescript-eslint/no-explicit-any */

import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DotDevicesService, DotMessageService } from '@dotcms/data-access';
import { DotDevice } from '@dotcms/dotcms-models';
import { DotIconComponent, DotMessagePipe } from '@dotcms/ui';
import {
    DotDevicesServiceMock,
    mockDotDevices,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotDeviceSelectorComponent } from './dot-device-selector.component';

describe('DotDeviceSelectorComponent', () => {
    let spectator: Spectator<DotDeviceSelectorComponent>;
    let dotDeviceService: DotDevicesService;

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

    const createComponent = createComponentFactory({
        component: DotDeviceSelectorComponent,
        imports: [NoopAnimationsModule, DotIconComponent, DotMessagePipe],
        providers: [{ provide: DotMessageService, useValue: messageServiceMock }],
        componentProviders: [{ provide: DotDevicesService, useClass: DotDevicesServiceMock }]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { value: mockDotDevices[0] } });
        dotDeviceService = spectator.debugElement.injector.get(DotDevicesService);
    });

    it('should have icon', () => {
        const icon = spectator.debugElement.query(By.css('dot-icon'));
        expect(icon?.attributes['name']).toBe('devices');
        expect(icon?.attributes['big']).toBeDefined();
    });

    it('should emit the selected Device', () => {
        const pSelect = spectator.debugElement.query(By.css('p-select'));
        jest.spyOn(spectator.component.selected, 'emit');
        jest.spyOn(spectator.component, 'change');

        pSelect.triggerEventHandler('onChange', { value: mockDotDevices });

        expect(spectator.component.change).toHaveBeenCalledWith(mockDotDevices);
        expect(spectator.component.selected.emit).toHaveBeenCalledWith(mockDotDevices);
    });

    it('should add Default Device as first position', () => {
        expect(spectator.component.options[0]).toEqual(defaultDevice);
    });

    it('should set devices that have Width & Height bigger than 0', () => {
        const devicesMock = mockDotDevices.filter(
            (device: DotDevice) => +device.cssHeight > 0 && +device.cssWidth > 0
        );
        expect(spectator.component.options.length).toEqual(2);
        expect(spectator.component.options[0]).toEqual(defaultDevice);
        expect(spectator.component.options[1]).toEqual(devicesMock[0]);
    });

    it('should reload options when value change', () => {
        jest.spyOn(dotDeviceService, 'get');
        spectator.setInput('value', { ...mockDotDevices[1] });
        spectator.detectChanges();
        expect(dotDeviceService.get).toHaveBeenCalledTimes(1);
    });

    describe('disabled', () => {
        let disabledSpectator: Spectator<DotDeviceSelectorComponent>;

        beforeEach(fakeAsync(() => {
            disabledSpectator = createComponent({ props: { value: mockDotDevices[0] } });
            const service = disabledSpectator.debugElement.injector.get(DotDevicesService);
            jest.spyOn(service, 'get').mockReturnValue(of([]));
            disabledSpectator.setInput('value', { ...mockDotDevices[1], inode: 'other' });
            tick();
            disabledSpectator.detectChanges();
        }));

        it('should disabled dropdown when just have just one device', () => {
            const pSelect = disabledSpectator.debugElement.query(By.css('p-select'));
            const disabled = pSelect?.componentInstance?.disabled;
            expect(typeof disabled === 'function' ? disabled() : disabled).toBe(true);
        });

        it('should add class to the host when disabled', () => {
            expect(disabledSpectator.component.disabled).toBe(true);
            expect(disabledSpectator.element.classList.contains('disabled')).toBe(true);
        });
    });
});
