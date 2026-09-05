import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

import { withDotAiPreferences } from './with-dot-ai-preferences.feature';
import { withRetrievalSettings } from './with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const KEY = 'dotcms.devtools.dotai.settings';

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withRetrievalSettings(),
    withDotAiPreferences()
);

describe('withDotAiPreferences', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;

    const createService = createServiceFactory({ service: TestStore });

    beforeEach(() => localStorage.clear());
    afterEach(() => localStorage.clear());

    it('should restore stored settings on init (FR-018)', () => {
        localStorage.setItem(
            KEY,
            JSON.stringify({ settingsThreshold: 0.9, settingsContentTypes: 'Blog' })
        );

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(spectator.service.settingsContentTypes()).toBe('Blog');
    });

    it('should merge over defaults rather than replacing them', () => {
        // Only one field stored; everything else must keep its default.
        localStorage.setItem(KEY, JSON.stringify({ settingsThreshold: 0.9 }));

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(spectator.service.settingsOperator()).toBe(DOT_AI_VECTOR_OPERATOR.COSINE);
        expect(spectator.service.settingsIndexName()).toBe('default');
    });

    it('should ignore keys it does not recognise, so a stale blob cannot pin anything', () => {
        localStorage.setItem(
            KEY,
            JSON.stringify({ settingsThreshold: 0.9, someRemovedControl: 'boom' })
        );

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(
            (spectator.service as unknown as Record<string, unknown>)['someRemovedControl']
        ).toBeUndefined();
    });

    it('should keep a stored null site, since null means all sites', () => {
        localStorage.setItem(KEY, JSON.stringify({ settingsSite: null }));

        spectator = createService();

        expect(spectator.service.settingsSite()).toBeNull();
    });

    it('should start from defaults when nothing is stored', () => {
        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.25);
    });

    it('should not throw on a malformed blob', () => {
        localStorage.setItem(KEY, '{not json');

        expect(() => createService()).not.toThrow();
    });

    it('should persist a changed setting', () => {
        spectator = createService();

        spectator.service.setSettings({ settingsThreshold: 0.75 });
        spectator.flushEffects();

        expect(JSON.parse(localStorage.getItem(KEY) ?? '{}').settingsThreshold).toBe(0.75);
    });
});
