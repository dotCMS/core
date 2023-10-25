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

import { DotContentThumbnailComponent, DotSpinnerModule } from '@dotcms/ui';

import { BinaryFile } from '../../interfaces';

export enum EDITABLE_CONTENT {
    image = 'image',
    text = 'text',
    icon = 'icon'
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
    @Input() file: BinaryFile;
    @Input() variableName: string;

    @Output() editFile: EventEmitter<{
        content?: string;
    }> = new EventEmitter();

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    private readonly editableFiles = {
        [EDITABLE_CONTENT.image]: true,
        [EDITABLE_CONTENT.text]: true
    };
    readonly EDITABLE_CONTENT = EDITABLE_CONTENT;

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
        const contenttype = EDITABLE_CONTENT[type];

        this.isEditable = this.editableFiles[contenttype] || this.file.content;
    }
}
