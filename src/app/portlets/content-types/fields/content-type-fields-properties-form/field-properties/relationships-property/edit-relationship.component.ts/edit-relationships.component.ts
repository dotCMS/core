import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { DotEditContentTypeCacheService } from '@portlets/content-types/services/edit-content-type-cache.service';
import { of as observableOf, Observable } from 'rxjs';
import { map, flatMap, toArray, take } from 'rxjs/operators';
import { RelationshipService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/relationships-property/services/relationship.service';
import { DotRelationship } from '@portlets/content-types/fields/shared/dot-relationship.model';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/shared/dot-relationship-cardinality.model';
import { DotRelationshipsPropertyValue } from '../relationships-property.component';

interface CardinalitySorted {
    [id: number]: DotRelationshipCardinality;
}
@Component({
    providers: [PaginatorService],
    selector: 'dot-edit-relationships',
    templateUrl: './edit-relationships.component.html'
})
export class EditRelationshipsComponent implements OnInit {

    @Output()
    change: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

    currentPage: Observable<{label: string, relationship: DotRelationship}[]>;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private cardinalities: CardinalitySorted;

    constructor(
        public dotMessageService: DotMessageService,
        public dotPaginatorService: PaginatorService,
        private editContentTypeCacheService: DotEditContentTypeCacheService,
        private relationshipService: RelationshipService) {

    }
    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.existing.label',
                'contenttypes.field.properties.relationship.existing.placeholder',
            ])
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.dotPaginatorService.url = 'v1/relationships';
    }

    /**
     * Call when the relationship global serach changed
     * @param any filter
     * @memberof EditRelationshipsComponent
     */
    handleFilterChange(filter: string): void {
        this.getRelationshipList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof EditRelationshipsComponent
     */
    handlePageChange(event: {filter: string, first: number}): void {
        this.getRelationshipList(event.filter, event.first);
    }

    /**
     * Trigger a change event, it send a object with the current content type's variable and
     * the current candinality's index.
     * @param relationship relationship selected
     * @memberof EditRelationshipsComponent
     */
    triggerChanged(relationship: DotRelationship): void {
        this.change.next({
            velocityVar: relationship.relationTypeValue,
            cardinality: relationship.cardinality
        });
    }

    private getCardinalities(): Observable<CardinalitySorted> {

        if (!this.cardinalities) {
            this.cardinalities = {};

            return this.relationshipService.loadCardinalities().pipe(
                map((relationshipCardinalities: DotRelationshipCardinality[]) => {
                    relationshipCardinalities.forEach((cardinality: DotRelationshipCardinality) => {
                        this.cardinalities[cardinality.id] = cardinality;
                    });

                    return this.cardinalities;
                })
            );
        } else {
            return observableOf(this.cardinalities);
        }
    }

    private getRelationshipList(filter = '', offset = 0): void {
        this.dotPaginatorService.setExtraParams('contentTypeId', this.editContentTypeCacheService.get().id);
        this.dotPaginatorService.filter = filter;

        this.getCardinalities().subscribe((cardinalities: CardinalitySorted) => {
            this.currentPage = this.dotPaginatorService.getWithOffset(offset).pipe(
                flatMap((relationships: DotRelationship[]) => relationships),
                map((relationship: DotRelationship) => {
                    return {
                        label: `${relationship.relationTypeValue}   .   ${cardinalities[relationship.cardinality].label}`,
                        relationship: <DotRelationship> relationship
                    };
                }),
                toArray()
            );
        });
    }
}
