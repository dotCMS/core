import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/vitest';

import { Select } from 'primeng/select';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotConfigurationLocaleComponent } from './dot-configuration-locale.component';

import { DotConfigurationStore } from '../../store/dot-configuration.store';
import {
    DotConfigurationStoreStub,
    createConfigurationStoreStub
} from '../../testing/configuration-store.stub';

describe('DotConfigurationLocaleComponent', () => {
    let spectator: Spectator<DotConfigurationLocaleComponent>;
    let store: DotConfigurationStoreStub;

    const createComponent = createComponentFactory({
        component: DotConfigurationLocaleComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'configuration.validation.required': 'This field is required.'
                })
            }
        ],
        detectChanges: false
    });

    const selects = () => spectator.queryAll(Select);

    beforeEach(() => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date('2026-10-02T14:20:00Z'));
        store = createConfigurationStoreStub();
        spectator = createComponent({
            providers: [{ provide: DotConfigurationStore, useValue: store }]
        });
        spectator.detectChanges();
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('server time badge', () => {
        it('shows the current time in UTC', () => {
            expect(spectator.query(byTestId('configuration-locale-server-time'))).toHaveText(
                '14:20 UTC'
            );
        });

        it('moves forward as time passes', () => {
            vi.advanceTimersByTime(60_000);
            spectator.detectChanges();

            expect(spectator.query(byTestId('configuration-locale-server-time'))).toHaveText(
                '14:21 UTC'
            );
        });
    });

    describe('language', () => {
        it('lists the admin UI locales as language_COUNTRY values', () => {
            const [language] = selects();

            expect(language.options).toEqual([
                { label: 'English (United States)', value: 'en_US' },
                { label: 'español (España)', value: 'es_ES' }
            ]);
        });

        it('writes the chosen language to the store', () => {
            spectator.triggerEventHandler(
                byTestId('configuration-language'),
                'ngModelChange',
                'es_ES'
            );

            expect(store.patchLocale).toHaveBeenCalledWith({ languageId: 'es_ES' });
        });
    });

    describe('time zone', () => {
        it('lists the server time zones and lets the user filter them', () => {
            const [, timezone] = selects();

            expect(timezone.options).toEqual([
                { label: 'Coordinated Universal Time (UTC)', value: 'UTC' },
                { label: 'Central European Time (Europe/Madrid)', value: 'Europe/Madrid' }
            ]);
            expect(timezone.filter).toBe(true);
        });

        it('writes the chosen time zone to the store', () => {
            spectator.triggerEventHandler(
                byTestId('configuration-timezone'),
                'ngModelChange',
                'Europe/Madrid'
            );

            expect(store.patchLocale).toHaveBeenCalledWith({ timeZoneId: 'Europe/Madrid' });
        });
    });

    it('shows the validation message for a missing time zone', () => {
        store.errors.set({ timeZoneId: 'configuration.validation.required' });
        spectator.detectChanges();

        expect(spectator.query(byTestId('configuration-locale'))).toContainText(
            'This field is required.'
        );
    });
});
