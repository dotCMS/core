import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';

import { DotAiConfigService, DotMessageService } from '@dotcms/data-access';
import {
    DotAiCapability,
    DotAiProviderFieldType,
    DotAiProviderMetadata
} from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAiCapabilityCardComponent } from './dot-ai-capability-card.component';

import { DotAiCapabilityMeta } from '../../dot-ai-config.constants';

describe('DotAiCapabilityCardComponent', () => {
    let spectator: Spectator<DotAiCapabilityCardComponent>;

    const chatMeta: DotAiCapabilityMeta = {
        capability: DotAiCapability.CHAT,
        sectionKey: 'chat',
        title: 'apps.ai.capability.chat.title',
        description: 'apps.ai.capability.chat.description',
        icon: 'pi pi-comments'
    };

    const openAiProvider: DotAiProviderMetadata = {
        provider: 'openai',
        supportedCapabilities: [DotAiCapability.CHAT],
        fields: {
            [DotAiCapability.CHAT]: [
                { name: 'apiKey', type: DotAiProviderFieldType.SECRET, required: true, hint: '' },
                { name: 'model', type: DotAiProviderFieldType.STRING, required: true, hint: '' }
            ]
        }
    };

    const googleAiProvider: DotAiProviderMetadata = {
        provider: 'google_ai',
        supportedCapabilities: [DotAiCapability.CHAT],
        fields: {
            [DotAiCapability.CHAT]: [
                { name: 'apiKey', type: DotAiProviderFieldType.SECRET, required: true, hint: '' },
                { name: 'model', type: DotAiProviderFieldType.STRING, required: true, hint: '' }
            ]
        }
    };

    const createComponent = createComponentFactory({
        component: DotAiCapabilityCardComponent,
        providers: [
            mockProvider(DotAiConfigService),
            { provide: DotMessageService, useValue: new MockDotMessageService({}) }
        ],
        schemas: [NO_ERRORS_SCHEMA],
        detectChanges: false
    });

    beforeEach(() => {
        spectator = createComponent({
            props: {
                meta: chatMeta,
                providers: [openAiProvider, googleAiProvider]
            }
        });
    });

    describe('selectProvider', () => {
        it('does not carry over apiKey/model when switching to a different provider', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);

            spectator.component.fieldsGroup().patchValue({
                apiKey: 'sk-openai-secret',
                model: 'gpt-4o'
            });

            spectator.component.selectProvider(googleAiProvider);

            const values = spectator.component.fieldsGroup().value;
            expect(values['apiKey']).toBeNull();
            expect(values['model']).toBeNull();
        });

        it('rebuilds the fields group for the newly selected provider', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);
            spectator.component.selectProvider(googleAiProvider);

            expect(spectator.component.providerId()).toBe('google_ai');
        });

        it('does nothing when re-selecting the already-active provider', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);
            spectator.component.fieldsGroup().patchValue({ apiKey: 'sk-openai-secret' });

            spectator.component.selectProvider(openAiProvider);

            expect(spectator.component.fieldsGroup().value['apiKey']).toBe('sk-openai-secret');
        });

        it('clears additional properties carried over from the previous provider', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);
            spectator.component.additionalProperties.push(
                new FormGroup({
                    key: new FormControl('customFlag', { nonNullable: true }),
                    value: new FormControl('true', { nonNullable: true })
                })
            );

            spectator.component.selectProvider(googleAiProvider);

            expect(spectator.component.additionalProperties.length).toBe(0);
        });
    });

    describe('buildPayloadSection', () => {
        it('drops an additional-property row whose key collides with a real field of the current provider', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);
            spectator.component.fieldsGroup().patchValue({
                apiKey: 'sk-openai-secret',
                model: 'gpt-4o'
            });
            spectator.component.additionalProperties.push(
                new FormGroup({
                    key: new FormControl('model', { nonNullable: true }),
                    value: new FormControl('stale-model-override', { nonNullable: true })
                })
            );

            const section = spectator.component.buildPayloadSection();

            expect(section?.['model']).toBe('gpt-4o');
        });

        it('still includes an additional-property row that does not collide with a real field', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);
            spectator.component.fieldsGroup().patchValue({
                apiKey: 'sk-openai-secret',
                model: 'gpt-4o'
            });
            spectator.component.additionalProperties.push(
                new FormGroup({
                    key: new FormControl('customFlag', { nonNullable: true }),
                    value: new FormControl('true', { nonNullable: true })
                })
            );

            const section = spectator.component.buildPayloadSection();

            expect(section?.['customFlag']).toBe('true');
        });
    });

    describe('visibleFields / advancedFields', () => {
        it('shows a required field above the Advanced panel', () => {
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(openAiProvider);

            const visibleNames = spectator.component.visibleFields().map((f) => f.name);
            expect(visibleNames).toContain('apiKey');
            expect(visibleNames).toContain('model');
            expect(spectator.component.advancedFields()).toEqual([]);
        });

        it('promotes an optional SECRET field above the Advanced panel', () => {
            const providerWithOptionalSecret: DotAiProviderMetadata = {
                provider: 'vertex_ai',
                supportedCapabilities: [DotAiCapability.CHAT],
                fields: {
                    [DotAiCapability.CHAT]: [
                        {
                            name: 'model',
                            type: DotAiProviderFieldType.STRING,
                            required: true,
                            hint: ''
                        },
                        {
                            name: 'credentialsJson',
                            type: DotAiProviderFieldType.SECRET,
                            required: false,
                            hint: ''
                        },
                        {
                            name: 'temperature',
                            type: DotAiProviderFieldType.NUMBER,
                            required: false,
                            hint: ''
                        }
                    ]
                }
            };
            spectator.setInput('providers', [providerWithOptionalSecret]);
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(providerWithOptionalSecret);

            const visibleNames = spectator.component.visibleFields().map((f) => f.name);
            const advancedNames = spectator.component.advancedFields().map((f) => f.name);

            expect(visibleNames).toContain('credentialsJson');
            expect(advancedNames).toContain('temperature');
            expect(advancedNames).not.toContain('credentialsJson');
        });
    });

    describe('hydrateFields (additional properties round-trip)', () => {
        it('hydrates a non-string saved value without corrupting it via String()', () => {
            const hydrated = createComponent({
                props: {
                    meta: chatMeta,
                    providers: [openAiProvider],
                    initialValue: {
                        provider: 'openai',
                        apiKey: 'sk-openai-secret',
                        model: 'gpt-4o',
                        listenerIndexer: { enabled: true, batchSize: 10 }
                    }
                }
            });
            hydrated.detectChanges();

            const propertyGroup = hydrated.component.additionalProperties.at(0);
            expect(propertyGroup.value.key).toBe('listenerIndexer');
            expect(propertyGroup.value.value).not.toBe('[object Object]');
            expect(JSON.parse(propertyGroup.value.value)).toEqual({
                enabled: true,
                batchSize: 10
            });
        });

        it('round-trips that hydrated object back out through buildPayloadSection', () => {
            const hydrated = createComponent({
                props: {
                    meta: chatMeta,
                    providers: [openAiProvider],
                    initialValue: {
                        provider: 'openai',
                        apiKey: 'sk-openai-secret',
                        model: 'gpt-4o',
                        listenerIndexer: { enabled: true, batchSize: 10 }
                    }
                }
            });
            hydrated.detectChanges();

            const section = hydrated.component.buildPayloadSection();

            expect(section?.['listenerIndexer']).toEqual({ enabled: true, batchSize: 10 });
        });
    });

    describe('requiredUnless cross-field validation (Azure model/deploymentName pattern)', () => {
        const azureLikeProvider: DotAiProviderMetadata = {
            provider: 'azure_openai',
            supportedCapabilities: [DotAiCapability.CHAT],
            fields: {
                [DotAiCapability.CHAT]: [
                    {
                        name: 'apiKey',
                        type: DotAiProviderFieldType.SECRET,
                        required: true,
                        hint: ''
                    },
                    {
                        name: 'model',
                        type: DotAiProviderFieldType.STRING,
                        required: false,
                        hint: 'Required if deploymentName is not set',
                        requiredUnless: 'deploymentName'
                    },
                    {
                        name: 'deploymentName',
                        type: DotAiProviderFieldType.STRING,
                        required: false,
                        hint: 'Required if model is not set',
                        requiredUnless: 'model'
                    }
                ]
            }
        };

        beforeEach(() => {
            spectator.setInput('providers', [azureLikeProvider]);
            spectator.detectChanges();
            spectator.component.onToggleEnabled(true);
            spectator.component.selectProvider(azureLikeProvider);
            spectator.component.fieldsGroup().patchValue({ apiKey: 'sk-azure-key' });
        });

        it('is invalid when both model and deploymentName are empty', () => {
            expect(spectator.component.isValid()).toBe(false);
        });

        it('becomes valid when only model is filled', () => {
            spectator.component.fieldsGroup().patchValue({ model: 'gpt-4o' });

            expect(spectator.component.isValid()).toBe(true);
        });

        it('becomes valid when only deploymentName is filled', () => {
            spectator.component.fieldsGroup().patchValue({ deploymentName: 'my-deployment' });

            expect(spectator.component.isValid()).toBe(true);
        });

        it('re-validates model when deploymentName changes after model was left empty', () => {
            const modelControl = spectator.component.fieldsGroup().get('model');
            // Touch model while both are empty — it fails, as expected.
            modelControl?.updateValueAndValidity();
            expect(modelControl?.valid).toBe(false);

            // Filling the sibling (deploymentName) must clear model's error too, even though
            // model's own value never changed.
            spectator.component.fieldsGroup().patchValue({ deploymentName: 'my-deployment' });

            expect(modelControl?.valid).toBe(true);
        });

        it('goes back to invalid if the only filled field is cleared again', () => {
            spectator.component.fieldsGroup().patchValue({ model: 'gpt-4o' });
            expect(spectator.component.isValid()).toBe(true);

            spectator.component.fieldsGroup().patchValue({ model: '' });

            expect(spectator.component.isValid()).toBe(false);
        });

        it('is valid on initial load from a saved config that only set the sibling field', () => {
            // Regression test: a control's initial status is computed in its own constructor,
            // before Angular wires its `parent` — so hydrating a saved Azure config that only
            // persisted `deploymentName` must not render `model` as falsely invalid on load.
            const hydrated = createComponent({
                props: {
                    meta: chatMeta,
                    providers: [azureLikeProvider],
                    initialValue: {
                        provider: 'azure_openai',
                        apiKey: 'sk-azure-key',
                        deploymentName: 'my-deployment'
                    }
                }
            });
            hydrated.detectChanges();

            expect(hydrated.component.isValid()).toBe(true);
            expect(hydrated.component.fieldsGroup().get('model')?.valid).toBe(true);
        });
    });
});
