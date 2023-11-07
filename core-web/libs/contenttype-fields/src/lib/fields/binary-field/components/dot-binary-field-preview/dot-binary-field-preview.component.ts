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
    DotMessagePipe,
    DotSpinnerModule,
    DotThumbnailOptions
} from '@dotcms/ui';

import { BinaryFile } from '../../interfaces';

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

    private readonly EDITABLE_FILE_FUNCTION_MAP: EDITABLE_FILE_FUNCTION_MAP = {
        [EDITABLE_FILE.image]: () => this.editableImage,
        [EDITABLE_FILE.text]: () => !!this.file?.content,
        [EDITABLE_FILE.unknown]: () => !!this.file?.content
    };
    private contenttype: EDITABLE_FILE;
    isEditable = false;

    get dotThumbnailOptions(): DotThumbnailOptions {
        return {
            tempUrl: this.file.url,
            inode: this.file.inode,
            name: this.file.name,
            contentType: this.file.mimeType,
            iconSize: '48px',
            titleImage: this.file.name
        };
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
        const type = this.file.mimeType?.split('/')[0];
        this.contenttype = EDITABLE_FILE[type];
        // If the file an unknown type, we check if it has content
        this.isEditable = this.contenttype
            ? this.EDITABLE_FILE_FUNCTION_MAP[this.contenttype]()
            : !!this.file?.content;
    }
}
