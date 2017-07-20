import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { Component, ViewChild, Input, Output, EventEmitter, Renderer2 } from '@angular/core';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { MessageService } from '../../../api/services/messages-service';
import { NgForm, FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { SelectItem } from 'primeng/components/common/api';
import { trigger, state, style, transition, animate } from '@angular/animations';
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
        trigger(
            'enterAnimation', [
                state('expanded', style({
                    height: '*',
                    overflow: 'visible',
                })),
                state('collapsed', style({
                    height: '0px',
                    overflow: 'hidden',
                })),
                transition('expanded <=> collapsed', animate('250ms ease-in-out')),
            ]
        )
    ],
    providers: [SiteSelectorComponent],
    selector: 'content-types-form',
    styles: [require('./content-types-form.component.scss')],
    templateUrl: 'content-types-form.component.html'
})

export class ContentTypesFormComponent extends BaseComponent {
    @Input() data: any;
    @Input() icon: string;
    @Input() name: string;
    @Input() type: string;
    @Output() onCancel: EventEmitter<any> = new EventEmitter();
    @Output() onSubmit: EventEmitter<any> = new EventEmitter();

    @ViewChild('contentTypesForm') contentTypesForm: NgForm;
    public actionButtonLabel: string;
    public form: FormGroup;
    public formState = 'collapsed';
    public submitAttempt = false;
    private dateVarOptions: SelectItem[] = [];
    private workflowOptions: SelectItem[] = [];

    constructor(public messageService: MessageService, private renderer: Renderer2, private fb: FormBuilder,
        private dotcmsConfig: DotcmsConfig) {
        super([
            'Detail-Page',
            'Expire-Date-Field',
            'Host-Folder',
            'Identifier',
            'No-Date-Fields-Defined',
            'Properties',
            'Publish-Date-Field',
            'URL-Map-Pattern-hint1',
            'URL-Pattern',
            'Variable',
            'Workflow',
            'Only-Default-Scheme-is-available-in-Community',
            'cancel',
            'description',
            'name',
            'save',
            'update'
        ], messageService);
    }

    ngOnInit(): void {
        this.initWorkflowtFieldOptions();

        this.messageService.messageMap$.subscribe(res => {
            this.actionButtonLabel = this.isEditMode ? this.i18nMessages['update'] : this.i18nMessages['save'];
        });

        this.dotcmsConfig.getConfig().subscribe(this.updateFormControls.bind(this));
    }

    ngOnChanges(changes): void {
        let isFirstChange =
            changes.data && changes.data.firstChange ||
            changes.name && changes.name.firstChange ||
            changes.type && changes.type.firstChange ||
            changes.icon && changes.icon.firstChange;

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

    ngAfterViewInit(): void {
        let nameEl = this.renderer.selectRootElement('#content-type-form-name');
        nameEl.focus();

        Observable.fromEvent(nameEl, 'keyup')
            .map((event: KeyboardEvent) => event.target)
            .debounceTime(250)
            .subscribe((target: EventTarget) => {
                this.handleNameFielEvent(target);
            });
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
        this.form.addControl('detailPage', new FormControl(this.data && this.data.detailPage  || ''));
        this.form.addControl('urlMapPattern', new FormControl(this.data && this.data.urlMapPattern  || ''));
    }

    private addEditModeSpecificFields(): void {
        this.dateVarOptions = this.getDateVarOptions(this.data.fields);

        let publishDateVar = new FormControl({
            disabled: !this.dateVarOptions.length,
            value: this.data.publishDateVar || null
        });
        let expireDateVar = new FormControl({
            disabled: !this.dateVarOptions.length,
            value: this.data.expireDateVar || null
        });

        this.form.addControl('publishDateVar', publishDateVar);
        this.form.addControl('expireDateVar', expireDateVar);
    }

    private getDateVarOptions(fields): SelectItem[] {
        let dateVarOptions = fields
            .filter(item => {
                return item.clazz === 'com.dotcms.contenttype.model.field.ImmutableDateTimeField' && item.indexed;
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
        let expireDateVar = this.form.get('expireDateVar');
        let publishDateVar = this.form.get('publishDateVar');

        if (field === 'publishDateVar' && expireDateVar.value === $event.value) {
            expireDateVar.patchValue(null);
        }
        if (field === 'expireDateVar' && publishDateVar.value === $event.value) {
            publishDateVar.patchValue(null);
        }
    }

    private handleNameFielEvent(el: EventTarget): void {
        let value: string = (<HTMLInputElement> el).value;
        if (!value && this.formState === 'expanded' || value && value.length && this.formState === 'collapsed') {
            this.toggleForm();
        }
    }

    private initFormGroup(): void {
        this.form = this.fb.group({
            description: '',
            host: '',
            name: ['', [
                Validators.required,
            ]],
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
        let formData: any = {
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
