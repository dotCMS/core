import type { Extensions } from '@tiptap/core';
import CharacterCount from '@tiptap/extension-character-count';
import Emoji, { emojis } from '@tiptap/extension-emoji';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { TableKit } from '@tiptap/extension-table';
import StarterKit from '@tiptap/starter-kit';

import { createBlockGutterDragHandle } from './block-gutter.extension';
import { DotContentlet } from './contentlet.extension';
import { GridBlock, GridColumn } from './grid.extension';
import { DotImage } from './image.extension';
import { createSlashCommandExtension } from './slash-command.extension';
import { UploadPlaceholderExtension } from './upload-placeholder.extension';
import { Video } from './video.extension';

import type { SlashMenuService } from '../slash-menu/slash-menu.service';

export function createEditorExtensions(
    menuService: SlashMenuService,
    allowedBlocks?: string[]
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
        ...(has('table') ? [TableKit] : []),
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
        ...(has('contentlet') ? [DotContentlet] : []),
        ...(has('gridBlock') ? [GridBlock, GridColumn] : []),
        UploadPlaceholderExtension,
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
