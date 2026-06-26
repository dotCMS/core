import { Component, Renderer2, inject, viewChild } from '@angular/core';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import {
    DialogButton,
    DotCMSClazzes,
    DotCMSContentType,
    DotCMSContentTypeField,
    DotDialogActions
} from '@dotcms/dotcms-models';

import { ContentTypeFieldsPropertiesFormComponent } from '../content-type-fields-properties-form';
import { FieldType } from '../models';

/** Data passed to the dialog when it is opened via `DialogService.open`. */
export interface DotEditFieldDialogData {
    currentField: DotCMSContentTypeField;
    currentFieldType: FieldType;
    contentType: DotCMSContentType;
}

/** Result returned through `DynamicDialogRef.close()` when the dialog is dismissed. */
export type DotEditFieldDialogResult =
    | { kind: 'saved'; field: DotCMSContentTypeField }
    | { kind: 'convert-to-block'; field: DotCMSContentTypeField }
    | { kind: 'settings-saved' }
    | undefined;

/**
 * Field-edit dialog rendered as a PrimeNG DynamicDialog.
 *
 * Owns all of its own UI state (tabs, action buttons), so every open starts
 * fresh — there is no reset machinery. Results are returned to the opener via
 * `DynamicDialogRef.close()`.
 */
@Component({
    selector: 'dot-edit-field-dialog',
    templateUrl: './dot-edit-field-dialog.component.html',
    standalone: false
})
export class DotEditFieldDialogComponent {
    private readonly config =
        inject<DynamicDialogConfig<DotEditFieldDialogData>>(DynamicDialogConfig);
    private readonly ref = inject(DynamicDialogRef);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly rendered = inject(Renderer2);

    /** Tab index for the Overview (field properties) tab. */
    readonly OVERVIEW_TAB_INDEX = 0;

    /** Tab index for the Settings tab (block-editor, binary, custom-field specific options). */
    readonly SETTINGS_TAB_INDEX = 1;

    /** Reference to the field-properties form rendered inside the dialog. */
    readonly $propertiesForm =
        viewChild.required<ContentTypeFieldsPropertiesFormComponent>('fieldPropertiesForm');

    private readonly data = this.config.data;
    readonly currentField = this.data.currentField;
    readonly currentFieldType = this.data.currentFieldType;
    readonly contentType = this.data.contentType;

    activeTab = 0;
    hideButtons = false;
    saveBtn: DialogButton = {
        label: this.dotMessageService.get('contenttypes.dropzone.action.save'),
        action: () => this.$propertiesForm().saveFieldProperties(),
        disabled: true
    };
    cancelBtn: DialogButton = {
        label: this.dotMessageService.get('contenttypes.dropzone.action.cancel'),
        action: () => this.ref.close()
    };

    private overviewFormChanged = false;

    /**
     * Whether the currently selected field type has a dedicated Settings tab
     * (Block Editor, Binary, or Custom Field).
     */
    get isFieldWithSettings(): boolean {
        return (
            [
                DotCMSClazzes.BLOCK_EDITOR,
                DotCMSClazzes.BINARY,
                DotCMSClazzes.CUSTOM_FIELD
            ] as string[]
        ).includes(this.currentFieldType?.clazz);
    }

    /**
     * Returns the tab index for the "Variables" tab.
     * Shifts to index 2 when a Settings tab is present for the current field type.
     */
    get variablesTabIndex(): number {
        return !!this.currentField?.id && this.isFieldWithSettings ? 2 : 1;
    }

    /**
     * Hide or show the action buttons according to the selected tab, and restore
     * the Overview Save button state when returning to the Overview tab.
     *
     * @param index - The newly selected tab index
     */
    handleTabChange(index: number): void {
        if (index === this.OVERVIEW_TAB_INDEX) {
            this.saveBtn = { ...this.saveBtn, disabled: !this.overviewFormChanged };
        }

        this.hideButtons =
            index !== this.OVERVIEW_TAB_INDEX &&
            !(index === this.SETTINGS_TAB_INDEX && this.isFieldWithSettings);
    }

    /**
     * Set the enabled/disabled state of the Save button based on form changes.
     *
     * @param formChanged - True when the form differs from its initial value
     */
    setDialogOkButtonState(formChanged: boolean): void {
        if (this.activeTab === this.OVERVIEW_TAB_INDEX) {
            this.overviewFormChanged = formChanged;
        }

        this.saveBtn = { ...this.saveBtn, disabled: !formChanged };
    }

    /**
     * Replace the Save button with the controls emitted by a Settings tab,
     * keeping the local Cancel button.
     *
     * @param controls - The dialog actions emitted by a Settings tab
     */
    changesDialogActions(controls: DotDialogActions): void {
        this.saveBtn = controls.accept ?? null;
    }

    /**
     * Close the dialog returning the saved field to the opener.
     *
     * @param field - The field value produced by the properties form
     */
    onPropertiesSaved(field: DotCMSContentTypeField): void {
        this.ref.close({ kind: 'saved', field });
    }

    /** Close the dialog after a Settings tab has persisted its own changes. */
    onSettingsSaved(): void {
        this.ref.close({ kind: 'settings-saved' });
    }

    /** Close the dialog requesting that the WYSIWYG field be converted to a Block field. */
    onConvertToBlock(): void {
        this.ref.close({
            kind: 'convert-to-block',
            field: {
                ...this.currentField,
                clazz: 'com.dotcms.contenttype.model.field.ImmutableStoryBlockField',
                fieldType: 'Story-Block'
            }
        });
    }

    /** Scroll the convert-to-block section into view. */
    scrollTo(): void {
        const el = this.rendered.selectRootElement('dot-convert-wysiwyg-to-block', true);

        el.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
            inline: 'nearest'
        });
    }
}
