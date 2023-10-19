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

import { DotContentThumbnailComponent } from '@dotcms/ui';

export interface BinaryPreview {
    type: string;
    resolution?: {
        width: string;
        height: string;
    };
    fileSize?: number;
    content: string;
    mimeType: string;
    inode: string;
    titleImage: string;
    name: string;
    title: string;
    hasTitleImage: string;
    contentType: string;
    __icon__: string;
    contentTypeIcon: string;
}

@Component({
    selector: 'dot-binary-field-preview',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotContentThumbnailComponent],
    templateUrl: './dot-binary-field-preview.component.html',
    styleUrls: ['./dot-binary-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldPreviewComponent implements OnInit {
    @Input() previewData: BinaryPreview;
    @Input() variableName: string;

    @Output() editFile: EventEmitter<string> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    contentType: string;
    resolution;

    ngOnInit(): void {
        this.contentType = this.previewData.contentType;
        this.resolution = this.previewData.resolution;
    }
}
