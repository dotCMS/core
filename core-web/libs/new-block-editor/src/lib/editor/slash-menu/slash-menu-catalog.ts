import { firstValueFrom } from 'rxjs';

import type { Editor } from '@tiptap/core';
import { SuggestionPluginKey } from '@tiptap/suggestion';

import { DOT_CONTENTLET_NODE_NAME } from '../extensions/contentlet.extension';

import type { BlockItem } from './slash-menu.types';
import type { DotCmsContentTypeService } from '../services/dot-cms-content-type.service';
import type { DotCmsContentletService } from '../services/dot-cms-contentlet.service';
import type { EditorDialogManagerService } from '../services/editor-dialog-manager.service';

// Narrow interface so the catalog doesn't import the full service class
interface SlashMenuSubMenuHost {
    openSubmenu(): void;
    setItems(items: BlockItem[], commandFn: (item: BlockItem) => void): void;
    close(): void;
}

function clearActiveSuggestionRange(editor: Editor): void {
    const match = SuggestionPluginKey.getState(editor.state);
    if (match?.active) {
        editor.chain().focus().deleteRange(match.range).run();
    }
}

export function createContentTypeItem(
    menuService: SlashMenuSubMenuHost,
    contentTypeService: DotCmsContentTypeService,
    contentletService: DotCmsContentletService,
    getLanguageId: () => number
): BlockItem {
    return {
        label: 'Content type',
        description: 'Insert a dotCMS content type',
        icon: '⬡',
        keywords: ['content', 'type', 'dotcms', 'contenttype', 'model'],
        blockName: 'contentlet',
        keepRange: true,
        onSelect: (editor, range) => {
            // keepRange=true: deleteRange was skipped, suggestion session stays alive.
            menuService.openSubmenu();

            // Delete the query text (e.g. "content") but keep the "/" so Tiptap's
            // suggestion resets to an empty query. The user can then type to filter
            // content types. range.from is the position of "/", range.from+1 onwards
            // is the query text.
            if (range && range.from + 1 < range.to) {
                editor
                    .chain()
                    .deleteRange({ from: range.from + 1, to: range.to })
                    .run();
            }

            firstValueFrom(contentTypeService.fetchAll())
                .then((types) => {
                    const resolvedTypes = types ?? [];
                    // Content type items are plain display items — drill-down logic lives in the
                    // commandFn below (closure over editor and services).
                    const typeItems: BlockItem[] =
                        resolvedTypes.length > 0
                            ? resolvedTypes.map((ct) => ({
                                  label: ct.name,
                                  description: ct.description || ct.variable,
                                  icon: ct.icon || '⬡',
                                  keywords: [ct.variable, ct.baseType.toLowerCase()]
                              }))
                            : [
                                  {
                                      label: 'No content types found',
                                      description:
                                          'No types returned from the API. Check permissions or configuration.',
                                      keywords: ['no', 'empty', 'content', 'types'],
                                      isEmptyState: true
                                  }
                              ];

                    menuService.setItems(typeItems, (selectedItem) => {
                        if (selectedItem.isEmptyState) {
                            clearActiveSuggestionRange(editor);
                            menuService.close();
                            return;
                        }

                        menuService.openSubmenu();

                        const slashMatch = SuggestionPluginKey.getState(editor.state);
                        if (slashMatch?.active && slashMatch.range.from + 1 < slashMatch.range.to) {
                            editor
                                .chain()
                                .deleteRange({
                                    from: slashMatch.range.from + 1,
                                    to: slashMatch.range.to
                                })
                                .run();
                        }

                        // keywords[0] is ct.variable (stored above)
                        const ctVariable = selectedItem.keywords[0];

                        firstValueFrom(contentletService.fetchByType(ctVariable, getLanguageId()))
                            .then((contentlets) => {
                                const resolvedContentlets = contentlets ?? [];
                                const contentletItems: BlockItem[] = resolvedContentlets.map(
                                    (cl) => ({
                                        label: cl.title || cl.identifier,
                                        description: cl.contentType,
                                        icon: '◈',
                                        keywords: [cl.contentType, cl.identifier],
                                        onSelect: (ed) => {
                                            const match = SuggestionPluginKey.getState(ed.state);
                                            const chain = ed.chain().focus();
                                            if (match?.active) {
                                                chain.deleteRange(match.range);
                                            }
                                            chain
                                                .insertContent({
                                                    type: DOT_CONTENTLET_NODE_NAME,
                                                    attrs: {
                                                        inode: cl.inode,
                                                        identifier: cl.identifier,
                                                        title: cl.title ?? '',
                                                        contentType: cl.contentType ?? '',
                                                        modDate: cl.modDate ?? null
                                                    }
                                                })
                                                .run();
                                        }
                                    })
                                );

                                const finalItems: BlockItem[] =
                                    contentletItems.length === 0
                                        ? [
                                              {
                                                  label: 'No contentlets found',
                                                  description: `No ${selectedItem.label} contentlets available`,
                                                  icon: '',
                                                  keywords: ['no', 'empty', 'contentlets'],
                                                  isEmptyState: true
                                              }
                                          ]
                                        : contentletItems;

                                menuService.setItems(finalItems, (contentletItem) => {
                                    if (contentletItem.onSelect) {
                                        contentletItem.onSelect(editor);
                                    } else {
                                        clearActiveSuggestionRange(editor);
                                    }
                                    menuService.close();
                                });
                            })
                            .catch(() => {
                                menuService.setItems(
                                    [
                                        {
                                            label: 'Could not load contentlets',
                                            description:
                                                'The request failed. Check your connection and try again.',
                                            icon: '',
                                            keywords: ['error', 'contentlets'],
                                            isEmptyState: true
                                        }
                                    ],
                                    (contentletItem) => {
                                        if (!contentletItem.onSelect) {
                                            clearActiveSuggestionRange(editor);
                                        }
                                        menuService.close();
                                    }
                                );
                            });
                    });
                })
                .catch(() => {
                    menuService.setItems(
                        [
                            {
                                label: 'Could not load content types',
                                description:
                                    'The request failed. Check your connection and API token.',
                                icon: '',
                                keywords: ['error', 'content', 'types'],
                                isEmptyState: true
                            }
                        ],
                        (item) => {
                            if (item.isEmptyState) {
                                clearActiveSuggestionRange(editor);
                            }
                            menuService.close();
                        }
                    );
                });
        }
    };
}

export const ALL_ITEMS: BlockItem[] = [
    {
        label: 'Text',
        description: 'Plain text paragraph',
        icon: 'P',
        keywords: ['paragraph', 'text'],
        blockName: 'paragraph',
        apply: (c) => c.setParagraph()
    },
    {
        label: 'Heading 1',
        description: 'Top-level title or page heading',
        icon: 'H1',
        keywords: ['h1', 'heading', 'title'],
        blockName: 'heading',
        apply: (c) => c.setHeading({ level: 1 })
    },
    {
        label: 'Heading 2',
        description: 'Section heading',
        icon: 'H2',
        keywords: ['h2', 'heading', 'subtitle'],
        blockName: 'heading',
        apply: (c) => c.setHeading({ level: 2 })
    },
    {
        label: 'Heading 3',
        description: 'Subsection heading',
        icon: 'H3',
        keywords: ['h3', 'heading'],
        blockName: 'heading',
        apply: (c) => c.setHeading({ level: 3 })
    },
    {
        label: 'Bullet List',
        description: 'Unordered list of items',
        icon: '•',
        keywords: ['ul', 'list', 'bullets'],
        blockName: 'bulletList',
        apply: (c) => c.toggleBulletList()
    },
    {
        label: 'Ordered List',
        description: 'Numbered list of steps or items',
        icon: '1.',
        keywords: ['ol', 'numbered', 'list'],
        blockName: 'orderedList',
        apply: (c) => c.toggleOrderedList()
    },
    {
        label: 'Blockquote',
        description: 'Highlighted quote or callout',
        icon: '"',
        keywords: ['quote', 'callout', 'cite'],
        blockName: 'blockquote',
        apply: (c) => c.toggleBlockquote()
    },
    {
        label: 'Code Block',
        description: 'Code snippet with syntax highlighting',
        icon: '</>',
        keywords: ['code', 'pre', 'snippet'],
        blockName: 'codeBlock',
        apply: (c) => c.setCodeBlock()
    },
    {
        label: 'Grid (2 columns)',
        description: 'Two-column layout',
        icon: 'view_column',
        keywords: ['grid', 'columns', 'layout', 'two-column'],
        blockName: 'gridBlock',
        apply: (c) => c.insertGrid()
    }
];

/** Slash entries that open a floating dialog before mutating the document. */
export function createSlashDialogBlockItems(
    dialogManager: EditorDialogManagerService
): BlockItem[] {
    return [
        {
            label: 'Table',
            description: 'Organize data in rows and columns',
            icon: '⊞',
            keywords: ['table', 'grid', 'spreadsheet', 'rows', 'columns'],
            blockName: 'table',
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                dialogManager.open('table', () => rect);
            }
        },
        {
            label: 'Image',
            description: 'Add a photo or graphic',
            icon: '🖼',
            keywords: ['image', 'photo', 'picture', 'upload', 'url'],
            blockName: 'image',
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                dialogManager.openImage(() => rect);
            }
        },
        {
            label: 'Video',
            description: 'Embed a video from a link or file',
            icon: '▶',
            keywords: ['video', 'mp4', 'upload', 'url', 'media'],
            blockName: 'video',
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                dialogManager.open('video', () => rect);
            }
        }
    ];
}
