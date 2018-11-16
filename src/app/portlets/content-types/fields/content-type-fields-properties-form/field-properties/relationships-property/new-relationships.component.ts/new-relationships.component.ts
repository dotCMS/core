import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { ContentType } from '@portlets/content-types/shared/content-type.model';
import { RelationshipCardinality } from '@portlets/content-types/fields/shared/relationship-cardinality.model';
import { RelationshipService } from '@portlets/content-types/fields/service/relationship.service';
import { Observable } from 'rxjs';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

@Component({
    providers: [PaginatorService],
    selector: 'dot-new-relationships',
    templateUrl: './new-relationships.component.html'
})
export class NewRelationshipsComponent implements OnInit {
    @Input()
    cardinalityIndex: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    change: EventEmitter<any> = new EventEmitter();

    contentTypeCurrentPage: Observable<ContentType[]>;
    relationshpsCardinalities: RelationshipCardinality[];

    cardinality: RelationshipCardinality;
    contentType: ContentType;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        public contentTypePaginatorService: PaginatorService,
        private relationshipService: RelationshipService,
        private contentTypeService: DotContentTypeService) {

    }
    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'contenttypes.field.properties.relationship.new.label',
                'contenttypes.field.properties.relationship.new.content_type.placeholder'
            ])
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.contentTypePaginatorService.url = 'v1/contenttype';

        this.relationshipService.loadCardinalities().subscribe((cardinalities: RelationshipCardinality[]) => {
            this.relationshpsCardinalities = cardinalities;
            this.cardinality = this.relationshpsCardinalities[this.cardinalityIndex];
        });

        if (this.velocityVar) {
            this.contentTypeService.getContentType(this.velocityVar).subscribe((contentType) => {
                this.contentType = contentType;
            });
        }
    }

    /**
     * Call when the content type global serach changed
     * @param any filter
     * @memberof CategoriesPropertyComponent
     */
    handleContentTypeFilterChange(filter): void {
        this.getContentTypeList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof CategoriesPropertyComponent
     */
    handleContentTypePageChange(event): void {
        this.getContentTypeList(event.filter, event.first);
    }

    tiggerChanged(): void {
        this.change.next({
            velocityVar: this.contentType.variable,
            cardinality: this.cardinality.id
        });
    }

    private getContentTypeList(filter = '', offset = 0): void {
        this.contentTypePaginatorService.filter = filter;
        this.contentTypeCurrentPage = this.contentTypePaginatorService.getWithOffset(offset);
    }
}
