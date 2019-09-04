import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-hint-property',
    templateUrl: './hint-property.component.html'
})
export class HintPropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['contenttypes.field.properties.hint.label'])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });
    }
}
