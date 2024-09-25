import {
    ChangeDetectionStrategy,
    Component,
    effect,
    forwardRef,
    inject,
    input,
    OnInit
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotAIImagePromptComponent,
    DotSpinnerModule,
    DropZoneFileEvent,
    DropZoneFileValidity
} from '@dotcms/ui';

import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { INPUT_TYPES } from './models';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';
import { getUiMessage } from './utils/messages';

@Component({
    selector: 'dot-edit-content-file-field',
    standalone: true,
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotDropZoneComponent,
        DotAIImagePromptComponent,
        DotSpinnerModule,
        DotFileFieldUiMessageComponent,
        DotFileFieldPreviewComponent
    ],
    providers: [
        DotFileFieldUploadService,
        FileFieldStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentFileFieldComponent)
        }
    ],
    templateUrl: './dot-edit-content-file-field.component.html',
    styleUrls: ['./dot-edit-content-file-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFileFieldComponent implements ControlValueAccessor, OnInit {
    readonly store = inject(FileFieldStore);

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });

    private onChange: (value: string | File) => void;
    private onTouched: () => void;

    constructor() {
        effect(() => {
            const value = this.store.value();
            this.onChange(value);
            this.onTouched();
        });
    }

    ngOnInit() {
        const field = this.$field();

        this.store.initLoad({
            fieldVariable: field.variable,
            inputType: field.fieldType as INPUT_TYPES
        });
    }

    writeValue(value: string): void {
        if (!value) {
            return;
        }

        this.store.getAssetData(value);
    }
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }

    /**
     * Handle file drop
     *
     * @param {DropZoneFileEvent} { validity, file }
     * @return {*}
     * @memberof DotEditContentBinaryFieldComponent
     */
    handleFileDrop({ validity, file }: DropZoneFileEvent): void {
        if (!file) {
            return;
        }

        if (!validity.valid) {
            this.handleFileDropError(validity);

            return;
        }

        this.store.handleUploadFile(file);
    }

    fileSelected(files: FileList) {
        const file = files[0];

        if (!file) {
            return;
        }

        this.store.handleUploadFile(file);
    }

    private handleFileDropError({ errorsType }: DropZoneFileValidity): void {
        const errorType = errorsType[0];
        const uiMessage = getUiMessage(errorType);
        this.store.setUIMessage(uiMessage);
    }
}
