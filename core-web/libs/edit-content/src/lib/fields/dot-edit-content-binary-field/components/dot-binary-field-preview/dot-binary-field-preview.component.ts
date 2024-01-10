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

import {
    DotContentThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotSpinnerModule
} from '@dotcms/ui';

import { DotFilePreview } from '../../interfaces';

export enum EDITABLE_FILE {
    image = 'image',
    text = 'text',
    unknown = 'unknown'
}

type EDITABLE_FILE_FUNCTION_MAP = {
    [key in EDITABLE_FILE]: () => boolean;
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
        DotMessagePipe,
        DotFileSizeFormatPipe
    ],
    templateUrl: './dot-binary-field-preview.component.html',
    styleUrls: ['./dot-binary-field-preview.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryFieldPreviewComponent implements OnChanges {
    @Input() file: DotFilePreview;
    @Input() editableImage: boolean;

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    private readonly EDITABLE_FILE_FUNCTION_MAP: EDITABLE_FILE_FUNCTION_MAP = {
        [EDITABLE_FILE.image]: () => this.editableImage,
        [EDITABLE_FILE.text]: () => !!this.file?.content,
        [EDITABLE_FILE.unknown]: () => !!this.file?.content
    };
    private contenttype: EDITABLE_FILE;
    isEditable = false;

    get objectFit(): string {
        if (this.file?.height > this.file?.width) {
            return 'contain';
        }

        return 'cover';
    }

    ngOnChanges(): void {
        this.setIsEditable();
    }

    /**
     * Emits event to remove the file
     *
     * @return {*}  {void}
     * @memberof DotBinaryFieldPreviewComponent
     */
    onEdit(): void {
        if (this.contenttype === EDITABLE_FILE.image) {
            this.editImage.emit();

            return;
        }

        this.editFile.emit();
    }

    private setIsEditable() {
        const type = this.file.contentType?.split('/')[0];
        this.contenttype = EDITABLE_FILE[type] || EDITABLE_FILE.unknown;
        this.isEditable = this.EDITABLE_FILE_FUNCTION_MAP[this.contenttype]();
    }
}
