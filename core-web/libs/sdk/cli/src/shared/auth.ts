import {
    DOTCMS_API,
    describeRequestFailure,
    endpoint,
    httpGet,
    httpPost,
    isHttpError
} from '@dotcms/http';

import { CredentialsRejectedError, InstanceUnreachableError, TokenRejectedError } from './errors';

import type { Token } from './types';

interface TokenResponse {
    entity: { token: string };
}

/** Mint a token (FR-006). Note `expirationDays` is a STRING — the API rejects a number. */
export async function mintToken(args: {
    url: string;
    user: string;
    password: string;
}): Promise<Token> {
    try {
        const { data } = await httpPost<TokenResponse>(endpoint(args.url, DOTCMS_API.apiToken), {
            user: args.user,
            password: args.password,
            expirationDays: '365',
            label: 'dotcms agent setup'
        });
        return { value: data.entity.token, origin: 'minted', verified: false };
    } catch (error) {
        if (isHttpError(error) && error.response?.status === 401) {
            throw new CredentialsRejectedError(args.url);
        }
        throw new InstanceUnreachableError(args.url, describeRequestFailure(error));
    }
}

/**
 * Verify a token actually authorizes (FR-008).
 *
 * Applies to EVERY source, minted or supplied. A token that mints but does not authorize is
 * what produces the confusing 401s later, and nothing is written while this is unproven.
 */
export async function verifyToken(url: string, token: Token): Promise<Token> {
    try {
        await httpGet(endpoint(url, DOTCMS_API.currentUser), { token: token.value });
        return { ...token, verified: true };
    } catch (error) {
        // A rejected token and an unreachable instance are different problems with different
        // remedies, and must never collapse into one message (FR-008b).
        if (
            isHttpError(error) &&
            (error.response?.status === 401 || error.response?.status === 403)
        ) {
            throw new TokenRejectedError(url);
        }
        throw new InstanceUnreachableError(url, describeRequestFailure(error));
    }
}
