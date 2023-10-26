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

    @Input() tempUrl: string;
    @Input() inode: string;
    @Input() name: string;
    @Input() contentType: string;
    @Input() iconSize: string;
    @Input() titleImage: string;

    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;
    private readonly DEFAULT_ICON = 'pi-file';
    private type = '';
    private thumbnailUrlMap = {
        image: this.getImageThumbnailUrl.bind(this),
        video: this.getVideoThumbnailUrl.bind(this),
        pdf: this.getPdfThumbnailUrl.bind(this)
    };

    ngOnInit(): void {
        this.type = this.contentType.split('/')[0];
        this.setSrc();
        this.setThumbnailType();
        this.setThumbnailIcon();
    }

    handleError() {
        this.thumbnailType = this.CONTENT_THUMBNAIL_TYPE.icon;
    }

    private setThumbnailType(): void {
        this.thumbnailType = CONTENT_THUMBNAIL_TYPE[this.type] || CONTENT_THUMBNAIL_TYPE.icon;
    }

    private setSrc(): void {
        this.src = this.tempUrl || this.thumbnailUrlMap[this.type]?.();
    }

    private setThumbnailIcon(): void {
        const extension = this.name.split('.').pop();
        this.thumbnailIcon = ICON_MAP[extension] || this.DEFAULT_ICON;
    }

    private getPdfThumbnailUrl(): string {
        return `/contentAsset/image/${this.inode}/${this.titleImage}/pdf_page/1/resize_w/250/quality_q/45`;
    }

    private getImageThumbnailUrl(): string {
        return `/dA/${this.inode}/500w/50q`;
    }

    private getVideoThumbnailUrl(): string {
        return `/dA/${this.inode}`;
    }
}
