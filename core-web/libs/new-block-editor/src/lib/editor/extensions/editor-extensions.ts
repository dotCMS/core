import { common, createLowlight } from 'lowlight';

import type { Injector } from '@angular/core';

import { flattenExtensions, type AnyExtension, type Extensions } from '@tiptap/core';
import CharacterCount from '@tiptap/extension-character-count';
import Emoji, { emojis } from '@tiptap/extension-emoji';
import Placeholder from '@tiptap/extension-placeholder';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
import TextAlign from '@tiptap/extension-text-align';
import { Youtube } from '@tiptap/extension-youtube';
import StarterKit from '@tiptap/starter-kit';

import type { DotMessageService } from '@dotcms/data-access';

import { createBlockGutterDragHandle } from './block-gutter.extension';
import { IndentExtension } from './indent.extension';
import { DotLink } from './link.extension';
import { AIContent } from './nodes/ai-content.extension';
import { Audio } from './nodes/audio.extension';
import { createCodeBlock } from './nodes/code-block/code-block.extension';
import { createDotContentlet } from './nodes/contentlet/contentlet.extension';
import { GridBlock, GridColumn } from './nodes/grid.extension';
import { DotImage } from './nodes/image.extension';
import {
    createUploadPlaceholderExtension,
    type UploadPlaceholderMediaType
} from './nodes/upload-placeholder.extension';
import { Video } from './nodes/video.extension';
import { SelectionPreserveExtension } from './selection-preserve.extension';
import { createSlashCommandExtension } from './slash-command.extension';
import { TableActiveCellsPlugin } from './table-active-cells.plugin';
import { createDotTableExtensions } from './table-extensions';

import { EditorPopoverService } from '../services/editor-popover.service';

import type { SlashMenuService } from '../components/slash-menu/slash-menu.service';

export function createEditorExtensions(
    menuService: SlashMenuService,
    allowedBlocks: string[] | undefined,
    injector: Injector,
    dotMessageService: DotMessageService,
    remoteExtensions: AnyExtension[] = []
): Extensions {
    const t = (key: string, ...args: string[]) => dotMessageService.get(key, ...args);
    const uploadCopy = {
        uploading: (mediaType: UploadPlaceholderMediaType) =>
            t(
                'dot.block.editor.upload.uploading',
                t(`dot.block.editor.upload.media-type.${mediaType}`)
            )
    };
    const has = (name: string): boolean => !allowedBlocks || allowedBlocks.includes(name);

    // Headings use customer-facing per-level names ("heading1".."heading6"). When the field
    // restricts to a subset, configure StarterKit with the matching `levels` so only those
    // are accepted; when none are allowed, disable the extension entirely.
    type HeadingLevel = 1 | 2 | 3 | 4 | 5 | 6;
    const ALL_HEADING_LEVELS: HeadingLevel[] = [1, 2, 3, 4, 5, 6];
    const allowedHeadingLevels = ALL_HEADING_LEVELS.filter((level) => has(`heading${level}`));
    const headingConfig =
        allowedHeadingLevels.length === 0
            ? false
            : allowedHeadingLevels.length === ALL_HEADING_LEVELS.length
              ? {}
              : { levels: allowedHeadingLevels };
    const lowlight = createLowlight(common);

    const baseExtensions: Extensions = [
        StarterKit.configure({
            dropcursor: {
                color: '#6366f1',
                width: 2
            },
            heading: headingConfig,
            bulletList: has('bulletList') ? {} : false,
            orderedList: has('orderedList') ? {} : false,
            blockquote: has('blockquote') ? {} : false,
            codeBlock: false,
            // StarterKit v3 bundles Link; the editor registers its own DotLink (name
            // 'link') below, so disable StarterKit's to avoid a duplicate 'link' extension.
            link: false,
            horizontalRule: has('horizontalRule') ? {} : false
        }),
        ...(has('codeBlock') ? [createCodeBlock(injector, lowlight)] : []),
        createBlockGutterDragHandle(t('dot.block.editor.gutter.add-block')),
        CharacterCount,
        ...(has('table')
            ? [
                  ...createDotTableExtensions({
                      table: { resizable: true },
                      cell: {
                          popovers: injector.get(EditorPopoverService),
                          columnAriaLabel: t('dot.block.editor.table.handle.column.aria-label'),
                          rowAriaLabel: t('dot.block.editor.table.handle.row.aria-label'),
                          selectionAriaLabel: t(
                              'dot.block.editor.table.handle.selection.aria-label'
                          )
                      },
                      header: {
                          popovers: injector.get(EditorPopoverService),
                          columnAriaLabel: t('dot.block.editor.table.handle.column.aria-label'),
                          rowAriaLabel: t('dot.block.editor.table.handle.row.aria-label'),
                          selectionAriaLabel: t(
                              'dot.block.editor.table.handle.selection.aria-label'
                          )
                      }
                  }),
                  TableActiveCellsPlugin
              ]
            : []),
        ...(has('image') ? [DotImage] : []),
        ...(has('link')
            ? [
                  DotLink.configure({
                      openOnClick: false,
                      enableClickSelection: true,
                      autolink: true,
                      linkOnPaste: true,
                      HTMLAttributes: {
                          rel: 'noopener noreferrer',
                          target: '_self'
                      }
                  })
              ]
            : []),
        ...(has('video') ? [Video] : []),
        ...(has('audio') ? [Audio] : []),
        ...(has('youtube')
            ? [
                  Youtube.configure({
                      height: 300,
                      width: 400,
                      interfaceLanguage: 'us',
                      nocookie: true,
                      modestBranding: true
                  })
              ]
            : []),
        ...(has('dotContent') ? [createDotContentlet(injector)] : []),
        ...(has('gridBlock') ? [GridBlock, GridColumn] : []),
        TextAlign.configure({ types: ['heading', 'paragraph'] }),
        IndentExtension,
        Superscript,
        Subscript,
        createUploadPlaceholderExtension(uploadCopy),
        // Legacy-compat only: the new editor never creates `aiContent` nodes (AI Content
        // dialog inserts the generated HTML via `commands.insertContent` so it becomes
        // normal paragraph/heading/list nodes). This registration exists so customer
        // content authored on the OLD block editor — which DID wrap AI output in an
        // `aiContent` block — still parses and renders. Removing it would silently drop
        // those blocks on load. See `ai-content.extension.ts` for details.
        AIContent,
        ...(has('emoji')
            ? [
                  Emoji.configure({
                      emojis,
                      enableEmoticons: true,
                      suggestion: {
                          char: ':',
                          items: () => [],
                          render: () => ({
                              onStart: () => undefined,
                              onUpdate: () => undefined,
                              onKeyDown: () => false,
                              onExit: () => undefined
                          })
                      }
                  })
              ]
            : []),
        SelectionPreserveExtension,
        createSlashCommandExtension(menuService)
    ];

    const placeholder = Placeholder.configure({
        showOnlyCurrent: true,
        placeholder: ({ node, editor }) => {
            if (editor.isEmpty && node.type.name === 'paragraph') {
                return t('dot.block.editor.placeholder.empty-paragraph');
            }
            if (node.type.name === 'heading') {
                return t('block-editor.placeholder.heading', String(node.attrs['level']));
            }
            if (node.type.name === 'paragraph') {
                return t('dot.block.editor.placeholder.paragraph');
            }
            if (node.type.name === 'blockquote') {
                return t('dot.block.editor.placeholder.blockquote');
            }
            return '';
        }
    });

    // Drop any remote (customBlocks) extension whose name collides with a built-in.
    // StarterKit bundles several extensions internally (e.g. `underline`), so we compare
    // against the FLATTENED built-in set — not just the top-level array — otherwise a remote
    // bundle that ships its own copies of core marks/nodes would double-register and TipTap
    // warns "Duplicate extension names found". The editor's own extension always wins.
    const builtInNames = new Set(
        flattenExtensions([...baseExtensions, placeholder]).map((ext) => ext.name)
    );
    const dedupedRemoteExtensions = remoteExtensions.filter((ext) => {
        if (builtInNames.has(ext.name)) {
            console.warn(
                `[remote-extension] skipping "${ext.name}": already registered by the editor`
            );

            return false;
        }

        return true;
    });

    return [...baseExtensions, ...dedupedRemoteExtensions, placeholder];
}
