import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
    OnDestroy
} from '@angular/core';
import {
    UntypedFormGroup,
    UntypedFormBuilder,
    Validators,
    UntypedFormControl
} from '@angular/forms';

import { Observable, Subject } from 'rxjs';
import { take, takeUntil, filter } from 'rxjs/operators';

import * as _ from 'lodash';
import { SelectItem } from 'primeng/api';

import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import {
    DotCMSContentType,
    DotCMSContentTypeField,
    DotCMSContentTypeLayoutRow,
    DotCMSSystemActionMappings,
    DotCMSSystemActionType,
    DotCMSWorkflow,
    DotCMSSystemAction
} from '@dotcms/dotcms-models';
import { FieldUtil } from '../fields/util/field-util';

/**
 * Form component to create or edit content types
 *
 * @export
 * @class ContentTypesFormComponent
 * @implements {OnInit}
 */
@Component({
    providers: [],
    selector: 'dot-content-types-form',
    styleUrls: ['./content-types-form.component.scss'],
    templateUrl: 'content-types-form.component.html'
})
export class ContentTypesFormComponent implements OnInit, OnDestroy {
    @ViewChild('name', { static: true }) name: ElementRef;

    @Input() data: DotCMSContentType;

    @Input() layout: DotCMSContentTypeLayoutRow[];

    @Output() send: EventEmitter<DotCMSContentType> = new EventEmitter();

    @Output() valid: EventEmitter<boolean> = new EventEmitter();

    canSave = false;
    dateVarOptions: SelectItem[] = [];
    form: UntypedFormGroup;
    nameFieldLabel: string;
    workflowsSelected$: Observable<string[]>;

    private originalValue: DotCMSContentType;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private fb: UntypedFormBuilder,
        private dotWorkflowService: DotWorkflowService,
        private dotLicenseService: DotLicenseService,
        private dotMessageService: DotMessageService
    ) {}

    ngOnInit(): void {
        this.initFormGroup();
        this.initWorkflowField();
        this.bindActionButtonState();

        this.nameFieldLabel = this.setNameFieldLabel();
        this.name.nativeElement.focus();
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
        return !!(this.data && this.data.id);
    }

    /**
     * If form is valid emit form submit event
     *
     * @memberof ContentTypesFormComponent
     */
    submitForm(): void {
        if (this.canSave) {
            this.send.emit(this.form.value);
        }
    }

    private setNameFieldLabel(): string {
        const type = this.data.baseType.toLowerCase();

        return `${this.dotMessageService.get(
            `contenttypes.content.${type}`
        )} ${this.dotMessageService.get('contenttypes.form.name')} *`;
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

        this.valid.next(this.canSave);
    }

    private getDateVarFieldOption(field: DotCMSContentTypeField): SelectItem {
        return {
            label: field.name,
            value: field.variable
        };
    }

    private getDateVarOptions(): SelectItem[] {
        const dateVarOptions = FieldUtil.getFieldsWithoutLayout(this.layout)
            .filter((field: DotCMSContentTypeField) => this.isDateVarField(field))
            .map((field: DotCMSContentTypeField) => this.getDateVarFieldOption(field));

        if (dateVarOptions.length) {
            dateVarOptions.unshift({
                label: '',
                value: ''
            });
        }

        return this.isNewDateVarFields(dateVarOptions) ? dateVarOptions : [];
    }

    private initFormGroup(): void {
        this.form = this.fb.group({
            defaultType: this.data.defaultType,
            icon: this.data.icon,
            fixed: this.data.fixed,
            system: this.data.system,
            clazz: this.getProp(this.data.clazz),
            description: this.getProp(this.data.description),
            host: this.getProp(this.data.host),
            folder: this.getProp(this.data.folder),
            expireDateVar: [{ value: this.getProp(this.data.expireDateVar), disabled: true }],
            name: [this.getProp(this.data.name), [Validators.required]],
            publishDateVar: [{ value: this.getProp(this.data.publishDateVar), disabled: true }],
            workflows: [
                {
                    value: this.data.workflows || [],
                    disabled: true
                }
            ],
            systemActionMappings: this.fb.group({
                [DotCMSSystemActionType.NEW]: [
                    {
                        value: this.data.systemActionMappings
                            ? this.getActionIdentifier(this.data.systemActionMappings)
                            : '',
                        disabled: true
                    }
                ]
            })
        });

        this.setBaseTypeContentSpecificFields();
        this.setOriginalValue();
        this.setDateVarFieldsState();
        this.setSystemWorkflow();
        this.workflowsSelected$ = this.form.get('workflows').valueChanges;
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
        return this.data && this.data.baseType === 'CONTENT';
    }

    private isDateVarField(field: DotCMSContentTypeField): boolean {
        return (
            field.clazz === 'com.dotcms.contenttype.model.field.ImmutableDateTimeField' &&
            field.indexed
        );
    }

    private isFormValueUpdated(): boolean {
        return !_.isEqual(this.form.value, this.originalValue);
    }

    private isNewDateVarFields(newOptions: SelectItem[]): boolean {
        return this.dateVarOptions.length !== newOptions.length;
    }

    private setBaseTypeContentSpecificFields(): void {
        if (this.isBaseTypeContent()) {
            this.form.addControl(
                'detailPage',
                new UntypedFormControl(this.getProp(this.data.detailPage))
            );
            this.form.addControl(
                'urlMapPattern',
                new UntypedFormControl(this.getProp(this.data.urlMapPattern))
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
        return !!(this.layout && this.layout.length);
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
}
