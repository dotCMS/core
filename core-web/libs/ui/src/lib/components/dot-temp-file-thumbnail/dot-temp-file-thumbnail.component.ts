import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';

import { DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';

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
    pdf = 'pdf',
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
    selector: 'dot-temp-file-thumbnail',
    imports: [CommonModule],
    templateUrl: './dot-temp-file-thumbnail.component.html',
    host: {
        class: 'flex h-full w-full items-center justify-center'
    },
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotTempFileThumbnailComponent implements OnInit {
    @Input() tempFile: DotCMSTempFile;
    @Input() iconSize = '1rem';

    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;
    private readonly DEFAULT_ICON = 'pi-file';

    src: string;
    icon: string;
    type: CONTENT_THUMBNAIL_TYPE;

    get name(): string {
        return this.metadata.name;
    }

    get metadata(): DotFileMetadata {
        return this.tempFile.metadata || this.fallbackMetadata;
    }

    get extension(): string {
        return this.metadata.name.split('.').pop();
    }

    get fileType(): string {
        return this.metadata.contentType?.split('/')[0];
    }

    ngOnInit(): void {
        this.icon = ICON_MAP[this.extension] || this.DEFAULT_ICON;
        this.type = this.getThumbnailType();
        this.src = this.tempFile.thumbnailUrl || this.tempFile.referenceUrl;
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
        if (this.metadata.isImage) {
            return CONTENT_THUMBNAIL_TYPE.image;
        }

        if (this.extension == 'pdf') {
            return CONTENT_THUMBNAIL_TYPE.pdf;
        }

        return CONTENT_THUMBNAIL_TYPE[this.fileType] || CONTENT_THUMBNAIL_TYPE.icon;
    }

    private get fallbackMetadata(): DotFileMetadata {
        return {
            contentType: this.tempFile?.mimeType || '',
            fileSize: this.tempFile?.length || 0,
            isImage: this.tempFile?.image || false,
            length: this.tempFile?.length || 0,
            modDate: 0,
            name: this.tempFile?.fileName || '',
            sha256: '',
            title: this.tempFile?.fileName || '',
            version: 0
        };
    }
}
