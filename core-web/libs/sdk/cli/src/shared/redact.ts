/** First 6 + last 4. The only form in which a token may appear anywhere (FR-022). */
export function redact(secret: string): string {
    if (!secret) return '';
    if (secret.length <= 12) return '*'.repeat(secret.length);
    return `${secret.slice(0, 6)}…${secret.slice(-4)}`;
}
