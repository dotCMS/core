import { Component, Input, EventEmitter, Output, OnInit, SimpleChanges, OnChanges } from '@angular/core';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/shared/dot-relationship-cardinality.model';
import { DotMessageService } from '@services/dot-messages-service';
import { DotRelationshipService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/relationships-property/services/dot-relationship.service';
import { take } from 'rxjs/operators';

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

    cardinalities: DotRelationshipCardinality[];

    cardinality: DotRelationshipCardinality;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        private relationshipService: DotRelationshipService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.cardinality.placeholder',
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.relationshipService.loadCardinalities().subscribe((cardinalities: DotRelationshipCardinality[]) => {
            this.cardinalities = cardinalities;

            if (this.value) {
                this.cardinality = this.cardinalities[this.value];
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.cardinalityIndex.currentValue) {
            if (this.cardinalities) {
                this.cardinality = this.cardinalities[changes.cardinalityIndex.currentValue];
            }
        }
    }

    tiggerChanged(cardinality: DotRelationshipCardinality): void {
        this.change.next(cardinality.id);
    }
}
