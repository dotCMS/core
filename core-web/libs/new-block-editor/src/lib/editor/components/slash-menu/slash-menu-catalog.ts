import { firstValueFrom } from 'rxjs';

import type { Editor } from '@tiptap/core';
import { SuggestionPluginKey } from '@tiptap/suggestion';

import type {
    DotContentSearchService,
    DotContentTypeService,
    DotMessageService
} from '@dotcms/data-access';
import type { Action, DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';

import { DOT_CONTENTLET_NODE_NAME } from '../../extensions/nodes/contentlet/contentlet.extension';

import type { BlockItem } from './slash-menu.types';
import type { EditorModalService } from '../../services/editor-modal.service';
import type { EditorPopoverService } from '../../services/editor-popover.service';

// Narrow interface so the catalog doesn't import the full service class
interface SlashMenuSubMenuHost {
    openAsyncSubmenu(
        search: (query: string) => Promise<BlockItem[]>,
        commandFn: (item: BlockItem) => void
    ): void;
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

// Strict UUID-v4 shape (8-4-4-4-12 hex): a real identifier searches without wildcards/title boost.
const UUID_LIKE = /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i;
// Lucene query-syntax characters that must be escaped before user input is interpolated.
const LUCENE_SPECIAL_CHARS = /[+\-!(){}[\]^"~*?:\\/&|]/g;
const escapeLucene = (value: string): string => value.replace(LUCENE_SPECIAL_CHARS, '\\$&');

/**
 * Lucene query for the contentlet sub-picker of a given content type.
 *
 * With no `filter` the trailing `+catchall:** title:''^15` lists the type's working content with
 * titled items boosted to the top. With a `filter` it searches SERVER-SIDE — each whitespace/hyphen
 * token becomes a mandatory wildcard clause (Lucene's analyzer splits on hyphens, so a wildcard over
 * a hyphenated token would never match) plus a title-phrase boost — so results aren't limited to the
 * first page. Mirrors the legacy block-editor's `getContentlets` behaviour, including Lucene escaping.
 */
function buildContentletByTypeQuery(variable: string, languageId: number, filter = ''): string {
    const trimmed = filter.trim();
    let searchClauses = `+catchall:** title:''^15`;
    if (UUID_LIKE.test(trimmed)) {
        searchClauses = `+catchall:${escapeLucene(trimmed)}`;
    } else if (trimmed.length > 0) {
        const tokenClauses = trimmed
            .split(/[-\s]+/)
            .filter((token) => token.length > 0)
            .map((token) => `+catchall:*${escapeLucene(token)}*`)
            .join(' ');
        searchClauses = `${tokenClauses} title:"${escapeLucene(trimmed)}"^15`;
    }

    return `+contentType:${variable} +languageId:${languageId} +deleted:false +working:true ${searchClauses}`;
}

interface ContentletSearchEntity {
    jsonObjectView?: { contentlets?: DotCMSContentlet[] };
}

export function createContentTypeItem(
    menuService: SlashMenuSubMenuHost,
    contentTypeService: DotContentTypeService,
    contentSearchService: DotContentSearchService,
    getLanguageId: () => number,
    getAllowedContentTypes: () => string,
    dotMessageService: DotMessageService
): BlockItem {
    const msg = (key: string, ...args: string[]) => dotMessageService.get(key, ...args);

    /** Single-row error state, reused for a failed content-type or contentlet request. */
    const errorItem = (kind: 'content-type' | 'contentlet'): BlockItem[] => [
        {
            label: msg(`dot.block.editor.slash-menu.${kind}.error.label`),
            description: msg(`dot.block.editor.slash-menu.${kind}.error.description`),
            icon: 'cloud_off',
            keywords: ['error', kind],
            isEmptyState: true
        }
    ];

    /** Maps content types to display rows; `keywords[0]` is the variable the drill-down reads back. */
    const toTypeItems = (types: DotCMSContentType[]): BlockItem[] =>
        types.length > 0
            ? types.map((ct) => ({
                  label: ct.name,
                  description: ct.description || ct.variable,
                  icon: materialIconOrFallback(ct.icon, 'folder_special'),
                  keywords: [ct.variable, ct.baseType.toLowerCase()]
              }))
            : [
                  {
                      label: msg('dot.block.editor.slash-menu.content-type.empty.label'),
                      description: msg('dot.block.editor.slash-menu.content-type.empty.description'),
                      icon: 'folder_off',
                      keywords: ['no', 'empty', 'content', 'types'],
                      isEmptyState: true
                  }
              ];

    /** Maps contentlets to rows whose `onSelect` inserts the embedded contentlet node. */
    const toContentletItems = (
        contentlets: DotCMSContentlet[],
        typeLabel: string
    ): BlockItem[] =>
        contentlets.length > 0
            ? contentlets.map((cl) => ({
                  label: cl.title || cl.identifier,
                  description: cl.contentType,
                  icon: 'note_stack',
                  keywords: [cl.contentType, cl.identifier],
                  onSelect: (ed: Editor) => {
                      const match = SuggestionPluginKey.getState(ed.state);
                      const chain = ed.chain().focus();
                      if (match?.active) {
                          chain.deleteRange(match.range);
                      }
                      chain
                          .insertContent({
                              type: DOT_CONTENTLET_NODE_NAME,
                              attrs: {
                                  // Full contentlet at runtime; the JSON-strip helper reduces it to
                                  // {identifier, languageId} when the document is serialised for storage.
                                  data: {
                                      ...cl,
                                      languageId:
                                          (cl as { languageId?: number }).languageId ??
                                          getLanguageId()
                                  }
                              }
                          })
                          .run();
                  }
              }))
            : [
                  {
                      label: msg('dot.block.editor.slash-menu.contentlet.empty.label'),
                      description: msg(
                          'dot.block.editor.slash-menu.contentlet.empty.description',
                          typeLabel
                      ),
                      icon: 'search_off',
                      keywords: ['no', 'empty', 'contentlets'],
                      isEmptyState: true
                  }
              ];

    // Deletes the text typed after the "/" (keeps the slash) so a sub-menu search starts empty.
    const clearQueryKeepSlash = (editor: Editor): void => {
        const match = SuggestionPluginKey.getState(editor.state);
        if (match?.active && match.range.from + 1 < match.range.to) {
            editor
                .chain()
                .deleteRange({ from: match.range.from + 1, to: match.range.to })
                .run();
        }
    };

    // Level 2 — contentlets of the chosen type, searched server-side (debounced) as the user types.
    const openContentletSearch = (editor: Editor, ctVariable: string, typeLabel: string): void => {
        menuService.openAsyncSubmenu(
            (query) =>
                firstValueFrom(
                    contentSearchService.get<ContentletSearchEntity>({
                        query: buildContentletByTypeQuery(ctVariable, getLanguageId(), query),
                        sort: 'modDate desc',
                        offset: 0,
                        limit: 40
                    })
                )
                    .then((entity) =>
                        toContentletItems(entity?.jsonObjectView?.contentlets ?? [], typeLabel)
                    )
                    .catch(() => errorItem('contentlet')),
            (contentletItem) => {
                // empty / error rows have no onSelect — just dismiss.
                if (contentletItem.onSelect) {
                    contentletItem.onSelect(editor);
                } else {
                    clearActiveSuggestionRange(editor);
                }
                menuService.close();
            }
        );
    };

    return {
        label: msg('dot.block.editor.slash-menu.content-type.label'),
        description: msg('dot.block.editor.slash-menu.content-type.description'),
        icon: 'category',
        keywords: ['content', 'type', 'dotcms', 'contenttype', 'model'],
        blockName: 'dotContent',
        keepRange: true,
        onSelect: (editor, range) => {
            // keepRange=true: deleteRange was skipped, suggestion session stays alive. Delete the
            // typed query text (e.g. "content") but keep the "/" so the content-type search starts
            // from an empty query and the user can then type to filter server-side.
            if (range && range.from + 1 < range.to) {
                editor
                    .chain()
                    .deleteRange({ from: range.from + 1, to: range.to })
                    .run();
            }

            // Level 1 — content types, searched server-side (debounced) as the user types. This
            // replaces the previous one-shot top-40 fetch + client-side filter, which hid every
            // content type past the alphabetical cap (the "list ends after Image Asset" report).
            menuService.openAsyncSubmenu(
                (query) =>
                    firstValueFrom(
                        contentTypeService.filterContentTypes(query, getAllowedContentTypes())
                    )
                        .then((types) => toTypeItems(types ?? []))
                        .catch(() => errorItem('content-type')),
                (selectedType) => {
                    if (selectedType.isEmptyState) {
                        clearActiveSuggestionRange(editor);
                        menuService.close();
                        return;
                    }
                    // Clear the type-filter text (keep "/") before drilling into the contentlets.
                    clearQueryKeepSlash(editor);
                    // keywords[0] is ct.variable (stored by toTypeItems).
                    openContentletSearch(editor, selectedType.keywords[0], selectedType.label);
                }
            );
        }
    };
}

/**
 * Returns the static block-type entries (paragraph, headings, lists, quote, code, grid)
 * with their labels and descriptions resolved through {@link DotMessageService}. Built lazily
 * because the message service must be available at call time.
 */
export function createBaseBlockItems(dotMessageService: DotMessageService): BlockItem[] {
    const msg = (key: string) => dotMessageService.get(key);
    return [
        {
            label: msg('dot.block.editor.slash-menu.text.label'),
            description: msg('dot.block.editor.slash-menu.text.description'),
            icon: 'article',
            keywords: ['paragraph', 'text'],
            blockName: 'paragraph',
            apply: (c) => c.setParagraph()
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-1.label'),
            description: msg('dot.block.editor.slash-menu.heading-1.description'),
            icon: 'format_h1',
            keywords: ['h1', 'heading', 'title'],
            blockName: 'heading1',
            apply: (c) => c.setHeading({ level: 1 })
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-2.label'),
            description: msg('dot.block.editor.slash-menu.heading-2.description'),
            icon: 'format_h2',
            keywords: ['h2', 'heading', 'subtitle'],
            blockName: 'heading2',
            apply: (c) => c.setHeading({ level: 2 })
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-3.label'),
            description: msg('dot.block.editor.slash-menu.heading-3.description'),
            icon: 'format_h3',
            keywords: ['h3', 'heading'],
            blockName: 'heading3',
            apply: (c) => c.setHeading({ level: 3 })
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-4.label'),
            description: msg('dot.block.editor.slash-menu.heading-4.description'),
            icon: 'format_h4',
            keywords: ['h4', 'heading'],
            blockName: 'heading4',
            apply: (c) => c.setHeading({ level: 4 })
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-5.label'),
            description: msg('dot.block.editor.slash-menu.heading-5.description'),
            icon: 'format_h5',
            keywords: ['h5', 'heading'],
            blockName: 'heading5',
            apply: (c) => c.setHeading({ level: 5 })
        },
        {
            label: msg('dot.block.editor.slash-menu.heading-6.label'),
            description: msg('dot.block.editor.slash-menu.heading-6.description'),
            icon: 'format_h6',
            keywords: ['h6', 'heading'],
            blockName: 'heading6',
            apply: (c) => c.setHeading({ level: 6 })
        },
        {
            label: msg('dot.block.editor.slash-menu.bullet-list.label'),
            description: msg('dot.block.editor.slash-menu.bullet-list.description'),
            icon: 'format_list_bulleted',
            keywords: ['ul', 'list', 'bullets'],
            blockName: 'bulletList',
            apply: (c) => c.toggleBulletList()
        },
        {
            label: msg('dot.block.editor.slash-menu.ordered-list.label'),
            description: msg('dot.block.editor.slash-menu.ordered-list.description'),
            icon: 'format_list_numbered',
            keywords: ['ol', 'numbered', 'list'],
            blockName: 'orderedList',
            apply: (c) => c.toggleOrderedList()
        },
        {
            label: msg('dot.block.editor.slash-menu.blockquote.label'),
            description: msg('dot.block.editor.slash-menu.blockquote.description'),
            icon: 'format_quote',
            keywords: ['quote', 'callout', 'cite'],
            blockName: 'blockquote',
            apply: (c) => c.toggleBlockquote()
        },
        {
            label: msg('dot.block.editor.slash-menu.code-block.label'),
            description: msg('dot.block.editor.slash-menu.code-block.description'),
            icon: 'code_blocks',
            keywords: ['code', 'pre', 'snippet'],
            blockName: 'codeBlock',
            apply: (c) => c.setCodeBlock()
        },
        {
            label: msg('dot.block.editor.slash-menu.grid-2.label'),
            description: msg('dot.block.editor.slash-menu.grid-2.description'),
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
    const msg = (key: string) => dotMessageService.get(key);
    return [
        {
            label: msg('dot.block.editor.slash-menu.table.label'),
            description: msg('dot.block.editor.slash-menu.table.description'),
            icon: 'table',
            keywords: ['table', 'grid', 'spreadsheet', 'rows', 'columns'],
            blockName: 'table',
            onSelect: (editor, range) => {
                // Use `range.from` (the position of the `/` character) — that's where the
                // cursor lands AFTER the slash extension's deleteRange runs. Capturing
                // `editor.state.selection.from` here would point to the end of the typed
                // query, which becomes a stale offset once the slash text is deleted, so
                // every subsequent reposition (via autoUpdate) reads the wrong document
                // location. Compute coords lazily so scroll/resize re-queries the line.
                const anchorPos = range.from;
                popovers.open('table', () => {
                    const coords = editor.view.coordsAtPos(anchorPos);
                    return new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                });
            }
        },
        {
            label: msg('dot.block.editor.slash-menu.image.label'),
            description: msg('dot.block.editor.slash-menu.image.description'),
            icon: 'image',
            keywords: ['image', 'photo', 'picture', 'upload', 'url'],
            blockName: 'image',
            onSelect: (editor) => editorModal.openImagePicker(editor)
        },
        {
            label: msg('dot.block.editor.slash-menu.video.label'),
            description: msg('dot.block.editor.slash-menu.video.description'),
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
    const msg = (key: string) => dotMessageService.get(key);
    return [
        {
            label: msg('dot.block.editor.slash-menu.ai-content.label'),
            description: msg('dot.block.editor.slash-menu.ai-content.description'),
            icon: 'auto_awesome',
            keywords: ['ai', 'generate', 'gpt', 'prompt', 'llm', 'chat'],
            blockName: 'aiContent',
            onSelect: (editor) => editorModal.openAiContent(editor)
        },
        {
            label: msg('dot.block.editor.slash-menu.ai-image.label'),
            description: msg('dot.block.editor.slash-menu.ai-image.description'),
            icon: 'imagesmode',
            keywords: ['ai', 'image', 'photo', 'picture', 'generate', 'dall-e', 'art'],
            blockName: 'aiImage',
            onSelect: (editor) => editorModal.openAiImage(editor)
        }
    ];
}
