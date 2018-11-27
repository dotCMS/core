import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { PaginatorService } from '@services/paginator';
import { ContentType } from '@portlets/content-types/shared/content-type.model';
import { Observable } from 'rxjs';
import { DotContentTypeService } from '@services/dot-content-type/dot-content-type.service';
import { DotRelationshipsPropertyValue } from '../dot-relationships-property.component';
import { take } from 'rxjs/operators';

@Component({
    providers: [PaginatorService],
    selector: 'dot-new-relationships',
    templateUrl: './dot-new-relationships.component.html'
})
export class DotNewRelationshipsComponent implements OnInit, OnChanges {
    @Input()
    cardinality: number;

    @Input()
    velocityVar: string;

    @Input()
    editing: boolean;

    @Output()
    change: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

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
            .pipe(take(1))
            .subscribe((res) => {
                this.i18nMessages = res;
            });

        this.paginatorService.url = 'v1/contenttype';
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.velocityVar) {
            this.loadContentType(changes.velocityVar.currentValue);
        }

        if (changes.cardinality) {
            this.currentCardinalityIndex = changes.cardinality.currentValue;
        }
    }

    /**
     * Trigger a change event, it send a object with the current content type's variable and
     * the current candinality's index.
     *
     * @memberof DotNewRelationshipsComponent
     */
    triggerChanged(): void {
        this.change.next({
            velocityVar: this.contentType ? this.contentType.variable : undefined,
            cardinality: this.currentCardinalityIndex
        });
    }

    /**
     *Call when the selected cardinality changed
     *
     * @param {number} cardinalityIndex selected cardinality index
     * @memberof DotNewRelationshipsComponent
     */
    cardinalityChanged(cardinalityIndex: number): void {
        this.currentCardinalityIndex = cardinalityIndex;
        this.triggerChanged();
    }

    /**
     *Load content types by pagination
     *
     * @param {string} [filter=''] content types's filter
     * @param {number} [offset=0] pagination index
     * @memberof DotNewRelationshipsComponent
     */
    getContentTypeList(filter = '', offset = 0): void {
        this.paginatorService.filter = filter;
        this.contentTypeCurrentPage = this.paginatorService.getWithOffset(offset);
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
}
