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
    iconSize?: string;
    titleImage?: string;
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

    private NO_TITLE_IMAGE = 'TITLE_IMAGE_NOT_FOUND';

    @Input() url: string;
    @Input() inode: string;
    @Input() titleImage: string;
    @Input() isImage = false;
    @Input() contentType = '';
    @Input() name = '';
    @Input() iconSize = '1rem';
    @Input() objectFit = 'cover';

    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;
    private readonly DEFAULT_ICON = 'pi-file';
    private readonly srcMap = {
        image: () => this.getImageThumbnailUrl(),
        video: () => this.getVideoThumbnailUrl(),
        pdf: () => this.getPdfThumbnailUrl()
    };

    get extension(): string {
        return this.name.split('.').pop();
    }

    get fileType(): string {
        return this.contentType?.split('/')[0];
    }

    get hasTitleImage(): boolean {
        return this.titleImage && this.titleImage !== this.NO_TITLE_IMAGE;
    }

    get isVeticalImage(): boolean {
        return this.type === CONTENT_THUMBNAIL_TYPE.image && this.isImage;
    }

    ngOnInit(): void {
        this.icon = ICON_MAP[this.extension] || this.DEFAULT_ICON;
        this.type = this.getThumbnailType();
        this.src = this.url || this.getURL?.();
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

    private getThumbnailType() {
        if (this.isImage || this.hasTitleImage) {
            return CONTENT_THUMBNAIL_TYPE.image;
        }

        return CONTENT_THUMBNAIL_TYPE[this.fileType] || CONTENT_THUMBNAIL_TYPE.icon;
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

    private getURL(): string {
        // If it's a pdf and has a title image, use the title image
        if (this.extension === 'pdf' && this.hasTitleImage) {
            return this.getPdfThumbnailUrl();
        }

        // If it's an image and has a title image, use the title image
        if (this.isImage || this.hasTitleImage) {
            return this.getImageThumbnailUrl();
        }

        // Else, check the srcMap for the file type or extension
        const urlFn = this.srcMap[this.fileType] || this.srcMap[this.extension];

        return urlFn?.();
    }
}
