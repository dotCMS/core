import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
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
     * Height only, so the picture is sized by the available height and its own ratio and the
     * border hugs it exactly.
     *
     * justify-start, not centre: for a tall ratio the action row is intrinsically wider than
     * the picture, so the column takes the row's width. Centring the picture inside that would
     * leave its left edge off from the buttons below it.
     */
    protected readonly imagePt = {
        root: { class: 'flex h-full items-center justify-start' }
    };

    protected onGenerate(): void {
        if (this.$prompt().trim() && this.store.isConfigured()) {
            this.store.generateImage(this.$prompt());
        }
    }
}
