import { Component, ViewChild, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import { DotTextareaContentComponent } from '@components/_common/dot-textarea-content/dot-textarea-content.component';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-values-property',
    templateUrl: './values-property.component.html'
})
export class ValuesPropertyComponent implements OnInit {
    @ViewChild('value')
    value: DotTextareaContentComponent;
    property: FieldProperty;
    group: FormGroup;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['contenttypes.field.properties.value.label'])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }
}
