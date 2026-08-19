import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { NO_ERRORS_SCHEMA } from '@angular/core';

import { DotAiService, DotMessageService } from '@dotcms/data-access';
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
            mockProvider(DotAiService),
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
});
