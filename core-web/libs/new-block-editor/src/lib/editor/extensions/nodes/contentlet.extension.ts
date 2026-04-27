import { Node, mergeAttributes } from '@tiptap/core';
import type { DOMOutputSpec } from '@tiptap/pm/model';

import type { DotCmsContentlet } from '../../services/dot-cms-contentlet.service';

/** TipTap node name for embedded dotCMS contentlets (slash menu → content type → contentlet). */
export const DOT_CONTENTLET_NODE_NAME = 'dotContent' as const;

/** Payload emitted by the editor when the user clicks "Edit contentlet". */
export interface ContentletEditEvent {
    identifier: string;
    inode: string;
    contentType: string;
    title: string;
}

/**
 * Skinny shape persisted to disk. The dotCMS backend filters all other fields on save and
 * re-hydrates the full contentlet on read, so the editor only needs to transmit the reference.
 */
export interface ContentletDataRef {
    identifier: string;
    languageId: number;
}

type ContentletData = ContentletDataRef & Partial<DotCmsContentlet>;

function buildCardDom(data: ContentletData | null): HTMLDivElement {
    const dom = document.createElement('div');
    dom.setAttribute('data-type', 'dot-content');
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

    if (!data) return dom;

    if (data.identifier) dom.setAttribute('data-identifier', String(data.identifier));
    if (data.languageId != null) dom.setAttribute('data-language-id', String(data.languageId));
    if (data.inode) dom.setAttribute('data-inode', String(data.inode));
    if (data.contentType) dom.setAttribute('data-content-type', String(data.contentType));

    const displayTitle = data.title || data.identifier || 'Contentlet';

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
    badge.textContent = data.contentType || 'Content';
    dom.appendChild(badge);

    const titleEl = document.createElement('p');
    titleEl.classList.add('text-base', 'font-semibold', 'text-gray-900', 'dark:text-gray-100');
    titleEl.textContent = displayTitle;
    dom.appendChild(titleEl);

    const idEl = document.createElement('p');
    idEl.classList.add('mt-1', 'font-mono', 'text-xs', 'text-gray-500', 'dark:text-gray-400');
    idEl.textContent = data.identifier ?? '';
    dom.appendChild(idEl);

    if (data.modDate) {
        const dateEl = document.createElement('p');
        dateEl.classList.add('mt-2', 'text-xs', 'text-gray-400', 'dark:text-gray-500');
        dateEl.textContent = `Updated ${String(data.modDate)}`;
        dom.appendChild(dateEl);
    }

    return dom;
}

export const DotContentlet = Node.create({
    name: DOT_CONTENTLET_NODE_NAME,
    group: 'block',
    atom: true,
    draggable: true,

    addAttributes() {
        return {
            data: {
                default: null as ContentletData | null,
                parseHTML: (element) => {
                    const raw = element.getAttribute('data');
                    if (raw) {
                        try {
                            return JSON.parse(raw) as ContentletData;
                        } catch {
                            return null;
                        }
                    }
                    // Legacy data-* attribute fallback for HTML-rendered output.
                    const identifier = element.getAttribute('data-identifier');
                    if (!identifier) return null;
                    const languageIdRaw = element.getAttribute('data-language-id');
                    return {
                        identifier,
                        languageId: languageIdRaw ? Number(languageIdRaw) : 1,
                        inode: element.getAttribute('data-inode') ?? undefined,
                        contentType: element.getAttribute('data-content-type') ?? undefined
                    } as ContentletData;
                },
                renderHTML: (attrs) => {
                    if (!attrs['data']) return {};
                    const skinny: ContentletDataRef = {
                        identifier: (attrs['data'] as ContentletData).identifier,
                        languageId: (attrs['data'] as ContentletData).languageId ?? 1
                    };
                    return { data: JSON.stringify(skinny) };
                }
            }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="dot-content"]' }];
    },

    addNodeView() {
        return ({ node }) => {
            let dom = buildCardDom(node.attrs['data'] as ContentletData | null);

            return {
                dom,
                update(updatedNode) {
                    if (updatedNode.type.name !== DOT_CONTENTLET_NODE_NAME) return false;
                    const next = buildCardDom(updatedNode.attrs['data'] as ContentletData | null);
                    dom.replaceWith(next);
                    dom = next;
                    return true;
                },
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
        const data = (node.attrs['data'] as ContentletData | null) ?? null;
        const displayTitle = data?.title || data?.identifier || 'Contentlet';

        const children: DOMOutputSpec[] = [
            [
                'span',
                {
                    class: 'mb-2 inline-flex max-w-full items-center rounded-full bg-indigo-100 px-2.5 py-0.5 text-xs font-medium text-indigo-800 dark:bg-indigo-900/50 dark:text-indigo-200'
                },
                String(data?.contentType || 'Content')
            ],
            [
                'p',
                { class: 'text-base font-semibold text-gray-900 dark:text-gray-100' },
                String(displayTitle)
            ],
            [
                'p',
                { class: 'mt-1 font-mono text-xs text-gray-500 dark:text-gray-400' },
                String(data?.identifier ?? '')
            ]
        ];

        if (data?.modDate) {
            children.push([
                'p',
                { class: 'mt-2 text-xs text-gray-400 dark:text-gray-500' },
                `Updated ${String(data.modDate)}`
            ]);
        }

        return [
            'div',
            mergeAttributes(
                {
                    'data-type': 'dot-content',
                    class: 'not-prose my-4 rounded-lg border border-gray-200 bg-gray-50 p-4 shadow-sm dark:border-gray-700 dark:bg-gray-900/40'
                },
                HTMLAttributes
            ),
            ...children
        ];
    }
});
