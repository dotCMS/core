import { existsSync, readFileSync } from 'fs';
import path from 'path';

import { applyStarterUrl } from './starter-url';

/**
 * AC-012 — `npx @dotcms/create-app --starter <url>` must keep working against the compose
 * file the CLI now *bundles* (previously it was downloaded at runtime).
 *
 * Contract under test — `src/utils/starter-url.ts` must export:
 *
 *     export function applyStarterUrl(composeContents: string, starterUrl: string): string
 *
 * A pure string -> string rewrite: no `fs`, no `path`, no `directory` argument. The caller
 * (`updateDockerComposeStarterUrl` in `src/index.ts`) keeps the read/write. It rewrites the
 * single line matching:
 *
 *     /^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m
 *
 * to `$1"<starterUrl>"`, and throws when nothing matched.
 */

/**
 * The regex the CLI ships. Mirrored here on purpose: this spec is the guard that the bundled
 * asset keeps a line this exact pattern can match, so it must not import the implementation's
 * copy — a change to the implementation regex has to fail here loudly, not silently agree.
 */
const CUSTOM_STARTER_URL_LINE = /^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*).+$/m;

/**
 * The real file shipped inside the published package: `libs/sdk/create-app/assets/docker-compose.yml`.
 *
 * NOTE: resolved as `../../assets` (not `../assets`) because this spec sits in `src/utils/`,
 * two levels below the package root where `assets/` lives.
 */
const BUNDLED_COMPOSE_PATH = path.resolve(__dirname, '../../assets/docker-compose.yml');

const NEW_STARTER_URL = 'https://downloads.dotcms.com/starters/my-custom-starter.zip';

/** Reads the bundled asset, failing with a diagnosable message rather than an ENOENT stack. */
function readBundledCompose(): string {
    if (!existsSync(BUNDLED_COMPOSE_PATH)) {
        throw new Error(
            `AC-012 regression guard cannot run: the bundled compose asset is MISSING at ` +
                `${BUNDLED_COMPOSE_PATH}.\n` +
                `The CLI ships its own compose file — create it (task T013) so \`--starter\` has ` +
                `something to rewrite. This test must never be skipped: without the asset there is ` +
                `nothing guarding the CUSTOM_STARTER_URL line shape that installed CLIs depend on.`
        );
    }

    return readFileSync(BUNDLED_COMPOSE_PATH, 'utf-8');
}

describe('applyStarterUrl', () => {
    describe('rewriting a conventional CUSTOM_STARTER_URL line', () => {
        it('replaces the existing value with the new starter url', () => {
            const compose = [
                'services:',
                '  dotcms:',
                '    environment:',
                "      CUSTOM_STARTER_URL: 'https://downloads.dotcms.com/starters/old.zip'",
                '      DB_BASE_URL: "jdbc:postgresql://db/dotcms"'
            ].join('\n');

            const result = applyStarterUrl(compose, NEW_STARTER_URL);

            expect(result).toContain(`CUSTOM_STARTER_URL: "${NEW_STARTER_URL}"`);
            expect(result).not.toContain('old.zip');
        });

        it('leaves every other line byte-identical', () => {
            const compose = [
                'services:',
                '  dotcms:',
                '    environment:',
                "      CUSTOM_STARTER_URL: 'https://downloads.dotcms.com/starters/old.zip'",
                '      DB_BASE_URL: "jdbc:postgresql://db/dotcms"'
            ].join('\n');

            const result = applyStarterUrl(compose, NEW_STARTER_URL).split('\n');
            const original = compose.split('\n');

            expect(result).toHaveLength(original.length);
            original.forEach((line, index) => {
                if (line.includes('CUSTOM_STARTER_URL')) {
                    expect(result[index]).not.toEqual(line);
                } else {
                    expect(result[index]).toEqual(line);
                }
            });
        });

        it('preserves the surrounding indentation exactly', () => {
            const compose = `        CUSTOM_STARTER_URL: 'https://old.example.com/starter.zip'`;

            const result = applyStarterUrl(compose, NEW_STARTER_URL);

            expect(result).toEqual(`        CUSTOM_STARTER_URL: "${NEW_STARTER_URL}"`);
        });

        it('rewrites only the first matching line, since the regex is not global', () => {
            const compose = [
                '      CUSTOM_STARTER_URL: https://first.example.com/a.zip',
                '      CUSTOM_STARTER_URL: https://second.example.com/b.zip'
            ].join('\n');

            const result = applyStarterUrl(compose, NEW_STARTER_URL).split('\n');

            expect(result[0]).toEqual(`      CUSTOM_STARTER_URL: "${NEW_STARTER_URL}"`);
            expect(result[1]).toEqual('      CUSTOM_STARTER_URL: https://second.example.com/b.zip');
        });
    });

    describe('key spellings the regex allows', () => {
        it.each([
            ['unquoted', '      CUSTOM_STARTER_URL: https://old.example.com/starter.zip'],
            ['double-quoted key', `      "CUSTOM_STARTER_URL": 'https://old.example.com/a.zip'`],
            ['single-quoted key', `      'CUSTOM_STARTER_URL': "https://old.example.com/a.zip"`],
            ['no space after colon', '      CUSTOM_STARTER_URL:https://old.example.com/a.zip'],
            ['space before colon', '      CUSTOM_STARTER_URL : https://old.example.com/a.zip'],
            ['top-level, no indentation', 'CUSTOM_STARTER_URL: https://old.example.com/a.zip']
        ])('rewrites the %s form', (_label, line) => {
            const result = applyStarterUrl(line, NEW_STARTER_URL);

            expect(result).toContain(`"${NEW_STARTER_URL}"`);
            expect(result).not.toContain('old.example.com');
        });

        it('keeps the key quoting the asset author chose', () => {
            const result = applyStarterUrl(
                `      "CUSTOM_STARTER_URL": 'https://old.example.com/a.zip'`,
                NEW_STARTER_URL
            );

            expect(result).toEqual(`      "CUSTOM_STARTER_URL": "${NEW_STARTER_URL}"`);
        });
    });

    describe('when the key is absent', () => {
        it('throws an actionable error naming CUSTOM_STARTER_URL and --starter', () => {
            const compose = [
                'services:',
                '  dotcms:',
                '    environment:',
                '      DB_BASE_URL: x'
            ].join('\n');

            expect(() => applyStarterUrl(compose, NEW_STARTER_URL)).toThrow(
                /CUSTOM_STARTER_URL entry not found/
            );
            expect(() => applyStarterUrl(compose, NEW_STARTER_URL)).toThrow(/--starter/);
        });

        it('throws on an empty compose file rather than returning it unchanged', () => {
            expect(() => applyStarterUrl('', NEW_STARTER_URL)).toThrow(
                /CUSTOM_STARTER_URL entry not found/
            );
        });

        it('throws on the `- CUSTOM_STARTER_URL=value` env list form, which the regex cannot match', () => {
            // The reformat that would silently break `--starter` for users if it ever reached
            // the bundled asset. It must fail loudly here instead.
            const compose = [
                'services:',
                '  dotcms:',
                '    environment:',
                '      - CUSTOM_STARTER_URL=https://old.example.com/starter.zip'
            ].join('\n');

            expect(() => applyStarterUrl(compose, NEW_STARTER_URL)).toThrow(
                /CUSTOM_STARTER_URL entry not found/
            );
        });
    });

    describe('AC-012 regression guard — the real bundled asset', () => {
        it('ships a CUSTOM_STARTER_URL line the rewrite regex matches, exactly once', () => {
            const compose = readBundledCompose();

            const keyLines = compose
                .split('\n')
                .filter((line) => line.includes('CUSTOM_STARTER_URL'));

            expect(keyLines).toHaveLength(1);
            expect(CUSTOM_STARTER_URL_LINE.test(compose)).toBe(true);
        });

        it('rewrites successfully when run against the asset the package ships', () => {
            const compose = readBundledCompose();

            const result = applyStarterUrl(compose, NEW_STARTER_URL);

            expect(result).not.toEqual(compose);
            expect(result).toContain(`"${NEW_STARTER_URL}"`);
        });

        it('changes the CUSTOM_STARTER_URL line and nothing else', () => {
            const compose = readBundledCompose();
            const original = compose.split('\n');

            const result = applyStarterUrl(compose, NEW_STARTER_URL).split('\n');

            expect(result).toHaveLength(original.length);

            const changed = original
                .map((line, index) => (result[index] === line ? null : index))
                .filter((index): index is number => index !== null);

            expect(changed).toHaveLength(1);
            expect(original[changed[0]]).toContain('CUSTOM_STARTER_URL');
        });

        it('preserves the asset indentation, key spelling and colon spacing', () => {
            const compose = readBundledCompose();
            const findKeyLine = (text: string) =>
                text.split('\n').find((line) => line.includes('CUSTOM_STARTER_URL')) as string;

            const originalLine = findKeyLine(compose);
            // Scoped to the single line on purpose: `\s*` matches newlines, so exec'ing the
            // multiline regex over the whole file can capture a preceding line break too.
            const prefix = (
                /^(\s*["']?CUSTOM_STARTER_URL["']?\s*:\s*)/.exec(originalLine) as RegExpExecArray
            )[1];

            const rewrittenLine = findKeyLine(applyStarterUrl(compose, NEW_STARTER_URL));

            expect(rewrittenLine).toEqual(`${prefix}"${NEW_STARTER_URL}"`);
        });

        it('leaves the rewritten file still matching the regex, so a re-run is possible', () => {
            const compose = readBundledCompose();

            const once = applyStarterUrl(compose, NEW_STARTER_URL);
            const twice = applyStarterUrl(once, 'https://downloads.dotcms.com/starters/other.zip');

            expect(once).toContain('CUSTOM_STARTER_URL');
            expect(CUSTOM_STARTER_URL_LINE.test(once)).toBe(true);
            expect(twice).toContain('"https://downloads.dotcms.com/starters/other.zip"');
            expect(twice).not.toContain(NEW_STARTER_URL);
        });

        it('keeps the value on a single line — a YAML block scalar would silently corrupt it', () => {
            // Documented hazard, and the reason the asset must keep CUSTOM_STARTER_URL on one
            // line: with a block scalar the regex matches the `>-` marker, the URL is written
            // over it, and the continuation line is orphaned into invalid YAML. No throw, no
            // warning — the user just gets the wrong starter.
            const blockScalar = [
                '      CUSTOM_STARTER_URL: >-',
                '        https://old.example.com/starter.zip'
            ].join('\n');

            const corrupted = applyStarterUrl(blockScalar, NEW_STARTER_URL);

            expect(corrupted).toContain(`CUSTOM_STARTER_URL: "${NEW_STARTER_URL}"`);
            expect(corrupted).toContain('        https://old.example.com/starter.zip');

            // The asset itself must therefore never use that shape.
            expect(readBundledCompose()).not.toMatch(/CUSTOM_STARTER_URL\s*:\s*[|>]/);
        });
    });
});
