import { parseBearer, type BearerInfo } from './auth';

import { type FixRequest, FixRequestSchema } from '../domain/contract';

/**
 * Auth + body validation for the /fix endpoints, kept out of routes.ts so the
 * route handlers stay thin (parse → call use case → translate). The JSON and SSE
 * routes share this — the request shape is IDENTICAL; only the response media type
 * differs (plan §8).
 */

export type ParseFailure = { ok: false; status: 401 | 400 | 422; error: object };
export type ParseSuccess = { ok: true; bearer: BearerInfo; request: FixRequest };
export type ParseResult = ParseSuccess | ParseFailure;

/** Auth + validate the /fix request body; returns the bearer + parsed request or a 4xx error. */
export async function parseFixRequest(
    header: string | undefined,
    rawBody: () => Promise<unknown>
): Promise<ParseResult> {
    // Dev fallback: when no Authorization header arrives, use a token from the env
    // as the bearer — A11Y_AGENT_DEV_TOKEN, or DOTCMS_AUTH from the repo .env (so
    // `nx serve` works with the existing .env, no extra var). This lets the Angular
    // dev server call the agent same-origin without the browser holding a token (the
    // WDS dev proxy can't reliably inject headers on streamed requests). Gated on the
    // env var, so production — which has neither and always sees the proxy-injected
    // JWT — keeps requiring a real bearer.
    const devToken = process.env.A11Y_AGENT_DEV_TOKEN ?? process.env.DOTCMS_AUTH;
    const bearer = parseBearer(header) ?? (devToken ? parseBearer(`Bearer ${devToken}`) : null);
    if (!bearer) {
        return {
            ok: false,
            status: 401,
            error: { error: 'Missing or malformed Authorization: Bearer token' }
        };
    }
    let body: unknown;
    try {
        body = await rawBody();
    } catch {
        return { ok: false, status: 400, error: { error: 'Request body must be valid JSON' } };
    }
    const parsed = FixRequestSchema.safeParse(body);
    if (!parsed.success) {
        return {
            ok: false,
            status: 422,
            error: { error: 'Invalid request', issues: parsed.error.issues }
        };
    }
    return { ok: true, bearer, request: parsed.data };
}
