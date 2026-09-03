/**
 * Generates the emoji shortcode → character map consumed by the Story Block renderers (#37340).
 *
 * Stored `emoji` nodes hold only a shortcode (`{"type":"emoji","attrs":{"name":"copyright"}}`) —
 * no literal character, no codepoint. Every renderer therefore needs a lookup table to turn
 * `copyright` back into `©`, and before this there was none: no emoji support anywhere in
 * `dotCMS/src/main/java`, and no table in any SDK.
 *
 * The source of truth is `@tiptap/extension-emoji`'s own `emojis` list — the exact list the
 * editor used — so the map cannot describe a character the editor could not have produced, and
 * it cannot drift as the extension is upgraded. CI regenerates and fails on any diff.
 *
 * Two outputs rather than one shared artifact:
 *
 *  - the JS SDKs import a JSON module from `@dotcms/client`, which react, vue and angular all
 *    already depend on;
 *  - Java reads a classpath resource from `dotCMS/src/main/resources`, which keeps a Java-only
 *    build (`./mvnw install -pl :dotcms-core`) working without the frontend toolchain.
 *
 * `core-web` builds before `dotCMS` in the Maven reactor, so a full build regenerates both
 * before the Java module compiles. Both files are committed so neither build depends on the
 * other having run.
 *
 * Usage: node tools/scripts/generate-emoji-map.mjs [--check]
 *   --check  exit non-zero if a committed file is stale (used by CI)
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

import { emojis } from '@tiptap/extension-emoji';

const HERE = dirname(fileURLToPath(import.meta.url));
const CORE_WEB = resolve(HERE, '../..');
const REPO = resolve(CORE_WEB, '..');

/**
 * The JS side gets a `.ts` module rather than JSON: importing JSON would require
 * `resolveJsonModule` across every consuming tsconfig, and a plain module tree-shakes and type-
 * checks without any build configuration. Java gets JSON, which it can read as a classpath
 * resource with no parser of its own.
 */
const TS_TARGET = resolve(CORE_WEB, 'libs/sdk/client/src/lib/block-editor/emoji-map.ts');
const JSON_TARGET = resolve(REPO, 'dotCMS/src/main/resources/emoji/emoji-shortcodes.json');

const build = () => {
    const map = {};

    for (const item of emojis) {
        // Entries without a literal character are custom/image-only emoji; a renderer cannot
        // resolve them to text, so they fall through to the `:name:` path by design.
        if (item?.name && item?.emoji) {
            map[item.name] = item.emoji;
        }
    }

    // Sorted so the committed artifact is stable and its diffs stay reviewable.
    return Object.fromEntries(Object.entries(map).sort(([a], [b]) => a.localeCompare(b)));
};

const asJson = (map) => `${JSON.stringify(map, null, 4)}\n`;

const asModule = (map) =>
    `/**\n` +
    ` * GENERATED FILE — DO NOT EDIT.\n` +
    ` *\n` +
    ` * Emoji shortcode to character map, generated from \`@tiptap/extension-emoji\`'s own list by\n` +
    ` * \`tools/scripts/generate-emoji-map.mjs\`. Regenerate with \`pnpm generate:emoji-map\`; CI fails\n` +
    ` * if this file drifts from the extension (#37340).\n` +
    ` */\n` +
    `export const EMOJI_MAP: Record<string, string> = ${JSON.stringify(map, null, 4)};\n`;

const check = process.argv.includes('--check');
const map = build();
const outputs = [
    [TS_TARGET, asModule(map)],
    [JSON_TARGET, asJson(map)]
];

let stale = false;

for (const [target, content] of outputs) {
    if (check) {
        let current = null;

        try {
            current = readFileSync(target, 'utf8');
        } catch {
            current = null;
        }

        if (current !== content) {
            stale = true;
            console.error(`✗ stale or missing: ${target}`);
        }

        continue;
    }

    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(target, content, 'utf8');
    console.log(`✓ wrote ${Object.keys(map).length} entries → ${target}`);
}

if (check && stale) {
    console.error(
        '\nThe emoji map is out of date. Run `pnpm generate:emoji-map` and commit the result.'
    );
    process.exit(1);
}

if (check) {
    console.log('✓ emoji map is up to date');
}
