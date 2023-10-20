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

interface Resolution {
    width: string;
    height: string;
}
export interface BinaryPreview {
    type: string;
    resolution?: Resolution;
    fileSize?: number;
    content?: string;
    mimeType: string;
    url?: string;
    inode?: string;
    title: string;
    name: string;
    titleImage?: string;
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
    @Input() previewFile: BinaryPreview;
    @Input() variableName: string;

    @Output() editFile: EventEmitter<string> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    resolution;

    ngOnInit(): void {
        this.resolution = this.previewFile?.resolution;
    }
}
