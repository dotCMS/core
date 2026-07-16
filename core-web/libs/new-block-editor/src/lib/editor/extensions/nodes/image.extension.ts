import { mergeAttributes } from '@tiptap/core';
import Image from '@tiptap/extension-image';

/** TipTap node name for embedded dotCMS images (slash menu → image). */
export const DOT_IMAGE_NODE_NAME = 'dotImage' as const;

export interface DotImageData {
    identifier: string;
    inode: string;
    languageId: number;
    title: string;
    asset: string;
}

function appendLanguageId(src: string, languageId: number | undefined): string {
    if (!src || !languageId) return src;
    return src.includes('language_id') ? src : `${src}?language_id=${languageId}`;
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        dotImage: {
            setImageTextWrap: (value: 'left' | 'right') => ReturnType;
            setImageTextAlign: (value: 'left' | 'center' | 'right') => ReturnType;
        };
    }
}

export const DotImage = Image.extend({
    // 'dotImage' matches the old block-editor node name — existing stored content uses this key.
    name: DOT_IMAGE_NODE_NAME,

    addAttributes() {
        return {
            ...this.parent?.(),
            src: {
                default: null,
                parseHTML: (element) => element.getAttribute('src'),
                renderHTML: (attributes) => ({
                    src: appendLanguageId(
                        attributes.src || attributes.data?.asset,
                        attributes.data?.languageId
                    )
                })
            },
            alt: {
                default: null,
                parseHTML: (element) => element.getAttribute('alt'),
                renderHTML: (attributes) => ({
                    alt: attributes.alt || attributes.data?.title || null
                })
            },
            title: {
                default: null,
                parseHTML: (element) => element.getAttribute('title'),
                renderHTML: (attributes) => ({
                    title: attributes.title || attributes.data?.title || null
                })
            },
            data: {
                default: null,
                parseHTML: (element) => {
                    const raw = element.getAttribute('data');
                    if (!raw) return null;
                    try {
                        return JSON.parse(raw) as DotImageData;
                    } catch {
                        return null;
                    }
                },
                renderHTML: (attributes) =>
                    attributes.data ? { data: JSON.stringify(attributes.data) } : {}
            },
            textWrap: {
                default: null,
                // Read from the parent <figure>'s class — set by renderHTML()
                parseHTML: (element) => {
                    const figure = element.closest('figure');
                    if (!figure) return null;
                    if (figure.classList.contains('image-wrap-left')) return 'left';
                    if (figure.classList.contains('image-wrap-right')) return 'right';
                    return null;
                },
                // textWrap goes on <figure>, not on <img> — return empty object
                renderHTML: () => ({})
            },
            textAlign: {
                default: null,
                // We write `image-align-X` as a figure class. The previous block editor wrote
                // either `text-align` as an inline figure style or `textalign` as an attribute
                // on the <img> itself — both are read here so old content round-trips.
                parseHTML: (element) => {
                    const figure = element.closest('figure');
                    if (figure) {
                        if (figure.classList.contains('image-align-left')) return 'left';
                        if (figure.classList.contains('image-align-center')) return 'center';
                        if (figure.classList.contains('image-align-right')) return 'right';
                        const inline = (figure as HTMLElement).style.textAlign;
                        if (inline === 'left' || inline === 'center' || inline === 'right')
                            return inline;
                    }
                    const attr = element.getAttribute('textalign');
                    if (attr === 'left' || attr === 'center' || attr === 'right') return attr;
                    return null;
                },
                renderHTML: () => ({})
            },
            href: {
                default: null,
                parseHTML: (element) => {
                    const anchor = element.closest('a');
                    return anchor ? anchor.getAttribute('href') : null;
                },
                renderHTML: () => ({})
            },
            target: {
                default: null,
                parseHTML: (element) => {
                    const anchor = element.closest('a');
                    return anchor ? anchor.getAttribute('target') : null;
                },
                renderHTML: () => ({})
            }
        };
    },

    parseHTML() {
        return [
            // Primary: our serialized format — <figure><img src="…"></figure>
            { tag: 'figure img[src]' },
            // Fallback: bare <img> tags from other sources / old content
            { tag: this.options.allowBase64 ? 'img[src]' : 'img[src]:not([src^="data:"])' }
        ];
    },

    renderHTML({ HTMLAttributes }) {
        const { textWrap, textAlign, href, target, ...imgAttrs } = HTMLAttributes;
        const figAttrs: Record<string, string> = {};
        if (textWrap) figAttrs['class'] = `image-wrap-${textWrap}`;
        else if (textAlign) figAttrs['class'] = `image-align-${textAlign}`;

        const img = ['img', mergeAttributes(this.options.HTMLAttributes, imgAttrs)];
        // Build conditionally so a null/absent href never serializes as href="null".
        const anchorAttrs: Record<string, string> = {};
        if (href) anchorAttrs['href'] = href;
        if (target) anchorAttrs['target'] = target;
        const inner = href ? ['a', anchorAttrs, img] : img;

        return ['figure', figAttrs, inner];
    },

    addNodeView() {
        return ({ node, getPos, editor }) => {
            const figure = document.createElement('figure');
            const img = document.createElement('img');

            // Layout attrs go on <figure>, not on <img>. Pull them off before applying the rest.
            const { textWrap, textAlign, href, target, ...imgAttrs } = node.attrs as Record<
                string,
                unknown
            >;
            Object.entries(imgAttrs).forEach(([key, value]) => {
                if (value == null) return;
                img.setAttribute(
                    key,
                    typeof value === 'object' ? JSON.stringify(value) : String(value)
                );
            });

            // Wrap and align are mutually exclusive on the figure class
            if (textWrap) figure.className = `image-wrap-${textWrap}`;
            else if (textAlign) figure.className = `image-align-${textAlign}`;

            if (href) {
                const a = document.createElement('a');
                a.setAttribute('href', String(href));
                if (target) a.setAttribute('target', String(target));
                a.appendChild(img);
                figure.appendChild(a);

                // The wrapping <a> makes the browser treat clicks as link interactions, so
                // ProseMirror won't cleanly node-select the image (it took several clicks to
                // activate the image + its toolbar options). Select the node ourselves on the
                // first mousedown and suppress the anchor's default behaviour (#36361).
                figure.addEventListener('mousedown', (event) => {
                    event.preventDefault();
                    if (typeof getPos === 'function') {
                        editor.commands.setNodeSelection(getPos());
                    }
                });
            } else {
                figure.appendChild(img);
            }

            return {
                dom: figure,
                selectNode() {
                    figure.classList.add('is-selected');
                },
                deselectNode() {
                    figure.classList.remove('is-selected');
                }
            };
        };
    },

    addCommands() {
        return {
            ...this.parent?.(),
            setImageTextWrap:
                (value) =>
                ({ commands, editor }) => {
                    const current = editor.getAttributes(DOT_IMAGE_NODE_NAME).textWrap;
                    // Toggle: clicking the same direction again clears it.
                    // Also clear textAlign — wrap and align are mutually exclusive.
                    return commands.updateAttributes(DOT_IMAGE_NODE_NAME, {
                        textWrap: current === value ? null : value,
                        textAlign: null
                    });
                },
            setImageTextAlign:
                (value) =>
                ({ commands }) =>
                    commands.updateAttributes(DOT_IMAGE_NODE_NAME, {
                        textAlign: value,
                        textWrap: null
                    })
        };
    }
});
