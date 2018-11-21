import { Component, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';
import * as _ from 'lodash';

enum STATUS {
    NEW, EXISTING
}

@Component({
    providers: [],
    selector: 'dot-relationships-property',
    templateUrl: './relationships-property.component.html'
})
export class RelationshipsPropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;

    status: STATUS = STATUS.NEW;

    editing: boolean;

    beforeValue: any;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService) {

    }
    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.existing.label',
                'contenttypes.field.properties.relationship.new.label',
                'contenttypes.field.properties.relationships.new.error.required',
                'contenttypes.field.properties.relationships.edit.error.required'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });


        this.beforeValue = _.cloneDeep(this.group.get(this.property.name).value);

        this.group.get(this.property.name).setValue({
            velocityVar: this.getVelocityVar(),
            cardinality: this.group.get(this.property.name).value.cardinality
        });

        this.editing = !!this.group.get(this.property.name).value.velocityVar;
    }

    handleChange(event): void {
        this.group.get(this.property.name).setValue(event);
    }

    clean(): void {
        this.group.get(this.property.name).setValue(_.cloneDeep(this.beforeValue));
    }

    getValidationMessage(): string {
        return this.status === STATUS.NEW ?
            this.i18nMessages['contenttypes.field.properties.relationships.new.error.required'] :
            this.i18nMessages['contenttypes.field.properties.relationships.edit.error.required'];
    }

    isNew(): boolean {
        return this.status === STATUS.NEW;
    }

    private getVelocityVar(): string {
        const value = this.group.get(this.property.name).value.velocityVar;
        return !!value ? value.split('.')[0] : '';
    }
}
