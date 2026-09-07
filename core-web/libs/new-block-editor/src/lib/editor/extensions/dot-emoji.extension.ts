import { escapeForRegEx, InputRule, PasteRule } from '@tiptap/core';
import Emoji, {
    type EmojiItem,
    inputRegex,
    pasteRegex,
    shortcodeToEmoji
} from '@tiptap/extension-emoji';
import { Plugin, PluginKey } from '@tiptap/pm/state';

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
export const DotEmoji = Emoji.extend({
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
