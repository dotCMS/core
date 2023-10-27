import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotContentThumbnailComponent, DotMessagePipe, DotSpinnerModule } from '@dotcms/ui';

import { BinaryFile } from '../../interfaces';

export enum EDITABLE_CONTENT {
    image = 'image',
    text = 'text'
}

type EDITABLE_CONTENT_FUNTION_MAP = {
    [key in EDITABLE_CONTENT]: () => boolean;
};

@Component({
    selector: 'dot-binary-field-preview',
    standalone: true,
    imports: [
        CommonModule,
        ButtonModule,
        DotContentThumbnailComponent,
        DotSpinnerModule,
        OverlayPanelModule,
        DotMessagePipe
    ],
    templateUrl: './dot-binary-field-preview.component.html',
    styleUrls: ['./dot-binary-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldPreviewComponent implements OnChanges {
    @Input() file: BinaryFile;
    @Input() editableImage: boolean;

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    private readonly editableFiles: EDITABLE_CONTENT_FUNTION_MAP = {
        [EDITABLE_CONTENT.image]: () => this.editableImage,
        [EDITABLE_CONTENT.text]: () => !!this.file?.content
    };
    private contenttype: EDITABLE_CONTENT;
    readonly EDITABLE_CONTENT = EDITABLE_CONTENT;

    isEditable = true;

    ngOnChanges(): void {
        this.setIsEditable();
    }

    onEdit(): void {
        if (this.contenttype === EDITABLE_CONTENT.image) {
            this.editImage.emit();

            return;
        }

        this.editFile.emit();
    }

    private setIsEditable() {
        const type = this.file.mimeType?.split('/')[0];
        this.contenttype = EDITABLE_CONTENT[type];

        this.isEditable = this.editableFiles[this.contenttype]?.();
    }
}
