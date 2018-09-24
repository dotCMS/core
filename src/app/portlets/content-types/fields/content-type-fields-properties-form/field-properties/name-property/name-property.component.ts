import { Component, ViewChild, OnInit, ElementRef } from '@angular/core';
import { FormGroup } from '@angular/forms/forms';
import { FieldProperty } from '../field-properties.model';
import { DotMessageService } from '@services/dot-messages-service';
import { BaseComponent } from '@components/_common/_base/base-component';

@Component({
    selector: 'dot-name-property',
    templateUrl: './name-property.component.html'
})
export class NamePropertyComponent extends BaseComponent implements OnInit {
    @ViewChild('name')
    name: ElementRef;

    property: FieldProperty;
    group: FormGroup;

    constructor(public dotMessageService: DotMessageService) {
        super(
            [
                'contenttypes.field.properties.name.label',
                'contenttypes.field.properties.name.error.required',
                'contenttypes.field.properties.name.variable'
            ],
            dotMessageService
        );
    }

    ngOnInit(): void {
        if (!this.group.get('name').disabled) {
            this.name.nativeElement.focus();
        }
    }
}
