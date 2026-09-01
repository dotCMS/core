import { FormControl, FormGroup } from '@angular/forms';

import { DotAiProviderFieldType } from '@dotcms/dotcms-models';

import { isFieldAlwaysVisible, requiredUnlessValidator } from './dot-ai-config.constants';

describe('isFieldAlwaysVisible', () => {
    it('returns true for a required field regardless of type', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'temperature',
                type: DotAiProviderFieldType.NUMBER,
                required: true,
                hint: ''
            })
        ).toBe(true);
    });

    it('returns true for an optional SECRET field (e.g. Bedrock accessKeyId, Vertex credentialsJson)', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'credentialsJson',
                type: DotAiProviderFieldType.SECRET,
                required: false,
                hint: 'GCP service account JSON key; omit to use Application Default Credentials'
            })
        ).toBe(true);
    });

    it('returns true for an optional field named "model" (e.g. Azure model/deploymentName pair)', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'model',
                type: DotAiProviderFieldType.STRING,
                required: false,
                hint: 'Required if deploymentName is not set'
            })
        ).toBe(true);
    });

    it('returns true for an optional field named "deploymentName"', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'deploymentName',
                type: DotAiProviderFieldType.STRING,
                required: false,
                hint: 'Required if model is not set'
            })
        ).toBe(true);
    });

    it('returns false for a true tuning field: optional, not a secret, not an identity name', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'temperature',
                type: DotAiProviderFieldType.NUMBER,
                required: false,
                hint: ''
            })
        ).toBe(false);
    });

    it('returns false for optional endpoint/timeout fields', () => {
        expect(
            isFieldAlwaysVisible({
                name: 'endpoint',
                type: DotAiProviderFieldType.STRING,
                required: false,
                hint: ''
            })
        ).toBe(false);

        expect(
            isFieldAlwaysVisible({
                name: 'timeout',
                type: DotAiProviderFieldType.NUMBER,
                required: false,
                hint: ''
            })
        ).toBe(false);
    });
});

describe('requiredUnlessValidator', () => {
    // A control computes its initial status in its own constructor, before Angular assigns its
    // `parent` — so the validator's sibling lookup sees no parent yet on the very first pass.
    // Real usage (`dot-ai-capability-card.component.ts`) re-triggers validation once every
    // control is wired into the group; this helper mirrors that so the test reflects how the
    // validator is actually used, not a construction-order artifact.
    function groupWith(modelValue: string | null, deploymentNameValue: string | null): FormGroup {
        const group = new FormGroup({
            model: new FormControl(modelValue, requiredUnlessValidator('deploymentName')),
            deploymentName: new FormControl(deploymentNameValue, requiredUnlessValidator('model'))
        });
        group.get('model')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });
        group.get('deploymentName')?.updateValueAndValidity({ onlySelf: true, emitEvent: false });

        return group;
    }

    it('is invalid when both the field and its sibling are empty', () => {
        const group = groupWith(null, '');

        expect(group.get('model')?.errors).toEqual({
            requiredUnless: { requires: 'deploymentName' }
        });
        expect(group.get('deploymentName')?.errors).toEqual({
            requiredUnless: { requires: 'model' }
        });
    });

    it('is valid when only the field itself has a value', () => {
        const group = groupWith('gpt-4o', null);

        expect(group.get('model')?.valid).toBe(true);
    });

    it('is valid when only the sibling has a value', () => {
        const group = groupWith(null, 'my-deployment');

        expect(group.get('model')?.valid).toBe(true);
    });

    it('is valid when both the field and its sibling have a value', () => {
        const group = groupWith('gpt-4o', 'my-deployment');

        expect(group.get('model')?.valid).toBe(true);
        expect(group.get('deploymentName')?.valid).toBe(true);
    });

    it('treats a control with no parent as invalid when its own value is empty', () => {
        const control = new FormControl(null, requiredUnlessValidator('deploymentName'));

        expect(control.errors).toEqual({ requiredUnless: { requires: 'deploymentName' } });
    });
});
