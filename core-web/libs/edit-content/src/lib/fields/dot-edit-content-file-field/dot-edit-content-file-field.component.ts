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

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotAIImagePromptComponent,
    DotSpinnerModule,
    DropZoneFileEvent
} from '@dotcms/ui';

import { DotFileFieldPreviewComponent } from './components/dot-file-field-preview/dot-file-field-preview.component';
import { DotFileFieldUiMessageComponent } from './components/dot-file-field-ui-message/dot-file-field-ui-message.component';
import { INPUT_TYPES } from './models';
import { FileFieldStore } from './store/file-field.store';

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
    readonly #messageService = inject(DotMessageService);

    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    $fieldVariable = input.required<string>({ alias: 'fieldVariable' });

    private onChange: (value: string) => void;
    private onTouched: () => void;

    constructor() {
        effect(() => {
            const value = this.store.value();
            console.log('current value', value);
            this.onChange(value);
            this.onTouched();
        });
    }

    ngOnInit() {
        this.store.initLoad({
            inputType: this.$field().fieldType as INPUT_TYPES,
            uiMessage: {
                message: this.#messageService.get('dot.file.field.drag.and.drop.message'),
                severity: 'info',
                icon: 'pi pi-upload'
            }
        });
    }

    writeValue(value: string): void {
        this.store.setValue(value);
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
            //this.handleFileDropError(validity);

            return;
        }

        const fileList = new FileList();
        fileList[0] = file;

        this.store.handleUploadFile(fileList);
    }
}
