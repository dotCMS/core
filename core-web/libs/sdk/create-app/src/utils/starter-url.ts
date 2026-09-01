/**
 * Rewrite of the `CUSTOM_STARTER_URL` line in a docker-compose file.
 *
 * This lives in its own module — split out of `updateDockerComposeStarterUrl` in
 * `src/index.ts` — purely so it can be pinned by a Jest spec (`starter-url.spec.ts`).
 * The transformation is a single regex against a hand-maintained YAML asset: a harmless
 * looking reformat of that one line (a block scalar, or the `- KEY=value` env-list form)
 * silently breaks `--starter` for every already-installed CLI, with no error at install
 * time. Keeping it pure — string in, string out, no `fs` — is what lets the spec run the
 * real bundled asset through it on every build. See dotCMS issue #37262, AC-012.
 */

/**
 * Matches the `CUSTOM_STARTER_URL` mapping line, capturing everything up to and including
 * the colon and its trailing whitespace so the author's indentation and key quoting survive
 * the rewrite. Deliberately not global: only the first entry is rewritten.
 */
const CUSTOM_STARTER_URL_LINE = /^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m;

/**
 * Replaces the value of the first `CUSTOM_STARTER_URL` entry with `starterUrl`.
 *
 * @param composeContents contents of a docker-compose file
 * @param starterUrl the starter URL to write in, as passed to `--starter`
 * @returns the compose contents with that one line rewritten
 * @throws if no `CUSTOM_STARTER_URL` mapping entry is present — an empty file and the
 *         `- CUSTOM_STARTER_URL=value` env-list form both fail loudly rather than
 *         silently dropping the user's `--starter` value.
 */
export function applyStarterUrl(composeContents: string, starterUrl: string): string {
    if (!CUSTOM_STARTER_URL_LINE.test(composeContents)) {
        throw new Error(
            'CUSTOM_STARTER_URL entry not found in docker-compose.yml. Unable to apply --starter value.'
        );
    }

    return composeContents.replace(CUSTOM_STARTER_URL_LINE, `$1"${starterUrl}"`);
}
