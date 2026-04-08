import { computePosition, flip, shift } from '@floating-ui/dom';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    CUSTOM_ELEMENTS_SCHEMA,
    afterNextRender,
    afterRenderEffect,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';

import { EmojiPickerService } from './emoji-picker.service';

@Component({
    selector: 'dot-block-editor-emoji-picker',
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA],
    imports: [],
    host: {
        class: 'absolute z-50',
        '[style.display]': 'service.isOpen() ? null : "none"',
        '[style.visibility]': 'positioned() ? "visible" : "hidden"',
        '[style.left.px]': 'floatX()',
        '[style.top.px]': 'floatY()'
    },
    template: ``
})
export class EmojiPickerComponent {
    protected readonly service = inject(EmojiPickerService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);

    protected readonly floatX = signal(0);
    protected readonly floatY = signal(0);
    protected readonly positioned = signal(false);

    constructor() {
        // Mount the emoji-mart web component once after the host element is in the DOM
        afterNextRender(() => {
            import('emoji-mart').then(({ Picker }) => {
                import('@emoji-mart/data').then(({ default: data }) => {
                    const picker = new Picker({
                        data,
                        theme: 'light',
                        previewPosition: 'none',
                        onEmojiSelect: (emoji: { native: string }) => {
                            this.zone.run(() => {
                                this.service.insert(emoji.native);
                                this.service.close();
                            });
                        }
                    });
                    this.el.nativeElement.appendChild(picker as unknown as Node);
                });
            });
        });

        // Re-position whenever the picker opens or the reference rect changes
        afterRenderEffect(() => {
            const isOpen = this.service.isOpen();
            const clientRectFn = this.service.clientRectFn();

            if (!isOpen || !clientRectFn) {
                untracked(() => this.positioned.set(false));
                return;
            }

            const virtualRef = {
                getBoundingClientRect: () => clientRectFn() ?? new DOMRect()
            };

            computePosition(virtualRef, this.el.nativeElement, {
                placement: 'bottom-start',
                strategy: 'absolute',
                middleware: [flip(), shift({ padding: 8 })]
            }).then(({ x, y }) => {
                this.zone.run(() => {
                    untracked(() => {
                        this.floatX.set(x);
                        this.floatY.set(y);
                        this.positioned.set(true);
                    });
                });
            });
        });

        // Close on Escape or click outside
        effect((onCleanup) => {
            if (!this.service.isOpen()) return;

            const handleKeyDown = (e: KeyboardEvent) => {
                if (e.key === 'Escape') {
                    this.zone.run(() => this.service.close());
                }
            };

            const handleClick = (e: MouseEvent) => {
                if (!this.el.nativeElement.contains(e.target as Node)) {
                    this.zone.run(() => this.service.close());
                }
            };

            document.addEventListener('keydown', handleKeyDown);
            document.addEventListener('mousedown', handleClick);

            onCleanup(() => {
                document.removeEventListener('keydown', handleKeyDown);
                document.removeEventListener('mousedown', handleClick);
            });
        });
    }
}
