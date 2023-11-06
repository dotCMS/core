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
    tempUrl?: string;
    inode?: string;
    name?: string;
    contentType?: string;
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
    thumbnailIcon: string;
    thumbnailType: CONTENT_THUMBNAIL_TYPE;

    @Input() dotThumbanilOptions: DotThumbnailOptions;

    private _type: string;
    private _tempUrl: string;
    private _inode: string;
    private _contentType: string;
    private _titleImage: string;
    private _name: string;
    private _iconSize: string;
    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;

    private readonly DEFAULT_ICON = 'pi-file';
    private readonly thumbnailUrlMap = {
        image: this.getImageThumbnailUrl.bind(this),
        video: this.getVideoThumbnailUrl.bind(this),
        pdf: this.getPdfThumbnailUrl.bind(this)
    };

    get iconSize(): string {
        return this._iconSize || '1rem';
    }

    get name(): string {
        return this._name || '';
    }

    ngOnInit(): void {
        this.buildThumbnail();
    }

    /**
     * Handle error when image/video is not found
     * Set thumbnail type to icon
     *
     * @memberof DotContentThumbnailComponent
     */
    handleError() {
        this.thumbnailType = this.CONTENT_THUMBNAIL_TYPE.icon;
    }

    private setProperties(): void {
        const { tempUrl, inode, name, contentType, titleImage, iconSize } =
            this.dotThumbanilOptions;
        this._tempUrl = tempUrl;
        this._inode = inode;
        this._name = name;
        this._contentType = contentType;
        this._titleImage = titleImage;
        this._iconSize = iconSize;
        this._type = this._contentType.split('/')[0];
    }

    private buildThumbnail(): void {
        this.setProperties();
        this.setSrc();
        this.setThumbnailType();
        this.setThumbnailIcon();
    }

    /**
     * Set thumbnail type
     *
     * @private
     * @memberof DotContentThumbnailComponent
     */
    private setThumbnailType(): void {
        this.thumbnailType = CONTENT_THUMBNAIL_TYPE[this._type] || CONTENT_THUMBNAIL_TYPE.icon;
    }

    /**
     * Set thumbnail src
     *
     * @private
     * @memberof DotContentThumbnailComponent
     */
    private setSrc(): void {
        this.src = this._tempUrl || this.thumbnailUrlMap[this._type]?.();
    }

    /**
     * Set thumbnail icon
     *
     * @private
     * @memberof DotContentThumbnailComponent
     */
    private setThumbnailIcon(): void {
        const extension = this._name.split('.').pop();
        this.thumbnailIcon = ICON_MAP[extension] || this.DEFAULT_ICON;
    }

    /**
     * Get pdf thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getPdfThumbnailUrl(): string {
        return `/contentAsset/image/${this._inode}/${this._titleImage}/pdf_page/1/resize_w/250/quality_q/45`;
    }

    /**
     * Get image thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getImageThumbnailUrl(): string {
        return `/dA/${this._inode}/500w/50q`;
    }

    /**
     * Get video thumbnail url
     *
     * @private
     * @return {*}  {string}
     * @memberof DotContentThumbnailComponent
     */
    private getVideoThumbnailUrl(): string {
        return `/dA/${this._inode}`;
    }
}
