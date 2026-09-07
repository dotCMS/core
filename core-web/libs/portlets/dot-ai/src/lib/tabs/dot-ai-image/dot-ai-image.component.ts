import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';

import { DotAiPromptInputComponent } from '@dotcms/ai-ui';
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
        ImageModule,
        DotAiPromptInputComponent,
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
     * Labelled by aspect ratio rather than pixel size: the ratio is what you are choosing,
     * and the exact pixel count is the API's business. Not translated — a ratio reads the
     * same in every language. The values stay the sizes the API takes.
     */
    protected readonly orientations = [
        { value: DotAIImageOrientation.HORIZONTAL, label: '16:9' },
        { value: DotAIImageOrientation.SQUARE, label: '1x1' },
        { value: DotAIImageOrientation.VERTICAL, label: '9:16' }
    ];

    /** Fills the frame so the picture can be centred and contained at any ratio. */
    protected readonly imagePt = {
        root: { class: 'flex size-full items-center justify-center' }
    };

    protected onGenerate(): void {
        if (this.$prompt().trim() && this.store.isConfigured()) {
            this.store.generateImage(this.$prompt());
        }
    }
}
