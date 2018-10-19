import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { EditContentTypeCacheService } from '@portlets/content-types/services/edit-content-type-cache.service';
import { Relationship } from '@portlets/content-types/fields/shared/relationship.model';
import { of as observableOf, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { RelationshipCardinality } from '@portlets/content-types/fields/shared/relationship-cardinality.model';
import { RelationshipService } from '@portlets/content-types/fields/service/relationship.service';

interface CardinalitySorted {
    [id: number]: RelationshipCardinality;
}
@Component({
    providers: [PaginatorService],
    selector: 'dot-edit-relationships',
    templateUrl: './edit-relationships.component.html'
})
export class EditRelationshipsComponent implements OnInit {

    @Output()
    change: EventEmitter<any> = new EventEmitter();

    relationshipsCurrentPage: Observable<{label: string, relationship: Relationship}[]>;

    i18nMessages: {
        [key: string]: string;
    } = {};

    private cardinalities: CardinalitySorted;

    constructor(
        public dotMessageService: DotMessageService,
        public relationshipPaginatorService: PaginatorService,
        private editContentTypeCacheService: EditContentTypeCacheService,
        private relationshipService: RelationshipService) {

    }
    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.existing.label',
                'contenttypes.field.properties.relationship.existing.placeholder',
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.relationshipPaginatorService.url = 'v1/relationships';
    }

    /**
     * Call when the relationship global serach changed
     * @param any filter
     * @memberof CategoriesPropertyComponent
     */
    handleRelationshipFilterChange(filter): void {
        this.getRelationshipList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof CategoriesPropertyComponent
     */
    handleRelationshipPageChange(event): void {
        this.getRelationshipList(event.filter, event.first);
    }

    tiggerChanged(relationship: Relationship): void {
        this.change.next({
            velocityVar: relationship.relationTypeValue,
            cardinality: relationship.cardinality
        });
    }

    private getCardinalities(): Observable<CardinalitySorted> {
        if (!this.cardinalities) {
            this.cardinalities = {};

            return this.relationshipService.loadCardinalities().pipe(
                map((relationshipCardinalities: RelationshipCardinality[]) => {
                    relationshipCardinalities.forEach((cardinality: RelationshipCardinality) => {
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
        this.relationshipPaginatorService.setExtraParams('contentTypeId', this.editContentTypeCacheService.contentType.id);
        this.relationshipPaginatorService.filter = filter;

        this.getCardinalities().subscribe((cardinalities: CardinalitySorted) => {
            console.log('cardinalities', cardinalities);
            this.relationshipsCurrentPage = this.relationshipPaginatorService.getWithOffset(offset).pipe(
                map((relationships: Relationship[]) => {
                    return relationships.map((relationship: Relationship) => {
                        return {
                            label: `${relationship.relationTypeValue}   .   ${cardinalities[relationship.cardinality].name}`,
                            relationship: <Relationship> relationship
                        };
                    });
                })
            );
        });
    }
}
