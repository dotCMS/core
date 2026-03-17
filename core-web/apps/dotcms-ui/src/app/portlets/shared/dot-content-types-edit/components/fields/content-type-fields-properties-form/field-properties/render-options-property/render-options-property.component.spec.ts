import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { ReactiveFormsModule, UntypedFormBuilder } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { RenderOptionsPropertyComponent } from './render-options-property.component';

const messageServiceMock = new MockDotMessageService({
    'contenttypes.field.properties.renderOptions.title': 'Render Options',
    'contenttypes.field.properties.renderOptions.showAsModal.label': 'Show as Modal',
    'contenttypes.field.properties.renderOptions.showAsModal.helper':
        'Display this field in an overlay',
    'contenttypes.field.properties.renderOptions.width': 'WIDTH',
    'contenttypes.field.properties.renderOptions.height': 'HEIGHT'
});

describe('RenderOptionsPropertyComponent', () => {
    let spectator: Spectator<RenderOptionsPropertyComponent>;
    let fb: UntypedFormBuilder;

    const createComponent = createComponentFactory({
        component: RenderOptionsPropertyComponent,
        imports: [ReactiveFormsModule, InputTextModule, ToggleSwitchModule, DotMessagePipe],
        providers: [
            UntypedFormBuilder,
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    const buildForm = (showAsModal = false, width = '398', height = '400') => {
        return fb.group({
            showAsModal: [showAsModal],
            customFieldWidth: [width],
            customFieldHeight: [height]
        });
    };

    beforeEach(() => {
        fb = new UntypedFormBuilder();
    });

    describe('toggle switch', () => {
        it('should render the Show as Modal toggle', () => {
            spectator = createComponent({ props: { group: buildForm() }, detectChanges: true });
            const toggle = spectator.query('[data-testid="render-options-show-as-modal"]');
            expect(toggle).toBeTruthy();
        });

        it('should reflect showAsModal false by default', () => {
            spectator = createComponent({
                props: { group: buildForm(false) },
                detectChanges: true
            });
            const control = spectator.component.group().get('showAsModal');
            expect(control?.value).toBe(false);
        });

        it('should reflect showAsModal true when pre-set', () => {
            spectator = createComponent({ props: { group: buildForm(true) }, detectChanges: true });
            const control = spectator.component.group().get('showAsModal');
            expect(control?.value).toBe(true);
        });
    });

    describe('width and height inputs', () => {
        it('should NOT render width/height inputs when showAsModal is false', () => {
            spectator = createComponent({
                props: { group: buildForm(false) },
                detectChanges: true
            });
            expect(spectator.query('[data-testid="render-options-width"]')).toBeNull();
            expect(spectator.query('[data-testid="render-options-height"]')).toBeNull();
        });

        it('should render width and height inputs when showAsModal is true', () => {
            spectator = createComponent({ props: { group: buildForm(true) }, detectChanges: true });
            expect(spectator.query('[data-testid="render-options-width"]')).toBeTruthy();
            expect(spectator.query('[data-testid="render-options-height"]')).toBeTruthy();
        });

        it('should show width/height inputs after toggling showAsModal to true', () => {
            const form = buildForm(false);
            spectator = createComponent({ props: { group: form }, detectChanges: true });

            expect(spectator.query('[data-testid="render-options-width"]')).toBeNull();

            form.get('showAsModal').setValue(true);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="render-options-width"]')).toBeTruthy();
            expect(spectator.query('[data-testid="render-options-height"]')).toBeTruthy();
        });

        it('should hide width/height inputs after toggling showAsModal to false', () => {
            const form = buildForm(true);
            spectator = createComponent({ props: { group: form }, detectChanges: true });

            expect(spectator.query('[data-testid="render-options-width"]')).toBeTruthy();

            form.get('showAsModal').setValue(false);
            spectator.detectChanges();

            expect(spectator.query('[data-testid="render-options-width"]')).toBeNull();
            expect(spectator.query('[data-testid="render-options-height"]')).toBeNull();
        });

        it('should render width and height inputs as type number', () => {
            spectator = createComponent({ props: { group: buildForm(true) }, detectChanges: true });
            const widthInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-width"]'
            );
            const heightInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-height"]'
            );
            expect(widthInput?.type).toBe('number');
            expect(heightInput?.type).toBe('number');
        });

        it('should have min="1" on width and height inputs', () => {
            spectator = createComponent({ props: { group: buildForm(true) }, detectChanges: true });
            const widthInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-width"]'
            );
            const heightInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-height"]'
            );
            expect(widthInput?.min).toBe('1');
            expect(heightInput?.min).toBe('1');
        });

        it('should bind width input to customFieldWidth form control', () => {
            const form = buildForm(true, '500', '400');
            spectator = createComponent({ props: { group: form }, detectChanges: true });
            const widthInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-width"]'
            );
            expect(widthInput?.value).toBe('500');
        });

        it('should bind height input to customFieldHeight form control', () => {
            const form = buildForm(true, '398', '600');
            spectator = createComponent({ props: { group: form }, detectChanges: true });
            const heightInput = spectator.query<HTMLInputElement>(
                '[data-testid="render-options-height"]'
            );
            expect(heightInput?.value).toBe('600');
        });
    });
});
