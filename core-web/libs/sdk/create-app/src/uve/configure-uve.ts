import { httpGet, httpPost } from '@dotcms/http';

import { getUVEConfigValue } from '../utils';

/**
 * Single owner of Universal Visual Editor configuration.
 *
 * This replaces two byte-for-byte identical fatal blocks — `src/index.ts:226-228` (the
 * existing-instance path) and `src/index.ts:369-371` (the local-Docker path) — each of which
 * called `process.exit(1)` when the UVE call failed. Because UVE setup runs *before*
 * scaffolding, that exit left the user with an empty directory and threw away a working API
 * token and site ID that had already been obtained. See issue #37262.
 *
 * Nothing here calls `process.exit` and nothing here throws. Failure is a return value the
 * caller warns on and continues past.
 */

/** Which entry path the run came in on. It decides what a 403 *means*, and so what to advise. */
export type UveMode = 'local' | 'remote';

export interface ConfigureUveOptions {
    /** Instance base URL, no trailing slash — e.g. `http://localhost:8082`. */
    host: string;
    siteId: string;
    /** API token, sent as `Authorization: Bearer <token>`. */
    token: string;
    mode: UveMode;
    /** The front-end origin the editor should load — e.g. `http://localhost:3000`. */
    frontendUrl: string;
    /** POST attempts. Spent on 5xx only. */
    maxRetries?: number;
    retryDelayMs?: number;
    /**
     * Caller-supplied reporter. Contract X4: this module must not `console.log` directly,
     * because retry chatter interleaving with an active `ora` spinner is what produced the
     * mangled output in the original report.
     */
    report?: (message: string) => void;
}

export type UveFailurePhase = 'probe' | 'write';
export type UveFailureReason = 'forbidden' | 'server-error' | 'unreachable' | 'unknown';

export type UveOutcome =
    | { readonly kind: 'configured' }
    | {
          readonly kind: 'failed';
          readonly phase: UveFailurePhase;
          readonly reason: UveFailureReason;
          /** HTTP status, or `null` when the request never got a response. */
          readonly status: number | null;
          readonly message: string;
      };

/** App key of the UVE configuration app. Part of the resource path and of remote guidance. */
const UVE_APP_KEY = 'dotema-config-v2';

const HEADLESS_UVE_GUIDE =
    'https://dev.dotcms.com/docs/author/pages-and-visual-editing/universal-visual-editor/uve-headless-config';

const DEFAULT_MAX_RETRIES = 3;
const DEFAULT_RETRY_DELAY_MS = 2000;

function uveResourceUrl(host: string, siteId: string): string {
    return `${host.replace(/\/+$/, '')}/api/v1/apps/${UVE_APP_KEY}/${siteId}`;
}

function statusOf(error: unknown): number | null {
    const response = (error as { response?: { status?: number } })?.response;

    return typeof response?.status === 'number' ? response.status : null;
}

function reasonFor(status: number | null): UveFailureReason {
    if (status === null) {
        return 'unreachable';
    }

    if (status === 403) {
        return 'forbidden';
    }

    if (status >= 500) {
        return 'server-error';
    }

    return 'unknown';
}

/**
 * A 403 is terminal, and what to do about it depends entirely on which stack you are talking to.
 *
 * On the CLI's own Docker stack it means an interrupted first boot never wrote the site's
 * permission rows: measured, the endpoint returned 403 on 193 consecutive attempts over ~7
 * minutes with zero successes, and configuring UVE by hand fails for exactly the same reason.
 * The only fix is to recreate the instance.
 *
 * On a server the user supplied there is no stack to recreate, and telling them to run
 * `docker compose down -v` would be actively wrong — destructive advice aimed at the wrong
 * machine. There it is an ordinary permissions problem and the manual steps do work.
 *
 * Do not merge these two messages.
 */
function forbiddenMessage(mode: UveMode, siteId: string): string {
    if (mode === 'local') {
        return [
            'The Universal Visual Editor could not be configured: the instance rejected the request (403).',
            '',
            'This local instance is unrecoverable. Its first boot was interrupted, so the starter',
            'import never wrote the permission rows for the demo site, and a restart does not repair',
            'them. Configuring the editor by hand would fail the same way.',
            '',
            'Recreate the instance from scratch:',
            '  docker compose down -v && docker compose up -d --wait',
            '',
            'Tracked as dotCMS issue #37268.'
        ].join('\n');
    }

    return [
        'The Universal Visual Editor could not be configured: the instance rejected the request (403).',
        '',
        'The API token does not have permission to write app configuration on the target site.',
        `  site id : ${siteId}`,
        `  app key : ${UVE_APP_KEY}`,
        '',
        'Check that the token belongs to a user who can administer that site, then finish the',
        'setup by hand:',
        `  ${HEADLESS_UVE_GUIDE}`
    ].join('\n');
}

function genericMessage(
    mode: UveMode,
    siteId: string,
    detail: string,
    { withGuide }: { withGuide: boolean }
): string {
    const lines = [
        `The Universal Visual Editor could not be configured: ${detail}`,
        '',
        'Your project is unaffected and setup will continue. To finish the editor configuration',
        'later, use these values:',
        `  site id : ${siteId}`,
        `  app key : ${UVE_APP_KEY}`
    ];

    // The guide is manual-setup advice, so it is withheld in exactly the case where manual
    // setup cannot work (local 403). Everything else is self-serviceable.
    if (withGuide) {
        lines.push('', `  ${HEADLESS_UVE_GUIDE}`);
    }

    void mode;

    return lines.join('\n');
}

function delay(ms: number): Promise<void> {
    return ms > 0 ? new Promise((resolve) => setTimeout(resolve, ms)) : Promise.resolve();
}

export async function configureUVE(options: ConfigureUveOptions): Promise<UveOutcome> {
    const {
        host,
        siteId,
        token,
        mode,
        frontendUrl,
        maxRetries = DEFAULT_MAX_RETRIES,
        retryDelayMs = DEFAULT_RETRY_DELAY_MS,
        report
    } = options;

    const url = uveResourceUrl(host, siteId);
    const notify = (message: string) => report?.(message);

    // Read before write, exactly once. A poll would be wrong here: the failure this guards
    // against never clears, so waiting for it to clear never terminates (contract X3).
    try {
        await httpGet(url, { token });
    } catch (error) {
        const status = statusOf(error);
        const reason = reasonFor(status);
        const message =
            reason === 'forbidden'
                ? forbiddenMessage(mode, siteId)
                : genericMessage(
                      mode,
                      siteId,
                      status === null
                          ? 'the instance could not be reached.'
                          : `the instance answered ${status} when the current configuration was read.`,
                      { withGuide: true }
                  );

        notify(message);

        return { kind: 'failed', phase: 'probe', reason, status, message };
    }

    const payload = {
        configuration: {
            hidden: false,
            // The endpoint expects the serialized UVE config object, not a bare URL. Building
            // it here rather than at the call sites is the point of this module owning the
            // operation — a caller passing the raw origin would be accepted with a 200 and
            // silently leave the editor misconfigured.
            value: getUVEConfigValue(frontendUrl)
        }
    };

    let lastStatus: number | null = null;

    for (let attempt = 1; attempt <= Math.max(1, maxRetries); attempt++) {
        try {
            await httpPost(url, payload, { token });

            return { kind: 'configured' };
        } catch (error) {
            const status = statusOf(error);
            lastStatus = status;

            // Only 5xx is genuinely transient. 4xx — 403 above all — is a decision, not a
            // hiccup, and retrying it just burns the user's time before the same answer.
            const retryable = status !== null && status >= 500 && attempt < Math.max(1, maxRetries);

            if (!retryable) {
                const reason = reasonFor(status);
                const message =
                    reason === 'forbidden'
                        ? forbiddenMessage(mode, siteId)
                        : genericMessage(
                              mode,
                              siteId,
                              status === null
                                  ? 'the instance could not be reached.'
                                  : `the instance answered ${status}.`,
                              { withGuide: true }
                          );

                notify(message);

                return { kind: 'failed', phase: 'write', reason, status, message };
            }

            notify(`dotCMS answered ${status}; retrying (${attempt}/${maxRetries})`);
            await delay(retryDelayMs);
        }
    }

    /* istanbul ignore next -- the loop above always returns; this satisfies the type checker. */
    const message = genericMessage(mode, siteId, `the instance answered ${lastStatus}.`, {
        withGuide: true
    });

    return {
        kind: 'failed',
        phase: 'write',
        reason: reasonFor(lastStatus),
        status: lastStatus,
        message
    };
}
