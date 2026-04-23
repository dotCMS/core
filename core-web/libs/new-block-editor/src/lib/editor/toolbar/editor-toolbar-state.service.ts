import { Injectable, NgZone, inject, signal } from '@angular/core';

import { Editor } from '@tiptap/core';
import { NodeSelection } from '@tiptap/pm/state';

import type { ContentletEditEvent } from '../extensions/nodes/contentlet.extension';

@Injectable({ providedIn: 'root' })
export class EditorToolbarStateService {
    private readonly zone = inject(NgZone);

    readonly isBold = signal(false);
    readonly isItalic = signal(false);
    readonly isStrike = signal(false);
    readonly isCode = signal(false);
    readonly isBulletList = signal(false);
    readonly isOrderedList = signal(false);
    readonly isBlockquote = signal(false);
    readonly isCodeBlock = signal(false);
    readonly headingLevel = signal<number | null>(null);
    readonly isLink = signal(false);
    readonly canUndo = signal(false);
    readonly canRedo = signal(false);
    readonly canIndent = signal(false);
    readonly canOutdent = signal(false);
    readonly isImageSelected = signal(false);
    readonly imageTextWrap = signal<string | null>(null);
    readonly textAlign = signal<'left' | 'center' | 'right' | 'justify'>('left');
    readonly isSuperscript = signal(false);
    readonly isSubscript = signal(false);
    readonly selectedContentlet = signal<ContentletEditEvent | null>(null);

    connect(editor: Editor): () => void {
        const update = () => {
            this.zone.run(() => {
                this.isBold.set(editor.isActive('bold'));
                this.isItalic.set(editor.isActive('italic'));
                this.isStrike.set(editor.isActive('strike'));
                this.isCode.set(editor.isActive('code'));
                this.isBulletList.set(editor.isActive('bulletList'));
                this.isOrderedList.set(editor.isActive('orderedList'));
                this.isBlockquote.set(editor.isActive('blockquote'));
                this.isCodeBlock.set(editor.isActive('codeBlock'));
                this.isLink.set(editor.isActive('link'));
                this.isImageSelected.set(editor.isActive('dotImage'));
                this.imageTextWrap.set(
                    editor.isActive('dotImage')
                        ? (editor.getAttributes('dotImage').textWrap ?? null)
                        : null
                );
                this.canUndo.set(editor.can().undo());
                this.canRedo.set(editor.can().redo());
                this.canIndent.set(editor.can().sinkListItem('listItem'));
                this.canOutdent.set(editor.can().liftListItem('listItem'));
                this.textAlign.set(
                    editor.isActive({ textAlign: 'center' })
                        ? 'center'
                        : editor.isActive({ textAlign: 'right' })
                          ? 'right'
                          : editor.isActive({ textAlign: 'justify' })
                            ? 'justify'
                            : 'left'
                );
                this.isSuperscript.set(editor.isActive('superscript'));
                this.isSubscript.set(editor.isActive('subscript'));

                const { selection } = editor.state;
                const contentletNode =
                    selection instanceof NodeSelection && selection.node.type.name === 'dotContent'
                        ? selection.node
                        : null;
                this.selectedContentlet.set(
                    contentletNode
                        ? {
                              identifier: contentletNode.attrs['identifier'] ?? '',
                              inode: contentletNode.attrs['inode'] ?? '',
                              contentType: contentletNode.attrs['contentType'] ?? '',
                              title: contentletNode.attrs['title'] ?? ''
                          }
                        : null
                );

                let level: number | null = null;
                for (const l of [1, 2, 3]) {
                    if (editor.isActive('heading', { level: l })) {
                        level = l;
                        break;
                    }
                }
                this.headingLevel.set(level);
            });
        };

        editor.on('update', update);
        editor.on('selectionUpdate', update);
        update();

        return () => {
            editor.off('update', update);
            editor.off('selectionUpdate', update);
        };
    }
}
