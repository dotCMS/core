import { Injectable, NgZone, inject, signal } from '@angular/core';

import { Editor } from '@tiptap/core';
import { NodeSelection } from '@tiptap/pm/state';

import type { ContentletEditEvent } from '../../extensions/nodes/contentlet/contentlet.extension';

@Injectable()
export class EditorToolbarStore {
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
    readonly imageTextAlign = signal<string | null>(null);
    readonly textAlign = signal<'left' | 'center' | 'right' | 'justify'>('left');
    readonly isSuperscript = signal(false);
    readonly isSubscript = signal(false);
    readonly isInTable = signal(false);
    readonly canMergeCells = signal(false);
    readonly canSplitCell = signal(false);
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
                this.imageTextAlign.set(
                    editor.isActive('dotImage')
                        ? (editor.getAttributes('dotImage').textAlign ?? null)
                        : null
                );
                this.canUndo.set(editor.can().undo());
                this.canRedo.set(editor.can().redo());
                // The toolbar's indent / outdent button routes through either
                // listItem sink/lift (in lists) or the IndentExtension (text
                // blocks). Mirror the same OR in the enabled-state check so the
                // button isn't greyed out when only the text-block path applies.
                this.canIndent.set(
                    editor.can().sinkListItem('listItem') || editor.can().indent()
                );
                this.canOutdent.set(
                    editor.can().liftListItem('listItem') || editor.can().outdent()
                );
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
                this.isInTable.set(editor.isActive('table'));
                this.canMergeCells.set(editor.can().mergeCells());
                this.canSplitCell.set(editor.can().splitCell());

                const { selection } = editor.state;
                const contentletNode =
                    selection instanceof NodeSelection && selection.node.type.name === 'dotContent'
                        ? selection.node
                        : null;
                const data = contentletNode?.attrs['data'] as
                    | {
                          identifier?: string;
                          inode?: string;
                          contentType?: string;
                          title?: string;
                      }
                    | null
                    | undefined;
                this.selectedContentlet.set(
                    contentletNode && data
                        ? {
                              identifier: data.identifier ?? '',
                              inode: data.inode ?? '',
                              contentType: data.contentType ?? '',
                              title: data.title ?? ''
                          }
                        : null
                );

                let level: number | null = null;
                for (const l of [1, 2, 3, 4, 5, 6]) {
                    if (editor.isActive('heading', { level: l })) {
                        level = l;
                        break;
                    }
                }
                this.headingLevel.set(level);
            });
        };

        // `transaction` fires for *every* dispatched transaction, including ones that
        // only change `storedMarks` (e.g. clicking Bold with an empty selection — no
        // doc/selection change, so `update` and `selectionUpdate` both stay silent and
        // the toolbar would show the wrong active state until the user typed).
        editor.on('transaction', update);
        update();

        return () => {
            editor.off('transaction', update);
        };
    }
}
