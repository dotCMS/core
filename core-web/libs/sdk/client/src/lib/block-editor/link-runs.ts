import type { BlockEditorMark, BlockEditorNode } from '@dotcms/types';

import { resolveEmoji, type EmojiWarnScope } from './emoji';

const LINK = 'link';
const EMOJI = 'emoji';

const linkMark = (node: BlockEditorNode): BlockEditorMark | undefined =>
    node.marks?.find((mark) => mark.type === LINK);

/**
 * Two `link` marks are the same link only if every attribute matches. Comparing `href` alone
 * would merge links that differ in `target`, `rel`, `title` or `aria-label` — changing where a
 * link opens, or what a screen reader announces.
 */
const sameLink = (a: BlockEditorMark | undefined, b: BlockEditorMark | undefined): boolean => {
    if (!a || !b) {
        return false;
    }

    const keys = new Set([...Object.keys(a.attrs ?? {}), ...Object.keys(b.attrs ?? {})]);

    for (const key of keys) {
        if ((a.attrs?.[key] ?? null) !== (b.attrs?.[key] ?? null)) {
            return false;
        }
    }

    return true;
};

/** An unmarked `emoji` node — the only node a link run absorbs. */
const isAbsorbableEmoji = (node: BlockEditorNode): boolean =>
    node.type === EMOJI && !node.marks?.length;

/**
 * Groups a node list into link runs so a run renders as ONE `<a>` (#37340).
 *
 * Renderers walk siblings one at a time and emit an `<a>` per text node, so stored JSON with
 * adjacent same-link text nodes produces several anchors for one logical link: duplicate tab
 * stops, duplicate screen-reader entries, fragmented announcement — WCAG 2.2 Level A failures
 * under 1.3.1, 2.4.4 and 4.1.2.
 *
 * The reported payload is harder than plain adjacency: the two linked text nodes are NOT
 * adjacent, because the mark-less `emoji` node the old editor created sits between them. A run
 * therefore continues across an intervening **unmarked `emoji` node**, which is absorbed into
 * the anchor. Only an `emoji` node — a `hardBreak` or any other atom breaks the run, so nothing
 * unrelated is ever silently pulled inside a link.
 *
 * Shared by all four JS renderers on purpose. Four copies of this logic would be four chances
 * for the same stored content to render differently per framework.
 *
 * @returns groups in document order. A group with a `link` renders as one `<a>` wrapping its
 *          nodes; a group without one renders exactly as before.
 */
export const groupLinkRuns = (
    nodes: BlockEditorNode[]
): Array<{ link?: BlockEditorMark; nodes: BlockEditorNode[] }> => {
    const groups: Array<{ link?: BlockEditorMark; nodes: BlockEditorNode[] }> = [];

    for (const node of nodes) {
        // Any node may carry the mark, not just text: applying a link OVER an existing emoji
        // node marks the emoji too (Shape 2), and that run is already correct today.
        const mark = linkMark(node);
        const previous = groups[groups.length - 1];

        // A linked text node continues the run when the link is identical — including across an
        // emoji the previous step absorbed.
        if (mark && previous?.link && sameLink(previous.link, mark)) {
            previous.nodes.push(node);
            continue;
        }

        // An unmarked emoji only joins an OPEN link run; on its own it stays a plain group so it
        // is never speculatively wrapped in an anchor.
        if (isAbsorbableEmoji(node) && previous?.link) {
            previous.nodes.push(node);
            continue;
        }

        groups.push(mark ? { link: mark, nodes: [node] } : { nodes: [node] });
    }

    // An absorbed emoji at the END of a run means the run closed on it; that is still one
    // anchor, so nothing to undo here — the emoji simply renders inside it.
    return groups;
};

/** Convenience for renderers that only need the text of an emoji node. */
export { resolveEmoji };
export type { EmojiWarnScope };
