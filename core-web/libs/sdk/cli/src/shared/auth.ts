import type { Token } from './types';

/** Mint a token. `expirationDays` is sent as a STRING (FR-006). */
export async function mintToken(_args: {
    url: string;
    user: string;
    password: string;
}): Promise<Token> {
    throw new Error('not implemented');
}

/**
 * Verify a token grants access, via GET /api/v1/users/current.
 * Applies to EVERY token source, minted or supplied (FR-008).
 */
export async function verifyToken(_url: string, _token: Token): Promise<Token> {
    throw new Error('not implemented');
}
