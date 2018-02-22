import { ActivatedRoute, Router } from '@angular/router';
import { Component, ViewChild, OnInit } from '@angular/core';
import { Location } from '@angular/common';

import { ContentType } from '../shared/content-type.model';
import { ContentTypesFormComponent } from '../form';
import { CrudService } from '../../../api/services/crud';
import { ContentTypeField } from '../fields/index';
import { FieldService } from '../fields/service';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-content-types-edit',
    templateUrl: './content-types-edit.component.html',
    styleUrls: ['./content-types-edit.component.scss']
})
export class ContentTypesEditComponent implements OnInit {
    @ViewChild('form') form: ContentTypesFormComponent;

    data: ContentType;
    fields: ContentTypeField[];
    show: boolean;
    templateInfo = {
        icon: '',
        header: ''
    };

    constructor(
        private contentTypesInfoService: ContentTypesInfoService,
        private crudService: CrudService,
        private dotRouterService: DotRouterService,
        private fieldService: FieldService,
        private location: Location,
        private route: ActivatedRoute,
        public dotMessageService: DotMessageService,
        public router: Router
    ) {}

    ngOnInit(): void {
        this.route.data.pluck('contentType').subscribe((contentType: ContentType) => {
            this.data = contentType;

            if (contentType.fields) {
                this.fields = contentType.fields;
            }
        });

        this.dotMessageService
            .getMessages([
                'contenttypes.action.create',
                'contenttypes.action.edit',
                'contenttypes.action.form.cancel',
                'contenttypes.action.update',
                'contenttypes.content.content',
                'contenttypes.content.create.contenttype',
                'contenttypes.content.edit.contenttype',
                'contenttypes.content.fileasset',
                'contenttypes.content.form',
                'contenttypes.content.htmlpage',
                'contenttypes.content.key_value',
                'contenttypes.content.persona',
                'contenttypes.content.vanity_url',
                'contenttypes.content.variable',
                'contenttypes.content.variable',
                'contenttypes.content.widget',
                'contenttypes.form.identifier'
            ])
            .subscribe();

        this.setTemplateInfo();

        this.show = !this.isEditMode();
    }

    /**
     * Handle cancel button in dialog
     *
     * @memberof ContentTypesEditComponent
     */
    cancelForm(): void {
        this.show = false;

        if (!this.isEditMode()) {
            this.dotRouterService.gotoPortlet('/content-types-angular');
        }
    }

    /**
     * Set the icon, labels and placeholder in the template
     * @memberof ContentTypesEditComponent
     */
    setTemplateInfo(): void {
        this.dotMessageService.messageMap$.subscribe(() => {
            const type = this.contentTypesInfoService.getLabel(this.data.baseType);
            const contentTypeName = this.dotMessageService.get(`contenttypes.content.${type}`);

            this.templateInfo = {
                icon: this.contentTypesInfoService.getIcon(type),
                header: this.isEditMode()
                    ? this.dotMessageService.get('contenttypes.content.edit.contenttype', contentTypeName)
                    : this.dotMessageService.get('contenttypes.content.create.contenttype', contentTypeName)
            };
        });
    }

    /**
     * Check if we need to update or create a content type
     *
     * @param {*} value;
     * @memberof ContentTypesEditComponent
     */
    handleFormSubmit(value: any): void {
        this.show = false;
        this.isEditMode() ? this.updateContentType(value) : this.createContentType(value);
    }

    /**
     * Check if the component is in edit mode
     *
     * @returns {boolean}
     * @memberof ContentTypesEditComponent
     */
    isEditMode(): boolean {
        return !!(this.data && this.data.id);
    }

    /**
     * Remove fields from the content type
     * @param fieldsToDelete Fields to be removed
     */
    removeFields(fieldsToDelete: ContentTypeField[]): void {
        this.fieldService
            .deleteFields(this.data.id, fieldsToDelete)
            .pluck('fields')
            .subscribe((fields: ContentTypeField[]) => {
                this.fields = fields;
            });
    }

    /**
     * Save fields to the content type
     * @param fieldsToSave Fields to be save
     */
    saveFields(fieldsToSave: ContentTypeField[]): void {
        this.fieldService.saveFields(this.data.id, fieldsToSave).subscribe((fields: ContentTypeField[]) => {
            this.fields = fields;
        });
    }

    private createContentType(value: ContentType): void {
        this.crudService
            .postData('v1/contenttype', value)
            .flatMap((contentTypes: ContentType[]) => contentTypes)
            .take(1)
            .subscribe((contentType: ContentType) => {
                this.data = contentType;
                this.fields = this.data.fields;
                this.location.replaceState(`/content-types-angular/edit/${this.data.id}`);
                this.show = false;
            });
    }

    private updateContentType(value: any): void {
        const data = Object.assign({}, value, { id: this.data.id });

        this.crudService.putData(`v1/contenttype/id/${this.data.id}`, data).subscribe((contentType: ContentType) => {
            this.data = contentType;
            this.show = false;
        });
    }
}
