import { Component, Input, EventEmitter, Output, OnInit, SimpleChanges, OnChanges } from '@angular/core';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { DotMessageService } from '@services/dot-messages-service';
import { DotRelationshipService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';
import { take } from 'rxjs/operators';


/**
 *Selector for relationships cardinalities
 *
 * @export
 * @class DotCardinalitySelectorComponent
 * @implements {OnInit}
 * @implements {OnChanges}
 */
@Component({
    providers: [],
    selector: 'dot-cardinality-selector',
    templateUrl: './dot-cardinality-selector.component.html'
})
export class DotCardinalitySelectorComponent implements OnInit, OnChanges {
    @Input()
    value: number;

    @Input()
    disabled: boolean;

    @Output()
    change: EventEmitter<number> = new EventEmitter();

    options: DotRelationshipCardinality[];

    cardinality: DotRelationshipCardinality;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        private dotRelationshipService: DotRelationshipService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.cardinality.placeholder',
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.dotRelationshipService.loadCardinalities().subscribe((cardinalities: DotRelationshipCardinality[]) => {
            this.options = cardinalities;

            if (this.value) {
                this.cardinality = this.options[this.value];
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.value.currentValue) {
            if (this.options) {
                this.cardinality = this.options[changes.value.currentValue];
            }
        }
    }

    /**
     *Trigger a change event
     *
     * @param {DotRelationshipCardinality} cardinality
     * @memberof DotCardinalitySelectorComponent
     */
    tiggerChanged(cardinality: DotRelationshipCardinality): void {
        this.change.next(cardinality.id);
    }
}
