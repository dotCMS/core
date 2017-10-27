import { ActivatedRoute, Router } from '@angular/router';
import { Component, ViewChild, OnInit } from '@angular/core';
import { Location } from '@angular/common';

import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { ContentType } from '../shared/content-type.model';
import { ContentTypesFormComponent } from '../form';
import { CrudService } from '../../../api/services/crud';
import { Field } from '../fields/index';
import { FieldService } from '../fields/service';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'dot-content-types-edit',
    templateUrl: './content-types-edit.component.html'
})
export class ContentTypesEditComponent implements OnInit {
    @ViewChild('form') form: ContentTypesFormComponent;

    data: ContentType;
    fields: Field[];

    constructor(
        private crudService: CrudService,
        private fieldService: FieldService,
        private location: Location,
        private route: ActivatedRoute,
        public router: Router
    ) {}

    ngOnInit(): void {
        this.route.data.pluck('contentType').subscribe((contentType: ContentType) => {
            this.data = contentType;
            if (contentType.fields) {
                this.fields = contentType.fields;
            }
        });
    }

    /**
     * Check if we need to update or create a content type
     *
     * @param {*} value;
     * @memberof ContentTypesEditComponent
     */
    handleFormSubmit(value: any): void {
        this.isEditMode() ? this.updateContentType(value) : this.createContentType(value);
    }

    /**
     * Check if the component is in edit mode
     *
     * @returns {boolean}
     * @memberof ContentTypesFormComponent
     */
    isEditMode(): boolean {
        return !!(this.data && this.data.id);
    }

    /**
     * Remove fields from the content type
     * @param fieldsToDelete Fields to be removed
     */
    removeFields(fieldsToDelete: Field[]): void {
        this.fieldService
            .deleteFields(this.data.id, fieldsToDelete)
            .pluck('fields')
            .subscribe((fields: Field[]) => {
                this.fields = fields;
            });
    }

    /**
     * Save fields to the content type
     * @param fieldsToSave Fields to be save
     */
    saveFields(fieldsToSave: Field[]): void {
        this.fieldService.saveFields(this.data.id, fieldsToSave).subscribe((fields: Field[]) => {
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
                this.form.resetForm();
                this.location.replaceState(`/content-types-angular/edit/${this.data.id}`);
            });
    }

    private updateContentType(value: any): void {
        const data = Object.assign({}, value, { id: this.data.id });

        this.crudService.putData(`v1/contenttype/id/${this.data.id}`, data).subscribe((contentType: ContentType) => {
            this.data = contentType;
            this.form.resetForm();
        });
    }
}
