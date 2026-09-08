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

/**
 * Coerces a stored `marks` value to an array.
 *
 * `?? []` is not enough. It only catches `null` and `undefined`, so a hand-crafted `"marks": {}`
 * sails through and then dies on `.some` / spread. Stored Story Block JSON reaches this transform
 * BEFORE TipTap validates anything, and there is no server-side schema check (data-model.md), so
 * the Contentlet REST API can put any shape here.
 */
function asMarks(value: unknown): NonNullable<JSONContent['marks']> {
    return Array.isArray(value) ? (value as NonNullable<JSONContent['marks']>) : [];
}

/** Coerces a stored `attrs` value to an object. Same reasoning as {@link asMarks}. */
function asAttrs(value: unknown): Record<string, unknown> {
    return value && typeof value === 'object' && !Array.isArray(value)
        ? (value as Record<string, unknown>)
        : {};
}

/**
 * A mark's attrs, with absent and null treated alike — stored JSON is inconsistent about both.
 *
 * Default parameters do NOT cover this: they fire only on `undefined`, so an explicit
 * `"attrs": null` reached `Object.keys(null)` and threw. Hence {@link asAttrs} at every call site
 * rather than a default value.
 */
function attrsEqual(a: unknown, b: unknown): boolean {
    const left = asAttrs(a);
    const right = asAttrs(b);
    const keys = new Set([...Object.keys(left), ...Object.keys(right)]);

    for (const key of keys) {
        const a1 = left[key] ?? null;
        const b1 = right[key] ?? null;

        if (a1 === b1) {
            continue;
        }

        if (typeof a1 === 'object' && typeof b1 === 'object' && a1 && b1) {
            if (JSON.stringify(a1) !== JSON.stringify(b1)) {
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
export function marksEqual(a: unknown, b: unknown): boolean {
    const first = asMarks(a);
    const second = asMarks(b);

    if (first.length !== second.length) {
        return false;
    }

    const sort = (marks: NonNullable<JSONContent['marks']>) =>
        [...marks].sort((x, y) => (x.type ?? '').localeCompare(y.type ?? ''));

    const left = sort(first);
    const right = sort(second);

    return left.every(
        (mark, index) =>
            mark.type === right[index].type && attrsEqual(mark.attrs, right[index].attrs)
    );
}

/**
 * The HTML sibling of {@link healEmojiNodes}: rewrites rendered emoji spans to their character.
 *
 * Needed on **two** paths, which is why it lives here rather than inside the extension:
 *
 *  - `transformPastedHTML`, for content pasted between Block Editor fields;
 *  - the editor's HTML-string load branch (`normalizeEditorContent` falls through to HTML for any
 *    non-JSON string value). dotCMS does not store Story Block fields that way, but a host
 *    embedding the editor can pass HTML.
 *
 * That second path was a real defect, caught by T051 rather than reasoning: the extension renders
 * two shapes, and with `parseHTML` neutralized the `fallbackImage` one leaves a bare
 * `<img src="cdn.jsdelivr.net/…">` for `DotImage` to claim as a **dotCMS image node**. A legal
 * symbol silently became a third-party image embed — worse than the bug being fixed. Paste was
 * already covered; the load branch was not.
 *
 * Returns the input string unchanged when it holds no emoji span, so the common case allocates
 * nothing.
 */
export function healEmojiHtml(html: string, emojis: readonly EmojiItem[]): string {
    // Quote- and case-tolerant, because the SELECTOR below is. `querySelectorAll` runs after
    // parsing, so it matches `data-type='emoji'`, `DATA-TYPE="Emoji"` and whitespace-padded
    // variants — a literal `includes('data-type="emoji"')` skipped exactly those before the
    // selector ever saw them, quietly reopening the defect this function exists to close (R11).
    //
    // Kept as a fast path rather than deleted: without it every field load and every paste pays
    // for a DOMParser.
    if (!/data-type\s*=\s*['"]?emoji/i.test(html)) {
        return html;
    }

    const doc = new DOMParser().parseFromString(html, 'text/html');
    const spans = doc.querySelectorAll('span[data-type="emoji"]');

    if (!spans.length) {
        return html;
    }

    spans.forEach((span) => {
        const name = span.getAttribute('data-name') ?? '';
        const item = shortcodeToEmoji(name, emojis as EmojiItem[]);
        // Fall back to the span's own text before giving up — it usually already holds the
        // character — so an unresolvable name never blanks content. An empty string is still
        // better than leaving the `<img>` exposed.
        const character = item?.emoji || span.textContent || '';

        span.replaceWith(doc.createTextNode(character));
    });

    return doc.body.innerHTML;
}

/** True when this is a `text` node — the only thing the sandwich rule and the merge look at. */
function isText(node: JSONContent | undefined): node is JSONContent {
    return node?.type === 'text';
}

/**
 * True for an `emoji` node this heal will convert to BARE text — no marks of its own, and a
 * `name` that resolves.
 *
 * These are the only nodes the run scan below steps over. Anything else — a `hardBreak`, an
 * image, an `emoji` carrying its own marks, an `emoji` whose name does not resolve — is a
 * boundary, and since none of them is a `text` node, reaching one means the rule does not fire.
 */
function isBareConvertibleEmoji(
    node: JSONContent | undefined,
    emojis: readonly EmojiItem[]
): boolean {
    if (node?.type !== 'emoji' || asMarks(node.marks).length > 0) {
        return false;
    }

    const name = (node.attrs?.['name'] ?? '') as string;

    return Boolean(shortcodeToEmoji(name, emojis as EmojiItem[])?.emoji);
}

/**
 * Walks outward from `index` past any run of bare convertible `emoji` nodes and returns the first
 * node that is not one, in the given direction.
 *
 * The rule started out looking at IMMEDIATE siblings only, which left a common payload out:
 * an author who typed two legal marks together inside a link — `©®` — produced two adjacent bare
 * nodes, and each one's neighbour on the inner side was the other symbol rather than text. The
 * fingerprint is the same; the run length is incidental.
 */
function boundaryOf(
    nodes: JSONContent[],
    index: number,
    step: -1 | 1,
    emojis: readonly EmojiItem[]
): JSONContent | undefined {
    let cursor = index + step;

    while (isBareConvertibleEmoji(nodes[cursor], emojis)) {
        cursor += step;
    }

    return nodes[cursor];
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

        const own = asMarks(node.marks);

        if (own.length > 0) {
            return { type: 'text', marks: own, text: character };
        }

        // Look past any adjacent symbols rather than only at immediate siblings, so a run like
        // `©®` between two identical links is treated the same as a single symbol. Anything that
        // is not a bare convertible emoji ends the walk, and since none of those are `text`
        // nodes, hitting one means the rule does not fire — an image or a line break beside the
        // symbol keeps it out of the link, which is the intended behaviour.
        const previous = boundaryOf(nodes, index, -1, emojis);
        const next = boundaryOf(nodes, index, 1, emojis);

        const sandwich =
            isText(previous) &&
            isText(next) &&
            asMarks(previous.marks).some((mark) => mark.type === LINK_SANDWICH) &&
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
    // Fails CLOSED, at this function's own boundary rather than in every caller.
    //
    // `loadContent` calls this before `setContent`, so a throw here propagates out of
    // `writeValue` / the value effect, `setContent` never runs, and the field renders EMPTY over
    // intact stored JSON. That is the #37145 mechanism this whole spec is written to avoid, and
    // arriving at it through the repair would be a poor joke.
    //
    // Returning the input untouched degrades to exactly the pre-fix behaviour: the document still
    // has its `emoji` nodes and TipTap handles it as it always did. `content-match.utils.ts` fails
    // closed for the same reason.
    try {
        if (Array.isArray(content)) {
            const healed = healContent(content, emojis);

            return (healed === content ? content : healed) as T;
        }

        return healNode(content, emojis) as T;
    } catch {
        return content;
    }
}
