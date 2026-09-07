import { createComponentFactory, Spectator } from '@openng/spectator/jest';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';

import { DotAiProviderField, DotAiProviderFieldType } from '@dotcms/dotcms-models';

import { DotAiDynamicFieldComponent, humanizeFieldName } from './dot-ai-dynamic-field.component';

import { MASKED_SECRET_VALUE } from '../../dot-ai-config.constants';

describe('DotAiDynamicFieldComponent', () => {
    let spectator: Spectator<DotAiDynamicFieldComponent>;

    const secretField: DotAiProviderField = {
        name: 'apiKey',
        type: DotAiProviderFieldType.SECRET,
        required: true,
        hint: ''
    };

    const createComponent = createComponentFactory({
        component: DotAiDynamicFieldComponent,
        schemas: [NO_ERRORS_SCHEMA],
        detectChanges: false
    });

    describe('isMaskedSecret', () => {
        it('is true when the control holds the saved-secret placeholder', () => {
            const formGroup = new FormGroup({ apiKey: new FormControl(MASKED_SECRET_VALUE) });
            spectator = createComponent({ props: { field: secretField, formGroup } });
            spectator.detectChanges();

            expect(spectator.component.isMaskedSecret()).toBe(true);
        });

        it('is false as soon as the placeholder is edited away, without waiting for a field/formGroup identity change', () => {
            const formGroup = new FormGroup({ apiKey: new FormControl(MASKED_SECRET_VALUE) });
            spectator = createComponent({ props: { field: secretField, formGroup } });
            spectator.detectChanges();

            formGroup.get('apiKey')?.setValue('sk-newly-typed-secret');

            expect(spectator.component.isMaskedSecret()).toBe(false);
        });

        it('is false for a freshly-created secret field with no saved value', () => {
            const formGroup = new FormGroup({ apiKey: new FormControl(null) });
            spectator = createComponent({ props: { field: secretField, formGroup } });
            spectator.detectChanges();

            expect(spectator.component.isMaskedSecret()).toBe(false);
        });

        it('is always false for a non-SECRET field, even if its value matches the placeholder text', () => {
            const textField: DotAiProviderField = {
                name: 'endpoint',
                type: DotAiProviderFieldType.STRING,
                required: false,
                hint: ''
            };
            const formGroup = new FormGroup({ endpoint: new FormControl(MASKED_SECRET_VALUE) });
            spectator = createComponent({ props: { field: textField, formGroup } });
            spectator.detectChanges();

            expect(spectator.component.isMaskedSecret()).toBe(false);
        });
    });

    describe('humanizeFieldName', () => {
        it('splits camelCase and capitalizes the first letter', () => {
            expect(humanizeFieldName('apiKey')).toBe('Api key');
            expect(humanizeFieldName('maxRetries')).toBe('Max retries');
        });
    });
});
