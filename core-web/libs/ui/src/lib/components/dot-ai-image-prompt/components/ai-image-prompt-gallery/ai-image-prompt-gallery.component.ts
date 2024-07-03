import {
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';

import { SharedModule } from 'primeng/api';
import { GalleriaModule } from 'primeng/galleria';
import { ImageModule } from 'primeng/image';
import { SkeletonModule } from 'primeng/skeleton';

import { DotMessageService } from '@dotcms/data-access';
import { DotAIImageOrientation, DotGeneratedAIImage } from '@dotcms/dotcms-models';

import {
    DotEmptyContainerComponent,
    PrincipalConfiguration
} from './../../../../components/dot-empty-container/dot-empty-container.component';
import { DotMessagePipe } from './../../../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-ai-image-prompt-gallery',
    standalone: true,
    templateUrl: './ai-image-prompt-gallery.component.html',
    imports: [
        GalleriaModule,
        ImageModule,
        SharedModule,
        SkeletonModule,
        DotMessagePipe,
        DotEmptyContainerComponent
    ],
    styleUrls: ['./ai-image-prompt-gallery.component.scss']
})
export class AiImagePromptGalleryComponent implements OnChanges {
    @Input()
    isLoading = false;

    /**
     * An event that is emitted when the generate action is triggered.
     */
    @Input()
    images: DotGeneratedAIImage[] = [];

    /**
     * The index of the currently active image.
     */
    @Input()
    activeImageIndex = 0;

    /**
     * The orientation of the images. helps to define the initial placeholder
     */
    @Input()
    orientation = DotAIImageOrientation.HORIZONTAL;

    /**
     * An event that is emitted when the active image index changes.
     */
    @Output()
    activeIndexChange = new EventEmitter<number>();

    /**
     * An event that is emitted when the generate action to create a new image is triggered.
     */
    @Output()
    regenerate = new EventEmitter<void>();

    dotMessageService = inject(DotMessageService);

    emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('block-editor.extension.ai-image.error'),
        icon: 'pi-exclamation-triangle'
    };

    ngOnChanges(changes: SimpleChanges): void {
        const error = changes.images?.currentValue?.[this.activeImageIndex]?.error;
        if (error) {
            this.emptyConfiguration.title = this.dotMessageService.get(error);
        }
    }
}
