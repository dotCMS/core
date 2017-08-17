import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { Component, ViewChild } from '@angular/core';
import { ContentType } from '../main/index';
import { ContentTypesFormComponent } from '../form';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { CrudService } from '../../../api/services/crud';
import { MessageService } from '../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';
import { StringUtils } from '../../../api/util/string.utils';
import { Field } from '../fields';
import { FieldService } from '../fields/service';
import { ConfirmationService } from 'primeng/primeng';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'content-types-edit',
    templateUrl: './content-types-edit.component.html'
})
export class ContentTypesEditComponent extends BaseComponent {
    @ViewChild('form') form: ContentTypesFormComponent;
    contentTypeItem: Observable<any>;
    contentTypeName: Observable<string>;
    contentTypeType: string;
    contentTypeIcon: string;
    data: ContentType;
    licenseInfo: any;

    constructor(
        messageService: MessageService,
        private confirmationService: ConfirmationService,
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private fieldService: FieldService,
        private route: ActivatedRoute,
        private stringUtils: StringUtils,
        public router: Router
    ) {
        super(
            [
                'File',
                'Content',
                'Form',
                'Persona',
                'Widget',
                'Page',
                'message.structure.cantdelete',
                'message.structure.delete.structure.and.content',
                'Yes',
                'No'
            ],
            messageService
        );
    }

    ngOnInit(): void {
        this.route.url.subscribe(res => {
            this.contentTypeItem = this.crudService
                .getDataById('v1/contenttype', res[1].path)
                .map(res => {
                    const type = this.contentTypesInfoService.getLabel(res.clazz);
                    this.contentTypeName = this.messageService.messageMap$.pluck(
                        this.stringUtils.titleCase(type)
                    );
                    this.contentTypeType = type;
                    this.contentTypeIcon = this.contentTypesInfoService.getIcon(res.clazz);
                    this.data = res;
                    return res;
                });
        });
    }

    /**
     * Delete a content type using the ID
     * @param {any} $event
     * @memberof ContentTypesEditComponent
     */
    public deleteContentType($event): void {
        this.confirmationService.confirm({
            accept: () => {
                this.crudService.delete(`v1/contenttype/id/`, this.data.id).subscribe(data => {
                    this.router.navigate(['../'], { relativeTo: this.route });
                });
            },
            header: this.i18nMessages['message.structure.cantdelete'],
            message: this.i18nMessages['message.structure.delete.structure.and.content']
        });
    }

    /**
     * Combine data from the form and submit to update content types
     *
     * @param {any} $event
     *
     * @memberof ContentTypesEditComponent
     */
    public handleFormSubmit($event): void {
        const contentTypeData: ContentType = Object.assign({}, this.data, $event.value);
        this.crudService
            .putData(`v1/contenttype/id/${this.data.id}`, contentTypeData)
            .subscribe(this.handleFormSubmissionResponse.bind(this));
    }

    /**
     * Save fields
     * @param fieldsToSave Fields to be save
     */
    saveFields(fields: Field[]): void {
        this.fieldService.saveFields(this.data.id, fields).subscribe(fields => {
            this.data.fields = fields;
        });
    }

    private handleFormSubmissionResponse(res: any): void {
        this.form.resetForm();
    }
}
