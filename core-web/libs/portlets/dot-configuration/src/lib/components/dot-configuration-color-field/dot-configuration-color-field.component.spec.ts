import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { ColorPicker } from 'primeng/colorpicker';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationColorFieldComponent } from './dot-configuration-color-field.component';

describe('DotConfigurationColorFieldComponent', () => {
    let spectator: Spectator<DotConfigurationColorFieldComponent>;

    const createComponent = createComponentFactory({
        component: DotConfigurationColorFieldComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.branding.primary-color': 'Primary color',
                    'configuration.validation.color': 'Enter a hex color'
                })
            }
        ],
        detectChanges: false
    });

    const render = (inputs: Record<string, unknown> = {}) => {
        spectator.setInput({
            inputId: 'primary',
            labelKey: 'configuration.branding.primary-color',
            value: '#4e65f1',
            ...inputs
        });
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createComponent();
    });

    it('labels the hex input', () => {
        render();

        expect(spectator.query('label')).toHaveText('Primary color');
        expect(spectator.query(byTestId('configuration-color-input'))).toHaveAttribute(
            'id',
            'primary'
        );
    });

    it('shows the current value in the hex input', async () => {
        render();
        await spectator.fixture.whenStable();

        expect(spectator.query(byTestId('configuration-color-input'))).toHaveValue('#4e65f1');
    });

    it('emits what the user types, trimmed', () => {
        render();
        const spy = vi.spyOn(spectator.component.valueChange, 'emit');

        spectator.typeInElement(' #111111 ', byTestId('configuration-color-input'));

        expect(spy).toHaveBeenCalledWith('#111111');
    });

    it('emits the color picked in the picker', () => {
        render();
        const spy = vi.spyOn(spectator.component.valueChange, 'emit');

        spectator.triggerEventHandler(ColorPicker, 'onChange', {
            originalEvent: new Event('change'),
            value: '#222222'
        });

        expect(spy).toHaveBeenCalledWith('#222222');
    });

    it('moves the picker to a complete six-digit code', async () => {
        render({ value: '#ff0000' });
        await spectator.fixture.whenStable();

        expect(spectator.query(ColorPicker)?.value).toEqual({ h: 0, s: 100, b: 100 });
    });

    it('leaves the picker alone while the typed code is incomplete', async () => {
        render({ value: '#00ff00' });
        await spectator.fixture.whenStable();
        spectator.setInput('value', '#12');
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        expect(spectator.query(ColorPicker)?.value).toEqual({ h: 120, s: 100, b: 100 });
    });

    it('flags an invalid value and links the message to the input', () => {
        render({ value: '#12zz', errorKey: 'configuration.validation.color' });
        const input = spectator.query(byTestId('configuration-color-input'));

        expect(spectator.query(byTestId('configuration-color-error'))).toHaveText(
            'Enter a hex color'
        );
        expect(input).toHaveAttribute('aria-invalid', 'true');
        expect(input).toHaveAttribute('aria-describedby', 'primary-error');
    });

    it('shows no message for a valid value', () => {
        render();

        expect(spectator.query(byTestId('configuration-color-error'))).not.toExist();
    });
});
