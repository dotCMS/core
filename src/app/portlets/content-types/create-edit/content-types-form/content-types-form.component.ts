import { BaseComponent } from '../../../../view/components/_common/_base/base-component';
import { Component, Renderer, ViewEncapsulation, ViewChild, ElementRef, Input } from '@angular/core';
import { ContentType } from '../main';
import { ContentTypesInfoService } from '../../../../api/services/content-types-info';
import { CrudService } from '../../../../api/services/crud';
import { LoginService } from '../../../../api/services/login-service';
import { MessageService } from '../../../../api/services/messages-service';
import { NgForm, FormGroup, FormBuilder, Validators, FormControl } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { Router, ActivatedRoute } from '@angular/router';
import { SelectItem } from 'primeng/components/common/api';
import { Site } from '../../../../api/services/site-service';
import { StringUtils } from '../../../../api/util/string.utils';
import { trigger, state, style, transition, animate } from '@angular/animations';

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
    encapsulation: ViewEncapsulation.None,
    selector: 'content-types-form',
    styles: [require('./content-types-form.component.scss')],
    templateUrl: 'content-types-form.component.html'
})
export class ContentTypesFormComponent extends BaseComponent {
    @Input() data: any;
    @ViewChild('contentTypesForm') contentTypesForm: NgForm;
    @ViewChild('nameEl') nameEl: ElementRef;
    public actionButtonLabel: string;
    public fieldNamePlaceholder: string;
    public form: FormGroup;
    public formData: ContentType;
    public formState = 'collapsed';
    public readyToAddFields = false;
    public submitAttempt = false;
    private expireDateFieldOptions: SelectItem[] = [];
    private icon: string;
    private initialFormData: ContentType = {
        clazz: null,
        defaultType: false,
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: null,
        name: null,
        owner: null,
        system: false
    };
    private publishDateFieldOptions: SelectItem[] = [];
    private sitesOrFolderOptions = [];
    private url: string;
    private type: string;
    private workflowOptions: SelectItem[] = [];

    constructor(public messageService: MessageService, private crudService: CrudService,
        private loginService: LoginService, private stringUtils: StringUtils,
        private renderer: Renderer, private fb: FormBuilder, private router: Router,
        private route: ActivatedRoute, private contentTypesInfoService: ContentTypesInfoService) {
            super([
            'Detail-Page',
            'Expire-Date-Field',
            'Host-Folder',
            'Identifier',
            'Properties',
            'Publish-Date-Field',
            'URL-Map-Pattern-hint1',
            'URL-Pattern',
            'Variable',
            'Workflow',
            'cancel',
            'description',
            'fields',
            'message.contentlet.required',
            'save',
            'update',
            'File',
            'Content',
            'Form',
            'Persona',
            'Widget',
            'Page',
            'name'
            ], messageService);
    }

    ngOnInit(): void {
        this.initWorkflowtFieldOptions();
        this.initDatesFieldOptions();

        Observable.combineLatest([this.route.url, this.messageService.messageMap$]).map(res => {
            return {
                messages: res[1],
                url: res[0]
            };
        }).subscribe((res) => {
            let urlSegments = res.url;
            this.url = urlSegments[0].path;

            if (this.url === 'create') {
                let type = urlSegments[1].path;
                this.setIcon(this.type = type);
                this.addOptionalFields(this.type);
            }

            this.actionButtonLabel = this.isEditMode ? this.i18nMessages['update'] : this.i18nMessages['save'];
            this.setPlaceholder();
        });
    }

    ngOnChanges(changes): void {
        if (changes.data.firstChange) {
            this.initFormGroup();
            this.initFormData();
        }

        if (changes.data.currentValue) {
            this.setIcon(this.data.clazz);
            this.propulateForm();
            this.addOptionalFields(this.data.clazz);
        }
    }

    ngAfterViewInit(): void {
        let nameEl: Element = this.nameEl.nativeElement;

        this.renderer.invokeElementMethod(nameEl, 'focus');

        Observable.fromEvent(nameEl, 'keyup')
            .map((event: KeyboardEvent) => event.target)
            .debounceTime(250)
            .subscribe((target: EventTarget) => {
                this.handleNameFielEvent(target);
            });
    }

    get isEditMode(): boolean {
        return this.data && this.route.snapshot.url[0].path === 'edit';
    }

    /**
     * Navigate to the content type listing
     *
     * @memberof ContentTypesFormComponent
     */
    public goToListing(): void {
        this.router.navigate(['content-types-angular']);
    }

    /**
     * Set the variable property base on the name and sbmit the form if it's valid
     * @memberof ContentTypesFormComponent
     */
    public submitContent(): void {
        if (!this.submitAttempt) {
            this.submitAttempt = true;
        }

        this.formData = this.buildFormData();

        if (this.form.valid) {
            if (this.isEditMode) {
                this.crudService.putData(`v1/contenttype/id/${this.data.id}`, this.formData)
                    .subscribe(this.handleFormSubmissionResponse.bind(this));
            } else {
                this.crudService.postData('v1/contenttype', this.formData)
                    .subscribe(this.handleFormSubmissionResponse.bind(this));
            }
        }
    }

    /**
     * Toggle formState property: 'collapsed' or 'expanded
     * @memberof ContentTypesFormComponent
     */
    public toggleForm(): void {
        this.formState = this.formState === 'collapsed' ? 'expanded' : 'collapsed';
    }

    private addOptionalFields(type: string): void {
        if (type === 'content' || type === 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType') {
            this.form.addControl('detailPage', new FormControl(this.data && this.data.detailPage  || ''));
            this.form.addControl('urlMapPattern', new FormControl(this.data && this.data.urlMapPattern  || ''));
        }

        if (this.isEditMode) {
            this.form.addControl('publishDateField', new FormControl({value: '', disabled: true}));
            this.form.addControl('expireDateField', new FormControl({value: '', disabled: true}));
        }
    }

    private buildFormData(): ContentType {
        if (this.data) {
            this.formData.id = this.data.id;
        }
        this.formData.variable = this.data ? this.data.variable : this.stringUtils.camelize(this.form.get('name').value);
        this.formData.clazz = this.data ? this.data.clazz : this.contentTypesInfoService.getClazz(this.type);
        return Object.assign({}, this.formData, this.form.value);
    }

    private createHostSiteDropdownOptions(sites: Site[]): SelectItem[] {
        return sites.map(item => {
            return {
                label: item.hostname,
                value: item.identifier
            };
        });
    }

    private handleFormSubmissionResponse(res: any): void {
        this.toggleForm();
        this.submitAttempt = false;
        this.readyToAddFields = true;
    }

    private handleNameFielEvent(el: EventTarget): void {
        let value: string = (<HTMLInputElement> el).value;
        if (!value && this.formState === 'expanded' || value && value.length && this.formState === 'collapsed') {
            this.toggleForm();
        }
    }

    private initDatesFieldOptions(): void {
        this.publishDateFieldOptions = [
            {
                label: 'Select one',
                value: null
            }
        ];

        this.expireDateFieldOptions = [
            {
                label: 'Select one',
                value: null
            }
        ];
    }

    private initFormData(): void {
        this.formData = this.initialFormData;

        if (this.loginService.auth) {
            this.formData.owner = this.loginService.auth.user.userId;
        }

        this.loginService.auth$.subscribe(res => {
            this.formData.owner = res.user.userId;
        });
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

    private initHostFieldOptions(): void {
        // TODO: when the backend it's fix this probably will change.
        this.sitesOrFolderOptions = [];
    }

    private initWorkflowtFieldOptions(): void {
        this.workflowOptions = [
            {
                label: 'Select Workflow',
                value: null
            }
        ];
    }

    private propulateForm(): void {
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

        this.formData = Object.assign(this.formData, formData);
        this.form.setValue(formData);
    }

    private setIcon(type: string): void {
        this.icon = this.contentTypesInfoService.getIcon(type);
    }

    private setPlaceholder(): void {
        let type = this.data && this.data.clazz || this.type;
        let label = this.contentTypesInfoService.getLabel(type);
        label = label.charAt(0).toUpperCase() + label.slice(1);
        label = this.i18nMessages[label];
        this.fieldNamePlaceholder = `${label} ${this.i18nMessages['name']}`;
    }
}
