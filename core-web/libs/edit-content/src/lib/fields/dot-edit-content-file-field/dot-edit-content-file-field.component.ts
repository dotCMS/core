import {
    ChangeDetectionStrategy,
    Component,
    forwardRef,
    inject,
    input,
    signal,
    OnInit
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotDropZoneComponent,
    DotMessagePipe,
    DotAIImagePromptComponent,
    DotSpinnerModule
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

    private onChange: (value: string) => void;
    private onTouched: () => void;

    $value = signal('');

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
        this.$value.set(value);
    }
    registerOnChange(fn: (value: string) => void) {
        this.onChange = fn;
    }

    registerOnTouched(fn: () => void) {
        this.onTouched = fn;
    }
}
