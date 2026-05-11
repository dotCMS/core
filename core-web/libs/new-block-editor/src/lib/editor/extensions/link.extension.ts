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
    }
});
