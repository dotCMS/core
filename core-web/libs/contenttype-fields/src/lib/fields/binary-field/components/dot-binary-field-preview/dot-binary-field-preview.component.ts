import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { CONTENT_THUMBNAIL_TYPE, DotContentThumbnailComponent, DotSpinnerModule } from '@dotcms/ui';

export interface BinaryPreview {
    mimeType: string;
    name: string;
    fileSize: number;
    content?: string;
    url?: string;
    inode?: string;
    titleImage?: string;
    width?: string;
    height?: string;
}

@Component({
    selector: 'dot-binary-field-preview',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotContentThumbnailComponent, DotSpinnerModule],
    templateUrl: './dot-binary-field-preview.component.html',
    styleUrls: ['./dot-binary-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldPreviewComponent implements OnInit {
    @Input() file: BinaryPreview;
    @Input() variableName: string;

    @Output() editFile: EventEmitter<{
        content?: string;
    }> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    private readonly editableFiles = {
        [CONTENT_THUMBNAIL_TYPE.image]: true,
        [CONTENT_THUMBNAIL_TYPE.text]: true
    };
    readonly CONTENT_THUMBNAIL_TYPE = CONTENT_THUMBNAIL_TYPE;

    isEditable = false;

    ngOnInit(): void {
        this.setIsEditable();
    }

    onEdit(): void {
        this.editFile.emit({
            content: this.file.content
        });
    }

    private setIsEditable() {
        const type = this.file.mimeType?.split('/')[0];
        const contenttype = CONTENT_THUMBNAIL_TYPE[type] || CONTENT_THUMBNAIL_TYPE.icon;

        this.isEditable = this.editableFiles[contenttype];
    }
}
