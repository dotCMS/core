import { Component, ViewChild, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { DotTextareaContentComponent } from '@components/_common/dot-textarea-content/dot-textarea-content.component';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-values-property',
    templateUrl: './values-property.component.html',
    styleUrls: ['./values-property.component.scss']
})
export class ValuesPropertyComponent implements OnInit {
    @ViewChild('value') value: DotTextareaContentComponent;
    property: FieldProperty;
    group: FormGroup;
    helpText: string;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private validTextHelperClazz = [
        'com.dotcms.contenttype.model.field.ImmutableRadioField',
        'com.dotcms.contenttype.model.field.ImmutableCheckboxField',
        'com.dotcms.contenttype.model.field.ImmutableMultiSelectField',
        'com.dotcms.contenttype.model.field.ImmutableSelectField'
    ];

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['contenttypes.field.properties.value.label'])
            .pipe(take(1))
            .subscribe(res => {
                this.i18nMessages = res;
            });
    }

    /**
     * Checks if helper should show, based on the clazz property.
     *
     * @returns {Boolean}
     * @memberof ValuesPropertyComponent
     */
    isValidHelperClass(): boolean {
        return this.validTextHelperClazz.includes(this.property.field.clazz);
    }
}
