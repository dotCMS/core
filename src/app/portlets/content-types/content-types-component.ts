import { Component } from '@angular/core';
import { DataTableColumn } from '../../view/components/listing-data-table/listing-data-table-component';
import { BaseComponent } from '../../view/components/_common/_base/base-component';
import { MessageService } from '../../api/services/messages-service';

/**
 * List of Content Types
 * use: listing-data-table-component
 * @export
 * @class ContentTypesPortletComponent
 * @extends {BaseComponent}
 */
@Component({
    selector: 'content-types-component',
    templateUrl: 'content-types-component.html'
})
export class ContentTypesPortletComponent extends BaseComponent {
    contentTypeColumns: DataTableColumn[];

    constructor(messageService: MessageService) {
        super(['Structure-Name', 'Variable', 'Description', 'Entries'], messageService);
    }

    /**
     * Callback call from BaseComponent when the messages are received.
     * @memberOf ContentTypesPortletComponent
     */
    onMessage(): void {
        this.contentTypeColumns = [
            {fieldName: 'name', header: this.i18nMessages['Structure-Name'], width: '40%'},
            {fieldName: 'velocityVarName', header: this.i18nMessages['Variable'], width: '10%'},
            {fieldName: 'description', header: this.i18nMessages['Description'], width: '40%'},
            {fieldName: 'nEntries', header: this.i18nMessages['Entries'], width: '10%'}
        ];
    }
}