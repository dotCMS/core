import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild
} from '@angular/core';
import { ESContent } from '@dotcms/app/shared/models/dot-es-content/dot-es-content.model';
import { DotContentletEditorService } from '@dotcms/app/view/components/dot-contentlet-editor/services/dot-contentlet-editor.service';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotESContentService } from '@services/dot-es-content/dot-es-content.service';
import { PaginatorService } from '@services/paginator';
import { LazyLoadEvent } from 'primeng/api';
import { take } from 'rxjs/operators';
import { DotPaletteInputFilterComponent } from '../dot-palette-input-filter/dot-palette-input-filter.component';

@Component({
    selector: 'dot-palette-contentlets',
    templateUrl: './dot-palette-contentlets.component.html',
    styleUrls: ['./dot-palette-contentlets.component.scss']
})
export class DotPaletteContentletsComponent implements OnChanges {
    @Input() contentTypeVariable: string;
    @Input() languageId: string;
    @Output() back = new EventEmitter();

    items: DotCMSContentlet[] | DotCMSContentType[] = [];
    isFormContentType: boolean;
    hideNoResults = true;
    filter: string;
    itemsPerPage = 25;
    totalRecords = 0;

    @ViewChild('inputFilter') inputFilter: DotPaletteInputFilterComponent;

    constructor(
        public paginatorESService: DotESContentService,
        public paginationService: PaginatorService,
        private dotContentletEditorService: DotContentletEditorService
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        if (changes?.contentTypeVariable?.currentValue) {
            this.isFormContentType = changes?.contentTypeVariable?.currentValue === 'forms';

            if (this.isFormContentType) {
                this.paginationService.url = `v1/contenttype`;
                this.paginationService.paginationPerPage = this.itemsPerPage;
                this.paginationService.sortField = 'modDate';
                this.paginationService.setExtraParams('type', 'Form');
                this.paginationService.sortOrder = 1;
            }

            this.loadData();
        }
    }

    /**
     * Loads data through pagination service
     *
     * @param LazyLoadEvent [event]
     * @memberof DotPaletteContentletsComponent
     */
    loadData(event?: LazyLoadEvent): void {
        if (this.isFormContentType) {
            this.paginationService.setExtraParams('filter', this.filter);
            this.paginationService
                .getWithOffset((event && event.first) || 0)
                .pipe(take(1))
                .subscribe((data: DotCMSContentType[] | DotCMSContentlet[]) => {
                    data.forEach((item) => (item.contentType = item.variable = 'FORM'));
                    this.items = data;
                    this.totalRecords = this.paginationService.totalRecords;
                    this.hideNoResults = !!data?.length;
                });
        } else {
            this.paginatorESService
                .get({
                    itemsPerPage: this.itemsPerPage,
                    lang: this.languageId || '1',
                    filter: this.filter || '',
                    offset: (event && event.first.toString()) || '0',
                    query: `+contentType: ${this.contentTypeVariable}`
                })
                .pipe(take(1))
                .subscribe((response: ESContent) => {
                    this.totalRecords = response.resultsSize;
                    this.items = response.jsonObjectView.contentlets;
                    this.hideNoResults = !!response.jsonObjectView.contentlets?.length;
                });
        }
    }

    /**
     * Loads data with a specific page
     *
     * @param LazyLoadEvent event
     * @memberof DotPaletteContentletsComponent
     */
    paginate(event: LazyLoadEvent): void {
        this.loadData(event);
    }

    /**
     * Clear component and emit back
     *
     * @memberof DotPaletteContentletsComponent
     */
    backHandler(): void {
        this.filter = '';
        this.back.emit();
        this.items = null;
    }

    /**
     * Set the contentlet being dragged from the Content palette to dotContentletEditorService
     *
     * @param DotCMSContentType contentType
     * @memberof DotPaletteContentletsComponent
     */
    dragStart(contentType: DotCMSContentlet): void {
        this.dotContentletEditorService.setDraggedContentType(contentType);
    }

    /**
     * Does the string formatting in order to do a filtering of the Contentlets,
     * finally call the loadData() to request the data
     *
     * @param string value
     * @memberof DotPaletteContentletsComponent
     */
    filterContentlets(value: string): void {
        value = value.trim();
        this.filter = value;

        if (this.isFormContentType) {
            this.paginationService.searchParam = 'variable';
            this.paginationService.filter = value;
        }

        this.loadData({ first: 0 });
    }

    /**
     * Focus the input filter
     *
     * @memberof DotPaletteContentletsComponent
     */
    focusInputFilter(): void {
        this.inputFilter.focus();
    }
}
