import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';
import { vi } from 'vitest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotAiSettingsCardComponent, DotAiSettingsValue } from './dot-ai-settings-card.component';

import { SETTINGS_ADVANCED_FIELDS } from '../../dot-ai-config.constants';

describe('DotAiSettingsCardComponent', () => {
    let spectator: Spectator<DotAiSettingsCardComponent>;

    const createComponent = createComponentFactory({
        component: DotAiSettingsCardComponent,
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService({}) }],
        detectChanges: false
    });

    function setup(initialValue: DotAiSettingsValue | null = null): void {
        spectator = createComponent({ props: { initialValue } });
        spectator.detectChanges();
    }

    describe('advanced field rendering', () => {
        // The advanced grid branches on `type`, and the three branches render different controls.
        // These pin each branch to a field that actually uses it in SETTINGS_ADVANCED_FIELDS.
        it('renders a checkbox for a checkbox-typed field', () => {
            setup();

            expect(spectator.query(byTestId('settings-checkbox-debugLogging'))).toExist();
            expect(spectator.query(byTestId('settings-number-debugLogging'))).not.toExist();
        });

        it('renders a number input for a number-typed field', () => {
            setup();

            expect(spectator.query(byTestId('settings-number-embeddingsSplitAtTokens'))).toExist();
            expect(
                spectator.query(byTestId('settings-text-embeddingsSplitAtTokens'))
            ).not.toExist();
        });

        it('renders a text input for a text-typed field', () => {
            setup();

            expect(spectator.query(byTestId('settings-text-embeddingsFileExtensions'))).toExist();
            expect(
                spectator.query(byTestId('settings-number-embeddingsFileExtensions'))
            ).not.toExist();
        });

        it('renders every advanced field exactly once, in one of the three branches', () => {
            setup();

            SETTINGS_ADVANCED_FIELDS.forEach(({ key, type }) => {
                const rendered = [
                    spectator.query(byTestId(`settings-checkbox-${key}`)),
                    spectator.query(byTestId(`settings-number-${key}`)),
                    spectator.query(byTestId(`settings-text-${key}`))
                ].filter(Boolean);

                expect({ key, count: rendered.length }).toEqual({ key, count: 1 });
                expect(spectator.query(byTestId(`settings-${type}-${key}`))).toExist();
            });
        });

        it('gives every advanced control a label bound to it', () => {
            setup();

            SETTINGS_ADVANCED_FIELDS.forEach(({ key }) => {
                const label = spectator.query(`label[for="${key}"]`);
                expect({ key, labelled: !!label }).toEqual({ key, labelled: true });
            });
        });
    });

    describe('checkbox defaults', () => {
        // `defaultValue` exists so an untouched checkbox doesn't silently save the wrong value
        // when the key was never set on the backend.
        it('seeds checkbox controls from their backend default', () => {
            setup();

            expect(spectator.component.advancedForm.get('embeddingsDeleteOldOnUpdate')?.value).toBe(
                true
            );
            expect(spectator.component.advancedForm.get('debugLogging')?.value).toBe(false);
        });

        it('lets a saved value override the default', () => {
            setup({ embeddingsDeleteOldOnUpdate: false });

            expect(spectator.component.advancedForm.get('embeddingsDeleteOldOnUpdate')?.value).toBe(
                false
            );
        });
    });

    describe('hydration', () => {
        it('patches common and advanced values into their own forms', () => {
            setup({ rolePrompt: 'You are a bot', embeddingsSplitAtTokens: 512 });

            expect(spectator.component.form.get('rolePrompt')?.value).toBe('You are a bot');
            expect(spectator.component.advancedForm.get('embeddingsSplitAtTokens')?.value).toBe(
                512
            );
        });

        it('routes an unknown key into additional properties', () => {
            setup({ someCustomKey: 'custom' });

            expect(spectator.component.additionalProperties.length).toBe(1);
            expect(spectator.component.additionalProperties.at(0).value).toEqual({
                key: 'someCustomKey',
                value: 'custom'
            });
        });

        // Regression: `imageSize` is rendered by the fixed <p-select>, not by the advanced list.
        // If it were not in `knownKeys` it would hydrate into both the dropdown and a duplicate
        // additional-property row — and since additional properties are applied last on save,
        // that stale row would silently overwrite whatever the user picked.
        it('does not duplicate a common field into additional properties', () => {
            setup({ imageSize: '1024x1024', rolePrompt: 'x', textPrompt: 'y', imagePrompt: 'z' });

            expect(spectator.component.additionalProperties.length).toBe(0);
            expect(spectator.component.form.get('imageSize')?.value).toBe('1024x1024');
        });

        it('stringifies a non-string saved value instead of corrupting it via String()', () => {
            setup({ listenerIndexer: { enabled: true, batchSize: 10 } });

            // `getRawValue()` rather than `.value`: a FormGroup's `value` is a Partial, so the
            // field reads as `string | undefined` and does not satisfy `JSON.parse`.
            const value = spectator.component.additionalProperties.at(0).getRawValue().value;

            expect(value).not.toBe('[object Object]');
            expect(JSON.parse(value)).toEqual({ enabled: true, batchSize: 10 });
        });
    });

    describe('buildPayloadSection', () => {
        it('includes common and advanced values', () => {
            setup({ rolePrompt: 'You are a bot', embeddingsSplitAtTokens: 512 });

            const section = spectator.component.buildPayloadSection();

            expect(section['rolePrompt']).toBe('You are a bot');
            expect(section['embeddingsSplitAtTokens']).toBe(512);
        });

        it('omits empty values so an untouched field is not saved as blank', () => {
            setup();

            const section = spectator.component.buildPayloadSection();

            expect('rolePrompt' in section).toBe(false);
            expect('textPrompt' in section).toBe(false);
        });

        it('round-trips a hydrated object additional property back out', () => {
            setup({ listenerIndexer: { enabled: true, batchSize: 10 } });

            const section = spectator.component.buildPayloadSection();

            expect(section['listenerIndexer']).toEqual({ enabled: true, batchSize: 10 });
        });

        it('keeps a checkbox default in the payload even when untouched', () => {
            setup();

            const section = spectator.component.buildPayloadSection();

            expect(section['embeddingsDeleteOldOnUpdate']).toBe(true);
        });
    });

    describe('changed output', () => {
        it('emits when a common field changes', () => {
            setup();
            const changed = vi.fn();
            spectator.output('changed').subscribe(changed);

            spectator.component.form.patchValue({ rolePrompt: 'new' });

            expect(changed).toHaveBeenCalled();
        });

        it('emits when an advanced field changes', () => {
            setup();
            const changed = vi.fn();
            spectator.output('changed').subscribe(changed);

            spectator.component.advancedForm.patchValue({ debugLogging: true });

            expect(changed).toHaveBeenCalled();
        });

        // Loading a saved config must not make the page announce "Unsaved changes". Today that is
        // guaranteed twice over: `ngOnInit` subscribes to `valueChanges` only *after* hydrating,
        // and hydration patches with `emitEvent: false`. Asserting the silence alone would pass
        // even with both protections removed, so the second half proves the subscription this
        // stayed silent through is actually live.
        it("stays silent while hydrating, then reports the user's own edits", () => {
            spectator = createComponent({ props: { initialValue: { rolePrompt: 'saved' } } });
            const changed = vi.fn();
            spectator.output('changed').subscribe(changed);

            spectator.detectChanges();
            expect(changed).not.toHaveBeenCalled();

            spectator.component.form.patchValue({ rolePrompt: 'edited by the user' });
            expect(changed).toHaveBeenCalledTimes(1);
        });
    });
});
