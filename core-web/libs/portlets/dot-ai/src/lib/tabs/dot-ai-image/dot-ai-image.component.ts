import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { ImageModule } from 'primeng/image';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';

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
        InputTextModule,
        SelectModule,
        SkeletonModule,
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

    protected readonly orientations = [
        { label: 'dotai.image.orientation.square', value: DotAIImageOrientation.SQUARE },
        { label: 'dotai.image.orientation.landscape', value: DotAIImageOrientation.HORIZONTAL },
        { label: 'dotai.image.orientation.portrait', value: DotAIImageOrientation.VERTICAL }
    ];

    protected onGenerate(): void {
        if (this.$prompt().trim() && this.store.isConfigured()) {
            this.store.generateImage(this.$prompt());
        }
    }
}
