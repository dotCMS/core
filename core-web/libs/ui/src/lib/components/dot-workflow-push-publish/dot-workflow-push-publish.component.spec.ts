import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotFormatDateService,
    DotMessageService,
    DotPushPublishFiltersService,
    PushPublishService
} from '@dotcms/data-access';
import { DotcmsConfigService } from '@dotcms/dotcms-js';

import {
    DotWorkflowPushPublishComponent,
    DotWorkflowPushPublishValue
} from './dot-workflow-push-publish.component';

const FILTERS = [
    { key: 'filter-a', title: 'Filter A', defaultFilter: false },
    { key: 'filter-b', title: 'Filter B', defaultFilter: true }
];

const TIMEZONES = [
    { id: 'America/New_York', label: 'New York', offset: '-5' },
    { id: 'Europe/Madrid', label: 'Madrid', offset: '+1' }
];

const ENVIRONMENTS = [
    { id: 'env-1', name: 'Staging' },
    { id: 'env-2', name: 'Production' }
];

describe('DotWorkflowPushPublishComponent', () => {
    let spectator: Spectator<DotWorkflowPushPublishComponent>;

    const getFilters = jest.fn();
    const getTimeZones = jest.fn();

    const createComponent = createComponentFactory({
        component: DotWorkflowPushPublishComponent,
        providers: [
            mockProvider(DotMessageService, {
                get: jest.fn().mockImplementation((key: string) => key)
            }),
            mockProvider(DotcmsConfigService, { getTimeZones }),
            // The embedded env selector loads its own options.
            mockProvider(PushPublishService, {
                getEnvironments: jest.fn(() => of(ENVIRONMENTS)),
                lastEnvironmentPushed: null
            }),
            // Formats to the `yyyy-MM-dd` / `HH-mm` pair the backend expects; stubbed so the
            // assertions are about which value is formatted, not about date-fns.
            mockProvider(DotFormatDateService, {
                format: jest
                    .fn()
                    .mockImplementation((_date: Date, pattern: string) =>
                        pattern === 'HH-mm' ? '10-30' : '2026-08-12'
                    )
            })
        ],
        componentProviders: [mockProvider(DotPushPublishFiltersService, { get: getFilters })],
        detectChanges: false
    });

    beforeEach(() => {
        jest.clearAllMocks();
        getFilters.mockReturnValue(of(FILTERS));
        getTimeZones.mockReturnValue(of(TIMEZONES));
    });

    const captureValues = (): DotWorkflowPushPublishValue[] => {
        const values: DotWorkflowPushPublishValue[] = [];
        spectator
            .output<DotWorkflowPushPublishValue>('valueChange')
            .subscribe((v) => values.push(v));

        return values;
    };

    const captureValidity = (): boolean[] => {
        const valid: boolean[] = [];
        spectator.output<boolean>('validChange').subscribe((v) => valid.push(v));

        return valid;
    };

    /** Chooses environments, standing in for the embedded selector's CVA. */
    const chooseEnvironments = (ids: string[] = ['env-1', 'env-2']): void => {
        spectator.component['onEnvironmentsChange'](ids);
        spectator.detectChanges();
    };

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    it('should render no dialog or submit control of its own', () => {
        expect(spectator.query('p-dialog')).toBeNull();
        expect(spectator.query('button[type="submit"]')).toBeNull();
    });

    it('should render no plugin custom-code container', () => {
        // The legacy form replaces itself with plugin HTML when `customCode` is set. A step embedded in
        // someone else's dialog has no contract for that, so this component has no such branch.
        expect(spectator.query('.custom-code')).toBeNull();
    });

    it('should embed the shared env selector rather than its own control', () => {
        expect(spectator.query('dot-push-publish-env-selector')).toBeTruthy();
    });

    describe('which fields the choice needs', () => {
        it('should show both dates and the filter for publish and expire', () => {
            spectator.component['onIWantToChange']('publishexpire');
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-publish-publish-date'))).toBeTruthy();
            expect(spectator.query(byTestId('push-publish-expire-date'))).toBeTruthy();
            expect(spectator.query(byTestId('push-publish-filter'))).toBeTruthy();
        });

        it('should drop the expiry date when only publishing', () => {
            spectator.component['onIWantToChange']('publish');
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-publish-publish-date'))).toBeTruthy();
            expect(spectator.query(byTestId('push-publish-expire-date'))).toBeNull();
        });

        it('should drop the publish date and the filter when only expiring', () => {
            // A filter shapes what gets pushed, so it is meaningless for an expiry.
            spectator.component['onIWantToChange']('expire');
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-publish-publish-date'))).toBeNull();
            expect(spectator.query(byTestId('push-publish-expire-date'))).toBeTruthy();
            expect(spectator.query(byTestId('push-publish-filter'))).toBeNull();
        });
    });

    describe('validity', () => {
        it('should be invalid until an environment is chosen', () => {
            // The one field with no sensible default: dates default to now, the filter to the
            // server's default, but there is no "somewhere" to push to.
            //
            // Re-created so the subscription precedes the first flush: the initial emission happens
            // then, and a host binding `(validChange)` in its template is subscribed before it.
            spectator = createComponent();
            const valid = captureValidity();
            spectator.detectChanges();

            expect(valid.at(-1)).toBe(false);
        });

        it('should become valid once an environment is chosen', () => {
            const valid = captureValidity();

            chooseEnvironments();

            expect(valid.at(-1)).toBe(true);
        });

        it('should go invalid again when environments are cleared', () => {
            const valid = captureValidity();
            chooseEnvironments();

            chooseEnvironments([]);

            expect(valid.at(-1)).toBe(false);
        });
    });

    describe('the emitted payload', () => {
        it('should comma-join environments into whereToSend', () => {
            const values = captureValues();

            chooseEnvironments(['env-1', 'env-2']);

            expect(values.at(-1)?.whereToSend).toBe('env-1,env-2');
        });

        it('should split each date into the date and time pair the backend expects', () => {
            const values = captureValues();

            chooseEnvironments();

            expect(values.at(-1)).toMatchObject({
                publishDate: '2026-08-12',
                publishTime: '10-30',
                expireDate: '2026-08-12',
                expireTime: '10-30'
            });
        });

        it('should arm the server default filter', () => {
            const values = captureValues();

            chooseEnvironments();

            expect(values.at(-1)?.filterKey).toBe('filter-b');
        });

        it('should send no filter when only expiring', () => {
            // Cleared rather than carried over, so a filter chosen before switching to expire does
            // not ride along into a request that ignores it.
            const values = captureValues();
            chooseEnvironments();

            spectator.component['onIWantToChange']('expire');
            spectator.detectChanges();

            expect(values.at(-1)?.filterKey).toBe('');
            expect(values.at(-1)?.iWantTo).toBe('expire');
        });

        it('should default the timezone to the browser zone when the server knows it', () => {
            jest.spyOn(Intl, 'DateTimeFormat').mockReturnValue({
                resolvedOptions: () => ({ timeZone: 'Europe/Madrid' })
            } as unknown as Intl.DateTimeFormat);

            spectator = createComponent();
            const values = captureValues();
            spectator.detectChanges();

            expect(values.at(-1)?.timezoneId).toBe('Europe/Madrid');
        });

        it('should leave the timezone unset when the server does not know the browser zone', () => {
            // Better unset than a zone the backend would reject.
            jest.spyOn(Intl, 'DateTimeFormat').mockReturnValue({
                resolvedOptions: () => ({ timeZone: 'Mars/Olympus_Mons' })
            } as unknown as Intl.DateTimeFormat);

            spectator = createComponent();
            const values = captureValues();
            spectator.detectChanges();

            expect(values.at(-1)?.timezoneId).toBe('');
        });
    });

    describe('the timezone reveal', () => {
        it('should keep the select collapsed until asked for', () => {
            expect(spectator.query(byTestId('push-publish-timezone-field'))).toBeNull();
        });

        it('should reveal the select on toggle', () => {
            spectator.click(byTestId('push-publish-timezone-toggle'));
            spectator.detectChanges();

            expect(spectator.query(byTestId('push-publish-timezone-field'))).toBeTruthy();
        });
    });

    describe('when the lookups fail', () => {
        it('should stay usable with no filters', () => {
            // Degrades to "no filter", which the backend accepts, rather than blocking the action.
            getFilters.mockReturnValue(throwError(() => new Error('boom')));

            spectator = createComponent();
            const values = captureValues();
            spectator.detectChanges();
            chooseEnvironments();

            expect(values.at(-1)?.filterKey).toBe('');
            expect(spectator.query(byTestId('workflow-push-publish'))).toBeTruthy();
        });

        it('should stay usable with no timezones', () => {
            getTimeZones.mockReturnValue(throwError(() => new Error('boom')));

            spectator = createComponent();
            const valid = captureValidity();
            spectator.detectChanges();
            chooseEnvironments();

            expect(valid.at(-1)).toBe(true);
        });
    });
});
