import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ControlContainer, ReactiveFormsModule } from '@angular/forms';

import { DialogService } from 'primeng/dynamicdialog';

import { DotWorkflowActionsFireService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotFileFieldComponent } from './components/dot-file-field/dot-file-field.component';
import { DotFileFieldUploadService } from './services/upload-file/upload-file.service';
import { FileFieldStore } from './store/file-field.store';

import { DotCardFieldContentComponent } from '../dot-card-field/components/dot-card-field-content.component';
import { DotCardFieldFooterComponent } from '../dot-card-field/components/dot-card-field-footer.component';
import { DotCardFieldLabelComponent } from '../dot-card-field/components/dot-card-field-label/dot-card-field-label.component';
import { DotCardFieldComponent } from '../dot-card-field/dot-card-field.component';
import { BaseWrapperField } from '../shared/base-wrapper-field';

@Component({
    selector: 'dot-edit-content-file-field',
    imports: [
        DotMessagePipe,
        DotCardFieldComponent,
        DotCardFieldContentComponent,
        DotCardFieldFooterComponent,
        DotCardFieldLabelComponent,
        DotFileFieldComponent,
        DotMessagePipe,
        ReactiveFormsModule
    ],
    providers: [
        DotFileFieldUploadService,
        FileFieldStore,
        DialogService,
        DotWorkflowActionsFireService
    ],
    templateUrl: './dot-edit-content-file-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ]
})
export class DotEditContentFileFieldComponent extends BaseWrapperField {
    /**
     * DotCMS Content Type Field
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * DotCMS Contentlet
     *
     * @memberof DotEditContentFileFieldComponent
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * Emits when the field value changes due to a user action. Bubbled from the
     * inner file field so the parent can sync FileAsset title/fileName.
     */
    valueUpdated = output<{ value: string; fileName: string }>();

    /**
     * Intercepts `valueUpdated` from the inner `dot-file-field` and patches the
     * FormControl directly.
     *
     * When this wrapper is rendered inside an `@defer` block the `formControlName`
     * on `dot-file-field` cannot reach the parent `ControlContainer` through the
     * deferred view's injector chain, so `registerOnChange` / `writeValue` are
     * never called and the CVA contract is silently broken.  The inner component
     * still knows its value (store) and emits `valueUpdated` for every user-driven
     * change (upload, remove, image edit, import).  Patching `formControl` here
     * ensures the reactive-form value stays in sync regardless of the CVA state.
     */
    onInnerValueUpdated(event: { value: string; fileName: string }): void {
        const control = this.formControl;

        if (control) {
            control.setValue(event.value, { emitEvent: true });
            control.markAsTouched();
        }

        this.valueUpdated.emit(event);
    }
}
