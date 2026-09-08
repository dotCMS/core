import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';

import { DotAiPromptInputComponent } from '@dotcms/ai-ui';
import { DotAIImageOrientation } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

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
        ImageModule,
        MessageModule,
        DotAiPromptInputComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-ai-image.component.html',
    host: { class: 'block h-full' }
})
export default class DotAiImageComponent {
    protected readonly store = inject(DotAiStore);

    protected readonly $prompt = signal('');

    /**
     * Labelled by aspect ratio rather than pixel size: the ratio is what you are choosing,
     * and the exact pixel count is the API's business. Not translated — a ratio reads the
     * same in every language. The values stay the sizes the API takes.
     */
    protected readonly orientations = [
        { value: DotAIImageOrientation.HORIZONTAL, label: '16:9' },
        { value: DotAIImageOrientation.SQUARE, label: '1x1' },
        { value: DotAIImageOrientation.VERTICAL, label: '9:16' }
    ];

    /**
     * Caps the root and lets it shrink; deliberately no `h-full`.
     *
     * `h-full w-fit` here forced the image to the full available height and then let its own
     * ratio decide the width, which for a 16:9 picture in a near-square panel came out wider
     * than the frame — the image overflowed while the border it carries wrapped a letterboxed
     * box. Measured: 1817px of picture inside a 1344px frame. With `max-*` and the min-0 pair
     * the image is only ever scaled down, so its box is exactly the rendered picture and the
     * frame shrink-wraps it.
     */
    protected readonly imagePt = {
        root: { class: 'flex max-h-full max-w-full min-h-0 min-w-0' }
    };

    /** One rule, read by both the button's disabled state and the generate path. */
    protected readonly $canGenerate = computed(
        () => this.store.isConfigured() && !!this.$prompt().trim() && !this.store.imageGenerating()
    );

    protected onGenerate(): void {
        if (this.$canGenerate()) {
            this.store.generateImage(this.$prompt());
        }
    }
}
