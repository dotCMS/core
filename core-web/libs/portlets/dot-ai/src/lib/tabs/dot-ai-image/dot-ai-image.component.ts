import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
import { MessageModule } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TooltipModule } from 'primeng/tooltip';

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
        TooltipModule,
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
     * Fills the frame, which is the element that carries the ratio and the caps.
     *
     * Nothing here sizes itself: `h-full w-fit` on this root was the original bug — it took
     * the full available height and let the ratio pick the width, so a 16:9 picture in a
     * near-square panel came out 1817px wide inside a 1344px frame and the border it carries
     * wrapped a letterboxed box.
     */
    protected readonly imagePt = {
        root: { class: 'flex h-full w-full' }
    };

    /**
     * `1792x1024` as the CSS `aspect-ratio` the frame needs.
     *
     * Falls back to `auto` for anything unexpected, so an unrecognised size degrades to the
     * old capped behaviour rather than collapsing the frame to nothing.
     */
    protected aspectRatio(size: string): string {
        const [width, height] = (size ?? '').split('x').map(Number);

        return width && height ? `${width} / ${height}` : 'auto';
    }

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
