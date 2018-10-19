import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';

import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import * as _ from 'lodash';
import { SelectItem } from 'primeng/primeng';

import { DotMessageService } from '@services/dot-messages-service';
import { SiteSelectorComponent } from '@components/_common/site-selector/site-selector.component';
import { DotWorkflow } from '@models/dot-workflow/dot-workflow.model';
import { DotWorkflowService } from '@services/dot-workflow/dot-workflow.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';

// TODO: move this to models
import { ContentTypeField } from '../fields';

/**
 * Form component to create or edit content types
 *
 * @export
 * @class ContentTypesFormComponent
 * @implements {OnInit}
 */
@Component({
    providers: [SiteSelectorComponent],
    selector: 'dot-content-types-form',
    styleUrls: ['./content-types-form.component.scss'],
    templateUrl: 'content-types-form.component.html'
})
export class ContentTypesFormComponent implements OnInit {
    @ViewChild('name')
    name: ElementRef;

    @Input()
    data: any;

    @Input()
    fields: ContentTypeField[];

    @Output()
    onSubmit: EventEmitter<any> = new EventEmitter();

    @Output()
    valid: EventEmitter<boolean> = new EventEmitter();

    canSave = false;
    dateVarOptions: SelectItem[] = [];
    form: FormGroup;
    nameFieldLabel: Observable<string>;

    private originalValue: any;

    constructor(
        private fb: FormBuilder,
        private dotWorkflowService: DotWorkflowService,
        private dotLicenseService: DotLicenseService,
        public dotMessageService: DotMessageService
    ) {
        dotMessageService
            .getMessages([
                'contenttypes.action.create',
                'contenttypes.action.delete',
                'contenttypes.action.edit',
                'contenttypes.action.form.cancel',
                'contenttypes.action.save',
                'contenttypes.action.update',
                'contenttypes.form.field.detail.page',
                'contenttypes.form.field.expire.date.field',
                'contenttypes.form.field.host_folder.label',
                'contenttypes.form.hint.error.only.default.scheme.available.in.Community',
                'contenttypes.form.identifier',
                'contenttypes.form.label.URL.pattern',
                'contenttypes.form.label.description',
                'contenttypes.form.label.publish.date.field',
                'contenttypes.form.label.workflow',
                'contenttypes.form.message.no.date.fields.defined',
                'contenttypes.form.name',
                'contenttypes.hint.URL.map.pattern.hint1',
                'dot.common.message.field.required'
            ])
            .subscribe();
    }

    ngOnInit(): void {
        this.initFormGroup();
        this.initWorkflowField();
        this.bindActionButtonState();

        this.nameFieldLabel = this.setNameFieldLabel();
        this.name.nativeElement.focus();

        if (!this.isEditMode()) {
            this.dotWorkflowService.getSystem().subscribe((workflow: DotWorkflow) => {
                this.form.get('workflow').setValue([workflow.id]);
            });
        }
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
            this.onSubmit.emit({
                ...this.form.value,
                workflow: this.form.getRawValue().workflow
            });
        }
    }

    private setNameFieldLabel(): Observable<string> {
        return this.dotMessageService.messageMap$.pipe(
            map(() => {
                const type = this.data.baseType.toLowerCase();
                return `${this.dotMessageService.get(`contenttypes.content.${type}`)}
            ${this.dotMessageService.get('contenttypes.form.name')} *`;
            })
        );
    }

    private bindActionButtonState(): void {
        this.form.valueChanges.subscribe(() => {
            this.setSaveState();
        });
    }

    private setSaveState() {
        this.canSave = this.isEditMode()
            ? this.form.valid && this.isFormValueUpdated()
            : this.form.valid;

        this.valid.next(this.canSave);
    }

    private getDateVarFieldOption(field: ContentTypeField): SelectItem {
        return {
            label: field.name,
            value: field.variable
        };
    }

    private getDateVarOptions(): SelectItem[] {
        const dateVarOptions = this.fields
            .filter((field: ContentTypeField) => this.isDateVarField(field))
            .map((field: ContentTypeField) => this.getDateVarFieldOption(field));

        if (dateVarOptions.length) {
            dateVarOptions.unshift({
                label: '',
                value: ''
            });
        }

        return dateVarOptions;
    }

    private initFormGroup(): void {
        this.form = this.fb.group({
            clazz: this.data.clazz || '',
            description: this.data.description || '',
            expireDateVar: [{ value: this.data.expireDateVar || '', disabled: true }],
            host: this.data.host || '',
            name: [this.data.name || '', [Validators.required]],
            publishDateVar: [{ value: this.data.publishDateVar || '', disabled: true }],
            workflow: [
                {
                    value: this.data.workflows
                        ? this.data.workflows.map((workflow) => workflow.id)
                        : [],
                    disabled: true
                }
            ],
            defaultType: this.data.defaultType,
            fixed: this.data.fixed,
            folder: this.data.folder,
            system: this.data.system
        });

        if (this.isBaseTypeContent()) {
            this.setBaseTypeContentSpecificFields();
        }

        if (this.isEditMode() && !this.originalValue) {
            this.originalValue = this.form.value;
        }

        if (this.fields && this.fields.length) {
            this.setDateVarFieldsState();
        }
    }

    private initWorkflowField(): void {
        this.dotLicenseService.isEnterprise().subscribe((isEnterpriseLicense: boolean) => {
            this.updateWorkflowFormControl(isEnterpriseLicense);
        });
    }

    private isBaseTypeContent(): boolean {
        return this.data && this.data.baseType === 'CONTENT';
    }

    private isDateVarField(field: ContentTypeField): boolean {
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
        this.form.addControl('detailPage', new FormControl(this.data.detailPage || ''));
        this.form.addControl(
            'urlMapPattern',
            new FormControl((this.data && this.data.urlMapPattern) || '')
        );
    }

    private setDateVarFieldsState(): void {
        const dateVarNewOptions = this.getDateVarOptions();

        if (this.isNewDateVarFields(dateVarNewOptions)) {
            this.dateVarOptions = dateVarNewOptions;
        }

        const publishDateVar = this.form.get('publishDateVar');
        const expireDateVar = this.form.get('expireDateVar');

        if (this.dateVarOptions.length) {
            publishDateVar.enable();
            expireDateVar.enable();

            if (this.originalValue) {
                this.originalValue.publishDateVar = publishDateVar.value;
                this.originalValue.expireDateVar = expireDateVar.value;
            }
        } else {
            publishDateVar.disable();
            expireDateVar.disable();

            if (this.originalValue) {
                delete this.originalValue.publishDateVar;
                delete this.originalValue.expireDateVar;
            }
        }

        this.setSaveState();
    }

    private updateWorkflowFormControl(isEnterpriseLicense: boolean): void {
        if (isEnterpriseLicense) {
            const workflowControl = this.form.get('workflow');

            workflowControl.enable();

            if (this.originalValue) {
                this.originalValue.workflow = workflowControl.value;
            }
            this.setSaveState();
        }
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
