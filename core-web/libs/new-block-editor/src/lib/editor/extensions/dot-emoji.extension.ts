import { escapeForRegEx, InputRule, PasteRule } from '@tiptap/core';
import Emoji, {
    type EmojiItem,
    EmojiSuggestionPluginKey,
    inputRegex,
    pasteRegex,
    shortcodeToEmoji
} from '@tiptap/extension-emoji';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import Suggestion, { type SuggestionKeyDownProps, type SuggestionProps } from '@tiptap/suggestion';

import { type BlockItem, type SlashMenuService } from '../components/slash-menu/slash-menu.service';

/**
 * dotCMS emoji node — registered for BACKWARD COMPATIBILITY and shortcode resolution ONLY (#37340).
 *
 * ## Nothing here creates an `emoji` node. That is the whole point.
 *
 * The upstream extension mints them from five independent paths, and each one produces a node with
 * NO marks. A bare inline atom inside a marked run ends that run, so a single linked phrase becomes
 * `text(link) + emoji(no marks) + text(link)` — two `<a>` elements where the author created one.
 * The split is persisted into the stored Story Block JSON, so every consumer inherits it. That is a
 * WCAG 2.2 Level A failure (1.3.1, 2.4.4, 4.1.2).
 *
 * Worse, the node stores a TipTap SHORTCODE (`attrs.name`), never a character, and the table that
 * resolves one ships inside this npm dependency. No renderer, SDK or Java path can perform that
 * lookup, so the character is simply dropped downstream — and `StoryBlockUtil` counts only `"text"`
 * nodes, which is why an emoji-only Story Block field currently fails required-field validation as
 * "empty".
 *
 * The five paths, all closed below:
 *
 *   1. `addProseMirrorPlugins` → `appendTransaction` — converted ANY emoji character on ANY change.
 *      1907 of the extension's 1949 catalogued characters, including 219 that render as ordinary
 *      typography (`©`, `®`, `™`, `✔`, `⚠`, arrows). This also explains the toolbar picker: it
 *      already inserts a literal character, which this hook converted straight back.
 *   2. `addInputRules` → `:shortcode:`
 *   3. `addInputRules` → `:)` emoticons
 *   4. `addPasteRules` → pasted `:shortcode:`
 *   5. `parseHTML` — not a rule, but pasting emoji HTML copied from another Block Editor field
 *      minted a node just the same.
 *
 * ## Why the registration STAYS
 *
 * Two reasons, both required:
 *
 *   - Content already saved with `emoji` nodes must still parse. `Node.fromJSON` resolves node
 *     types from the schema; without this registration TipTap aborts and boots an EMPTY document,
 *     logging only `[tiptap warn]: Invalid content`. The field looks emptied while the stored JSON
 *     is intact, and the next save makes that loss real. That is the `aiContent` mechanism this
 *     lib's CLAUDE.md documents, and the one that blanked a whole field in #37145.
 *   - The `emojis` option is the shortcode → character table the input and paste rules read, and
 *     the one the heal (`emoji-heal.utils.ts`) uses to turn stored nodes back into text. It must
 *     stay UNFILTERED: removing an entry makes already-stored nodes of that name render as literal
 *     `:copyright:` text.
 *
 * `addAttributes` / `renderHTML` / `renderText` are inherited untouched, so a stored node still
 * resolves its character for as long as one exists.
 *
 * @see specs/37340-emoji-text-node/spec.md
 * @see specs/37340-emoji-text-node/research.md — R1 (the five paths), R3 (two paste defenses),
 *      R4 (why the double-click plugin is kept)
 */
const DotEmojiBase = Emoji.extend({
    /**
     * Path 5. Parse rules govern HTML entry points only — `Node.fromJSON` never consults them — so
     * neutralizing this closes copy-paste between fields WITHOUT weakening the registration above.
     * Asserted as a pair (AC-012 with AC-013) precisely so the two cannot drift apart.
     */
    parseHTML() {
        return [];
    },

    /**
     * Path 1, plus the paste defense `parseHTML` alone cannot provide.
     *
     * Returns only the double-click plugin — NOT an empty array. Upstream bundles two plugins here:
     * the `:` Suggestion trigger, which `editor-extensions.ts` already configured inert because
     * insertion goes through the toolbar popover, and a plugin carrying BOTH `handleDoubleClickOn`
     * and the `appendTransaction` conversion. Only the conversion is the defect.
     * `handleDoubleClickOn` is how an author selects a stored node in one gesture — still useful on
     * legacy content, and on the editorial cleanup the heal deliberately declines to do.
     */
    addProseMirrorPlugins() {
        return [
            new Plugin({
                key: new PluginKey('dotEmoji'),
                props: {
                    /**
                     * Upstream renders TWO HTML shapes, and they fail differently once `parseHTML`
                     * is neutralized. A span wrapping the character degrades gracefully — no rule
                     * matches, so the text survives. A span wrapping the `fallbackImage` does not:
                     * the span is skipped and its `<img>` is left exposed for `DotImage` to claim
                     * as a dotCMS image node pointing at `cdn.jsdelivr.net`. That is worse than the
                     * bug being fixed — a legal symbol silently becoming an external image embed.
                     *
                     * Rewriting the span to its character before ProseMirror parses handles both.
                     * Not an edge case: `isEmojiSupported()` is false under jsdom and anywhere the
                     * probe fails, so the image shape is the DEFAULT there.
                     */
                    transformPastedHTML: (html: string): string => {
                        if (!html.includes('data-type="emoji"')) {
                            return html;
                        }

                        const doc = new DOMParser().parseFromString(html, 'text/html');
                        const spans = doc.querySelectorAll('span[data-type="emoji"]');

                        if (!spans.length) {
                            return html;
                        }

                        spans.forEach((span) => {
                            const name = span.getAttribute('data-name') ?? '';
                            const item = shortcodeToEmoji(name, this.options.emojis);
                            // Fall back to the span's own text before giving up: it usually holds
                            // the character already, and an unresolvable name must never blank
                            // content.
                            const character = item?.emoji || span.textContent || '';

                            span.replaceWith(doc.createTextNode(character));
                        });

                        return doc.body.innerHTML;
                    },

                    // Double-clicking an atom does not select it by default; upstream simulates it.
                    handleDoubleClickOn: (_view, pos, node) => {
                        if (node.type !== this.type) {
                            return false;
                        }

                        this.editor.commands.setTextSelection({
                            from: pos,
                            to: pos + node.nodeSize
                        });

                        return true;
                    }
                }
            })
        ];
    },

    /**
     * Paths 2 and 3 — same author shortcuts, different output. They resolve through the `emojis`
     * table and insert the CHARACTER, so it lands in the surrounding text node and inherits its
     * marks like anything else typed. Removing the shortcuts would be a functional regression;
     * keeping the node would not fix the bug.
     */
    addInputRules() {
        const rules: InputRule[] = [];

        // `:copyright:` → ©
        rules.push(
            new InputRule({
                find: inputRegex,
                handler: ({ range, match, chain }) => {
                    const item = shortcodeToEmoji(match[1], this.options.emojis);

                    if (!item?.emoji) {
                        return;
                    }

                    chain().insertContentAt(range, item.emoji).run();
                }
            })
        );

        if (this.options.enableEmoticons) {
            const emoticons = this.options.emojis
                .flatMap((item: EmojiItem) => item.emoticons ?? [])
                .filter(Boolean);

            const find = new RegExp(
                `(?:^|\\s)(${emoticons.map((item) => escapeForRegEx(item)).join('|')}) $`
            );

            // `:) ` → 🙂
            rules.push(
                new InputRule({
                    find,
                    handler: ({ range, match, chain }) => {
                        const item = this.options.emojis.find((entry: EmojiItem) =>
                            entry.emoticons?.includes(match[1])
                        );

                        if (!item?.emoji) {
                            return;
                        }

                        // The pattern is `(?:^|\s)(:\)) $` — it matches only once the author types
                        // the trailing space, and that space has NOT been inserted yet: TipTap
                        // runs input rules from `handleTextInput`, and a rule that produces steps
                        // marks the event handled, so the default insertion never happens. That is
                        // why upstream appears to "swallow" it.
                        //
                        // So replace the emoticon alone — computed by offset, since `match[0]`
                        // also holds the leading whitespace — and re-add the space the author
                        // typed. Anything less loses a keystroke they made.
                        const offset = match[0].lastIndexOf(match[1]);
                        const from = range.from + offset;

                        chain()
                            .insertContentAt({ from, to: from + match[1].length }, `${item.emoji} `)
                            .run();
                    }
                })
            );
        }

        return rules;
    },

    /** Path 4 — pasted `:shortcode:`, same treatment as typing one. */
    addPasteRules() {
        return [
            new PasteRule({
                find: pasteRegex,
                handler: ({ range, match, chain }) => {
                    const prefix = match[1] || '';
                    const item = shortcodeToEmoji(match[2], this.options.emojis);

                    if (!item?.emoji) {
                        return;
                    }

                    chain()
                        .insertContentAt(
                            { from: range.from + prefix.length, to: range.to },
                            item.emoji,
                            { updateSelection: false }
                        )
                        .run();
                }
            })
        ];
    }
});

/**
 * How many rows the `:` menu shows.
 *
 * Five, not more: the dropdown is a fixed `w-72` and the tiered ranking below puts the row the
 * author meant in the first one or two. A longer list is more to read, not more to choose from.
 */
const SUGGESTION_LIMIT = 5;

/**
 * A slash-menu row that also carries the character to insert.
 *
 * `BlockItem` describes a BLOCK to apply, so it has no field for "the text this inserts". Rather
 * than widen that shared type for one consumer, the emoji rows carry the glyph alongside it.
 */
type EmojiBlockItem = BlockItem & { emoji: string };

/**
 * Filters the emoji table for the `:` autocomplete.
 *
 * Upstream ships **no** `items` default — it never has. The inert `items: () => []` this
 * extension used to be configured with was not disabling a working menu; it was filling in a
 * blank upstream deliberately leaves to the host, with nothing.
 *
 * Ranked in tiers, because a flat "does it match" search buries the row the author meant:
 *
 *   1. name or shortcode equals the query      — `rocket` -> `:rocket:`
 *   2. name or shortcode starts with the query — `smi`    -> `:smile:`, `:smiley:`
 *   3. a TAG starts with the query             — `rocket` also tags `:astronaut:`
 *   4. anything merely contains it
 *
 * Tiers 1 and 2 have to outrank tier 3 explicitly. An earlier draft treated a tag prefix as
 * equal to a shortcode prefix, and `:rocket:` came back with `:astronaut:` in first place —
 * because astronaut carries "rocket" as a tag and sits earlier in the table.
 */
export function filterEmojis(query: string, emojis: readonly EmojiItem[]): EmojiBlockItem[] {
    const needle = query.toLowerCase();

    // Entries with no character are the image-only ones (the `regional_indicator_*` set).
    // Offering them would insert nothing.
    const insertable = emojis.filter((item): item is EmojiItem & { emoji: string } =>
        Boolean(item.emoji)
    );

    if (!needle) {
        return insertable.slice(0, SUGGESTION_LIMIT).map(toBlockItem);
    }

    const scored: { item: EmojiItem & { emoji: string }; tier: number; index: number }[] = [];

    insertable.forEach((item, index) => {
        const names = [item.name, ...(item.shortcodes ?? [])].map((value) => value.toLowerCase());
        const tags = (item.tags ?? []).map((value) => value.toLowerCase());

        let tier: number | null = null;

        if (names.includes(needle)) {
            tier = 1;
        } else if (names.some((value) => value.startsWith(needle))) {
            tier = 2;
        } else if (tags.some((value) => value.startsWith(needle))) {
            tier = 3;
        } else if ([...names, ...tags].some((value) => value.includes(needle))) {
            tier = 4;
        }

        if (tier !== null) {
            scored.push({ item, tier, index });
        }
    });

    // Table order breaks ties, so results are stable rather than dependent on sort internals.
    scored.sort((a, b) => a.tier - b.tier || a.index - b.index);

    return scored.slice(0, SUGGESTION_LIMIT).map((entry) => toBlockItem(entry.item));
}

/**
 * Maps an emoji onto the slash menu's row shape, so the `:` menu reuses that component whole.
 *
 * `icon` renders inside a `material-symbols-outlined` span. That is a ligature font, so a glyph
 * it does not know falls through to the emoji font and draws correctly.
 */
function toBlockItem(item: EmojiItem): EmojiBlockItem {
    return {
        // `name`, not `shortcodes[0]`. They are often different and the first shortcode is
        // frequently the verbose one — `smile` ships as
        // `["grinning_face_with_closed_eyes", "smile"]`, so labelling by shortcode showed
        // `:grinning_face_with_closed_eyes:` for a query of `smi`. `name` is also the value
        // `attrs.name` would carry, so the row reads as the thing it is.
        label: `:${item.name}:`,
        description: '',
        icon: item.emoji,
        // Renders the glyph bare — no bordered box, no ligature font. See BlockItem.iconKind.
        iconKind: 'glyph',
        keywords: [item.name, ...(item.shortcodes ?? [])],
        emoji: item.emoji as string
    };
}

/**
 * Builds the emoji node extension.
 *
 * A factory rather than a const because the `:` autocomplete reuses {@link SlashMenuService} for
 * its dropdown, mirroring `createSlashCommandExtension`.
 */
export function createDotEmoji(menuService: SlashMenuService) {
    return DotEmojiBase.extend({
        addProseMirrorPlugins() {
            const base = DotEmojiBase.config.addProseMirrorPlugins?.call(this) ?? [];

            return [
                ...base,
                Suggestion<EmojiBlockItem>({
                    editor: this.editor,
                    // Reused from upstream verbatim: the trigger char, its own plugin key so the
                    // session cannot collide with the slash menu's, and the `allow` guard that
                    // keeps the menu from opening where the node could not go.
                    char: ':',
                    pluginKey: EmojiSuggestionPluginKey,
                    allow: this.options.suggestion.allow,

                    items: ({ query }) => filterEmojis(query, this.options.emojis),

                    /**
                     * Replaces upstream's `command`, which inserted an emoji NODE — the sixth
                     * creation path, and the one thing about the `:` menu that had to change.
                     * The `overrideSpace` handling is upstream's and is kept: it avoids doubling
                     * the space when the caret already sits before one.
                     */
                    command: ({ editor, range, props }) => {
                        const character = props.emoji;

                        if (!character) {
                            return;
                        }

                        const nodeAfter = editor.view.state.selection.$to.nodeAfter;
                        const overrideSpace = nodeAfter?.text?.startsWith(' ');
                        const to = overrideSpace ? range.to + 1 : range.to;

                        editor
                            .chain()
                            .focus()
                            .insertContentAt({ from: range.from, to }, `${character} `)
                            .run();
                    },

                    render: () => ({
                        onStart: (props: SuggestionProps<EmojiBlockItem>) => {
                            menuService.open(props.items, props.clientRect ?? null, props.command);
                        },
                        onUpdate: (props: SuggestionProps<EmojiBlockItem>) => {
                            menuService.update(
                                props.items,
                                props.clientRect ?? null,
                                props.command
                            );
                        },
                        onExit: (props: SuggestionProps<EmojiBlockItem>) => {
                            // Suggestion fires onExit for real exits AND for (moved && changed)
                            // while the match is still live. Only tear down once the plugin has
                            // actually deactivated — same reasoning as the slash menu.
                            const state = EmojiSuggestionPluginKey.getState(props.editor.state);

                            if (state?.active) {
                                return;
                            }

                            menuService.close();
                        },
                        onKeyDown: ({ event }: SuggestionKeyDownProps) =>
                            menuService.handleKeyDown(event)
                    })
                })
            ];
        }
    });
}
