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

    addNodeView() {
        return ({ node }) => {
            const { identifier, title, contentType, modDate } = node.attrs as Record<
                string,
                unknown
            >;

            const displayTitle =
                (typeof title === 'string' && title) ||
                (typeof identifier === 'string' && identifier) ||
                'Contentlet';

            const dom = document.createElement('div');
            dom.setAttribute('data-type', 'dot-contentlet');
            dom.classList.add(
                'not-prose',
                'my-4',
                'rounded-lg',
                'border',
                'border-gray-200',
                'bg-gray-50',
                'p-4',
                'shadow-sm',
                'dark:border-gray-700',
                'dark:bg-gray-900/40'
            );

            if (node.attrs.inode != null && node.attrs.inode !== '') {
                dom.setAttribute('data-inode', String(node.attrs.inode));
            }

            if (identifier != null && identifier !== '') {
                dom.setAttribute('data-identifier', String(identifier));
            }

            if (title != null && title !== '') {
                dom.setAttribute('data-title', String(title));
            }

            if (contentType != null && contentType !== '') {
                dom.setAttribute('data-content-type', String(contentType));
            }

            if (modDate != null && modDate !== '') {
                dom.setAttribute('data-mod-date', String(modDate));
            }

            // Content type badge
            const badge = document.createElement('span');
            badge.classList.add(
                'mb-2',
                'inline-flex',
                'max-w-full',
                'items-center',
                'rounded-full',
                'bg-indigo-100',
                'px-2.5',
                'py-0.5',
                'text-xs',
                'font-medium',
                'text-indigo-800',
                'dark:bg-indigo-900/50',
                'dark:text-indigo-200'
            );
            badge.textContent = String(contentType || 'Content');
            dom.appendChild(badge);

            // Title paragraph
            const titleEl = document.createElement('p');
            titleEl.classList.add(
                'text-base',
                'font-semibold',
                'text-gray-900',
                'dark:text-gray-100'
            );
            titleEl.textContent = displayTitle;
            dom.appendChild(titleEl);

            // Identifier paragraph
            const idEl = document.createElement('p');
            idEl.classList.add(
                'mt-1',
                'font-mono',
                'text-xs',
                'text-gray-500',
                'dark:text-gray-400'
            );
            idEl.textContent = String(identifier ?? '');
            dom.appendChild(idEl);

            // Updated date paragraph (conditional)
            if (modDate) {
                const dateEl = document.createElement('p');
                dateEl.classList.add('mt-2', 'text-xs', 'text-gray-400', 'dark:text-gray-500');
                dateEl.textContent = `Updated ${String(modDate)}`;
                dom.appendChild(dateEl);
            }

            return {
                dom,
                selectNode() {
                    dom.classList.add('is-selected');
                },
                deselectNode() {
                    dom.classList.remove('is-selected');
                }
            };
        };
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
