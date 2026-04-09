import { take } from 'rxjs/operators';

import { SuggestionPluginKey } from '@tiptap/suggestion';

import { DOT_CONTENTLET_NODE_NAME } from '../extensions/contentlet.extension';

import type { BlockItem } from './slash-menu.types';
import type { ImageDialogService } from '../blocks/image/image-dialog.service';
import type { LinkDialogService } from '../blocks/link/link-dialog.service';
import type { TableDialogService } from '../blocks/table/table-dialog.service';
import type { VideoDialogService } from '../blocks/video/video-dialog.service';
import type {
    DotCmsContentType,
    DotCmsContentTypeService
} from '../services/dot-cms-content-type.service';
import type {
    DotCmsContentlet,
    DotCmsContentletService
} from '../services/dot-cms-contentlet.service';

// Narrow interface so the catalog doesn't import the full service class
interface SlashMenuSubMenuHost {
    openSubmenu(): void;
    setItems(items: BlockItem[], commandFn: (item: BlockItem) => void): void;
    close(): void;
}

export function createContentTypeItem(
    menuService: SlashMenuSubMenuHost,
    contentTypeService: DotCmsContentTypeService,
    contentletService: DotCmsContentletService
): BlockItem {
    return {
        label: 'Content type',
        description: 'Insert a dotCMS content type',
        icon: '⬡',
        keywords: ['content', 'type', 'dotcms', 'contenttype', 'model'],
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

            contentTypeService
                .fetchAll()
                .pipe(take(1))
                .toPromise()
                .then((types: DotCmsContentType[] | undefined) => {
                    if (!types) return;
                    // Content type items are plain display items — drill-down logic lives in the
                    // commandFn below (closure over editor and services).
                    const items: BlockItem[] = types.map((ct) => ({
                        label: ct.name,
                        description: ct.description || ct.variable,
                        icon: ct.icon || '⬡',
                        keywords: [ct.variable, ct.baseType.toLowerCase()]
                    }));

                    menuService.setItems(items, (selectedItem) => {
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

                        contentletService
                            .fetchByType(ctVariable)
                            .pipe(take(1))
                            .toPromise()
                            .then((contentlets: DotCmsContentlet[] | undefined) => {
                                if (!contentlets) return;
                                const contentletItems: BlockItem[] = contentlets.map((cl) => ({
                                    label: cl.title || cl.identifier,
                                    description: cl.contentType,
                                    icon: '◈',
                                    keywords: [cl.contentType, cl.identifier],
                                    onSelect: (editor) => {
                                        const match = SuggestionPluginKey.getState(editor.state);
                                        const chain = editor.chain().focus();
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
                                }));

                                const finalItems: BlockItem[] =
                                    contentletItems.length === 0
                                        ? [
                                              {
                                                  label: 'No contentlets found',
                                                  description: `No ${selectedItem.label} contentlets available`,
                                                  icon: '○',
                                                  keywords: []
                                              }
                                          ]
                                        : contentletItems;

                                menuService.setItems(finalItems, (contentletItem) => {
                                    if (contentletItem.onSelect) {
                                        contentletItem.onSelect(editor);
                                    } else {
                                        const slashMatch = SuggestionPluginKey.getState(
                                            editor.state
                                        );
                                        if (slashMatch?.active) {
                                            editor
                                                .chain()
                                                .focus()
                                                .deleteRange(slashMatch.range)
                                                .run();
                                        }
                                    }
                                    menuService.close();
                                });
                            })
                            .catch(() => menuService.close());
                    });
                })
                .catch(() => menuService.close());
        }
    };
}

export const ALL_ITEMS: BlockItem[] = [
    {
        label: 'Text',
        description: 'Plain text paragraph',
        icon: 'P',
        keywords: ['paragraph', 'text'],
        apply: (c) => c.setParagraph()
    },
    {
        label: 'Heading 1',
        description: 'Top-level title or page heading',
        icon: 'H1',
        keywords: ['h1', 'heading', 'title'],
        apply: (c) => c.setHeading({ level: 1 })
    },
    {
        label: 'Heading 2',
        description: 'Section heading',
        icon: 'H2',
        keywords: ['h2', 'heading', 'subtitle'],
        apply: (c) => c.setHeading({ level: 2 })
    },
    {
        label: 'Heading 3',
        description: 'Subsection heading',
        icon: 'H3',
        keywords: ['h3', 'heading'],
        apply: (c) => c.setHeading({ level: 3 })
    },
    {
        label: 'Bullet List',
        description: 'Unordered list of items',
        icon: '•',
        keywords: ['ul', 'list', 'bullets'],
        apply: (c) => c.toggleBulletList()
    },
    {
        label: 'Ordered List',
        description: 'Numbered list of steps or items',
        icon: '1.',
        keywords: ['ol', 'numbered', 'list'],
        apply: (c) => c.toggleOrderedList()
    },
    {
        label: 'Blockquote',
        description: 'Highlighted quote or callout',
        icon: '"',
        keywords: ['quote', 'callout', 'cite'],
        apply: (c) => c.toggleBlockquote()
    },
    {
        label: 'Code Block',
        description: 'Code snippet with syntax highlighting',
        icon: '</>',
        keywords: ['code', 'pre', 'snippet'],
        apply: (c) => c.setCodeBlock()
    }
];

export interface SlashDialogServices {
    table: TableDialogService;
    image: ImageDialogService;
    video: VideoDialogService;
    link: LinkDialogService;
}

/** Slash entries that open a floating dialog before mutating the document. */
export function createSlashDialogBlockItems(services: SlashDialogServices): BlockItem[] {
    const { table, image, video, link } = services;

    return [
        {
            label: 'Table',
            description: 'Organize data in rows and columns',
            icon: '⊞',
            keywords: ['table', 'grid', 'spreadsheet', 'rows', 'columns'],
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                table.open(
                    (config) => {
                        editor.chain().focus().insertTable(config).run();
                    },
                    () => rect
                );
            }
        },
        {
            label: 'Image',
            description: 'Add a photo or graphic',
            icon: '🖼',
            keywords: ['image', 'photo', 'picture', 'upload', 'url'],
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                image.open(
                    (src, title, alt) => {
                        editor
                            .chain()
                            .focus()
                            .setImage({ src, title: title || undefined, alt: alt || undefined })
                            .run();
                    },
                    () => rect
                );
            }
        },
        {
            label: 'Video',
            description: 'Embed a video from a link or file',
            icon: '▶',
            keywords: ['video', 'mp4', 'upload', 'url', 'media'],
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                video.open(
                    (src, title) => {
                        editor
                            .chain()
                            .focus()
                            .insertContent({ type: 'video', attrs: { src, title: title ?? null } })
                            .run();
                    },
                    () => rect
                );
            }
        },
        {
            label: 'Link',
            description: 'Add a clickable hyperlink',
            icon: '↗',
            keywords: ['link', 'url', 'href', 'hyperlink', 'anchor'],
            onSelect: (editor) => {
                const { from } = editor.state.selection;
                const coords = editor.view.coordsAtPos(from);
                const rect = new DOMRect(coords.left, coords.top, 0, coords.bottom - coords.top);
                link.open(
                    (href, displayText) => {
                        editor
                            .chain()
                            .focus()
                            .insertContent({
                                type: 'text',
                                text: displayText ?? href,
                                marks: [{ type: 'link', attrs: { href } }]
                            })
                            .run();
                    },
                    () => rect
                );
            }
        }
    ];
}
