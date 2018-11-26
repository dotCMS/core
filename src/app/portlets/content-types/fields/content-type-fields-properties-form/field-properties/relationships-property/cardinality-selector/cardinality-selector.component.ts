import { Component, Input, EventEmitter, Output, OnInit, SimpleChanges, OnChanges } from '@angular/core';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/shared/dot-relationship-cardinality.model';
import { DotMessageService } from '@services/dot-messages-service';
import { RelationshipService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/relationships-property/services/relationship.service';
import { take } from 'rxjs/operators';

@Component({
    providers: [],
    selector: 'dot-cardinality-selector',
    templateUrl: './cardinality-selector.component.html'
})
export class CardinalitySelectorComponent implements OnInit, OnChanges {
    @Input()
    cardinalityIndex: number;

    @Input()
    disabled: boolean;

    @Output()
    change: EventEmitter<DotRelationshipCardinality> = new EventEmitter();

    cardinalities: DotRelationshipCardinality[];

    cardinality: DotRelationshipCardinality;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        private relationshipService: RelationshipService) {

    }

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

            if (this.cardinalityIndex) {
                this.cardinality = this.cardinalities[this.cardinalityIndex];
            }
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.cardinalityIndex.currentValue) {
            this.changeCardinality(changes.cardinalityIndex.currentValue);
        }
    }

    tiggerChanged(cardinality: DotRelationshipCardinality): void {
        this.change.next(cardinality);
    }

    private changeCardinality(cardinality: number): void {
        if (this.cardinalities) {
            this.cardinality = this.cardinalities[cardinality];
        }
    }
}
