import {
    ChangeDetectionStrategy,
    Component,
    computed,
    HostBinding,
    inject,
    input,
    output
} from '@angular/core';
import { ControlContainer, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { DividerModule } from 'primeng/divider';

import { BlockEditorModule } from '@dotcms/block-editor';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotSystemTimezone
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

import { DotCardFieldLabelComponent } from '../../fields/dot-card-field/components/dot-card-field-label.component';
import { DotEditContentBinaryFieldComponent } from '../../fields/dot-edit-content-binary-field/dot-edit-content-binary-field.component';
import { DotEditContentCalendarFieldComponent } from '../../fields/dot-edit-content-calendar-field/dot-edit-content-calendar-field.component';
import { DotEditContentCategoryFieldComponent } from '../../fields/dot-edit-content-category-field/dot-edit-content-category-field.component';
import { DotEditContentCheckboxFieldComponent } from '../../fields/dot-edit-content-checkbox-field/dot-edit-content-checkbox-field.component';
import { DotEditContentCustomFieldComponent } from '../../fields/dot-edit-content-custom-field/dot-edit-content-custom-field.component';
import { DotEditContentFileFieldComponent } from '../../fields/dot-edit-content-file-field/dot-edit-content-file-field.component';
import { DotEditContentHostFolderFieldComponent } from '../../fields/dot-edit-content-host-folder-field/dot-edit-content-host-folder-field.component';
import { DotEditContentJsonFieldComponent } from '../../fields/dot-edit-content-json-field/dot-edit-content-json-field.component';
import { DotEditContentKeyValueComponent } from '../../fields/dot-edit-content-key-value/dot-edit-content-key-value.component';
import { DotEditContentMultiSelectFieldComponent } from '../../fields/dot-edit-content-multi-select-field/dot-edit-content-multi-select-field.component';
import { DotEditContentRadioFieldComponent } from '../../fields/dot-edit-content-radio-field/dot-edit-content-radio-field.component';
import { DotEditContentRelationshipFieldComponent } from '../../fields/dot-edit-content-relationship-field/dot-edit-content-relationship-field.component';
import { DotEditContentSelectFieldComponent } from '../../fields/dot-edit-content-select-field/dot-edit-content-select-field.component';
import { DotEditContentTagFieldComponent } from '../../fields/dot-edit-content-tag-field/dot-edit-content-tag-field.component';
import { DotEditContentTextAreaComponent } from '../../fields/dot-edit-content-text-area/dot-edit-content-text-area.component';
import { DotEditContentTextFieldComponent } from '../../fields/dot-edit-content-text-field/dot-edit-content-text-field.component';
import { DotEditContentWYSIWYGFieldComponent } from '../../fields/dot-edit-content-wysiwyg-field/dot-edit-content-wysiwyg-field.component';
import { CALENDAR_FIELD_TYPES } from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';

@Component({
    selector: 'dot-edit-content-field',
    templateUrl: './dot-edit-content-field.component.html',
    styleUrls: ['./dot-edit-content-field.component.scss'],
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        DotEditContentTextAreaComponent,
        DotEditContentRadioFieldComponent,
        DotEditContentSelectFieldComponent,
        DotEditContentTextFieldComponent,
        DotEditContentCalendarFieldComponent,
        DotEditContentTagFieldComponent,
        DotEditContentCheckboxFieldComponent,
        DotEditContentMultiSelectFieldComponent,
        DotEditContentBinaryFieldComponent,
        DotEditContentJsonFieldComponent,
        DotEditContentCustomFieldComponent,
        DotEditContentWYSIWYGFieldComponent,
        DotEditContentHostFolderFieldComponent,
        DotEditContentCategoryFieldComponent,
        BlockEditorModule,
        DotEditContentKeyValueComponent,
        DotEditContentWYSIWYGFieldComponent,
        DotEditContentFileFieldComponent,
        DotEditContentRelationshipFieldComponent,
        DividerModule,
        DotCardFieldLabelComponent
    ]
})
export class DotEditContentFieldComponent {
    readonly #globalStore = inject(GlobalStore);
    #parentForm = inject(ControlContainer, { skipSelf: true })?.control as FormGroup;

    @HostBinding('class') class = 'field';

    /**
     * The field.
     */
    $field = input<DotCMSContentTypeField>(null, { alias: 'field' });

    /**
     * The contentlet.
     */
    $contentlet = input<DotCMSContentlet | undefined>(null, { alias: 'contentlet' });

    /**
     * The content type.
     */
    $contentType = input<DotCMSContentType | null>(null, { alias: 'contentType' });

    /**
     * Event emitted when disabledWYSIWYG changes in any field component.
     * Emits the updated disabledWYSIWYG array.
     */
    disabledWYSIWYGChange = output<string[]>();

    /**
     * The system timezone from the global store.
     */
    $systemTimezone = computed((): DotSystemTimezone | null => {
        return this.#globalStore.systemConfig()?.systemTimezone ?? null;
    });

    /**
     * The field types.
     */
    readonly fieldTypes = FIELD_TYPES;

    /**
     * The calendar types.
     */
    readonly calendarTypes = CALENDAR_FIELD_TYPES as string[];

    /**
     * Whether to show the label.
     */
    $showLabel = computed(() => {
        const field = this.$field();
        if (!field) return true;

        return field.fieldVariables.find(({ key }) => key === 'hideLabel')?.value !== 'true';
    });

    /**
     * Event emitted when the binary field value is updated.
     * @param event
     */
    onBinaryFieldValueUpdated(event: { value: string; fileName: string }) {
        if (!this.shouldAutoFillFields(this.$contentType())) {
            return;
        }

        const { fileName } = event;

        const titleControl = this.#parentForm.get('title');
        const fileNameControl = this.#parentForm.get('fileName');

        // Auto-fill title if exists and is empty
        if (titleControl && !titleControl.value) {
            titleControl.setValue(fileName);
            titleControl.markAsTouched();
        }

        // Auto-fill fileName if exists and is empty
        if (fileNameControl && !fileNameControl.value) {
            fileNameControl.setValue(fileName);
            fileNameControl.markAsTouched();
        }
    }

    /**
     * Whether to auto-fill the fields.
     * @param contentType
     * @returns
     */
    private shouldAutoFillFields(contentType: DotCMSContentType | null): boolean {
        if (!contentType) return false;

        return contentType.baseType === DotCMSBaseTypesContentTypes.FILEASSET;
    }
}
