import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';
import { By } from '@angular/platform-browser';

import { mockDotDevices } from '@dotcms/utils-testing';

import { DotEmaDeviceDisplayComponent } from './dot-ema-device-display.component';

describe('DotEmaDeviceDisplayComponent', () => {
    let spectator: Spectator<DotEmaDeviceDisplayComponent>;

    const createComponent = createComponentFactory({
        component: DotEmaDeviceDisplayComponent,
        imports: [CommonModule]
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                currentDevice: { ...mockDotDevices[0], icon: 'someIcon' }
            }
        });
    });

    it('should show name, sizes and icon of the selected device', () => {
        expect(spectator.query(byTestId('device-name')).textContent).toBe('iphone 200 x 100');
        expect(spectator.query(byTestId('device-icon'))).not.toBeNull();
    });

    it('should show a x button to reset the device selection', () => {
        expect(spectator.query(byTestId('reset-device'))).not.toBeNull();
    });

    it("should emit deviceReset event when click on 'x' button", () => {
        const resetDevice = jest.spyOn(spectator.component.resetDevice, 'emit');

        const resetButton = spectator.debugElement.query(By.css('[data-testId="reset-device"]'));

        spectator.triggerEventHandler(resetButton, 'onClick', {});

        expect(resetDevice).toHaveBeenCalled();
    });
});
