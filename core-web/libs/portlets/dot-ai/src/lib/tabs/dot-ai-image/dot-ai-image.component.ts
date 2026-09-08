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
     * `h-full` sizes the picture from the available height and its own ratio; `w-fit` is what
     * stops the wrapper stretching past it.
     *
     * Without `w-fit` this element is a stretched flex item, so for a tall ratio it took the
     * column's width — set by the wider action row — and left dead space beside the picture
     * that the zoom overlay still covered. Sizing it to content also lines its left edge up
     * with the buttons below, so no alignment classes are needed.
     */
    protected readonly imagePt = {
        root: { class: 'flex h-full w-fit' }
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
