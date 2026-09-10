import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

import { withRetrievalSettings } from './with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * The single most important store spec in this feature.
 *
 * `retrievalPayload` is the ONE place a CompletionsForm body is assembled, and both Search and
 * Chat spread it. Everything the backend is fussy about — and the two live defects the legacy
 * screen shipped — is asserted here.
 */
const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withRetrievalSettings()
);

describe('withRetrievalSettings', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({ service: TestStore });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should map the panel controls onto the payload', () => {
        store.setSettings({
            settingsIndexName: 'blogs',
            settingsThreshold: 0.4,
            settingsOperator: DOT_AI_VECTOR_OPERATOR.DISTANCE,
            settingsModel: 'gpt-4o',
            settingsTemperature: 1.2,
            settingsResponseLength: 512
        });

        expect(store.retrievalPayload()).toMatchObject({
            indexName: 'blogs',
            threshold: 0.4,
            operator: DOT_AI_VECTOR_OPERATOR.DISTANCE,
            model: 'gpt-4o',
            temperature: 1.2,
            responseLengthTokens: 512
        });
    });

    describe('operator (FR-024)', () => {
        it('should emit innerProduct, never the legacy `product`', () => {
            store.setSettings({ settingsOperator: DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT });

            const payload = store.retrievalPayload();

            expect(payload.operator).toBe('innerProduct');
            // `product` is not in the backend's OPERATORS map and silently becomes cosine.
            expect(payload.operator).not.toBe('product');
        });

        it('should default to cosine', () => {
            expect(store.retrievalPayload().operator).toBe(DOT_AI_VECTOR_OPERATOR.COSINE);
        });
    });

    describe('content types (FR-020)', () => {
        it('should omit the field entirely when nothing is selected', () => {
            store.setSettings({ settingsContentTypes: '' });

            expect('contentType' in store.retrievalPayload()).toBe(false);
        });

        it('should send a trimmed array when types are given', () => {
            store.setSettings({ settingsContentTypes: ' Blog , News ' });

            expect(store.retrievalPayload().contentType).toEqual(['Blog', 'News']);
        });
    });

    describe('site (FR-021)', () => {
        it('should treat a cleared site as all sites', () => {
            store.setSettings({ settingsSite: null });

            expect(store.retrievalPayload().site).toBe('');
        });
    });

    describe('temperature (FR-022)', () => {
        it('should clamp above the supported range', () => {
            store.setSettings({ settingsTemperature: 5 });
            expect(store.retrievalPayload().temperature).toBe(2);
        });

        it('should clamp below the supported range', () => {
            store.setSettings({ settingsTemperature: -1 });
            expect(store.retrievalPayload().temperature).toBe(0);
        });
    });

    describe('response length (FR-023)', () => {
        it('should raise a value below the declared 128 minimum', () => {
            // The legacy screen advertised min=10. The server declares @Min(128) but does not
            // enforce it, so a smaller value is accepted and silently truncates the answer —
            // this field is the only place the declared limit can be honored.
            store.setSettings({ settingsResponseLength: 10 });

            expect(store.retrievalPayload().responseLengthTokens).toBe(128);
        });

        it('should pass a valid value through untouched', () => {
            store.setSettings({ settingsResponseLength: 1024 });

            expect(store.retrievalPayload().responseLengthTokens).toBe(1024);
        });
    });

    it('should keep the payload stable across unrelated state changes (FR-017)', () => {
        store.setSettings({ settingsIndexName: 'blogs', settingsThreshold: 0.4 });
        const before = store.retrievalPayload();

        store.setSearchPrompt('anything');

        expect(store.retrievalPayload()).toEqual(before);
    });
});
