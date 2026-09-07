import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TextareaModule } from 'primeng/textarea';

import { DotAIImageOrientation } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotAiStore } from '../../store/dot-ai.store';

/**
 * Image tab: describe an image, generate it, then decide what to do with it.
 *
 * Generate, Save and Download are three separate actions on purpose. Generating publishes
 * nothing (FR-037), and Download is a plain same-origin anchor to the temp asset, so it needs
 * no backend and works whether or not the image was ever saved (FR-038).
 */
@Component({
    selector: 'dot-ai-image',
    imports: [
        FormsModule,
        ButtonModule,
        SelectModule,
        SkeletonModule,
        TextareaModule,
        ImageModule,
        DotCopyButtonComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-image.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiImageComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly $prompt = signal('');

    /**
     * The enum values are the pixel sizes the API takes, so the label is derived from the
     * value rather than translated: `1792x1024` shown as `1792×1024`. That keeps the two from
     * drifting, and dimensions read the same in every language.
     */
    protected readonly orientations = [
        DotAIImageOrientation.HORIZONTAL,
        DotAIImageOrientation.SQUARE,
        DotAIImageOrientation.VERTICAL
    ].map((value) => ({ value, label: value.replace('x', '×') }));

    /** Enter generates; Shift+Enter inserts a newline, as in the Chat tab's composer. */
    protected onKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            this.onGenerate();
        }
    }

    protected onGenerate(): void {
        if (this.$prompt().trim() && this.store.isConfigured()) {
            this.store.generateImage(this.$prompt());
        }
    }
}
