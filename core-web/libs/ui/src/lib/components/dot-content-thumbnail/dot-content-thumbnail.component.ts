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

@Component({
    selector: 'dot-content-thumbnail',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dot-content-thumbnail.component.html',
    styleUrls: ['./dot-content-thumbnail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotContentThumbnailComponent implements OnInit {
    url: string;
    type: string;
    subType: string;

    thumbnailIcon: string;

    @Input() tempUrl: string;
    @Input() inode: string;
    @Input() name: string;
    @Input() icon: string;
    @Input() contentType: string;
    @Input() iconSize: string;
    @Input() titleImage: string;

    private defaultIcon = 'pi-file';

    ngOnInit(): void {
        const [type, subtype] = this.contentType.split('/') || [];
        this.url = this.tempUrl || this.getThumbnailUrl();
        this.type = type;
        this.thumbnailIcon = this.icon || ICON_MAP[subtype] || this.defaultIcon;
    }

    handleError() {
        this.type = 'icon';
    }

    private getThumbnailUrl(): string {
        return this.contentType === 'application/pdf'
            ? `/contentAsset/image/${this.inode}/${this.titleImage}/pdf_page/1/resize_w/250/quality_q/45`
            : `/dA/${this.inode}/500w/50q`;
    }
}
