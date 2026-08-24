import {
    AllowedActionsByExperimentStatus,
    type GOAL_TYPES,
    type Goals,
    type DotExperimentStatus,
    type RangeOfDateAndTime,
    type TrafficProportion
} from '@dotcms/dotcms-models';

import { CONFIGURATION_SEGMENT, EXPERIMENTS_URL, RESULTS_SEGMENT } from '../shared/constants';
import { DotExperimentPageInfo, ExperimentListAction } from '../shared/models';

/** Day-level format shared by every schedule cell of the experiments list (e.g. `Jun 25, 2026`). */
const SCHEDULE_DATE_FORMAT: Intl.DateTimeFormatOptions = {
    month: 'short',
    day: 'numeric',
    year: 'numeric'
};

/** Literal arrow drawn between the start and the end of a scheduled range. */
const SCHEDULE_RANGE_SEPARATOR = '→';

/**
 * Translated labels the schedule formatter needs. They are passed in — instead of being
 * hardcoded here — so the util stays free of user-facing English and the call site owns i18n.
 */
export interface ExperimentScheduleLabels {
    /** Shown in place of the end date when the experiment runs until manually stopped. */
    open: string;
    /** Shown when the experiment has no usable start date. */
    none: string;
}

/**
 * Formats an experiment schedule as a single display string.
 *
 * - both dates set → `Jun 25, 2026 → Jul 9, 2026`
 * - start only → `Jun 25, 2026 → {labels.open}`
 * - no usable start date (null scheduling, both dates null, or an end-only range) → `{labels.none}`
 *
 * @param scheduling - Epoch-millisecond range returned by the experiments API
 * @param labels - Already translated fallback labels
 * @param locale - BCP 47 tag; defaults to the runtime locale. Pass it explicitly for deterministic tests.
 */
export function formatSchedule(
    scheduling: RangeOfDateAndTime | null | undefined,
    labels: ExperimentScheduleLabels,
    locale?: string
): string {
    const start = toDisplayDate(scheduling?.startDate, locale);

    if (!start) {
        return labels.none;
    }

    const end = toDisplayDate(scheduling?.endDate, locale);

    return `${start} ${SCHEDULE_RANGE_SEPARATOR} ${end ?? labels.open}`;
}

/**
 * Returns the type of the experiment's primary goal, or `null` when no goal is configured.
 * The empty-state placeholder is rendered by the template, not by this function.
 */
export function goalTypeOf(goals: Goals | null | undefined): GOAL_TYPES | null {
    return goals?.primary?.type ?? null;
}

/** Counts the variants of an experiment, tolerating a missing traffic proportion. */
export function variantsCount(trafficProportion: TrafficProportion | null | undefined): number {
    return trafficProportion?.variants?.length ?? 0;
}

/**
 * Resolves the readable page path for an experiment, falling back to the raw `pageId`
 * when the page is missing from the map (deleted page, or metadata not loaded yet).
 */
export function resolvePagePath(
    pageId: string,
    pageInfoByPageId: Record<string, DotExperimentPageInfo>
): string {
    const url = pageInfoByPageId?.[pageId]?.url;

    return url ? url : pageId;
}

/** Formats an epoch-millisecond timestamp, or returns `null` when it is absent or invalid. */
function toDisplayDate(epochMillis: number | null | undefined, locale?: string): string | null {
    if (epochMillis == null || !Number.isFinite(epochMillis)) {
        return null;
    }

    const date = new Date(epochMillis);

    if (Number.isNaN(date.getTime())) {
        return null;
    }

    return new Intl.DateTimeFormat(locale, SCHEDULE_DATE_FORMAT).format(date);
}

/** Whether an action is offered for a status, per the shared `AllowedActionsByExperimentStatus`. */
export function isAllowed(action: ExperimentListAction, status: DotExperimentStatus): boolean {
    return AllowedActionsByExperimentStatus[action].includes(status);
}

/**
 * Router commands for the Configure screen of an experiment that already exists.
 *
 * Shared rather than repeated: the list's row action and the Results header's Configuration button
 * are two ways to the same URL, and a URL spelled out twice is a URL that can drift.
 *
 * @param experimentId - Identifier of the experiment to configure
 * @returns Absolute router commands, since Configure always hangs off the portlet root
 */
export function configureCommandsOf(experimentId: string): string[] {
    return [EXPERIMENTS_URL, experimentId, CONFIGURATION_SEGMENT];
}

/**
 * Router commands for the Results screen of an experiment.
 *
 * Shared for the same reason as its Configure twin: the list's menu entry and the Configure
 * header's View Results button are two ways to the same URL.
 *
 * @param experimentId - Identifier of the experiment to report on
 * @returns Absolute router commands, since Results always hangs off the portlet root
 */
export function resultsCommandsOf(experimentId: string): string[] {
    return [EXPERIMENTS_URL, experimentId, RESULTS_SEGMENT];
}
