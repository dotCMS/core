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

import { EditorPopoverService } from '../../services/editor-popover.service';
import { EditorPopoverComponent } from '../editor-popover/editor-popover.component';

@Component({
    selector: 'dot-emoji-picker',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [EditorPopoverComponent],
    template: `
        <dot-editor-popover popoverId="emoji">
            <div #pickerMount></div>
        </dot-editor-popover>
    `
})
export class EmojiPickerComponent {
    readonly editor = input.required<Editor>();

    @ViewChild('pickerMount', { read: ElementRef }) pickerMount!: ElementRef<HTMLElement>;

    private readonly manager = inject(EditorPopoverService);
    private readonly zone = inject(NgZone);

    constructor() {
        // Mount the emoji-mart web component once after the host element is in the DOM.
        afterNextRender(() => {
            Promise.all([import('emoji-mart'), import('@emoji-mart/data')])
                .then(([{ Picker }, { default: data }]) => {
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
                })
                .catch((err) => {
                    // Lazy chunk fetch failed (offline / network blip / asset hosting issue).
                    // Close the popover so the user gets a click-out signal instead of an
                    // empty box; surface enough info in the console for support.
                    // eslint-disable-next-line no-console
                    console.error('[emoji-picker] failed to load emoji-mart', err);
                    this.zone.run(() => this.manager.close());
                });
        });
    }
}
