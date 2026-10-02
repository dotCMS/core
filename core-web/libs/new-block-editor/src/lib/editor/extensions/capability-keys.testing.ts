import { readdirSync, readFileSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

import { getEditorBlockOptions } from '@dotcms/block-editor';

import {
    createBaseBlockItems,
    createSlashAiBlockItems,
    createSlashOverlayBlockItems
} from '../components/slash-menu/slash-menu-catalog';

/**
 * Test support for the Allowed Blocks capability-key invariants (I1 / I2).
 *
 * See `specs/37601-allowed-blocks-capability-contract/contracts/capability-key-registry.md`.
 * This file is test support, not production code — nothing here ships in the bundle.
 */

/**
 * Keys that are deliberately consulted without being producible by the Settings tab.
 *
 * `paragraph` is filtered out of `getEditorBlockOptions()` on purpose (#29772 — a field must
 * never be able to exclude it) and is already excused by a hardcoded check in the slash menu:
 * `slash-menu.service.ts` -> `item.blockName === 'paragraph' ||`. The invariant mirrors that
 * decision rather than reporting it.
 *
 * An exemption is a recorded decision. It is never a way to silence a failing invariant: if a
 * key shows up here to make the build green, the contract has become decorative.
 */
export const EXEMPT_KEYS: readonly string[] = ['paragraph'];

const EDITOR_ROOT = join(dirname(fileURLToPath(import.meta.url)), '..');

/**
 * Every file under `editor/` that could hold a gate literal — walked, not listed.
 *
 * An earlier draft kept a fixed array of four paths. That made the invariant opt-in: a gate
 * introduced in a *new* file stayed invisible until someone remembered to extend the list, which
 * is the same "nothing enforces it" failure this whole contract exists to end. The list also got
 * it wrong on the first try — `editor-extensions.ts` was missing, and the Red run only caught it
 * via I2, where `dotContent` looked like an orphan because its only consumer was unscanned.
 *
 * `editor-extensions.ts` is the consequential one: its gates decide which TipTap extensions are
 * registered at all, so a bad key there does not merely hide a button — it makes the editor unable
 * to parse content that uses the node.
 *
 * Specs and test support are excluded: an `isAllowed` stub in a fixture is not a gate. The
 * structural half below covers the slash-menu catalog and needs no file walking.
 *
 * @returns absolute paths, so callers read them directly and report them via `relative()`.
 */
export function scannedFiles(dir: string = EDITOR_ROOT): string[] {
    return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
        const full = join(dir, entry.name);

        if (entry.isDirectory()) {
            return scannedFiles(full);
        }

        const isScannable = /\.(ts|html)$/.test(entry.name);
        const isTestCode = /\.(spec|testing)\.ts$/.test(entry.name);

        return isScannable && !isTestCode ? [full] : [];
    });
}

/**
 * Every capability key the Settings tab can write into a field's `allowedBlocks`.
 *
 * Imported from the single producer via the public export of `@dotcms/block-editor`
 * (`src/public-api.ts`). Never copy this list into a fixture — a copy cannot detect drift in the
 * thing it was copied from, which is the entire purpose of I1.
 *
 * Note the field is `code`, not `id`: `getEditorBlockOptions()` maps
 * `({ label, id }) => ({ label, code: id })`. Reading `.id` yields `undefined` for every entry
 * and makes the invariant pass vacuously.
 */
export function producibleOptionCodes(): Set<string> {
    return new Set(getEditorBlockOptions().map(({ code }) => code));
}

/** Minimal stub: the catalog factories only need `get()` to build their labels. */
const messageServiceStub = { get: (key: string) => key } as never;

/**
 * Capability keys consulted structurally, by reading `blockName` off the slash-menu catalog.
 *
 * Preferred over text scanning wherever the keys are reachable as data: it survives reformatting
 * and renames, and it is where the `aiContent` / `aiImage` mismatch lives.
 *
 * Two factories are deliberately excluded:
 *   - `createSlashRemoteBlockItems()` — its items are built from customer-supplied remote actions
 *     at runtime, so their names are not ours to constrain.
 *   - `createContentTypeItem()` — it needs six injected services, and its only key, `dotContent`,
 *     is already covered by the `has('dotContent')` gate in `editor-extensions.ts`. Stubbing six
 *     services to re-derive a key we already see would add noise, not coverage.
 */
export function consumedKeysFromCatalog(): Set<string> {
    const items = [
        ...createBaseBlockItems(messageServiceStub),
        ...createSlashOverlayBlockItems({} as never, {} as never, messageServiceStub),
        ...createSlashAiBlockItems({} as never, messageServiceStub)
    ];

    return new Set(
        items.map(({ blockName }) => blockName).filter((name): name is string => !!name)
    );
}

/**
 * Matches a capability gate and nothing else.
 *
 * Anchored to `isAllowed(` / `has(` because several same-named strings in these files are NOT
 * gates and must never be reported: `popovers.isOpen('link')`, `popovers.toggle('emoji')` and
 * `m.type.name === 'link'`. `link` and `emoji` were correctly ungated by #37539 and #37442 — if
 * either appears in the result, this regex is wrong. Fix the regex, never the production code.
 *
 * All three quote styles are accepted, with a backreference so the closing quote matches the
 * opening one. An earlier version took `'…'` only, which would have let `isAllowed("youtube")`
 * through unseen — a hole of exactly the kind this invariant exists to close. Backticks are not
 * hypothetical: `editor-extensions.ts` and `toolbar.component.ts` both gate on
 * `` `heading${level}` ``. Those stay unmatched on purpose — the key class below excludes `$`
 * and `{`, so an interpolated template literal is not a static key and cannot be checked here.
 */
const GATE_CALL = /\b(?:isAllowed|has)\(\s*(['"`])([A-Za-z][A-Za-z0-9]*)\1\s*\)/g;

/**
 * Strips comments so prose about a gate is never mistaken for one.
 *
 * This is load-bearing, not tidiness. The files that were fixed carry long comments explaining
 * *why* a capability is no longer gated, and those comments quote the removed call verbatim —
 * `toolbar.component.html:377` says "`isAllowed('link')` was true ONLY on a field with no
 * restriction", and `editor-extensions.ts:134,182` do the same for `link` and `emoji`. Scanning
 * raw text reports those as live gates: the invariant would then demand that someone "fix"
 * capabilities that are already correct, and the obvious way to silence it is to delete the
 * explanation of why the bug happened. Exactly backwards.
 *
 * Order matters: block comments first, then line comments, so a `//` inside a block comment
 * cannot leave a dangling fragment.
 */
function stripComments(source: string): string {
    return source
        .replace(/<!--[\s\S]*?-->/g, ' ')
        .replace(/\/\*[\s\S]*?\*\//g, ' ')
        .replace(/(^|[^:])\/\/[^\n]*/g, '$1 ');
}

/**
 * Capability keys consulted as string literals in component code and templates.
 *
 * Reading source as text in a unit test is blunt on purpose. It is the only technique that sees a
 * literal inside an Angular `@if`, and unlike rendering each component against a restricted store
 * it cannot be satisfied by adjusting a fixture — it fails loudly and names the file.
 *
 * @returns each key with the files it was found in, so a failure is actionable.
 */
export function consumedKeysFromSource(
    files: readonly string[] = scannedFiles()
): Map<string, string[]> {
    const found = new Map<string, string[]>();

    for (const file of files) {
        const source = stripComments(readFileSync(file, 'utf-8'));

        for (const [, , key] of source.matchAll(GATE_CALL)) {
            found.set(key, [...(found.get(key) ?? []), relative(EDITOR_ROOT, file)]);
        }
    }

    return found;
}
