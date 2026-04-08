import type { Extensions } from '@tiptap/core';
import CharacterCount from '@tiptap/extension-character-count';
import Emoji, { emojis } from '@tiptap/extension-emoji';
import Image from '@tiptap/extension-image';
import Link from '@tiptap/extension-link';
import Placeholder from '@tiptap/extension-placeholder';
import { TableKit } from '@tiptap/extension-table';
import StarterKit from '@tiptap/starter-kit';

import { createBlockGutterDragHandle } from './block-gutter.extension';
import { createSlashCommandExtension } from './slash-command.extension';
import { UploadPlaceholderExtension } from './upload-placeholder.extension';
import { Video } from './video.extension';

import type { SlashMenuService } from '../slash-menu/slash-menu.service';

export function createEditorExtensions(menuService: SlashMenuService): Extensions {
    return [
        StarterKit.configure({
            dropcursor: {
                color: '#6366f1',
                width: 2
            }
        }),
        createBlockGutterDragHandle(),
        CharacterCount,
        TableKit,
        Image,
        Link.configure({
            openOnClick: false,
            enableClickSelection: true,
            autolink: true,
            linkOnPaste: true,
            HTMLAttributes: {
                rel: 'noopener noreferrer',
                target: '_self'
            }
        }),
        Video,
        UploadPlaceholderExtension,
        Emoji.configure({
            emojis,
            enableEmoticons: true,
            // No suggestion — toolbar button opens the emoji-mart picker instead.
            // Input rules still work: typing :shortcode: auto-converts.
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
        }),
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
