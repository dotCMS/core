import { firstValueFrom } from 'rxjs';

import type { Editor } from '@tiptap/core';
import { SuggestionPluginKey } from '@tiptap/suggestion';

import type { DotMessageService } from '@dotcms/data-access';
import type { Action } from '@dotcms/dotcms-models';

import { DOT_CONTENTLET_NODE_NAME } from '../../extensions/nodes/contentlet/contentlet.extension';

import type { BlockItem } from './slash-menu.types';
import type { DotContentTypeService } from '../../services/dot-content-type.service';
import type { DotContentletService } from '../../services/dot-contentlet.service';
import type { EditorModalService } from '../../services/editor-modal.service';
import type { EditorPopoverService } from '../../services/editor-popover.service';

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
    getAllowedContentTypes: () => string,
    dotMessageService: DotMessageService
): BlockItem {
    const t = (key: string, ...args: string[]) => dotMessageService.get(key, ...args);
    return {
        label: t('dot.block.editor.slash-menu.content-type.label'),
        description: t('dot.block.editor.slash-menu.content-type.description'),
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
                                      label: t(
                                          'dot.block.editor.slash-menu.content-type.empty.label'
                                      ),
                                      description: t(
                                          'dot.block.editor.slash-menu.content-type.empty.description'
                                      ),
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
                                                  label: t(
                                                      'dot.block.editor.slash-menu.contentlet.empty.label'
                                                  ),
                                                  description: t(
                                                      'dot.block.editor.slash-menu.contentlet.empty.description',
                                                      selectedItem.label
                                                  ),
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
                                            label: t(
                                                'dot.block.editor.slash-menu.contentlet.error.label'
                                            ),
                                            description: t(
                                                'dot.block.editor.slash-menu.contentlet.error.description'
                                            ),
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
                                label: t('dot.block.editor.slash-menu.content-type.error.label'),
                                description: t(
                                    'dot.block.editor.slash-menu.content-type.error.description'
                                ),
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

/**
 * Returns the static block-type entries (paragraph, headings, lists, quote, code, grid)
 * with their labels and descriptions resolved through {@link DotMessageService}. Built lazily
 * because the message service must be available at call time.
 */
export function createBaseBlockItems(dotMessageService: DotMessageService): BlockItem[] {
    const t = (key: string) => dotMessageService.get(key);
    return [
        {
            label: t('dot.block.editor.slash-menu.text.label'),
            description: t('dot.block.editor.slash-menu.text.description'),
            icon: 'article',
            keywords: ['paragraph', 'text'],
            blockName: 'paragraph',
            apply: (c) => c.setParagraph()
        },
        {
            label: t('dot.block.editor.slash-menu.heading-1.label'),
            description: t('dot.block.editor.slash-menu.heading-1.description'),
            icon: 'format_h1',
            keywords: ['h1', 'heading', 'title'],
            blockName: 'heading1',
            apply: (c) => c.setHeading({ level: 1 })
        },
        {
            label: t('dot.block.editor.slash-menu.heading-2.label'),
            description: t('dot.block.editor.slash-menu.heading-2.description'),
            icon: 'format_h2',
            keywords: ['h2', 'heading', 'subtitle'],
            blockName: 'heading2',
            apply: (c) => c.setHeading({ level: 2 })
        },
        {
            label: t('dot.block.editor.slash-menu.heading-3.label'),
            description: t('dot.block.editor.slash-menu.heading-3.description'),
            icon: 'format_h3',
            keywords: ['h3', 'heading'],
            blockName: 'heading3',
            apply: (c) => c.setHeading({ level: 3 })
        },
        {
            label: t('dot.block.editor.slash-menu.bullet-list.label'),
            description: t('dot.block.editor.slash-menu.bullet-list.description'),
            icon: 'format_list_bulleted',
            keywords: ['ul', 'list', 'bullets'],
            blockName: 'bulletList',
            apply: (c) => c.toggleBulletList()
        },
        {
            label: t('dot.block.editor.slash-menu.ordered-list.label'),
            description: t('dot.block.editor.slash-menu.ordered-list.description'),
            icon: 'format_list_numbered',
            keywords: ['ol', 'numbered', 'list'],
            blockName: 'orderedList',
            apply: (c) => c.toggleOrderedList()
        },
        {
            label: t('dot.block.editor.slash-menu.blockquote.label'),
            description: t('dot.block.editor.slash-menu.blockquote.description'),
            icon: 'format_quote',
            keywords: ['quote', 'callout', 'cite'],
            blockName: 'blockquote',
            apply: (c) => c.toggleBlockquote()
        },
        {
            label: t('dot.block.editor.slash-menu.code-block.label'),
            description: t('dot.block.editor.slash-menu.code-block.description'),
            icon: 'code_blocks',
            keywords: ['code', 'pre', 'snippet'],
            blockName: 'codeBlock',
            apply: (c) => c.setCodeBlock()
        },
        {
            label: t('dot.block.editor.slash-menu.grid-2.label'),
            description: t('dot.block.editor.slash-menu.grid-2.description'),
            icon: 'view_column_2',
            keywords: ['grid', 'columns', 'layout', 'two-column'],
            blockName: 'gridBlock',
            apply: (c) => c.insertGrid()
        }
    ];
}

/**
 * Slash entries that open an overlay before mutating the document.
 *
 * Table is a caret-anchored popover via {@link EditorPopoverService}. Image and Video skip
 * the popover entirely and open the centered `DotBrowserSelectorComponent` directly via
 * {@link EditorModalService} (per design + PM call — no in-popover Upload / URL tabs).
 */
export function createSlashOverlayBlockItems(
    popovers: EditorPopoverService,
    editorModal: EditorModalService,
    dotMessageService: DotMessageService
): BlockItem[] {
    const t = (key: string) => dotMessageService.get(key);
    return [
        {
            label: t('dot.block.editor.slash-menu.table.label'),
            description: t('dot.block.editor.slash-menu.table.description'),
            icon: 'table',
            keywords: ['table', 'grid', 'spreadsheet', 'rows', 'columns'],
            blockName: 'table',
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                popovers.open('table', () => rect);
            }
        },
        {
            label: t('dot.block.editor.slash-menu.image.label'),
            description: t('dot.block.editor.slash-menu.image.description'),
            icon: 'image',
            keywords: ['image', 'photo', 'picture', 'upload', 'url'],
            blockName: 'image',
            onSelect: (editor) => editorModal.openImagePicker(editor)
        },
        {
            label: t('dot.block.editor.slash-menu.video.label'),
            description: t('dot.block.editor.slash-menu.video.description'),
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
    editorModal: EditorModalService,
    dotMessageService: DotMessageService
): BlockItem[] {
    const t = (key: string) => dotMessageService.get(key);
    return [
        {
            label: t('dot.block.editor.slash-menu.ai-content.label'),
            description: t('dot.block.editor.slash-menu.ai-content.description'),
            icon: 'auto_awesome',
            keywords: ['ai', 'generate', 'gpt', 'prompt', 'llm', 'chat'],
            blockName: 'aiContent',
            onSelect: (editor) => editorModal.openAiContent(editor)
        },
        {
            label: t('dot.block.editor.slash-menu.ai-image.label'),
            description: t('dot.block.editor.slash-menu.ai-image.description'),
            icon: 'imagesmode',
            keywords: ['ai', 'image', 'photo', 'picture', 'generate', 'dall-e', 'art'],
            blockName: 'aiImage',
            onSelect: (editor) => editorModal.openAiImage(editor)
        }
    ];
}
