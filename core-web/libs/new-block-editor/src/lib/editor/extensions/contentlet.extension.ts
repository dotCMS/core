import { Node, mergeAttributes } from '@tiptap/core';
import type { DOMOutputSpec } from '@tiptap/pm/model';

/** TipTap node name for embedded dotCMS contentlets (slash menu → content type → contentlet). */
export const DOT_CONTENTLET_NODE_NAME = 'dotContentlet' as const;

export const DotContentlet = Node.create({
    name: DOT_CONTENTLET_NODE_NAME,
    group: 'block',
    atom: true,
    draggable: true,

    addAttributes() {
        return {
            inode: {
                default: null,
                parseHTML: (element) => element.getAttribute('data-inode'),
                renderHTML: (attrs) =>
                    attrs.inode != null && attrs.inode !== ''
                        ? { 'data-inode': String(attrs.inode) }
                        : {}
            },
            identifier: {
                default: null,
                parseHTML: (element) => element.getAttribute('data-identifier'),
                renderHTML: (attrs) =>
                    attrs.identifier != null && attrs.identifier !== ''
                        ? { 'data-identifier': String(attrs.identifier) }
                        : {}
            },
            title: {
                default: '',
                parseHTML: (element) => element.getAttribute('data-title') ?? '',
                renderHTML: (attrs) =>
                    attrs.title != null && attrs.title !== ''
                        ? { 'data-title': String(attrs.title) }
                        : {}
            },
            contentType: {
                default: '',
                parseHTML: (element) => element.getAttribute('data-content-type') ?? '',
                renderHTML: (attrs) =>
                    attrs.contentType != null && attrs.contentType !== ''
                        ? { 'data-content-type': String(attrs.contentType) }
                        : {}
            },
            modDate: {
                default: null,
                parseHTML: (element) => element.getAttribute('data-mod-date'),
                renderHTML: (attrs) =>
                    attrs.modDate != null && attrs.modDate !== ''
                        ? { 'data-mod-date': String(attrs.modDate) }
                        : {}
            }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="dot-contentlet"]' }];
    },

    renderHTML({ node, HTMLAttributes }) {
        const { identifier, title, contentType, modDate } = node.attrs;
        const displayTitle =
            (typeof title === 'string' && title) ||
            (typeof identifier === 'string' && identifier) ||
            'Contentlet';

        const children: DOMOutputSpec[] = [
            [
                'span',
                {
                    class: 'mb-2 inline-flex max-w-full items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-medium text-indigo-800 dark:bg-indigo-900/50 dark:text-indigo-200'
                },
                String(contentType || 'Content')
            ],
            [
                'p',
                { class: 'text-base font-semibold text-gray-900 dark:text-gray-100' },
                String(displayTitle)
            ],
            [
                'p',
                { class: 'mt-1 font-mono text-xs text-gray-500 dark:text-gray-400' },
                String(identifier ?? '')
            ]
        ];

        if (modDate) {
            children.push([
                'p',
                { class: 'mt-2 text-xs text-gray-400 dark:text-gray-500' },
                `Updated ${String(modDate)}`
            ]);
        }

        return [
            'div',
            mergeAttributes(
                {
                    'data-type': 'dot-contentlet',
                    class: 'not-prose my-4 rounded-lg border border-gray-200 bg-gray-50 p-4 shadow-sm dark:border-gray-700 dark:bg-gray-900/40'
                },
                HTMLAttributes
            ),
            ...children
        ];
    }
});
