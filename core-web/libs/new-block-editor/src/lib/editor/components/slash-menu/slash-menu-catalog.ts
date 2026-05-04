import { firstValueFrom } from 'rxjs';

import type { Editor } from '@tiptap/core';
import { SuggestionPluginKey } from '@tiptap/suggestion';

import type { Action } from '@dotcms/dotcms-models';

import { DOT_CONTENTLET_NODE_NAME } from '../../extensions/nodes/contentlet/contentlet.extension';

import type { BlockItem } from './slash-menu.types';
import type { DotContentTypeService } from '../../services/dot-content-type.service';
import type { DotContentletService } from '../../services/dot-contentlet.service';
import type { EditorDialogManagerService } from '../../services/editor-dialog.service';
import type { EditorModalService } from '../../services/editor-modal.service';

// Narrow interface so the catalog doesn't import the full service class
interface SlashMenuSubMenuHost {
    openSubmenu(): void;
    setItems(items: BlockItem[], commandFn: (item: BlockItem) => void): void;
    close(): void;
}

/**
 * Use {@link raw} only when it is a Material Symbols ligature (snake_case); otherwise {@link fallback}.
 * dotCMS may supply non-ligature labels for content types.
 */
function materialIconOrFallback(raw: string | null | undefined, fallback: string): string {
    const v = (raw ?? '').trim();
    return /^[a-z][a-z0-9_]*$/.test(v) ? v : fallback;
}

function clearActiveSuggestionRange(editor: Editor): void {
    const match = SuggestionPluginKey.getState(editor.state);
    if (match?.active) {
        editor.chain().focus().deleteRange(match.range).run();
    }
}

export function createContentTypeItem(
    menuService: SlashMenuSubMenuHost,
    contentTypeService: DotContentTypeService,
    contentletService: DotContentletService,
    getLanguageId: () => number,
    getAllowedContentTypes: () => string
): BlockItem {
    return {
        label: 'Content type',
        description: 'Insert a dotCMS content type',
        icon: 'category',
        keywords: ['content', 'type', 'dotcms', 'contenttype', 'model'],
        blockName: 'dotContent',
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

            firstValueFrom(contentTypeService.fetchAll(getAllowedContentTypes()))
                .then((types) => {
                    const resolvedTypes = types ?? [];
                    // Content type items are plain display items — drill-down logic lives in the
                    // commandFn below (closure over editor and services).
                    const typeItems: BlockItem[] =
                        resolvedTypes.length > 0
                            ? resolvedTypes.map((ct) => ({
                                  label: ct.name,
                                  description: ct.description || ct.variable,
                                  icon: materialIconOrFallback(ct.icon, 'folder_special'),
                                  keywords: [ct.variable, ct.baseType.toLowerCase()]
                              }))
                            : [
                                  {
                                      label: 'No content types found',
                                      description:
                                          'No types returned from the API. Check permissions or configuration.',
                                      icon: 'folder_off',
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
                                        icon: 'note_stack',
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
                                                        // Full contentlet at runtime; the JSON-strip
                                                        // helper reduces it to {identifier, languageId}
                                                        // when the document is serialised for storage.
                                                        data: {
                                                            ...cl,
                                                            languageId:
                                                                (cl as { languageId?: number })
                                                                    .languageId ?? getLanguageId()
                                                        }
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
                                                  icon: 'search_off',
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
                                            icon: 'cloud_off',
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
                                icon: 'cloud_off',
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
        icon: 'article',
        keywords: ['paragraph', 'text'],
        blockName: 'paragraph',
        apply: (c) => c.setParagraph()
    },
    {
        label: 'Heading 1',
        description: 'Top-level title or page heading',
        icon: 'format_h1',
        keywords: ['h1', 'heading', 'title'],
        blockName: 'heading1',
        apply: (c) => c.setHeading({ level: 1 })
    },
    {
        label: 'Heading 2',
        description: 'Section heading',
        icon: 'format_h2',
        keywords: ['h2', 'heading', 'subtitle'],
        blockName: 'heading2',
        apply: (c) => c.setHeading({ level: 2 })
    },
    {
        label: 'Heading 3',
        description: 'Subsection heading',
        icon: 'format_h3',
        keywords: ['h3', 'heading'],
        blockName: 'heading3',
        apply: (c) => c.setHeading({ level: 3 })
    },
    {
        label: 'Bullet List',
        description: 'Unordered list of items',
        icon: 'format_list_bulleted',
        keywords: ['ul', 'list', 'bullets'],
        blockName: 'bulletList',
        apply: (c) => c.toggleBulletList()
    },
    {
        label: 'Ordered List',
        description: 'Numbered list of steps or items',
        icon: 'format_list_numbered',
        keywords: ['ol', 'numbered', 'list'],
        blockName: 'orderedList',
        apply: (c) => c.toggleOrderedList()
    },
    {
        label: 'Blockquote',
        description: 'Highlighted quote or callout',
        icon: 'format_quote',
        keywords: ['quote', 'callout', 'cite'],
        blockName: 'blockquote',
        apply: (c) => c.toggleBlockquote()
    },
    {
        label: 'Code Block',
        description: 'Code snippet with syntax highlighting',
        icon: 'code_blocks',
        keywords: ['code', 'pre', 'snippet'],
        blockName: 'codeBlock',
        apply: (c) => c.setCodeBlock()
    },
    {
        label: 'Grid (2 columns)',
        description: 'Two-column layout',
        icon: 'view_column_2',
        keywords: ['grid', 'columns', 'layout', 'two-column'],
        blockName: 'gridBlock',
        apply: (c) => c.insertGrid()
    }
];

/**
 * Slash entries that open a dialog before mutating the document.
 *
 * Table is a caret-anchored popover via {@link EditorDialogManagerService}. Image and Video
 * skip the popover entirely and open the centered `DotBrowserSelectorComponent` directly
 * via {@link EditorModalService} (per design + PM call — no in-popover Upload / URL tabs).
 */
export function createSlashDialogBlockItems(
    dialogManager: EditorDialogManagerService,
    editorModal: EditorModalService
): BlockItem[] {
    return [
        {
            label: 'Table',
            description: 'Organize data in rows and columns',
            icon: 'table',
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
            icon: 'image',
            keywords: ['image', 'photo', 'picture', 'upload', 'url'],
            blockName: 'image',
            onSelect: (editor) => editorModal.openImagePicker(editor)
        },
        {
            label: 'Video',
            description: 'Embed a video from a link or file',
            icon: 'videocam',
            keywords: ['video', 'mp4', 'upload', 'url', 'media'],
            blockName: 'video',
            onSelect: (editor) => editorModal.openVideoPicker(editor)
        }
    ];
}

/**
 * Slash entries for customer-supplied remote extensions (`customBlocks` field variable).
 *
 * Each {@link Action} maps to a {@link BlockItem} that dispatches `editor.commands[command]`
 * on selection. Remote actions intentionally omit `blockName` so they are not gated by
 * the `allowedBlocks` filter — this matches the legacy block-editor contract. If the
 * remote module fails to register the named command, we log a warning and no-op rather
 * than throwing.
 */
export function createSlashRemoteBlockItems(actions: Action[]): BlockItem[] {
    return actions.map((action) => ({
        label: action.menuLabel,
        description: '',
        icon: action.icon,
        keywords: [action.menuLabel.toLowerCase(), action.command.toLowerCase()],
        onSelect: (editor) => {
            const commands = editor.commands as unknown as Record<string, () => unknown>;
            const fn = commands[action.command];
            if (typeof fn !== 'function') {
                console.warn(`[remote-extension] command "${action.command}" not registered`);
                return;
            }
            try {
                fn();
            } catch (e) {
                console.warn(`[remote-extension] command "${action.command}" threw`, e);
            }
        }
    }));
}

/**
 * Slash entries for the AI plugin: text generation and image generation. The caller
 * spreads this list conditionally based on `store.aiInstalled()`.
 */
export function createSlashAiBlockItems(
    dialogManager: EditorDialogManagerService,
    editorModal: EditorModalService
): BlockItem[] {
    return [
        {
            label: 'Ask AI',
            description: 'Generate text with AI',
            icon: 'auto_awesome',
            keywords: ['ai', 'generate', 'gpt', 'prompt', 'llm', 'chat'],
            blockName: 'aiContent',
            onSelect: () => dialogManager.openAiContent()
        },
        {
            label: 'AI Image',
            description: 'Generate an image with AI',
            icon: 'imagesmode',
            keywords: ['ai', 'image', 'photo', 'picture', 'generate', 'dall-e', 'art'],
            blockName: 'aiImage',
            onSelect: (editor) => editorModal.openAiImage(editor)
        }
    ];
}
