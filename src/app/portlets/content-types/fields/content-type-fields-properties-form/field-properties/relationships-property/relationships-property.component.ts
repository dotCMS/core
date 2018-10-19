import { Component, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { FieldProperty } from '../field-properties.model';
import { FormGroup } from '@angular/forms';

@Component({
    providers: [],
    selector: 'dot-relationships-property',
    templateUrl: './relationships-property.component.html'
})
export class RelationshipsPropertyComponent implements OnInit {
    property: FieldProperty;
    group: FormGroup;

    status = 'NEW';

    cardinalityIndex: number;
    velocityVar: string;

    editing: boolean;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService) {

    }
    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.new.label',
                'contenttypes.field.properties.relationship.existing.label',
                'contenttypes.field.properties.relationship.existing.placeholder',
                'contenttypes.field.properties.relationship.new.content_type.placeholder'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });


        this.velocityVar = this.getVelocityVar();
        this.cardinalityIndex = this.group.get(this.property.name).value.cardinality;

        this.editing = !!this.group.get(this.property.name).value.velocityVar;
        console.log('this.property', this.property);
    }

    handleChange(event): void {
        this.group.get(this.property.name).setValue(event);
    }

    private getVelocityVar(): string {
        const value = this.group.get(this.property.name).value.velocityVar;
        return !!value ? value.split('.')[0] : '';
    }
}
