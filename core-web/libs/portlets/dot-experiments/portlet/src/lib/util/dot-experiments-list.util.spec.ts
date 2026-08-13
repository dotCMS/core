import {
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES,
    Goals,
    TrafficProportion,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';

import {
    ExperimentScheduleLabels,
    formatSchedule,
    goalTypeOf,
    resolvePagePath,
    variantsCount
} from './dot-experiments-list.util';

import { DotExperimentPageInfo } from '../shared/models';

const LABELS: ExperimentScheduleLabels = {
    open: 'Until stopped',
    none: 'Not scheduled'
};

/** Local noon keeps the formatted day stable regardless of the runner timezone. */
const atLocalNoon = (year: number, monthIndex: number, day: number): number =>
    new Date(year, monthIndex, day, 12).getTime();

const JUN_25_2026 = atLocalNoon(2026, 5, 25);
const JUL_9_2026 = atLocalNoon(2026, 6, 9);

describe('dot-experiments-list.util', () => {
    describe('formatSchedule', () => {
        it('should render both dates joined by an arrow', () => {
            expect(
                formatSchedule({ startDate: JUN_25_2026, endDate: JUL_9_2026 }, LABELS, 'en-US')
            ).toBe('Jun 25, 2026 → Jul 9, 2026');
        });

        it('should render the open label when only the start date is set', () => {
            expect(formatSchedule({ startDate: JUN_25_2026, endDate: null }, LABELS, 'en-US')).toBe(
                `Jun 25, 2026 → ${LABELS.open}`
            );
        });

        it('should render the none label when the scheduling is null', () => {
            expect(formatSchedule(null, LABELS, 'en-US')).toBe(LABELS.none);
        });

        it('should render the none label when the scheduling is undefined', () => {
            expect(formatSchedule(undefined, LABELS, 'en-US')).toBe(LABELS.none);
        });

        it('should render the none label when both dates are null', () => {
            expect(formatSchedule({ startDate: null, endDate: null }, LABELS, 'en-US')).toBe(
                LABELS.none
            );
        });

        it('should render the none label when only the end date is set', () => {
            expect(formatSchedule({ startDate: null, endDate: JUL_9_2026 }, LABELS, 'en-US')).toBe(
                LABELS.none
            );
        });

        it('should treat a NaN start date as absent', () => {
            expect(formatSchedule({ startDate: NaN, endDate: JUL_9_2026 }, LABELS, 'en-US')).toBe(
                LABELS.none
            );
        });

        it('should treat a non-finite end date as absent', () => {
            expect(
                formatSchedule({ startDate: JUN_25_2026, endDate: Infinity }, LABELS, 'en-US')
            ).toBe(`Jun 25, 2026 → ${LABELS.open}`);
        });
    });

    describe('goalTypeOf', () => {
        it('should return the type of the primary goal', () => {
            const goals: Goals = {
                primary: {
                    name: 'default',
                    type: GOAL_TYPES.BOUNCE_RATE,
                    conditions: [
                        {
                            parameter: GOAL_PARAMETERS.URL,
                            operator: GOAL_OPERATORS.EQUALS,
                            value: 'index'
                        }
                    ]
                }
            };

            expect(goalTypeOf(goals)).toBe(GOAL_TYPES.BOUNCE_RATE);
        });

        it('should return null when there are no goals', () => {
            expect(goalTypeOf(null)).toBeNull();
            expect(goalTypeOf(undefined)).toBeNull();
            expect(goalTypeOf({} as Goals)).toBeNull();
        });
    });

    describe('variantsCount', () => {
        it('should count the variants of the traffic proportion', () => {
            const trafficProportion: TrafficProportion = {
                type: TrafficProportionTypes.SPLIT_EVENLY,
                variants: [
                    { id: 'DEFAULT', name: 'Original', weight: 50 },
                    { id: '111', name: 'Variant A', weight: 50 }
                ]
            };

            expect(variantsCount(trafficProportion)).toBe(2);
        });

        it('should return 0 when the traffic proportion is missing', () => {
            expect(variantsCount(null)).toBe(0);
            expect(variantsCount(undefined)).toBe(0);
            expect(variantsCount({} as TrafficProportion)).toBe(0);
        });
    });

    describe('resolvePagePath', () => {
        const PAGE_INFO: Record<string, DotExperimentPageInfo> = {
            'page-1': { url: '/blog/index', host: 'host-1' },
            'page-empty-url': { url: '', host: 'host-1' }
        };

        it('should return the resolved url', () => {
            expect(resolvePagePath('page-1', PAGE_INFO)).toBe('/blog/index');
        });

        it('should fall back to the pageId when the page is not in the map', () => {
            expect(resolvePagePath('missing-page', PAGE_INFO)).toBe('missing-page');
        });

        it('should fall back to the pageId when the resolved url is empty', () => {
            expect(resolvePagePath('page-empty-url', PAGE_INFO)).toBe('page-empty-url');
        });
    });
});
