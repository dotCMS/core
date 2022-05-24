import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { PaginatorService } from '@services/paginator';
import { of as observableOf, Observable } from 'rxjs';
import { map, flatMap, toArray, switchMap } from 'rxjs/operators';
import { DotRelationshipsPropertyValue } from '../model/dot-relationships-property-value.model';
import { DotRelationship } from '../model/dot-relationship.model';
import { DotEditContentTypeCacheService } from '../services/dot-edit-content-type-cache.service';
import { DotRelationshipService } from '../services/dot-relationship.service';
import { DotRelationshipCardinality } from '../model/dot-relationship-cardinality.model';

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
    switch: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

    currentPage: Observable<{ label: string; relationship: DotRelationship }[]>;

    private cardinalities: CardinalitySorted;

    constructor(
        public dotPaginatorService: PaginatorService,
        private dotEditContentTypeCacheService: DotEditContentTypeCacheService,
        private dotRelationshipService: DotRelationshipService
    ) {}

    ngOnInit() {
        this.dotPaginatorService.url = 'v1/relationships';
    }

    /**
     *Filter relationchips by name
     *
     * @param {string} filter
     * @memberof DotEditRelationshipsComponent
     */
    handleFilterChange(filter: string): void {
        this.getRelationshipList(filter);
    }

    /**
     *Change relationships current page
     *
     * @param {{filter: string, first: number}} event
     * @memberof DotEditRelationshipsComponent
     */
    handlePageChange(event: { filter: string; first: number }): void {
        this.getRelationshipList(event.filter, event.first);
    }

    /**
     *Trigger a change event, it send a object with the current content type's variable and
     *
     * @param {DotRelationship} relationship relationship selected
     * @memberof DotEditRelationshipsComponent
     */
    triggerChanged(relationship: DotRelationship): void {
        this.switch.next({
            velocityVar: relationship.relationTypeValue,
            cardinality: relationship.cardinality
        });
    }

    private getCardinalities(): Observable<CardinalitySorted> {
        if (!this.cardinalities) {
            return this.dotRelationshipService.loadCardinalities().pipe(
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
        this.dotPaginatorService.setExtraParams(
            'contentTypeId',
            this.dotEditContentTypeCacheService.get().id
        );
        this.dotPaginatorService.filter = filter;

        this.currentPage = this.getCardinalities().pipe(
            switchMap((cardinalities: CardinalitySorted) => {
                return this.dotPaginatorService.getWithOffset(offset).pipe(
                    flatMap((relationships: DotRelationship[]) => relationships),
                    map((relationship: DotRelationship) => {
                        return {
                            label: `${relationship.relationTypeValue}.${
                                cardinalities[relationship.cardinality].label
                            }`,
                            relationship: <DotRelationship>relationship
                        };
                    }),
                    toArray()
                );
            })
        );
    }
}
