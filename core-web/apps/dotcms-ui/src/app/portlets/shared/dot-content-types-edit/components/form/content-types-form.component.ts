import { Observable, Subject } from 'rxjs';

import { AsyncPipe, CommonModule } from '@angular/common';
import {
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { SelectItem } from 'primeng/api';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';

import { filter, startWith, take, takeUntil } from 'rxjs/operators';

import {
    DotLicenseService,
    DotMessageService,
    DotWorkflowService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSSystemAction,
    DotCMSSystemActionMappings,
    DotCMSSystemActionType,
    DotCMSWorkflow,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe,
    DotSiteComponent
} from '@dotcms/ui';
import { FieldUtil, isEqual } from '@dotcms/utils';

import { DotMdIconSelectorComponent } from '../../../../../view/components/_common/dot-md-icon-selector/dot-md-icon-selector.component';
import { DotPageSelectorComponent } from '../../../../../view/components/_common/dot-page-selector/dot-page-selector.component';
import { DotWorkflowsActionsSelectorFieldComponent } from '../../../../../view/components/_common/dot-workflows-actions-selector-field/dot-workflows-actions-selector-field.component';
import { DotWorkflowsActionsSelectorFieldService } from '../../../../../view/components/_common/dot-workflows-actions-selector-field/services/dot-workflows-actions-selector-field.service';
import { DotWorkflowsSelectorFieldComponent } from '../../../../../view/components/_common/dot-workflows-selector-field/dot-workflows-selector-field.component';
import { DotFieldHelperComponent } from '../../../../../view/components/dot-field-helper/dot-field-helper.component';

/**
 * Form component to create or edit content types
 *
 * @export
 * @class ContentTypesFormComponent
 * @implements {OnInit}
 */
@Component({
    providers: [DotWorkflowsActionsService, DotWorkflowsActionsSelectorFieldService],
    selector: 'dot-content-types-form',
    templateUrl: 'content-types-form.component.html',
    imports: [
        CommonModule,
        ReactiveFormsModule,
        AsyncPipe,
        CheckboxModule,
        SelectModule,
        InputTextModule,
        DotMessagePipe,
        DotFieldRequiredDirective,
        DotAutofocusDirective,
        DotFieldValidationMessageComponent,
        DotMdIconSelectorComponent,
        DotSiteComponent,
        DotWorkflowsSelectorFieldComponent,
        DotWorkflowsActionsSelectorFieldComponent,
        DotPageSelectorComponent,
        DotFieldHelperComponent
    ]
})
export class ContentTypesFormComponent implements OnInit, OnDestroy {
    private fb = inject(UntypedFormBuilder);
    private dotWorkflowService = inject(DotWorkflowService);
    private dotLicenseService = inject(DotLicenseService);
    private dotMessageService = inject(DotMessageService);
    private readonly route = inject(ActivatedRoute);

    readonly $inputName = viewChild.required<ElementRef>('name');

    readonly $contentType = input.required<DotCMSContentType>({ alias: 'contentType' });

    readonly send = output<DotCMSContentType>();
    readonly valid = output<boolean>();

    canSave = false;
    dateVarOptions: SelectItem[] = [];
    form: UntypedFormGroup;
    nameFieldLabel: string;
    workflowsSelected$: Observable<DotCMSWorkflow[]>;
    newContentEditorEnabled: boolean;

    private originalValue: DotCMSContentType;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        this.initFormGroup();
        this.initWorkflowField();
        this.bindActionButtonState();

        this.nameFieldLabel = this.setNameFieldLabel();
        this.$inputName().nativeElement.focus();
        this.newContentEditorEnabled =
            this.route.snapshot?.data?.featuredFlags[
                FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
            ];
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Update expireDateVar and publishDateVar fields base on selection
     *
     * @param any $event
     * @param any field
     * @memberof ContentTypesFormComponent
     */
    handleDateVarChange($event, field): void {
        if (field === 'publishDateVar') {
            this.updateExpireDateVar($event.value);
        } else {
            this.updatePublishDateVar($event.value);
        }
    }

    /**
     * Check if the form is in edit mode
     *
     * @returns boolean
     * @memberof ContentTypesFormComponent
     */
    isEditMode(): boolean {
        const data = this.$contentType();
        return !!(data && data.id);
    }

    /**
     * If form is valid emit form submit event
     *
     * @memberof ContentTypesFormComponent
     */
    submitForm(): void {
        if (this.canSave) {
            this.send.emit(this.addMetadataToForm());
        }
    }

    private setNameFieldLabel(): string {
        const type = this.$contentType().baseType.toLowerCase();

        return `${this.dotMessageService.get(
            `contenttypes.content.${type}`
        )} ${this.dotMessageService.get('contenttypes.form.name')}`;
    }

    private bindActionButtonState(): void {
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.setSaveState();
        });
    }

    private setSaveState() {
        this.canSave = this.isEditMode()
            ? this.form.valid && this.isFormValueUpdated()
            : this.form.valid;

        this.valid.emit(this.canSave);
    }

    private getDateVarFieldOption(field: DotCMSContentTypeField): SelectItem {
        return {
            label: field.name,
            value: field.variable
        };
    }

    private getDateVarOptions(): SelectItem[] {
        const dateVarOptions = FieldUtil.getFieldsWithoutLayout(this.$contentType().layout)
            .filter((field: DotCMSContentTypeField) => this.isDateVarField(field))
            .map((field: DotCMSContentTypeField) => this.getDateVarFieldOption(field));

        return this.isNewDateVarFields(dateVarOptions) ? dateVarOptions : [];
    }

    private initFormGroup(): void {
        const data = this.$contentType();
        this.form = this.fb.group({
            defaultType: data.defaultType,
            icon: data.icon,
            fixed: data.fixed,
            system: data.system,
            clazz: this.getProp(data.clazz),
            description: this.getProp(data.description),
            host: this.getProp(data.host),
            folder: this.getProp(data.folder),
            expireDateVar: [{ value: this.getProp(data.expireDateVar), disabled: true }],
            name: [this.getProp(data.name), [Validators.required]],
            publishDateVar: [{ value: this.getProp(data.publishDateVar), disabled: true }],
            workflows: [
                {
                    value: data.workflows || [],
                    disabled: true
                }
            ],
            systemActionMappings: this.fb.group({
                [DotCMSSystemActionType.NEW]: [
                    {
                        value: data.systemActionMappings
                            ? this.getActionIdentifier(data.systemActionMappings)
                            : '',
                        disabled: true
                    }
                ]
            }),
            newEditContent: !!this.getMetaDataProperty(
                FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED
            )
        });

        this.setBaseTypeContentSpecificFields();
        this.setOriginalValue();
        this.setDateVarFieldsState();
        this.setSystemWorkflow();
        this.workflowsSelected$ = this.form
            .get('workflows')
            .valueChanges.pipe(startWith(this.form.get('workflows').value));
    }

    private getActionIdentifier(actionMap: DotCMSSystemActionMappings): string {
        if (Object.keys(actionMap).length) {
            const item = actionMap[DotCMSSystemActionType.NEW];

            return this.getWorkflowActionId(item);
        }

        return '';
    }

    private getWorkflowActionId(item: DotCMSSystemAction | string): string {
        return item && typeof item !== 'string' ? item.workflowAction.id : '';
    }

    private getProp(item: string): string {
        return item || '';
    }

    private setSystemWorkflow(): void {
        if (!this.isEditMode()) {
            this.dotWorkflowService
                .getSystem()
                .pipe(take(1))
                .subscribe((workflow: DotCMSWorkflow) => {
                    this.form.get('workflows').setValue([workflow]);
                });
        }
    }

    private setOriginalValue(): void {
        if (this.isEditMode() && !this.originalValue) {
            this.originalValue = this.form.value;
        }
    }

    private initWorkflowField(): void {
        this.dotLicenseService
            .isEnterprise()
            .pipe(
                take(1),
                filter((isEnterpriseLicense: boolean) => isEnterpriseLicense)
            )
            .subscribe(() => {
                this.enableWorkflowFormControls();
            });
    }

    private isBaseTypeContent(): boolean {
        const data = this.$contentType();
        return data && data.baseType === 'CONTENT';
    }

    private isDateVarField(field: DotCMSContentTypeField): boolean {
        return (
            field.clazz === 'com.dotcms.contenttype.model.field.ImmutableDateTimeField' &&
            field.indexed
        );
    }

    private isFormValueUpdated(): boolean {
        return !isEqual(this.form.value, this.originalValue);
    }

    private isNewDateVarFields(newOptions: SelectItem[]): boolean {
        return this.dateVarOptions.length !== newOptions.length;
    }

    private setBaseTypeContentSpecificFields(): void {
        if (this.isBaseTypeContent()) {
            const data = this.$contentType();
            this.form.addControl(
                'detailPage',
                new UntypedFormControl(this.getProp(data.detailPage))
            );
            this.form.addControl(
                'urlMapPattern',
                new UntypedFormControl(this.getProp(data.urlMapPattern))
            );
        }
    }

    private setDateVarFieldsState(): void {
        if (this.isLayoutSet()) {
            this.dateVarOptions = this.getDateVarOptions();

            const publishDateVar = this.form.get('publishDateVar');
            const expireDateVar = this.form.get('expireDateVar');

            if (this.dateVarOptions.length) {
                publishDateVar.enable();
                expireDateVar.enable();

                if (this.originalValue) {
                    this.originalValue.publishDateVar = publishDateVar.value;
                    this.originalValue.expireDateVar = expireDateVar.value;
                }
            }

            this.setSaveState();
        }
    }

    private isLayoutSet(): boolean {
        const layout = this.$contentType().layout;
        return !!(layout && layout.length);
    }

    private enableWorkflowFormControls(): void {
        const workflowControl = this.form.get('workflows');
        const workflowActionControl = this.form
            .get('systemActionMappings')
            .get(DotCMSSystemActionType.NEW);

        workflowControl.enable();
        workflowActionControl.enable();

        if (this.originalValue) {
            this.originalValue.workflows = workflowControl.value;
            this.originalValue.systemActionMappings = {};
            this.originalValue.systemActionMappings[DotCMSSystemActionType.NEW] =
                workflowActionControl.value;
        }

        this.setSaveState();
    }

    private updateExpireDateVar(value: string): void {
        const expireDateVar = this.form.get('expireDateVar');

        if (expireDateVar.value === value) {
            expireDateVar.patchValue('');
        }
    }

    private updatePublishDateVar(value: string): void {
        const publishDateVar = this.form.get('publishDateVar');

        if (publishDateVar.value === value) {
            publishDateVar.patchValue('');
        }
    }

    private getMetaDataProperty(_prop: string): string | number | boolean {
        return this.$contentType().metadata?.[_prop];
    }

    private addMetadataToForm(): DotCMSContentType {
        const metadata = this.$contentType().metadata || {};
        const newEditContent = this.form.get('newEditContent').value;
        const form = this.form.value;
        delete form.newEditContent;
        metadata[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] = newEditContent;

        return {
            ...form,
            metadata
        };
    }
}
