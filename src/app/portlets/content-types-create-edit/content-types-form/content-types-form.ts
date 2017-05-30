import { Component, trigger, state, transition, style, animate, Renderer, ViewEncapsulation, ViewChild, ElementRef } from '@angular/core';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { CrudService } from '../../../api/services/crud-service';
import { LoginService } from '../../../api/services/login-service';
import { MessageService } from '../../../api/services/messages-service';
import { SiteService, Site } from '../../../api/services/site-service';
import { StringUtils } from '../../../api/util/string.utils';
import { SelectItem } from 'primeng/components/common/api';
import { NgForm, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Observable } from 'rxjs/Observable';
import { ContentType } from '../content-types-create-edit-component';
import { Router } from '@angular/router';

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
    styles: [require('./content-types-form.scss')],
    templateUrl: 'content-types-form.html'
})
export class ContentTypesForm extends BaseComponent {
    @ViewChild('contentTypesForm') contentTypesForm: NgForm;
    @ViewChild('nameEl') nameEl: ElementRef;
    public formData: ContentType;
    public formState = 'collapsed';
    public form: FormGroup;
    public readyToAddFields = false;
    public submitAttempt = false;
    public isEditMode = false;
    private initialFormData: ContentType = {
        clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
        defaultType: false,
        description: '',
        fixed: false,
        folder: 'SYSTEM_FOLDER',
        host: '',
        name: '',
        owner: '',
        system: false,
        variable: '',
        workflow: ''
    };
    private expireDateFieldOptions: SelectItem[] = [];
    private publishDateFieldOptions: SelectItem[] = [];
    private sitesOrFolderOptions: Observable<SelectItem[]>;
    private workflowOptions: SelectItem[] = [];

    constructor(messageService: MessageService, private siteService: SiteService,
        private crudService: CrudService, private loginService: LoginService,
        private stringUtils: StringUtils, private renderer: Renderer,
        private fb: FormBuilder, private router: Router) {
        super([
            'Variable',
            'description',
            'Identifier',
            'Publish-Date-Field',
            'Expire-Date-Field',
            'Host-Folder',
            'Detail-Page',
            'Workflow',
            'URL-Pattern',
            'cancel',
            'save',
            'message.contentlet.required',
            'fields',
            'Properties',
            'URL-Map-Pattern-hint1'
        ], messageService);
    }

    ngOnInit(): void {
        this.initFormGroup();
        this.initFormData();
        this.initHostFieldOptions();
        this.initWorkflowtFieldOptions();
        this.initDatesFieldOptions();
    }

    ngAfterViewInit(): void {
        this.renderer.invokeElementMethod(this.nameEl.nativeElement, 'focus');
    }

    /**
     * Toggle the form collapsible behavior based on the value of the name
     * @param {string} value
     * @memberof ContentTypesForm
     */
    public handleNameModelChange(value: string): void {
        if (!value && this.formState === 'expanded' || value && value.length && this.formState === 'collapsed') {
            this.toggleForm();
        }
    }

    /**
     * Set the variable property base on the name and sbmit the form if it's valid
     * @memberof ContentTypesForm
     */
    public submitContent(): void {
        if (!this.submitAttempt) {
            this.submitAttempt = true;
        }
        this.formData = this.buildFormData();
        if (this.form.valid) {
            this.crudService.postData('v1/contenttype', this.formData).subscribe(res => {
                this.toggleForm();
                this.submitAttempt = false;
                this.readyToAddFields = true;
            });
        }
    }

    /**
     * Navigate to the content type listing
     *
     * @memberof ContentTypesForm
     */
    public goToListing(): void {
        this.router.navigate(['content-types-angular']);
    }

    /**
     * Toggle formState property: 'collapsed' or 'expanded
     * @memberof ContentTypesForm
     */
    public toggleForm(): void {
        this.formState = this.formState === 'collapsed' ? 'expanded' : 'collapsed';
    }

    /**
     * Prepare and return the data to submit to the endpoint and create a contentytpe
     *
     * @private
     * @returns {ContentType}
     *
     * @memberof ContentTypesForm
     */
    private buildFormData(): ContentType {
        this.formData.variable = this.stringUtils.camelize(this.form.get('name').value);
        return Object.assign({}, this.formData, this.form.value);
    }

    /**
     * Create an array of options for the dropdown for the Site or Folder field.
     *
     * @private
     * @param {Site[]} sites;
     * @returns {SelectItem[]};
     *
     * @memberof ContentTypesForm
     */
    private createHostSiteDropdownOptions(sites: Site[]): SelectItem[] {
        return sites.map(item => {
            return {
                label: item.hostname,
                value: item.identifier
            };
        });
    }

    /**
     * Initialize options for the Publish and Expire Date Field
     *
     * @private
     *
     * @memberof ContentTypesForm
     */
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

    /**
     * Initialize form data model
     *
     * @private
     *
     * @memberof ContentTypesForm
     */
    private initFormData(): void {
        this.formData = this.initialFormData;

        if (this.loginService.auth) {
            this.formData.owner = this.loginService.auth.user.userId;
        }

        this.loginService.auth$.subscribe(res => {
            this.formData.owner = res.user.userId;
        });
    }

    /**
     * Initialize ng form group
     *
     * @private
     *
     * @memberof ContentTypesForm
     */
    private initFormGroup(): void {
        this.form = this.fb.group({
            description: '',
            detailPage: '',
            host: '',
            name: ['', [
                Validators.required,
            ]],
            urlMapPattern: '',
            workflow: ''
        });
    }

    /**
     * Initialize options for the Host Field
     *
     * @private
     *
     * @memberof ContentTypesForm
     */
    private initHostFieldOptions(): void {
        // TODO: when the backend it's fix this probably will change.
        this.sitesOrFolderOptions = this.siteService.loadedSites ?
            Observable.of(this.siteService.loadedSites).map(this.createHostSiteDropdownOptions) :
            this.siteService.sites$.map(this.createHostSiteDropdownOptions);

        if (this.siteService.loadedSites) {
            this.form.patchValue({
                host: this.siteService.loadedSites[0].identifier
            });
        } else {
            this.siteService.sites$.subscribe(sites => {
                this.form.patchValue({
                    host: sites[0].identifier
                });
            });
        }
    }

    /**
     * Initialize options for the Workflow Field
     *
     * @private
     *
     * @memberof ContentTypesForm
     */
    private initWorkflowtFieldOptions(): void {
        this.workflowOptions = [
            {
                label: 'Select Workflow',
                value: null
            }
        ];
    }
}