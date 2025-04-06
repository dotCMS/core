import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletIconComponent } from '../dot-contentlet-icon/dot-contentlet-icon.component';

@Component({
    selector: 'dot-contentlet-thumbnail-ng',
    standalone: true,
    imports: [CommonModule, DotContentletIconComponent],
    templateUrl: './dot-contentlet-thumbnail.component.html',
    styleUrl: './dot-contentlet-thumbnail.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentletThumbnailComponent {
    @Input() contentlet: DotCMSContentlet;
    @Input() width = 64;
    @Input() height = 64;
    @Input() showLabel = true;

    /**
     * Get thumbnail URL for the contentlet
     */
    getThumbnailUrl(): string {
        if (!this.contentlet) {
            return '';
        }

        // Handle PDF files
        if (this.contentlet.mimeType === 'application/pdf') {
            return `/contentAsset/image/${this.contentlet.inode}/${
                this.contentlet.titleImage
            }/pdf_page/1/resize_w/250/quality_q/45`;
        }

        // Handle SVG files
        if (this.contentlet.mimeType === 'image/svg+xml') {
            return `/contentAsset/image/${this.contentlet.inode}/asset`;
        }

        // Handle regular images with dA path
        if (this.contentlet.titleImage && this.contentlet.titleImage !== 'TITLE_IMAGE_NOT_FOUND') {
            return `/dA/${this.contentlet.inode}/${this.contentlet.titleImage}/500w/50q`;
        }

        return '';
    }

    /**
     * Get title for the contentlet
     */
    getTitle(): string {
        return this.contentlet?.title || '';
    }

    /**
     * Get icon for the contentlet
     */
    getContentletIcon(): string {
        if (!this.contentlet) {
            return '';
        }

        if (this.contentlet.__icon__) {
            return this.contentlet.__icon__;
        }

        return this.contentlet.contentTypeIcon || '';
    }

    /**
     * Check if thumbnail should display an image or icon
     */
    shouldShowImage(): boolean {
        return !!this.getThumbnailUrl();
    }
}
