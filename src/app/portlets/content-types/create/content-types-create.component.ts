import { ActivatedRoute, Router } from '@angular/router';
import { CONTENT_TYPE_INITIAL_DATA, ContentType } from '../main';
import { Component, ViewChild, OnInit } from '@angular/core';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { CrudService } from '../../../api/services/crud';
import { LoginService, StringUtils } from 'dotcms-js/dotcms-js';
import { MessageService } from '../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';
import { ContentTypesFormComponent } from '../form';
import { Field } from '../fields';
import { FieldService } from '../fields/service';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'content-types-create',
    templateUrl: './content-types-create.component.html'
})
export class ContentTypesCreateComponent extends BaseComponent implements OnInit {
    contentTypeType: string;
    @ViewChild('form') form: ContentTypesFormComponent;
    public contentTypeName: Observable<string>;
    public contentTypeIcon: string;
    data: ContentType;
    contentTypeId: string;

    constructor(
        public contentTypesInfoService: ContentTypesInfoService,
        public crudService: CrudService,
        public fieldService: FieldService,
        public loginService: LoginService,
        messageService: MessageService,
        public route: ActivatedRoute,
        public stringUtils: StringUtils,
        router: Router
    ) {
        super([
            'contenttypes.content.file',
            'contenttypes.content.content',
            'contenttypes.content.form',
            'contenttypes.content.persona',
            'contenttypes.content.widget',
            'contenttypes.content.page'
        ], messageService);
    }

    ngOnInit(): void {
        this.route.url.subscribe(res => {
            const type = res[1].path;
            this.contentTypeName = this.messageService.messageMap$.pluck(`contenttypes.content.${type}`);
            this.contentTypeType = type;
            this.contentTypeIcon = this.contentTypesInfoService.getIcon(type);
        });

        this.setOwnerToContentTypeData();
    }

    /**
     * Combine data from the form and submit to create content types
     *
     * @param {any} $event;
     *
     * @memberof ContentTypesCreateComponent
     */
    public handleFormSubmit($event): void {
        if (this.data && this.data.id) {
            this.updateContentType($event.value);
        } else {
            this.saveContentType($event.value);
        }
    }

    private  saveContentType(contentType: ContentType): void {
        const contentTypeData: ContentType = Object.assign(
            {},
            CONTENT_TYPE_INITIAL_DATA,
            contentType
        );

        contentTypeData.clazz = this.contentTypesInfoService.getClazz(
            this.contentTypeType
        );

        this.crudService
            .postData('v1/contenttype', contentTypeData)
            .subscribe(resp => this.handleFormSubmissionResponse(resp));
    }

    public updateContentType(contentType: ContentType): void {
        const contentTypeData: ContentType = Object.assign(
            {},
            this.data,
            contentType
        );

        this.crudService
            .putData(`v1/contenttype/id/${this.data.id}`, contentTypeData)
            .subscribe(resp => this.handleFormSubmissionResponse(resp));
    }

    /**
     * Save fields
     * @param fieldsToSave Fields to be save
     */
    saveFields(fieldsToSave: Field[]): void {
        this.fieldService.saveFields(this.data.id, fieldsToSave).subscribe(fields => {
            this.data.fields = fields;
            this.form.updateFormFields();
        });
    }

    removeFields(fieldsToDelete: Field[]): void {
        this.fieldService.deleteFields(this.data.id, fieldsToDelete)
        .pluck('fields')
        .subscribe((fields: Field[]) => {
            this.data.fields = fields;
            this.form.updateFormFields();
        });
    }

    private handleFormSubmissionResponse(res: ContentType[]): void {
        this.data = res[0];
        this.contentTypeId = this.data.id;
        this.form.resetForm();
    }

    private setOwnerToContentTypeData(): void {
        if (this.loginService.auth) {
            CONTENT_TYPE_INITIAL_DATA.owner = this.loginService.auth.user.userId;
        }

        this.loginService.auth$.subscribe(res => {
            CONTENT_TYPE_INITIAL_DATA.owner = res.user.userId;
        });
    }
}
