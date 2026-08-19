import { DotAiProviderFieldType } from '@dotcms/dotcms-models';

import { isFieldAlwaysVisible } from './dot-ai-config.constants';

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
