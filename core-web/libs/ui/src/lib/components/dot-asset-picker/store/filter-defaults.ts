import { DotAssetPickerConfig, DotAssetPickerFilters } from './models';

import {
    SHARED_ASSETS_DISABLED_VALUE,
    SHARED_ASSETS_FILTER_KEY
} from '../../dot-filter-bar/chips/dot-shared-assets-filter/constants';
import {
    DotContentStatus,
    PUBLISHED_ONLY_STATUSES
} from '../../dot-filter-bar/chips/dot-status-filter/constants';

/**
 * The filters a picker opens with, derived from what its caller seeded.
 *
 * One function rather than inline logic in two places, because `initPicker` and `clearFilters` have
 * to agree: clearing lands on the same visible state a fresh open shows, or "Clear all" strands an
 * Image field's editor in an unfiltered, unlocalized library. Content Drive solves the same problem
 * with `withFilterDefaults`, applied on every path that builds filters from scratch.
 *
 * `sharedAssets` is deliberately absent. Content Drive seeds it so the applied state is visible in
 * its URL rather than implied by a missing key; the picker has no URL, so the reason evaporates and
 * an absent key simply means on — which is how the shared chip already reads it.
 *
 * Only *seeds* appear here. `config.mimeTypes` and `config.allowedBaseTypes` are caller
 * restrictions — part of what the picker is, not something the editor can change — and they are
 * applied where the request is built, surviving every clear.
 *
 * @param config The picker's configuration, or `null` before `initPicker` has run.
 * @return The seeded filter set. A fresh object every call; never mutates `config`.
 */
export function buildPickerFilterDefaults(
    config: DotAssetPickerConfig | null
): DotAssetPickerFilters {
    if (!config) {
        return {};
    }

    const status = admittedStatuses(config);

    return {
        ...(config.languageId ? { languageId: [config.languageId] } : {}),
        ...(config.baseTypes?.length ? { baseType: config.baseTypes } : {}),
        ...(status.length ? { status } : {})
    };
}

/**
 * The seeded conditions the caller's own version state admits.
 *
 * A picker pinned to published content cannot carry Archived or Unpublished: neither has a
 * published version, so the platform would answer with the working version instead — content
 * described by a version the caller did not ask for (SC-009).
 *
 * Enforced **here**, at the seed, and not only by bounding the chip's options: a seed arrives
 * before any chip exists, and an invariant that depends on a control being on screen is an
 * invariant that holds by luck. The chip's bound (FR-014d) is what keeps the editor from re-adding
 * one afterwards.
 *
 * @param config The picker's configuration.
 * @return The admitted conditions, in the caller's order. Empty when nothing was seeded.
 */
function admittedStatuses(config: DotAssetPickerConfig): string[] {
    const seeded = config.status ?? [];

    if (!seeded.length || config.browse?.showWorking !== false) {
        return seeded;
    }

    return seeded.filter((value) => PUBLISHED_ONLY_STATUSES.includes(value as DotContentStatus));
}

/** Whether two filter values are the same selection, order included. */
const sameValue = (a?: string | string[], b?: string | string[]): boolean => {
    if (Array.isArray(a) && Array.isArray(b)) {
        return a.length === b.length && a.every((value, index) => value === b[index]);
    }

    return a === b;
};

/**
 * Whether anything differs from what the picker opened with — which is what decides whether
 * "Clear all" is worth offering.
 *
 * Not the same question as "are there filters at all": the seeds are always present, so counting
 * keys would offer to clear a picker nobody has filtered. A filter explicitly set back to its
 * seeded value counts as default too: re-selecting the caller's locale by hand is indistinguishable
 * from the seeded state, and clearing it would just re-select the same thing.
 *
 * @param filters The current filter set.
 * @param config The picker's configuration, which is what "default" means here.
 * @return True when at least one filter differs from the opening state.
 */
export function hasNonDefaultPickerFilters(
    filters: DotAssetPickerFilters,
    config: DotAssetPickerConfig | null
): boolean {
    const defaults = buildPickerFilterDefaults(config);
    const keys = new Set([...Object.keys(filters ?? {}), ...Object.keys(defaults)]);

    for (const key of keys) {
        // Absence means on, so an explicit "on" is the default said out loud.
        if (key === SHARED_ASSETS_FILTER_KEY) {
            if (filters[key] === SHARED_ASSETS_DISABLED_VALUE) {
                return true;
            }

            continue;
        }

        if (!sameValue(filters?.[key], defaults[key])) {
            return true;
        }
    }

    return false;
}
