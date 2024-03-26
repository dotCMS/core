import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { CommonModule } from '@angular/common';

import { mockDotDevices } from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

describe('DotEmaInfoDisplayComponent', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
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
        expect(spectator.query(byTestId('device-name')).textContent.trim()).toBe(
            'iphone 200 x 100'
        );
        expect(spectator.query(byTestId('device-icon'))).not.toBeNull();
    });
});
