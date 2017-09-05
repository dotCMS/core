import { trigger, state, style, transition, animate } from '@angular/animations';
import { Component, ViewChild, Input, Output, EventEmitter, Renderer2, OnInit, OnChanges } from '@angular/core';
import { NgForm, FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';

import { Observable } from 'rxjs/Observable';
import { SplitButtonModule, MenuItem, ConfirmationService, SelectItem } from 'primeng/primeng';

import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { DotcmsConfig } from 'dotcms-js/dotcms-js';
import { MessageService } from '../../../api/services/messages-service';
import { SiteSelectorComponent } from '../../../view/components/_common/site-selector/site-selector.component';

/**
 * Form component to create or edit content types
 *
 * @export
 * @class ContentTypesFormComponent
 * @extends {BaseComponent}
 */
@Component({
    animations: [
        trigger('enterAnimation', [
            state(
                'expanded',
                style({
                    height: '*',
                    overflow: 'visible'
                })
            ),
            state(
                'collapsed',
                style({
                    height: '0px',
                    overflow: 'hidden'
                })
            ),
            transition('expanded <=> collapsed', animate('250ms ease-in-out'))
        ])
    ],
    providers: [SiteSelectorComponent],
    selector: 'content-types-form',
    styleUrls: ['./content-types-form.component.scss'],
    templateUrl: 'content-types-form.component.html'
})
export class ContentTypesFormComponent extends BaseComponent implements OnInit, OnChanges {
    @Input() data: any;
    @Input() icon: string;
    @Input() name: string;
    @Input() type: string;
    @Output() onCancel: EventEmitter<any> = new EventEmitter();
    @Output() onSubmit: EventEmitter<any> = new EventEmitter();
    @Output() onDelete: EventEmitter<any> = new EventEmitter();

    @ViewChild('contentTypesForm') contentTypesForm: NgForm;

    public actionButtonLabel: string;
    public form: FormGroup;
    public formState = 'collapsed';
    public submitAttempt = false;
    public formOptions: MenuItem[];
    private dateVarOptions: SelectItem[] = [];
    private workflowOptions: SelectItem[] = [];

    constructor(
        public messageService: MessageService,
        private renderer: Renderer2,
        private fb: FormBuilder,
        private dotcmsConfig: DotcmsConfig
    ) {
        super(
            [
                'contenttypes.form.field.detail.page',
                'contenttypes.form.field.expire.date.field',
                'contenttypes.form.field.host_folder.label',
                'contenttypes.form.identifier',
                'contenttypes.form.message.no.date.fields.defined',
                'contenttypes.form.label.publish.date.field',
                'contenttypes.hint.URL.map.pattern.hint1',
                'contenttypes.form.label.URL.pattern',
                'contenttypes.content.variable',
                'contenttypes.form.label.workflow',
                'contenttypes.form.hint.error.only.default.scheme.available.in.Community',
                'contenttypes.action.cancel',
                'contenttypes.form.label.description',
                'contenttypes.form.name',
                'contenttypes.action.save',
                'contenttypes.action.update',
                'contenttypes.action.edit',
                'contenttypes.action.delete',
                'contenttypes.form.name.error.required'
            ],
            messageService
        );
    }

    ngOnInit(): void {
        this.initWorkflowtFieldOptions();

        this.messageService.messageMap$.subscribe(res => {
            this.actionButtonLabel = this.isEditMode
                ? this.i18nMessages['contenttypes.action.update']
                : this.i18nMessages['contenttypes.action.save'];
            this.formOptions = [
                {
                    command: this.toggleForm.bind(this),
                    label: this.i18nMessages['contenttypes.action.edit']
                },
                {
                    command: () => {
                        this.onDelete.emit();
                    },
                    label: this.i18nMessages['contenttypes.action.delete']
                }
            ];
        });

        this.dotcmsConfig.getConfig().subscribe(this.updateFormControls.bind(this));
    }

    ngOnChanges(changes): void {
        const isFirstChange =
            (changes.data && changes.data.firstChange) ||
            (changes.name && changes.name.firstChange) ||
            (changes.type && changes.type.firstChange) ||
            (changes.icon && changes.icon.firstChange);

        if (isFirstChange) {
            this.initFormGroup();
        }

        if (changes.data && changes.data.currentValue) {
            this.populateForm();
            this.addEditModeSpecificFields();
        }

        if (changes.type && changes.type.currentValue === 'content') {
            this.addContentSpecificFields();
        }
    }

    /**
     * Trigger the even of cancel form
     * @param $event
     */
    public onCancelHandle($event): void {
        this.onCancel.emit($event);
    }

    /**
     * Reset from to basic state
     * @memberof ContentTypesFormComponent
     */
    public resetForm(): void {
        this.formState = 'collapsed';
        this.submitAttempt = false;
    }

    /**
     * Set the variable property base on the name and sbmit the form if it's valid
     * @memberof ContentTypesFormComponent
     */
    public submitContent($event): void {
        if (!this.submitAttempt) {
            this.submitAttempt = true;
        }

        if (this.form.valid) {
            this.onSubmit.emit({
                originalEvent: $event,
                value: this.form.value
            });
        }
    }

    /**
     * Toggle the variable that expand or collapse the form
     * @memberof ContentTypesFormComponent
     */
    public toggleForm(): void {
        this.formState = this.formState === 'collapsed' ? 'expanded' : 'collapsed';
    }

    get isEditMode(): boolean {
        return !!this.data;
    }

    private addContentSpecificFields(): void {
        this.form.addControl(
            'detailPage',
            new FormControl((this.data && this.data.detailPage) || '')
        );
        this.form.addControl(
            'urlMapPattern',
            new FormControl((this.data && this.data.urlMapPattern) || '')
        );
    }

    private addEditModeSpecificFields(): void {
        this.dateVarOptions = this.getDateVarOptions(this.data.fields);

        const publishDateVar = new FormControl({
            disabled: !this.dateVarOptions.length,
            value: this.data.publishDateVar || null
        });
        const expireDateVar = new FormControl({
            disabled: !this.dateVarOptions.length,
            value: this.data.expireDateVar || null
        });

        this.form.addControl('publishDateVar', publishDateVar);
        this.form.addControl('expireDateVar', expireDateVar);
    }

    private getDateVarOptions(fields): SelectItem[] {
        const dateVarOptions = fields
            .filter(item => {
                return (
                    item.clazz === 'com.dotcms.contenttype.model.field.ImmutableDateTimeField' &&
                    item.indexed
                );
            })
            .map(item => {
                return {
                    label: item.name,
                    value: item.variable
                };
            });

        if (dateVarOptions.length) {
            dateVarOptions.unshift({
                label: '',
                value: null
            });
        }

        return dateVarOptions;
    }

    private handleDateVarChange($event, field): void {
        const expireDateVar = this.form.get('expireDateVar');
        const publishDateVar = this.form.get('publishDateVar');

        if (field === 'publishDateVar' && expireDateVar.value === $event.value) {
            expireDateVar.patchValue(null);
        }
        if (field === 'expireDateVar' && publishDateVar.value === $event.value) {
            publishDateVar.patchValue(null);
        }
    }

    private initFormGroup(): void {
        this.form = this.fb.group({
            description: '',
            host: '',
            name: ['', [Validators.required]],
            workflow: ''
        });
    }

    private initWorkflowtFieldOptions(): void {
        this.workflowOptions = [
            {
                label: 'Select Workflow',
                value: null
            }
        ];
    }

    private populateForm(): void {
        const formData: any = {
            description: this.data.description || '',
            host: this.data.host || '',
            name: this.data.name || '',
            workflow: this.data.workflow || ''
        };

        if (this.form.get('detailPage')) {
            formData.detailPage = this.data.detailPage || '';
        }

        if (this.form.get('urlMapPattern')) {
            formData.detailPage = this.data.urlMapPattern || '';
        }

        this.form.setValue(formData);
    }

    private updateFormControls(res): void {
        if (res.license.isCommunity) {
            this.form.get('workflow').disable();
        }
    }
}
