import { IS_USER_INCLUDED_PATH, IS_USER_INCLUDED_REQUEST_TIMEOUT_MS } from './constants';

import type { DotCMSIsUserIncludedEntity, IsUserIncludedResult } from './models';

const asStringArray = (value: unknown): string[] =>
    Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];

const toEntity = (raw: unknown): DotCMSIsUserIncludedEntity => {
    const entity = (raw ?? {}) as Record<string, unknown>;

    return {
        experiments: Array.isArray(entity['experiments'])
            ? (entity['experiments'] as DotCMSIsUserIncludedEntity['experiments'])
            : [],
        includedExperimentIds: asStringArray(entity['includedExperimentIds']),
        excludedExperimentIds: asStringArray(entity['excludedExperimentIds']),
        excludedExperimentIdsEnded: asStringArray(entity['excludedExperimentIdsEnded'])
    };
};

/**
 * Asks dotCMS which running experiments the visitor is in.
 *
 * @param dotcmsUrl - The dotCMS origin, without a trailing slash
 * @param exclude - Experiments already evaluated, so the server skips them
 * @returns `ok` with the entity, `disabled` on a 403 (Experiments off), or `failed`
 */
export const fetchIsUserIncluded = async (
    dotcmsUrl: string,
    exclude: string[]
): Promise<IsUserIncludedResult> => {
    const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
    const timer = setTimeout(() => controller?.abort(), IS_USER_INCLUDED_REQUEST_TIMEOUT_MS);

    try {
        const response = await fetch(`${dotcmsUrl}${IS_USER_INCLUDED_PATH}`, {
            method: 'POST',
            headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
            body: JSON.stringify({ exclude }),
            signal: controller?.signal ?? null
        });

        if (response.status === 403) {
            return { status: 'disabled' };
        }

        if (!response.ok) {
            return { status: 'failed', httpStatus: response.status };
        }

        const json = (await response.json()) as { entity?: unknown };

        return { status: 'ok', entity: toEntity(json?.entity) };
    } catch {
        return { status: 'failed' };
    } finally {
        clearTimeout(timer);
    }
};
