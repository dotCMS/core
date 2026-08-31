import Link from '@tiptap/extension-link';

/**
 * dotCMS link mark — extends `@tiptap/extension-link` with `aria-label`, the only
 * SEO / accessibility attribute the base extension does not already cover. `title`,
 * `target`, and `rel` are already declared by the base via `addAttributes()` and
 * round-trip through `mergeAttributes` in `renderHTML`, so they don't need a custom
 * declaration here.
 *
 * The `aria-label` key keeps the hyphen literally (matches the HTML attribute name),
 * which means callers access it via bracket notation (`attrs['aria-label']`).
 */
export const DotLink = Link.extend({
    addAttributes() {
        return {
            ...this.parent?.(),
            'aria-label': {
                default: null,
                parseHTML: (el) => el.getAttribute('aria-label'),
                renderHTML: (attrs) =>
                    attrs['aria-label'] ? { 'aria-label': attrs['aria-label'] } : {}
            }
        };
    },

    /**
     * The base extension returns its URL paste rule unconditionally — `linkOnPaste` only
     * governs wrapping the *selection*, so pasting text that merely contains a URL still
     * created a link mark on a field where `link` is not an allowed block (#37175).
     *
     * The mark stays in the schema either way (dropping it aborts `Node.fromJSON` for the
     * whole document); what `allowedBlocks` gates is authoring, and auto-linking pasted text
     * is an authoring path. Compare `@tiptap/extension-youtube`, which guards its own paste
     * handler with `addPasteHandler`.
     */
    addPasteRules() {
        return this.options.linkOnPaste ? (this.parent?.() ?? []) : [];
    }
});
