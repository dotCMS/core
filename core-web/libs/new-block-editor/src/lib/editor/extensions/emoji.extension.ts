import { escapeForRegEx, InputRule } from '@tiptap/core';
import Emoji, { inputRegex, shortcodeToEmoji } from '@tiptap/extension-emoji';

/**
 * dotCMS emoji node — registered for BACKWARD COMPATIBILITY ONLY (#37340).
 *
 * The upstream extension runs an `appendTransaction` on every document change that scans text
 * with `emoji-regex` and replaces any match with an inline `emoji` node. Two problems:
 *
 *  1. **It is indiscriminate.** The gate is "is this character in the Unicode emoji set", which
 *     is true for 1907 of the 1949 characters it catalogues — including 219 that render as
 *     ordinary typography (`©`, `®`, `™`, `✔`, `⚠`, `→`-like arrows). Authors type these as
 *     punctuation and have no reason to expect them to become emoji.
 *  2. **It discards formatting.** The replacement uses `tr.replaceRangeWith`, which builds the
 *     node with NO marks. `tr.setStoredMarks` afterwards only affects what is typed *next*, so a
 *     mark the replaced text carried is lost at that position. A single linked phrase becomes
 *     `text(link) + emoji(no marks) + text(link)` — two `<a>` elements where the author created
 *     one. That is a WCAG 2.2 Level A failure (1.3.1, 2.4.4, 4.1.2), and because the split is
 *     persisted into the stored Story Block JSON, every consumer inherits it.
 *
 * Dropping the plugins fixes both structurally: an emoji typed into a paragraph stays a character
 * in the text node, and text nodes carry marks natively, so there is no bare node left to strip
 * them.
 *
 * **The node itself stays registered on purpose.** `addAttributes` / `parseHTML` / `renderHTML`
 * are inherited untouched, so content already saved with `emoji` nodes still parses and renders.
 * Removing the registration would silently drop those nodes on load — the same failure mode
 * documented for `aiContent` in this lib's CLAUDE.md, and the one that made #37145 blank a whole
 * field. Nothing creates new `emoji` nodes any more; old ones keep working.
 *
 * @see specs/37340-emoji-text-node/spec.md
 */
export const DotEmoji = Emoji.extend({
    /**
     * Drops both upstream plugins:
     *
     * - the `appendTransaction` character-to-node conversion (the defect above), and
     * - the `:` suggestion plugin, which `editor-extensions.ts` already configured inert
     *   (empty `items`, no-op `render`) because insertion goes through the toolbar popover.
     */
    addProseMirrorPlugins() {
        return [];
    },

    /**
     * Same author shortcuts, different output: they now insert the literal character into the
     * surrounding text node instead of creating an `emoji` node, so the shortcut survives while
     * the mark-stripping path does not.
     */
    addInputRules() {
        const rules: InputRule[] = [];

        // `:rocket:` → 🚀
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
                .flatMap((item) => item.emoticons ?? [])
                .filter(Boolean);

            const find = new RegExp(
                `(?:^|\\s)(${emoticons.map((item) => escapeForRegEx(item)).join('|')}) $`
            );

            // `:) ` → 🙂
            rules.push(
                new InputRule({
                    find,
                    handler: ({ range, match, chain }) => {
                        const item = this.options.emojis.find((entry) =>
                            entry.emoticons?.includes(match[1])
                        );

                        if (!item?.emoji) {
                            return;
                        }

                        // The pattern also captures the whitespace around the emoticon. Replace
                        // only the emoticon itself so the author's spacing survives — upstream
                        // swallowed the trailing space.
                        const offset = match[0].lastIndexOf(match[1]);
                        const from = range.from + offset;

                        chain()
                            .insertContentAt({ from, to: from + match[1].length }, item.emoji)
                            .run();
                    }
                })
            );
        }

        return rules;
    }
});
