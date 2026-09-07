import { type JSONContent } from '@tiptap/core';
import { type EmojiItem, shortcodeToEmoji } from '@tiptap/extension-emoji';

/**
 * Turns stored `emoji` nodes back into text, on the editor's parse path (#37340).
 *
 * The Block Editor no longer creates `emoji` nodes (`dot-emoji.extension.ts`), but customers have
 * them in stored content, and the node is the wrong shape for stored content: it holds a TipTap
 * SHORTCODE rather than a character, and the table that resolves one ships inside an npm dependency
 * of the editor. Nothing downstream can perform that lookup — VTL drops the character, the SDKs
 * render an unknown block, `StoryBlockUtil` counts only `"text"` nodes so an emoji-only field fails
 * required-field validation as "empty", and Elasticsearch never indexes it.
 *
 * Converting the node to text fixes all of those at once, with no change anywhere but here.
 *
 * ## The rule
 *
 * `emoji(marks) → text(same marks, character)`. **Marks are preserved, never invented.** A
 * transform that guessed at marks could alter documents that never had this defect, silently and
 * irreversibly.
 *
 * There is exactly ONE exception, and it is the only inference in this change: {@link LINK_SANDWICH}.
 *
 * ## Why this merges, when the earlier design said it would not
 *
 * An earlier draft assumed ProseMirror joined adjacent identical-mark text nodes during
 * normalization. It does not, on any load path: `Fragment.fromJSON` is
 * `new Fragment(value.map(schema.nodeFromJSON))` and never reaches `Fragment.fromArray`, which is
 * where joining lives. The assumption survived three drafts because the EDITOR renders a mark run
 * as one element however many nodes span it — the document and the DOM disagreed, and only the DOM
 * had been looked at.
 *
 * It matters because the stored JSON is what VTL and the SDKs consume, and those emit one `<a>` per
 * text node. Leaving the run unmerged would store three adjacent `text(link)` nodes and render
 * THREE anchors where the defect currently produces two — worse than the bug.
 *
 * So the merge is ours. It is not a second inference: the marks are already identical, so
 * concatenating decides nothing. See research.md R10.
 *
 * @see specs/37340-emoji-text-node/spec.md — AC-013 through AC-019, AC-021
 */

/**
 * THE LINK SANDWICH — the single inference this transform permits.
 *
 * A **bare** `emoji` node between two `text` nodes carrying **identical** mark sets that include a
 * `link` is a fingerprint of this defect rather than an editorial choice:
 *
 *   - applying a link *over* an existing node marks the node too, so an author who links across a
 *     symbol produces a MARKED node, not a bare one;
 *   - after the fix no typed symbol becomes a node at all, so the shape cannot be newly created.
 *
 * The one construction that still reaches it — linking two runs separately *around* an
 * already-converted symbol — requires having hit this defect first, so it lives in the same legacy
 * population being repaired. And unlike a general mark rewrite, a wrong merge here is visible (the
 * underline extends over the symbol) and reversible (select, remove the link).
 *
 * Honouring it closes #37340's "single link without a re-save" criterion instead of amending it.
 *
 * Requiring the FULL mark sets to match, rather than only the `link` marks, is deliberately
 * narrower than the acceptance criterion demands: it can only fire less often, and it keeps the
 * merge below coherent — a healed node whose marks differed from its neighbours' would not merge,
 * leaving the run split anyway.
 */
const LINK_SANDWICH = 'link';

/** A mark's attrs, with absent and null treated alike — stored JSON is inconsistent about both. */
function attrsEqual(a: Record<string, unknown> = {}, b: Record<string, unknown> = {}): boolean {
    const keys = new Set([...Object.keys(a), ...Object.keys(b)]);

    for (const key of keys) {
        const left = a[key] ?? null;
        const right = b[key] ?? null;

        if (left === right) {
            continue;
        }

        if (typeof left === 'object' && typeof right === 'object' && left && right) {
            if (JSON.stringify(left) !== JSON.stringify(right)) {
                return false;
            }

            continue;
        }

        return false;
    }

    return true;
}

/**
 * Deep equality over two mark lists.
 *
 * Sorted by type first: ProseMirror keeps marks in schema order, but stored JSON can arrive from
 * the REST API in any order, and two runs that differ only in mark ORDER are the same formatting.
 */
export function marksEqual(a: JSONContent['marks'] = [], b: JSONContent['marks'] = []): boolean {
    if (a.length !== b.length) {
        return false;
    }

    const sort = (marks: NonNullable<JSONContent['marks']>) =>
        [...marks].sort((x, y) => (x.type ?? '').localeCompare(y.type ?? ''));

    const left = sort(a);
    const right = sort(b);

    return left.every(
        (mark, index) =>
            mark.type === right[index].type &&
            attrsEqual(
                mark.attrs as Record<string, unknown>,
                right[index].attrs as Record<string, unknown>
            )
    );
}

/** True when this is a `text` node — the only thing the sandwich rule and the merge look at. */
function isText(node: JSONContent | undefined): node is JSONContent {
    return node?.type === 'text';
}

/**
 * Merges adjacent `text` nodes whose marks are deep-equal.
 *
 * Only ever called on an inline array the heal actually modified. A document-wide pass would
 * normalize identical-mark runs unrelated to this defect and break the identity guarantee (AC-019).
 */
function mergeAdjacentText(nodes: JSONContent[]): JSONContent[] {
    return nodes.reduce<JSONContent[]>((acc, node) => {
        const previous = acc[acc.length - 1];

        if (isText(previous) && isText(node) && marksEqual(previous.marks, node.marks)) {
            acc[acc.length - 1] = { ...previous, text: `${previous.text ?? ''}${node.text ?? ''}` };

            return acc;
        }

        acc.push(node);

        return acc;
    }, []);
}

/**
 * Heals one inline array. Returns the original reference when it holds no resolvable `emoji` node,
 * so an untouched array is never rewritten and never merged.
 */
function healInline(nodes: JSONContent[], emojis: readonly EmojiItem[]): JSONContent[] {
    let changed = false;

    const healed = nodes.map((node, index) => {
        if (node.type !== 'emoji') {
            return node;
        }

        const name = (node.attrs?.['name'] ?? '') as string;
        const character = shortcodeToEmoji(name, emojis as EmojiItem[])?.emoji;

        // An unresolvable name is left exactly as it is — never blanked, never a literal `:name:`.
        // Unreachable from the shipped table, but reachable through hand-crafted JSON via the
        // Contentlet REST API, since no Story Block schema validation exists server-side.
        if (!character) {
            return node;
        }

        changed = true;

        const own = node.marks ?? [];

        if (own.length > 0) {
            return { type: 'text', marks: own, text: character };
        }

        const previous = nodes[index - 1];
        const next = nodes[index + 1];

        const sandwich =
            isText(previous) &&
            isText(next) &&
            (previous.marks ?? []).some((mark) => mark.type === LINK_SANDWICH) &&
            marksEqual(previous.marks, next.marks);

        return sandwich
            ? { type: 'text', marks: previous.marks, text: character }
            : { type: 'text', text: character };
    });

    return changed ? mergeAdjacentText(healed) : nodes;
}

/** Recurses into a node's children, preserving identity when nothing beneath it changed. */
function healNode(node: JSONContent, emojis: readonly EmojiItem[]): JSONContent {
    if (!Array.isArray(node.content)) {
        return node;
    }

    const healedChildren = healContent(node.content, emojis);

    return healedChildren === node.content ? node : { ...node, content: healedChildren };
}

/** Heals an array of nodes at any depth. Identity-preserving when nothing changed. */
function healContent(nodes: JSONContent[], emojis: readonly EmojiItem[]): JSONContent[] {
    const descended = nodes.map((node) => healNode(node, emojis));
    const inlineHealed = healInline(descended, emojis);

    if (inlineHealed !== descended) {
        return inlineHealed;
    }

    return descended.every((node, index) => node === nodes[index]) ? nodes : descended;
}

/**
 * Heals every `emoji` node in a Block Editor value.
 *
 * Accepts BOTH shapes `normalizeEditorContent` can produce: a `{ type: 'doc' }` object, and the
 * bare node array some hosts pass — notably the UVE side panel. Spreading an array into an object
 * drops the document type and makes TipTap throw `RangeError: Unknown node type: undefined`, which
 * is how #37145 blanked a field for ALL content. The branch is not cosmetic.
 *
 * Returns the input reference unchanged when there is nothing to heal — the overwhelmingly common
 * case, and the one AC-019 asserts.
 */
export function healEmojiNodes<T extends JSONContent | JSONContent[]>(
    content: T,
    emojis: readonly EmojiItem[]
): T {
    if (Array.isArray(content)) {
        const healed = healContent(content, emojis);

        return (healed === content ? content : healed) as T;
    }

    return healNode(content, emojis) as T;
}
