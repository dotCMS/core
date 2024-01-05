import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

const ICON_MAP = {
    html: 'pi-file',
    pdf: 'pi-file-pdf',
    image: 'pi-image',
    video: 'pi-video',
    msword: 'pi-file-word',
    doc: 'pi-file-word',
    docx: 'pi-file-word'
};

export enum CONTENT_THUMBNAIL_TYPE {
    image = 'image',
    video = 'video',
    icon = 'icon'
}

export interface DotThumbnailOptions {
    inode: string;
    name: string;
    contentType: string;
    tempUrl?: string;
    iconSize?: string; // Remove
    titleImage?: string; //
}

@Component({
    selector: 'dot-content-thumbnail',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-content-thumbnail.component.html',
    styleUrls: ['./dot-content-thumbnail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailComponent implements OnInit {
    src: string;
    icon: string;
    type: CONTENT_THUMBNAIL_TYPE;

    @Input() url: string;
    @Input() inode: string;
    @Input() titleImage: string;
    @Input() isImage = false;
    @Input() contentType = '';
    @Input() name = '';
    @Input() iconSize = '1rem';

    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;
    private readonly DEFAULT_ICON = 'pi-file';
    private readonly srcMap = {
        image: () => this.getImageThumbnailUrl(),
        video: () => this.getVideoThumbnailUrl(),
        pdf: () => this.getPdfThumbnailUrl()
    };

    ngOnInit(): void {
        const extension = this.name.split('.').pop();
        const fileType = this.contentType?.split('/')[0];
        const getSrc = this.srcMap[fileType] || this.srcMap[extension];
        this.icon = ICON_MAP[extension] || this.DEFAULT_ICON;
        this.type = this.getThumbnailType(fileType);
        this.src = this.url || getSrc?.();
    }

    /**
     * Handle error when image/video is not found
     * Set thumbnail type to icon
     *
     * @memberof DotContentThumbnailComponent
     */
    handleError() {
        this.type = this.CONTENT_THUMBNAIL_TYPE.icon;
    }

    private getThumbnailType(fileType: string) {
        if (this.titleImage || this.isImage) {
            return CONTENT_THUMBNAIL_TYPE.image;
        }

        return CONTENT_THUMBNAIL_TYPE[fileType] || CONTENT_THUMBNAIL_TYPE.icon;
    }

    /**
     * Get pdf thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getPdfThumbnailUrl(): string {
        return `/contentAsset/image/${this.inode}/${this.titleImage}/pdf_page/1/resize_w/250/quality_q/45`;
    }

    /**
     * Get image thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getImageThumbnailUrl(): string {
        return `/dA/${this.inode}/500w/50q/${this.name}`;
    }

    /**
     * Get video thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getVideoThumbnailUrl(): string {
        return `/dA/${this.inode}`;
    }
}
