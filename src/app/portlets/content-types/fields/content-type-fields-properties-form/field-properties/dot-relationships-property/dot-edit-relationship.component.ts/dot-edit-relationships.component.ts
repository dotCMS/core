import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { DotEditContentTypeCacheService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-edit-content-type-cache.service';
import { of as observableOf, Observable } from 'rxjs';
import { map, flatMap, toArray, take, switchMap } from 'rxjs/operators';
import { DotRelationshipService } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/services/dot-relationship.service';
import { DotRelationship } from '@portlets/content-types/fields/shared/dot-relationship.model';
import { DotRelationshipCardinality } from '@portlets/content-types/fields/content-type-fields-properties-form/field-properties/dot-relationships-property/model/dot-relationship-cardinality.model';
import { DotRelationshipsPropertyValue } from '../dot-relationships-property.component';


/**
 *Cardinalities sorted by id
 *
 * @interface CardinalitySorted
 */
interface CardinalitySorted {
    [id: number]: DotRelationshipCardinality;
}


/**
 *List the orphans relationships for a content type. The content type to filter the relationships
 *is take from {@see DotEditContentTypeCacheService}
 *
 * @export
 * @class DotEditRelationshipsComponent
 * @implements {OnInit}
 */
@Component({
    providers: [PaginatorService],
    selector: 'dot-edit-relationships',
    templateUrl: './dot-edit-relationships.component.html'
})
export class DotEditRelationshipsComponent implements OnInit {

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
        private relationshipService: DotRelationshipService) {}

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
     * Filter relationchips by name
     * @param any filter
     * @memberof DotEditRelationshipsComponent
     */
    handleFilterChange(filter: string): void {
        this.getRelationshipList(filter);
    }

    /**
     * Change relationships current page
     * @param any event
     * @memberof DotEditRelationshipsComponent
     */
    handlePageChange(event: {filter: string, first: number}): void {
        this.getRelationshipList(event.filter, event.first);
    }

    /**
     * Trigger a change event, it send a object with the current content type's variable and
     * the current candinality's index.
     * @param relationship relationship selected
     * @memberof DotEditRelationshipsComponent
     */
    triggerChanged(relationship: DotRelationship): void {
        this.change.next({
            velocityVar: relationship.relationTypeValue,
            cardinality: relationship.cardinality
        });
    }

    private getCardinalities(): Observable<CardinalitySorted> {

        if (!this.cardinalities) {

            return this.relationshipService.loadCardinalities().pipe(
                map((relationshipCardinalities: DotRelationshipCardinality[]) => {
                    this.cardinalities = {};

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

        this.currentPage = this.getCardinalities().pipe(
            switchMap((cardinalities: CardinalitySorted) => {
                return this.dotPaginatorService.getWithOffset(offset).pipe(
                    flatMap((relationships: DotRelationship[]) => relationships),
                    map((relationship: DotRelationship) => {
                        return {
                            label: `${relationship.relationTypeValue}.${cardinalities[relationship.cardinality].label}`,
                            relationship: <DotRelationship> relationship
                        };
                    }),
                    toArray()
                );
            }),
        );
    }
}
