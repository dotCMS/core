import {
    ChangeDetectionStrategy,
    Component,
    CUSTOM_ELEMENTS_SCHEMA,
    ElementRef,
    NgZone,
    ViewChild,
    afterNextRender,
    inject,
    input
} from '@angular/core';

import { Editor } from '@tiptap/core';

import { EditorDialogManagerService } from '../../services/editor-dialog-manager.service';
import { EditorDialogComponent } from '../editor-dialog/editor-dialog.component';

@Component({
    selector: 'dot-emoji-picker',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [EditorDialogComponent],
    template: `
        <dot-editor-dialog dialogId="emoji">
            <div #pickerMount></div>
        </dot-editor-dialog>
    `
})
export class EmojiPickerComponent {
    readonly editor = input.required<Editor>();

    @ViewChild('pickerMount', { read: ElementRef }) pickerMount!: ElementRef<HTMLElement>;

    private readonly manager = inject(EditorDialogManagerService);
    private readonly zone = inject(NgZone);

    constructor() {
        // Mount the emoji-mart web component once after the host element is in the DOM.
        afterNextRender(() => {
            import('emoji-mart').then(({ Picker }) => {
                import('@emoji-mart/data').then(({ default: data }) => {
                    const picker = new Picker({
                        data,
                        theme: 'light',
                        previewPosition: 'none',
                        onEmojiSelect: (emoji: { native: string }) => {
                            this.zone.run(() => {
                                this.editor().chain().focus().insertContent(emoji.native).run();
                                this.manager.close();
                            });
                        }
                    });
                    this.pickerMount.nativeElement.appendChild(picker as unknown as Node);
                });
            });
        });
    }
}
