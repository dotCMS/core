import { DOTCMS_API, describeRequestFailure, endpoint, httpGet } from '@dotcms/http';

import { ENV_KEYS, readEnv } from './env';
import { InstanceUnreachableError, InvalidUrlError } from './errors';

import type { RunOptions } from './types';

export interface InstanceInfo {
    /** Normalized base URL, no trailing slash. */
    url: string;
    /** Instance version from /api/v1/appconfiguration, or null when absent/unreadable. */
    version: string | null;
}

/** Trailing slashes only — the rest of the address is the user's to get right. */
export function normalizeUrl(url: string): string {
    return url.trim().replace(/\/+$/, '');
}

export function validateUrl(url: string): void {
    if (!/^https?:\/\//i.test(url)) throw new InvalidUrlError(url);
    try {
         
        new URL(url);
    } catch {
        throw new InvalidUrlError(url);
    }
}

/**
 * Option -> environment -> prompt (FR-004). The prompt is supplied by the caller so this stays
 * usable from a non-interactive context; `promptFor` is only reached when both earlier sources
 * are empty.
 */
export async function resolveUrl(
    opts: Partial<RunOptions>,
    promptFor?: () => Promise<string>
): Promise<string> {
    const raw = opts.url ?? readEnv(ENV_KEYS.url) ?? (promptFor ? await promptFor() : undefined);
    if (!raw) throw new InvalidUrlError('');
    const url = normalizeUrl(raw);
    validateUrl(url);
    return url;
}

/**
 * Reachability (FR-005).
 *
 * `/api/v1/appconfiguration`, NOT `/probes/alive`: the probe endpoints carry IP ACLs and fail
 * from outside the container (dotCMS/core#34509). The same response carries the instance
 * version, which is why the compatibility check costs no extra request.
 */
export async function checkReachable(url: string): Promise<InstanceInfo> {
    try {
        const { data } = await httpGet<{ entity?: Record<string, unknown> }>(
            endpoint(url, DOTCMS_API.appConfiguration)
        );
        return { url, version: readVersion(data) };
    } catch (error) {
        throw new InstanceUnreachableError(url, describeRequestFailure(error));
    }
}

function readVersion(data: unknown): string | null {
    const entity = (data as { entity?: Record<string, unknown> })?.entity;
    const candidate = entity?.['version'] ?? entity?.['dotcmsVersion'] ?? entity?.['releaseVersion'];
    return typeof candidate === 'string' && candidate.trim() !== '' ? candidate : null;
}

/** Date-lockstep versions look like `2026.9.4` (leading zeros dropped) — ADR-0019. */
function parseVersion(value: string): number[] | null {
    const match = /^(\d+)\.(\d+)\.(\d+)/.exec(value.trim());
    return match ? [Number(match[1]), Number(match[2]), Number(match[3])] : null;
}

/**
 * Warn when the instance is older than this tool (FR-005a, ADR-0019).
 *
 * FAIL OPEN, always: an absent or unparseable version returns null rather than throwing. The
 * ADR is explicit that this check must never turn a possible incompatibility into a guaranteed
 * outage, and it must never become a second thing that can fail a run.
 */
export function compatibilityWarning(
    instanceVersion: string | null,
    toolVersion: string
): string | null {
    if (!instanceVersion) return null;
    const instance = parseVersion(instanceVersion);
    const tool = parseVersion(toolVersion);
    if (!instance || !tool) return null;

    for (let i = 0; i < 3; i++) {
        if (instance[i] > tool[i]) return null;
        if (instance[i] < tool[i]) {
            return (
                `This tool targets dotCMS ${toolVersion}, but the instance reports ${instanceVersion}. ` +
                `Install dotcms@${instanceVersion} to match your instance.`
            );
        }
    }
    return null;
}
