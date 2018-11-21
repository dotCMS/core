import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { ContentType } from '@portlets/content-types/shared/content-type.model';
import { Observable } from 'rxjs';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';

@Component({
    providers: [PaginatorService],
    selector: 'dot-new-relationships',
    templateUrl: './new-relationships.component.html'
})
export class NewRelationshipsComponent implements OnInit, OnChanges {
    @Input()
    cardinalityIndex: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    change: EventEmitter<any> = new EventEmitter();

    contentTypeCurrentPage: Observable<ContentType[]>;

    contentType: ContentType;
    currentCardinalityIndex: number;

    i18nMessages: {
        [key: string]: string;
    } = {};

    constructor(
        public dotMessageService: DotMessageService,
        public paginatorService: PaginatorService,
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

        this.paginatorService.url = 'v1/contenttype';
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.velocityVar) {
            this.loadContentType(changes.velocityVar.currentValue);
        }

        if (changes.cardinalityIndexInput) {
            this.currentCardinalityIndex = changes.cardinalityIndexInput.currentValue;
        }
    }

    /**
     * Call when the content type global serach changed
     * @param any filter
     * @memberof CategoriesPropertyComponent
     */
    handleFilterChange(filter): void {
        this.getContentTypeList(filter);
    }

    /**
     * Call when the current page changed
     * @param any event
     * @memberof CategoriesPropertyComponent
     */
    handlePageChange(event): void {
        this.getContentTypeList(event.filter, event.first);
    }

    /**
     * Trigger a change event
     */
    triggerChanged(): void {
        this.change.next({
            velocityVar: this.contentType ? this.contentType.variable : undefined,
            cardinality: this.currentCardinalityIndex
        });
    }

    cardinalityChanged(cardinalityIndex: number): void {
        this.currentCardinalityIndex = cardinalityIndex;
        this.triggerChanged();
    }

    private loadContentType(velocityVar: string) {
        if (velocityVar) {
            this.contentTypeService.getContentType(velocityVar).subscribe((contentType) => {
                this.contentType = contentType;
            });
        } else {
            this.contentType = undefined;
        }
    }

    private getContentTypeList(filter = '', offset = 0): void {
        this.paginatorService.filter = filter;
        this.contentTypeCurrentPage = this.paginatorService.getWithOffset(offset);
    }
}
