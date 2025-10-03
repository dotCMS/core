import { Observable } from 'rxjs';

import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    SimpleChanges,
    inject,
    signal
} from '@angular/core';

import { DotContentTypeService, PaginatorService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { DotRelationshipsPropertyValue } from '../model/dot-relationships-property-value.model';

@Component({
    providers: [PaginatorService],
    selector: 'dot-new-relationships',
    templateUrl: './dot-new-relationships.component.html',
    styleUrls: ['./dot-new-relationships.component.scss'],
    standalone: false
})
export class DotNewRelationshipsComponent implements OnInit, OnChanges {
    paginatorService = inject(PaginatorService);
    private contentTypeService = inject(DotContentTypeService);

    @Input() cardinality: number;

    @Input() velocityVar: string;

    @Input() editing: boolean;

    @Output() switch: EventEmitter<DotRelationshipsPropertyValue> = new EventEmitter();

    contentTypeCurrentPage: Observable<DotCMSContentType[]>;

    contentType: DotCMSContentType;
    currentCardinalityIndex: number;

    protected readonly lastSearch = signal({
        filter: null,
        offset: null
    });

    ngOnInit() {
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
        this.switch.next({
            velocityVar:
                this.velocityVar || (this.contentType ? this.contentType.variable : undefined),
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
        const shouldSkip = this.shouldSkipSearch(filter, offset);

        if (shouldSkip) {
            return;
        }

        this.paginatorService.filter = filter;
        // Temporary fix for a customer; we can remove it after this is fixed: #33435
        this.paginatorService.links = {};
        this.lastSearch.set({ filter, offset });
        this.contentTypeCurrentPage = this.paginatorService.getWithOffset(offset);
    }

    private loadContentType(velocityVar: string) {
        if (velocityVar) {
            if (velocityVar.includes('.')) {
                velocityVar = velocityVar.split('.')[0];
            }

            this.contentTypeService.getContentType(velocityVar).subscribe((contentType) => {
                this.contentType = contentType;
            });
        } else {
            this.contentType = null;
        }
    }

    private shouldSkipSearch(filter: string, offset: number): boolean {
        return (
            this.editing ||
            (filter === this.lastSearch().filter && offset === this.lastSearch().offset)
        );
    }
}
