import { ActivatedRoute, Router } from '@angular/router';
import { BaseComponent } from '../../../view/components/_common/_base/base-component';
import { Component, ViewChild } from '@angular/core';
import { ContentType } from '../main/index';
import { ContentTypesFormComponent } from '../common/content-types-form';
import { ContentTypesInfoService } from '../../../api/services/content-types-info';
import { CrudService } from '../../../api/services/crud';
import { MessageService } from '../../../api/services/messages-service';
import { Observable } from 'rxjs/Observable';
import { StringUtils } from '../../../api/util/string.utils';

/**
 * Portlet component for edit content types
 *
 * @export
 * @class ContentTypesEditComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'content-types-edit',
    templateUrl: './content-types-edit.component.html',
})
export class ContentTypesEditComponent extends BaseComponent {
    @ViewChild('form') form: ContentTypesFormComponent;
    private contentTypeItem: Observable<any>;
    private contentTypeName: Observable<string>;
    private contentTypeType: string;
    private contentTypeIcon: string;
    private data: any;
    private readyToAddFields = false;
    private licenseInfo: any;

    constructor(messageService: MessageService, private route: ActivatedRoute,
        private crudService: CrudService, public router: Router, private stringUtils: StringUtils,
        private contentTypesInfoService: ContentTypesInfoService) {
        super([
            'File',
            'Content',
            'Form',
            'Persona',
            'Widget',
            'Page',
        ], messageService);
    }

    ngOnInit(): void {
        this.route.url.subscribe(res => {
            this.contentTypeItem = this.crudService.getDataById('v1/contenttype', res[1].path).map(res => {
                let type = this.contentTypesInfoService.getLabel(res.clazz);
                this.contentTypeName = this.messageService.messageMap$.pluck(this.stringUtils.titleCase(type));
                this.contentTypeType = type;
                this.contentTypeIcon = this.contentTypesInfoService.getIcon(res.clazz);
                this.data = res;
                return res;
            });
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
        let contentTypeData: ContentType = Object.assign({}, this.data, $event.value);
        this.crudService.putData(`v1/contenttype/id/${this.data.id}`, contentTypeData)
            .subscribe(this.handleFormSubmissionResponse.bind(this));
    }

    private handleFormSubmissionResponse(res: any): void {
        this.form.resetForm();
        this.readyToAddFields = true;
    }
}
