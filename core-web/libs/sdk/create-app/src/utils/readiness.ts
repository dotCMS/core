import { isSuccessStatus } from './fetch-retry';

/**
 * Answers one question: will the call we are about to make succeed?
 *
 * The CLI probed `/api/v1/appconfiguration` because the management endpoints were unreachable
 * from the Docker host (issue #34509). The bundled compose file now publishes 8090 on loopback,
 * so `/dotmgt/readyz` — the purpose-built readiness endpoint, which does not depend on the web
 * app being up — is available and preferred.
 *
 * Measured against a real stack on 2026-08-31: `docker compose up --wait` reporting *healthy*
 * means LIVE, not READY. The container healthcheck probes `livez`, and `readyz` returned 503 for
 * a few seconds after `--wait` had already returned, then 200. So a 503 here is the ordinary
 * "still starting" state and not an error.
 */

export type Readiness = { kind: 'ready' } | { kind: 'not-ready'; detail: string };

export interface ProbeReadinessOptions {
    /** `/dotmgt/readyz` on the management port. */
    readyzUrl: string;
    /** `/api/v1/appconfiguration`, for images that do not serve the management endpoints. */
    fallbackUrl: string;
    get: (url: string) => Promise<{ status: number }>;
}

const STILL_STARTING = 503;

export async function probeReadiness({
    readyzUrl,
    fallbackUrl,
    get
}: ProbeReadinessOptions): Promise<Readiness> {
    let readyzStatus: number | null = null;

    try {
        const { status } = await get(readyzUrl);
        readyzStatus = status;

        if (isSuccessStatus(status)) {
            return { kind: 'ready' };
        }

        if (status === STILL_STARTING) {
            // Authoritative: readyz answered and said not yet. Do NOT consult the fallback here.
            // `appconfiguration` can return 200 while the stack is still coming up, so a second
            // opinion would report a booting instance as ready — the exact failure being fixed.
            return { kind: 'not-ready', detail: 'dotCMS is still starting up' };
        }
    } catch {
        // Unreachable — fall through to the fallback below.
    }

    // Anything else from readyz (404 on an older image, a transport failure) means the endpoint
    // cannot be trusted to exist, so fall back to the app endpoint the CLI used before.
    try {
        const { status } = await get(fallbackUrl);

        if (isSuccessStatus(status)) {
            return { kind: 'ready' };
        }

        return {
            kind: 'not-ready',
            detail: `${readyzUrl} answered ${readyzStatus ?? 'nothing'} and ${fallbackUrl} answered ${status}`
        };
    } catch (error) {
        const reason = error instanceof Error ? error.message : String(error);

        return {
            kind: 'not-ready',
            detail: `neither ${readyzUrl} nor ${fallbackUrl} could be reached (${reason})`
        };
    }
}

/**
 * Polls {@link probeReadiness} until the instance is ready or the budget runs out.
 *
 * Kept separate from `fetchWithRetry` because the semantics differ: this never throws, and a 503
 * is a normal intermediate state rather than a failed attempt. The reporter is injected for the
 * same reason as everywhere else in this CLI — the caller owns the spinner (AC-009).
 */
export async function waitForReadiness(options: {
    readyzUrl: string;
    fallbackUrl: string;
    get: (url: string) => Promise<{ status: number }>;
    attempts: number;
    delayMs: number;
    onAttempt?: (attempt: number, attempts: number, detail: string) => void;
}): Promise<Readiness> {
    const { attempts, delayMs, onAttempt, ...probe } = options;

    let last: Readiness = { kind: 'not-ready', detail: 'not probed yet' };

    for (let attempt = 1; attempt <= attempts; attempt++) {
        last = await probeReadiness(probe);

        if (last.kind === 'ready') {
            return last;
        }

        onAttempt?.(attempt, attempts, last.detail);

        if (attempt < attempts) {
            await new Promise((resolve) => setTimeout(resolve, delayMs));
        }
    }

    return last;
}
