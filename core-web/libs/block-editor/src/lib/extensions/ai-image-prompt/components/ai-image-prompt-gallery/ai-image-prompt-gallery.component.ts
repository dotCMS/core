import { NgIf } from '@angular/common';
import { Component, EventEmitter, inject, Input, Output } from '@angular/core';

import { SharedModule } from 'primeng/api';
import { GalleriaModule } from 'primeng/galleria';
import { ImageModule } from 'primeng/image';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import {
    DotAIImageOrientation,
    DotGeneratedAIImage
} from '../../../../shared/services/dot-ai/dot-ai.models';

@Component({
    selector: 'dot-ai-image-prompt-gallery',
    standalone: true,
    templateUrl: './ai-image-prompt-gallery.component.html',
    imports: [GalleriaModule, ImageModule, NgIf, SharedModule, SkeletonModule, DotMessagePipe],
    styleUrls: ['./ai-image-prompt-gallery.component.scss']
})
export class AiImagePromptGalleryComponent {
    @Input()
    isLoading = false;

    @Input()
    images: DotGeneratedAIImage[] = [];

    @Input()
    activeImageIndex = 0;

    @Input()
    orientation: DotAIImageOrientation;

    @Output()
    activeIndexChange = new EventEmitter<number>();

    dotMessageService = inject(DotMessageService);
}
