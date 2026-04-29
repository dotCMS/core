import type { Injector } from '@angular/core';

import type { Extensions } from '@tiptap/core';
import CharacterCount from '@tiptap/extension-character-count';
import Emoji, { emojis } from '@tiptap/extension-emoji';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import Subscript from '@tiptap/extension-subscript';
import Superscript from '@tiptap/extension-superscript';
import { TableKit } from '@tiptap/extension-table';
import TextAlign from '@tiptap/extension-text-align';
import { Youtube } from '@tiptap/extension-youtube';
import StarterKit from '@tiptap/starter-kit';

import { createBlockGutterDragHandle } from './block-gutter.extension';
import { AIContent } from './nodes/ai-content.extension';
import { createDotContentlet } from './nodes/contentlet/contentlet.extension';
import { GridBlock, GridColumn } from './nodes/grid.extension';
import { DotImage } from './nodes/image.extension';
import { UploadPlaceholderExtension } from './nodes/upload-placeholder.extension';
import { Video } from './nodes/video.extension';
import { SelectionPreserveExtension } from './selection-preserve.extension';
import { createSlashCommandExtension } from './slash-command.extension';

import type { SlashMenuService } from '../components/slash-menu/slash-menu.service';

export function createEditorExtensions(
    menuService: SlashMenuService,
    allowedBlocks: string[] | undefined,
    injector: Injector
): Extensions {
    const has = (name: string): boolean => !allowedBlocks || allowedBlocks.includes(name);

    return [
        StarterKit.configure({
            dropcursor: {
                color: '#6366f1',
                width: 2
            },
            heading: has('heading') ? {} : false,
            bulletList: has('bulletList') ? {} : false,
            orderedList: has('orderedList') ? {} : false,
            blockquote: has('blockquote') ? {} : false,
            codeBlock: has('codeBlock') ? {} : false,
            horizontalRule: has('horizontalRule') ? {} : false
        }),
        createBlockGutterDragHandle(),
        CharacterCount,
        ...(has('table') ? [TableKit.configure({ table: { resizable: true } })] : []),
        ...(has('image') ? [DotImage] : []),
        ...(has('link')
            ? [
                  Link.configure({
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
        ...(has('contentlet') ? [createDotContentlet(injector)] : []),
        ...(has('gridBlock') ? [GridBlock, GridColumn] : []),
        TextAlign.configure({ types: ['heading', 'paragraph'] }),
        Superscript,
        Subscript,
        UploadPlaceholderExtension,
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
        createSlashCommandExtension(menuService),
        Placeholder.configure({
            showOnlyCurrent: true,
            placeholder: ({ node, editor }) => {
                if (editor.isEmpty && node.type.name === 'paragraph') {
                    return 'Type \u2019/\u2019 to insert a block, or just start writing\u2026';
                }
                if (node.type.name === 'heading') {
                    return `Heading ${node.attrs['level']}`;
                }
                if (node.type.name === 'paragraph') {
                    return 'Type \u2019/\u2019 for commands';
                }
                if (node.type.name === 'blockquote') {
                    return 'Write a quote or callout\u2026';
                }
                return '';
            }
        })
    ];
}
