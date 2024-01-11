import { CommonModule } from '@angular/common';
import {
    CUSTOM_ELEMENTS_SCHEMA,
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotCMSContentlet, DotCMSTempFile, DotFileMetadata } from '@dotcms/dotcms-models';
import {
    DotContentThumbnailComponent,
    DotFileSizeFormatPipe,
    DotMessagePipe,
    DotSpinnerModule
} from '@dotcms/ui';

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
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotBinaryFieldPreviewComponent implements OnChanges {
    @Input() contentlet: DotCMSContentlet;
    @Input() tempFile: DotCMSTempFile;
    @Input() editableImage: boolean;

    @Output() editImage: EventEmitter<void> = new EventEmitter();
    @Output() editFile: EventEmitter<void> = new EventEmitter();
    @Output() removeFile: EventEmitter<void> = new EventEmitter();

    get content(): string {
        return this.tempFile?.content || this.contentlet?.content;
    }

    get metadata(): DotFileMetadata {
        return this.tempFile?.metadata || this.contentletMetadata;
    }

    get contentletMetadata(): DotFileMetadata {
        const { metaData, fieldVariable } = this.contentlet;

        return metaData || this.contentlet[`${fieldVariable}MetaData`];
    }

    private readonly EDITABLE_FILE_FUNCTION_MAP: EDITABLE_FILE_FUNCTION_MAP = {
        [EDITABLE_FILE.image]: () => this.editableImage,
        [EDITABLE_FILE.text]: () => !!this.content,
        [EDITABLE_FILE.unknown]: () => !!this.content
    };
    private contenttype: EDITABLE_FILE;
    isEditable = false;

    get objectFit(): string {
        if (this.metadata?.height > this.metadata?.width) {
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
        const type = this.metadata.contentType?.split('/')[0];
        this.contenttype = EDITABLE_FILE[type] || EDITABLE_FILE.unknown;
        this.isEditable = this.EDITABLE_FILE_FUNCTION_MAP[this.contenttype]();
    }
}
