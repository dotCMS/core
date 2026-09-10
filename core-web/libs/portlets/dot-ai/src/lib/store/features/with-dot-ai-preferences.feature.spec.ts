import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, SpectatorService } from '@openng/spectator/jest';

import { DOT_AI_VECTOR_OPERATOR } from '@dotcms/dotcms-models';

import { withDotAiPreferences } from './with-dot-ai-preferences.feature';
import { withRetrievalSettings } from './with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const KEY = 'dotcms.devtools.dotai.settings';
const VERSION = 2;

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withRetrievalSettings(),
    withDotAiPreferences()
);

describe('withDotAiPreferences', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;

    const createService = createServiceFactory({ service: TestStore });

    /**
     * Writes are debounced, and rxMethod reads its signal source inside an effect. Flushing
     * once on creation consumes the hydrated emission that `skip(1)` is there to drop —
     * without it the first real change is the one skipped, and nothing is ever written.
     */
    const startTracking = () => spectator.flushEffects();

    const settleWrite = () => {
        spectator.flushEffects();
        jest.advanceTimersByTime(400);
    };

    beforeEach(() => {
        jest.useFakeTimers();
        localStorage.clear();
    });

    afterEach(() => {
        jest.useRealTimers();
        localStorage.clear();
    });

    it('should restore stored settings on init (FR-018)', () => {
        localStorage.setItem(
            KEY,
            JSON.stringify({
                version: VERSION,
                settingsThreshold: 0.9,
                settingsContentTypes: 'Blog'
            })
        );

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(spectator.service.settingsContentTypes()).toBe('Blog');
    });

    it('should merge over defaults rather than replacing them', () => {
        // Only one field stored; everything else must keep its default.
        localStorage.setItem(KEY, JSON.stringify({ version: VERSION, settingsThreshold: 0.9 }));

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(spectator.service.settingsOperator()).toBe(DOT_AI_VECTOR_OPERATOR.COSINE);
        expect(spectator.service.settingsIndexName()).toBe('default');
    });

    it('should ignore keys it does not recognise, so a stale blob cannot pin anything', () => {
        localStorage.setItem(
            KEY,
            JSON.stringify({ version: VERSION, settingsThreshold: 0.9, someRemovedControl: 'boom' })
        );

        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.9);
        expect(
            (spectator.service as unknown as Record<string, unknown>)['someRemovedControl']
        ).toBeUndefined();
    });

    it('should keep a stored null site, since null means all sites', () => {
        localStorage.setItem(KEY, JSON.stringify({ version: VERSION, settingsSite: null }));

        spectator = createService();

        expect(spectator.service.settingsSite()).toBeNull();
    });

    it('should start from defaults when nothing is stored', () => {
        spectator = createService();

        expect(spectator.service.settingsThreshold()).toBe(0.75);
    });

    it('should not throw on a malformed blob', () => {
        localStorage.setItem(KEY, '{not json');

        expect(() => createService()).not.toThrow();
    });

    it('should persist a changed setting', () => {
        spectator = createService();
        startTracking();

        spectator.service.setSettings({ settingsThreshold: 0.9 });
        settleWrite();

        expect(JSON.parse(localStorage.getItem(KEY) ?? '{}').settingsThreshold).toBe(0.9);
    });

    it('should coalesce rapid edits into a single write', () => {
        // The panel writes into the store on every keystroke and spinner tick, and each write
        // is a synchronous localStorage round trip.
        spectator = createService();
        startTracking();
        const setItem = jest.spyOn(Storage.prototype, 'setItem');

        spectator.service.setSettings({ settingsThreshold: 0.3 });
        spectator.flushEffects();
        spectator.service.setSettings({ settingsThreshold: 0.6 });
        spectator.flushEffects();
        spectator.service.setSettings({ settingsThreshold: 0.9 });
        settleWrite();

        expect(setItem).toHaveBeenCalledTimes(1);
        expect(JSON.parse(localStorage.getItem(KEY) ?? '{}').settingsThreshold).toBe(0.9);

        setItem.mockRestore();
    });

    it('should stamp the version when persisting, so the migration runs once', () => {
        spectator = createService();
        startTracking();

        spectator.service.setSettings({ settingsThreshold: 0.9 });
        settleWrite();

        expect(JSON.parse(localStorage.getItem(KEY) ?? '{}').version).toBe(VERSION);
    });

    describe('re-defaulting on an older blob', () => {
        it('should apply every re-default newer than the stored version', () => {
            // A v1 blob upgrading straight past v2 must still give up what v2 re-defaulted.
            localStorage.setItem(
                KEY,
                JSON.stringify({
                    version: 1,
                    settingsThreshold: 0.25,
                    settingsContentTypes: 'Blog'
                })
            );

            spectator = createService();

            expect(spectator.service.settingsThreshold()).toBe(0.75);
            expect(spectator.service.settingsContentTypes()).toBe('Blog');
        });

        it('should not throw when the current version has no re-defaults of its own', () => {
            // Indexing RE_DEFAULTED_KEYS by the current version alone yields undefined on the
            // next bump, and `.has` on it would kill the portlet during hydrate.
            localStorage.setItem(KEY, JSON.stringify({ version: 99, settingsThreshold: 0.3 }));

            expect(() => createService()).not.toThrow();
        });

        it('should take the new threshold default instead of the stored one', () => {
            // Without this, raising the default would only reach people who had never opened
            // the portlet — everyone else stays pinned to the value they have persisted.
            localStorage.setItem(
                KEY,
                JSON.stringify({ settingsThreshold: 0.25, settingsContentTypes: 'Blog' })
            );

            spectator = createService();

            expect(spectator.service.settingsThreshold()).toBe(0.75);
        });

        it('should keep every other stored control, so it is a re-default not a reset', () => {
            localStorage.setItem(
                KEY,
                JSON.stringify({
                    settingsThreshold: 0.25,
                    settingsContentTypes: 'Blog',
                    settingsIndexName: 'blogs',
                    settingsOperator: DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT
                })
            );

            spectator = createService();

            expect(spectator.service.settingsContentTypes()).toBe('Blog');
            expect(spectator.service.settingsIndexName()).toBe('blogs');
            expect(spectator.service.settingsOperator()).toBe(DOT_AI_VECTOR_OPERATOR.INNER_PRODUCT);
        });
    });

    describe('a stored value of the wrong shape', () => {
        // localStorage is a trust boundary: the blob can be stale, hand-edited, or written by
        // an older build, and whatever survives hydrate is patched straight into typed state
        // and then assembled into a request body.
        it('should reject a non-numeric threshold and keep the default', () => {
            localStorage.setItem(
                KEY,
                JSON.stringify({ version: VERSION, settingsThreshold: 'a lot' })
            );

            spectator = createService();

            expect(spectator.service.settingsThreshold()).toBe(0.75);
        });

        it('should reject a NaN temperature', () => {
            // JSON cannot carry NaN, but `null` round-trips to it through a coercing writer.
            localStorage.setItem(
                KEY,
                JSON.stringify({ version: VERSION, settingsTemperature: 'hot' })
            );

            spectator = createService();

            expect(spectator.service.settingsTemperature()).toBe(0);
        });

        it('should reject an operator the API does not accept', () => {
            // The numerics are clamped downstream; the operator has no such guard, so an
            // unrecognised one would reach the wire and the server would reject the search.
            localStorage.setItem(
                KEY,
                JSON.stringify({ version: VERSION, settingsOperator: 'product' })
            );

            spectator = createService();

            expect(spectator.service.settingsOperator()).toBe(DOT_AI_VECTOR_OPERATOR.COSINE);
        });

        it('should keep the good keys in a blob that has one bad one', () => {
            localStorage.setItem(
                KEY,
                JSON.stringify({
                    version: VERSION,
                    settingsOperator: 'product',
                    settingsIndexName: 'blogs'
                })
            );

            spectator = createService();

            expect(spectator.service.settingsOperator()).toBe(DOT_AI_VECTOR_OPERATOR.COSINE);
            expect(spectator.service.settingsIndexName()).toBe('blogs');
        });

        it('should survive an array, which passes the typeof object guard', () => {
            localStorage.setItem(KEY, JSON.stringify([1, 2, 3]));

            expect(() => createService()).not.toThrow();
        });
    });
});
