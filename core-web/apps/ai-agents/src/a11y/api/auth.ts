/**
 * Bearer-token handling for the agent service.
 *
 * The agent is reached behind the dotCMS proxy (plan §8.2), which is the trust
 * boundary: it mints + signs the short-lived JWT and is responsible for
 * verifying the caller. The agent therefore does NOT verify the signature — it
 * only needs (a) the raw token to use as its api.request credential and (b) the
 * subject claim to key the per-user active-run slot (§8.7). Signature
 * verification is the proxy's job (S2).
 */

export interface BearerInfo {
    token: string;
    /** JWT `sub` claim (a dotCMS API-token id) — the per-user slot key. */
    userId: string;
}

/** Pull the raw token out of an `Authorization: Bearer <jwt>` header. */
export function extractBearer(authHeader: string | undefined): string | null {
    if (!authHeader) {
        return null;
    }
    const match = /^Bearer\s+(.+)$/i.exec(authHeader.trim());
    return match ? match[1].trim() : null;
}

/** Decode (NOT verify) a JWT payload and return its `sub`, or null. */
export function decodeSubject(token: string): string | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
        return null;
    }
    try {
        const json = Buffer.from(parts[1], 'base64url').toString('utf-8');
        const payload = JSON.parse(json) as { sub?: string };
        return typeof payload.sub === 'string' && payload.sub.length > 0 ? payload.sub : null;
    } catch {
        return null;
    }
}

/** Parse the Authorization header into token + userId, or null if unusable. */
export function parseBearer(authHeader: string | undefined): BearerInfo | null {
    const token = extractBearer(authHeader);
    if (!token) {
        return null;
    }
    const userId = decodeSubject(token);
    return { token, userId: userId ?? 'unknown' };
}
